package xyz.xenondevs.stringremapper

import com.google.gson.JsonObject
import xyz.xenondevs.commons.gson.fromJson
import xyz.xenondevs.commons.gson.parseJson
import xyz.xenondevs.stringremapper.util.GSON
import xyz.xenondevs.stringremapper.util.TypeUtils
import java.io.BufferedReader
import java.io.File
import java.io.Serializable
import java.nio.file.Path
import kotlin.io.path.bufferedReader

private val MOJANG_DEFINE_CLASS_PATTERN = Regex("""^(\S*) -> (\S*):$""")
private val MOJANG_DEFINE_FIELD_PATTERN = Regex("""^ {4}(\S*) ([^\s()]*) -> (\S*)$""")
private val MOJANG_DEFINE_METHOD_PATTERN = Regex("""^ {4}[\d:]*(\S*) (\S*)\((\S*)\) -> (\S*)$""")
private val SPIGOT_DEFINE_CLASS_PATTERN = Regex("""^(\S*) (\S*)$""")
private val REMAP_INSTRUCTION_BEGIN_PATTERN = Regex("""(A)?SR([CMF])(/)?\(""")

internal sealed interface MappingsParser {
    
    val classMappings: Map<String, String>
    val fieldMappings: Map<String, String>
    val methodMappings: Map<String, String>
    
    fun parse(file: File) =
        parse(file.bufferedReader())
    
    fun parse(reader: BufferedReader) =
        reader.use { it.forEachLine(::parseLine) }
    
    fun parseLine(line: String)
    
}

internal class MojangMappingsParser : MappingsParser {
    
    private var currentClass: String? = null
    
    private val _classMappings = HashMap<String, String>()
    override val classMappings: Map<String, String> get() = _classMappings
    private val _fieldMappings = HashMap<String, String>()
    override val fieldMappings: Map<String, String> get() = _fieldMappings
    private val _methodMappings = HashMap<String, String>()
    override val methodMappings: Map<String, String> get() = _methodMappings
    
    override fun parseLine(line: String) {
        // ignore comments and blank lines
        if (line.startsWith('#') || line.isBlank())
            return
        // ignore constructor and static initializer
        if ("<init>" in line || "<clinit>" in line)
            return
        
        // check for new class definition
        val defineClassResult = MOJANG_DEFINE_CLASS_PATTERN.find(line)
        if (defineClassResult != null) {
            val (className, obfClassName) = defineClassResult.destructured
            currentClass = className
            storeClassMappings(className, obfClassName)
            return
        }
        
        // retrieve current class name and obfuscated name
        val className = currentClass
            ?: throw IllegalStateException("No class defined for line: $line")
        
        // check for field definition
        val defineFieldResult = MOJANG_DEFINE_FIELD_PATTERN.find(line)
        if (defineFieldResult != null) {
            val (fieldType, fieldName, obfFieldName) = defineFieldResult.destructured
            storeFieldMappings(className, fieldType, fieldName, obfFieldName)
            return
        }
        
        // check for method definition
        val defineMethodResult = MOJANG_DEFINE_METHOD_PATTERN.find(line)
        if (defineMethodResult != null) {
            val (returnType, methodName, paramsStr, obfMethodName) = defineMethodResult.destructured
            storeMethodMappings(className, returnType, methodName, paramsStr, obfMethodName)
            return
        }
        
        throw IllegalStateException("Invalid line: $line")
    }
    
    private fun storeClassMappings(className: String, obfClassName: String) {
        _classMappings[className.internalName] = obfClassName.internalName
    }
    
    private fun storeFieldMappings(className: String, fieldType: String, fieldName: String, obfFieldName: String) {
        val typeDesc = TypeUtils.getMojangDescriptor(fieldType)
        _fieldMappings["${className.internalName}.$fieldName.$typeDesc"] = obfFieldName
    }
    
