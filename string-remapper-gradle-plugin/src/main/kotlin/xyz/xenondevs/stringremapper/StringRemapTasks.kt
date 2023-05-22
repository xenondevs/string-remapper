package xyz.xenondevs.stringremapper

import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction
import org.gradle.kotlin.dsl.withType
import java.io.File

abstract class StringRemapTask : DefaultTask() {
    
    @get:Input
    abstract val inputClasses: ListProperty<File>
    
    @get:Input
    abstract val outputClasses: ListProperty<File>
    
    @get:InputFile
    abstract val spigotMappings: RegularFileProperty
    
    @get:InputFile
    abstract val mojangMappings: RegularFileProperty
    
    @get:Input
    abstract val goal: Property<RemapGoal>
    
    @Internal
    lateinit var remapper: ProjectRemapper
    
    @TaskAction
    fun remap() {
        remapper = ProjectRemapper(
            project.logger, 
            project.buildDir,
            inputClasses.get(), 
            outputClasses.get(),
            mojangMappings.asFile.get(), spigotMappings.asFile.get(),
            goal.get()
        )
        remapper.remap()
    }
    
}

abstract class StringRemapRevertTask : DefaultTask() {
    
    @TaskAction
    fun cleanup() {
        val remapTask = project.tasks.withType<StringRemapTask>().named("remapStrings").get()
        remapTask.remapper.cleanup()
    }
    
}