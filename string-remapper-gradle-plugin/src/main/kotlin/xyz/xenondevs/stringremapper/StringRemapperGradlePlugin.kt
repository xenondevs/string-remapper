package xyz.xenondevs.stringremapper

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.Dependency
import org.gradle.api.provider.ListProperty
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
        fun setInputClasses(inputClasses: ListProperty<File>) {
            if (extension.inputClasses.isPresent && extension.inputClasses.get().isNotEmpty()) {
                inputClasses.set(extension.inputClasses.get().map(::File))
            } else {
                println("No input classes specified, defaulting to 'classes/main/kotlin' and 'classes/main/java'")
                inputClasses.set(listOf(
                    project.buildDir.resolve("classes/kotlin/main"),
                    project.buildDir.resolve("classes/java/main")
                ))
            }
        }
        
        // remapStrings task
        val remapTask = project.tasks.register<StringRemapTask>("remapStrings") { dependsOn(classesTask) }
        remapTask.configure {
            val (mojangMappings, spigotMappings) = resolveMappings(project, extension.spigotVersion.get())
            this.mojangMappings.set(mojangMappings)
            this.spigotMappings.set(spigotMappings)
            this.goal.set(RemapGoal.valueOf(extension.remapGoal.get().uppercase()))
            this.classes.set(extension.classes)
            setInputClasses(this.inputClasses)
        }
        classesTask.finalizedBy(remapTask)
        
        // revertRemapStrings task
        val cleanupTask = project.tasks.register<StringRemapRevertTask>("revertRemapStrings") { dependsOn(jarTask) }
        cleanupTask.configure { setInputClasses(this.inputClasses) }
        jarTask.finalizedBy(cleanupTask)
    }
    
    private fun resolveMappings(project: Project, version: String): Pair<File, File> {
        val mojangMappings = project.dependencies.create("org.spigotmc:minecraft-server:$version:maps-mojang@txt").getFile(project)
        val spigotMappings = project.dependencies.create("org.spigotmc:minecraft-server:$version:maps-spigot@csrg").getFile(project)
        return Pair(mojangMappings, spigotMappings)
    }
    
    private fun Dependency.getFile(project: Project) =
        project.configurations.detachedConfiguration(this).singleFile
    
}