    private fun storeMethodMappings(className: String, returnType: String, methodName: String, paramsStr: String, obfMethodName: String) {
        val paramDesc = if (paramsStr.isNotEmpty())
            paramsStr.split(',').joinToString("") { TypeUtils.getMojangDescriptor(it) }
        else ""
        val returnDesc = TypeUtils.getMojangDescriptor(returnType)
        _methodMappings["${className.internalName}.$methodName($paramDesc)$returnDesc"] = obfMethodName
    }
    
    private val String.internalName: String get() = replace('.', '/')
    
}

internal class SpigotMappingsParser : MappingsParser {
    
    private val _classMappings = HashMap<String, String>()
    override val classMappings: Map<String, String> get() = _classMappings
    override val fieldMappings: Map<String, String> get() = emptyMap()
    override val methodMappings: Map<String, String> get() = emptyMap()
    
    override fun parseLine(line: String) {
        // ignore comments and blank lines
        if (line.startsWith('#') || line.isBlank())
            return
        
        val defineClassResult = SPIGOT_DEFINE_CLASS_PATTERN.find(line)
        if (defineClassResult != null) {
            val (obfClassName, className) = defineClassResult.destructured
            _classMappings[obfClassName] = className
            return
        }
        
        throw IllegalStateException("Invalid line: $line")
    }
    
}

