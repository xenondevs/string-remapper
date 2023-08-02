package xyz.xenondevs.stringremapper

import org.slf4j.Logger
import java.io.File

class ProjectRemapper(
    private val logger: Logger,
    private val buildDir: File,
    private val classesIn: List<File>,
    private val classesOut: List<File>,
    private val version: String,
    private val goal: RemapGoal
) {
    
    private val remappedClasses = HashMap<File, ByteArray>()
    
    init {
        require(classesIn.size == classesOut.size || classesOut.isEmpty()) { "If custom output directories are defined, the list must have the same size as input directories list" }
        for(inDir in classesIn) {
            require(inDir.exists() && inDir.isDirectory) { "Input directory $inDir does not exist or is not a directory" }
        }
    }
    
    fun remap() {
        logger.debug("Loading mappings...")
        val remapper = getRemapper()
        
        logger.debug("Remapping strings...")
        remapClasses(remapper)
    }
    
    private fun getRemapper(): FileRemapper {
        if (!buildDir.exists())
            buildDir.mkdirs()
        val mojangMappings = File(buildDir, "maps-mojang.txt")
        val spigotMappings = File(buildDir, "maps-spigot.csrg")
        val mappingsCache = File(buildDir, "mappings.json")
        val mappings = Mappings.loadOrDownload(version, mojangMappings, spigotMappings, mappingsCache)
        return FileRemapper(mappings, goal)
    }
    
    private fun remapClasses(remapper: FileRemapper) {
        classesIn.forEachIndexed { idx, dir ->
            val outDir = if (classesOut.isEmpty()) dir else classesOut[idx]
            
            dir.walkTopDown()
                .filter { it.isFile && it.extension == "class" }
                .forEach { file ->
                    val relPath = file.relativeTo(dir).path
                    val toFile = File(outDir, relPath)
                    try {
                        val bin = file.readBytes()
                        val newBin = remapper.remap(bin.inputStream())
                        if (newBin != null) {
                            remappedClasses[toFile] = bin
                            toFile.writeBytes(newBin)
                            logger.debug("Remapped strings in {}", relPath)
                        } else if (file != toFile) {
                            file.copyTo(toFile, true)
                        }
                    } catch (t: Throwable) {
                        logger.error("An exception occurred while remapping $relPath", t)
                    }
                }
        }
    }
    
    fun cleanup() {
        if (classesOut.isNotEmpty()) {
            logger.debug("Skipping cleanup because classes were written to a custom output directory")
            return
        }
        
        for ((file, bin) in remappedClasses) {
            logger.debug("Reverting remapped strings in {}", file)
            file.writeBytes(bin)
        }
    }
    
}