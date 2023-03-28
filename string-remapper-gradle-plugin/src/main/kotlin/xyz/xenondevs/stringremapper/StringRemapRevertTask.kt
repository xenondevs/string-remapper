package xyz.xenondevs.stringremapper

import org.gradle.api.DefaultTask
import org.gradle.api.logging.LogLevel
import org.gradle.api.provider.ListProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction
import org.gradle.kotlin.dsl.withType
import java.io.File

abstract class StringRemapRevertTask : DefaultTask() {
    
    @get:Input
    abstract val inputClasses: ListProperty<File>
    
    @TaskAction
    fun cleanup() {
        val remapTask = project.tasks.withType<StringRemapTask>().named("remapStrings").get()
        for ((file, bin) in remapTask.remappedClasses) {
            project.logger.log(LogLevel.DEBUG, "Reverting remapped strings in $file")
            file.writeBytes(bin)
        }
    }
    
}