class Mappings(
    val classMappings: Map<String, String>,
    val fieldMappings: Map<String, String>,
    val methodMappings: Map<String, String>
) : Serializable {
    
    private val fieldNameMappings: Map<String, String> by lazy { fieldMappings.mapKeysTo(HashMap()) { (key, _) -> key.substringBeforeLast('.') } }
    private val methodNameMappings: Map<String, String> by lazy { methodMappings.mapKeysTo(HashMap()) { (key, _) -> key.substringBeforeLast('(') } }
    
    companion object {
        
        fun load(mojangMappingsFile: File, spigotMappingsFile: File): Mappings =
            load(mojangMappingsFile.bufferedReader(), spigotMappingsFile.bufferedReader())
        
        fun load(mojangMappingsFile: Path, spigotMappingsFile: Path): Mappings =
            load(mojangMappingsFile.bufferedReader(), spigotMappingsFile.bufferedReader())
        
        fun load(mojangMappingsReader: BufferedReader, spigotMappingsReader: BufferedReader): Mappings {
            val mojangMappings = MojangMappingsParser().apply { parse(mojangMappingsReader) }
            val spigotMappings = SpigotMappingsParser().apply { parse(spigotMappingsReader) }.classMappings
            
            // create mojang->spigot class name mappings
            val classMappings = HashMap<String, String>()
            mojangMappings.classMappings.forEach { (mojang, obf) ->
                val spigotMapping = spigotMappings[obf]
                if (spigotMapping != null) {
                    // exact spigot mapping
                    classMappings[mojang] = spigotMapping
                    return@forEach
                } else {
                    // no exact spigot mapping, search for outer class mapping
                    var outerObf = obf
                    var innerObf = ""
                    while('$' in outerObf) {
                        innerObf += "$" + outerObf.substringAfterLast('$') + innerObf
                        outerObf = outerObf.substringBeforeLast('$')
                        val outerSpigot = spigotMappings[outerObf]
                        if (outerSpigot != null) {
                            classMappings[mojang] = outerSpigot + innerObf
                            return@forEach
                        }
                    }
                }
                
                // no spigot mapping
                classMappings[mojang] = obf
            }
            
            return Mappings(classMappings, mojangMappings.fieldMappings, mojangMappings.methodMappings)
        }
        
        fun loadFromJson(mappingsFile: File): Mappings {
            val obj = mappingsFile.parseJson() as JsonObject
            return loadFromJson(obj)
        }
        
        fun loadFromJson(obj: JsonObject): Mappings {
            return Mappings(
                GSON.fromJson<HashMap<String, String>>(obj.get("classMappings"))!!,
                GSON.fromJson<HashMap<String, String>>(obj.get("fieldMappings"))!!,
                GSON.fromJson<HashMap<String, String>>(obj.get("methodMappings"))!!
            )
        }
        
    }
    
    fun writeToJson(file: File) {
        val obj = JsonObject()
        obj.add("classMappings", GSON.toJsonTree(classMappings))
        obj.add("fieldMappings", GSON.toJsonTree(fieldMappings))
        obj.add("methodMappings", GSON.toJsonTree(methodMappings))
        file.writeText(GSON.toJson(obj))
    }
    
    private fun resolveClassLookup(className: String, goal: RemapGoal): String {
        if (goal == RemapGoal.SPIGOT) {
            return classMappings[className]
                ?: throw IllegalArgumentException("Could not resolve class lookup: $className")
        }
        
        return className
    }
    
    // class.field.desc
    private fun resolveFieldLookup(fieldMapping: String, goal: RemapGoal): String {
        if (goal == RemapGoal.SPIGOT) {
            return fieldMappings[fieldMapping] 
                ?: throw IllegalArgumentException("Could not resolve field lookup: $fieldMapping")
        }
        
        return fieldMapping.split('.')[1]
    }
    
    // class.methoddesc
    private fun resolveMethodLookup(methodMapping: String, goal: RemapGoal): String {
        if (goal == RemapGoal.SPIGOT) {
            return methodMappings[methodMapping]
                ?: throw IllegalArgumentException("Could not resolve method lookup: $methodMapping")
        }
        
        return methodMapping.split('.')[1].substringBefore('(')
    }
    
    private fun resolveSimpleFieldLookup(lookup: String, goal: RemapGoal): String {
        val (className, fieldName) = lookup.split(' ')
        if (goal == RemapGoal.SPIGOT) {
            return fieldNameMappings["$className.$fieldName"]
                ?: throw IllegalArgumentException("Could not resolve field lookup: $lookup")
        }
        
        return fieldName
    }
    
    private fun resolveSimpleMethodLookup(lookup: String, goal: RemapGoal): String {
        val (className, methodName) = lookup.split(' ')
        if (goal == RemapGoal.SPIGOT) {
            return methodNameMappings["$className.$methodName"]
                ?: throw IllegalArgumentException("Could not resolve method lookup: $lookup")
        }
        
        return methodName
    }
    
    fun processString(value: String, goal: RemapGoal): String {
        var result = value
    
        generateSequence {
            REMAP_INSTRUCTION_BEGIN_PATTERN.find(result)
        }.forEach { matchResult ->
            val lookupStartIdx = matchResult.range.last + 1
            var lookupEndIdx = -1
            
            var openBrackets = 1
            for (i in lookupStartIdx until result.length) {
                val char = result[i]
                if (char == '(') {
                    ++openBrackets
                } else if (char == ')') {
                    --openBrackets
                    if (openBrackets == 0) {
                        lookupEndIdx = i
                        break
                    }
                }
            }
            
            if (lookupEndIdx == -1)
                throw IllegalArgumentException("No closing bracket for remap instruction: $value")
            
            var lookup = result.substring(lookupStartIdx, lookupEndIdx)
            val advancedMode = matchResult.groups[1] != null
            val lookupType = matchResult.groupValues[2]
            val useSlashes = matchResult.groups[3] != null
            
            var resolvedLookup: String
            if (advancedMode) {
                resolvedLookup = when (lookupType) {
                    "C" -> resolveClassLookup(lookup, goal)
                    "M" -> resolveMethodLookup(lookup, goal)
                    "F" -> resolveFieldLookup(lookup, goal)
                    else -> throw UnsupportedOperationException()
                }
            } else {
                lookup = lookup.replace('.', '/')
                resolvedLookup = when (lookupType) {
                    "C" -> resolveClassLookup(lookup, goal)
                    "M" -> resolveSimpleMethodLookup(lookup, goal)
                    "F" -> resolveSimpleFieldLookup(lookup, goal)
                    else -> throw UnsupportedOperationException()
                }
            }
            
            if (!useSlashes)
                resolvedLookup = resolvedLookup.replace('/', '.')
            
            result = result.replaceRange(matchResult.range.first, lookupEndIdx + 1, resolvedLookup)
        }
        
        return result
    }
    
}