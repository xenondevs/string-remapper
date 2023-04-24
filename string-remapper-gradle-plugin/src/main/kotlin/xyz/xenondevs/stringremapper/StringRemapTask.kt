package xyz.xenondevs.stringremapper

import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.logging.LogLevel
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction
import java.io.File

abstract class StringRemapTask : DefaultTask() {
    
    @get:Input
    abstract val inputClasses: ListProperty<File>
    
    @get:InputFile
    abstract val spigotMappings: RegularFileProperty
    
    @get:InputFile
    abstract val mojangMappings: RegularFileProperty
    
    @get:Input
    abstract val goal: Property<RemapGoal>
    
    @Internal
    val remappedClasses = HashMap<File, ByteArray>()
    
    @TaskAction
    fun remap() {
        remapClasses(getRemapper())
    }
    
    private fun getRemapper(): FileRemapper {
        project.logger.log(LogLevel.DEBUG, "Loading mappings...")
        val mappingsFile = project.buildDir.resolve("mappings.json")
        val mappings: Mappings
        if (!mappingsFile.exists()) {
            mappings = Mappings.load(mojangMappings.asFile.get(), spigotMappings.asFile.get())
            mappings.writeToJson(mappingsFile)
        } else {
            mappings = Mappings.loadFromJson(mappingsFile)
        }
        
        return FileRemapper(mappings, goal.get())
    }
    
    private fun remapClasses(remapper: FileRemapper) {
        project.logger.log(LogLevel.DEBUG, "Remapping strings...")
        inputClasses.get().filter { it.exists() && it.isDirectory }.forEach { dir ->
            dir.walkTopDown()
                .filter { it.isFile && it.extension == "class" }
                .forEach { file ->
                    val relPath = file.relativeTo(dir).path
                    try {
                        val bin = file.readBytes()
                        val newBin = remapper.remap(bin.inputStream())
                        if (newBin != null) {
                            remappedClasses[file] = bin
                            file.writeBytes(newBin)
                            project.logger.log(LogLevel.DEBUG, "Remapped strings in $relPath")
                        }
                    } catch (t: Throwable) {
                        project.logger.log(LogLevel.ERROR, "An exception occurred while remapping $relPath", t)
                    }
                }
        }
    }
    
}