package xyz.xenondevs.stringremapper

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.Dependency
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.get
import org.gradle.kotlin.dsl.register
import java.io.File

class StringRemapperGradlePlugin : Plugin<Project> {
    
    override fun apply(project: Project) {
        val classesTask = project.tasks["classes"]
        val jarTask = project.tasks["jar"]
        
        // extension
        val extension = project.extensions.create<StringRemapExtension>("remapStrings")
        
        // revertRemapStrings task
        val cleanupTask = project.tasks.register<StringRemapRevertTask>("revertRemapStrings") { mustRunAfter(jarTask) }
        
        // remapStrings task
        val remapTask = project.tasks.register<StringRemapTask>("remapStrings") { dependsOn(classesTask); finalizedBy(cleanupTask) }
        remapTask.configure {
            this.gameVersion.set(extension.gameVersion)
            goal.set(extension.remapGoal.map { RemapGoal.valueOf(it.uppercase()) })
            
            if (extension.inputClasses.isPresent && extension.inputClasses.get().isNotEmpty()) {
                inputClasses.set(extension.inputClasses.get().map(::File))
            } else {
                project.logger.debug("No input classes specified, defaulting to 'classes/main/kotlin' and 'classes/main/java'")
                inputClasses.set(listOfNotNull(
                    project.buildDir.resolve("classes/kotlin/main").takeIf(File::exists),
                    project.buildDir.resolve("classes/java/main").takeIf(File::exists)
                ))
            }
            
            if (extension.outputClasses.isPresent && extension.outputClasses.get().isNotEmpty()) {
                outputClasses.set(extension.outputClasses.get().map(::File))
            } else {
                outputClasses.set(emptyList())
            }
        }
        classesTask.finalizedBy(remapTask)
    }
    
    private fun Dependency.getFile(project: Project) =
        project.configurations.detachedConfiguration(this).singleFile
    
}