@file:Suppress("DEPRECATION", "UNCHECKED_CAST")

package xyz.xenondevs.stringremapper

import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Type
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.LdcInsnNode
import xyz.xenondevs.bytebase.util.internalName
import xyz.xenondevs.bytebase.util.previous
import xyz.xenondevs.stringremapper.ReflectType.FUNCTION
import xyz.xenondevs.stringremapper.ReflectType.PROPERTY
import xyz.xenondevs.stringremapper.visitor.ClassRemapVisitor
import java.io.File
import java.io.InputStream
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.jvm.internal.FunctionReferenceImpl
import kotlin.jvm.internal.MutablePropertyReference0Impl
import kotlin.jvm.internal.MutablePropertyReference1Impl
import kotlin.jvm.internal.MutablePropertyReference2Impl
import kotlin.jvm.internal.PropertyReference0Impl
import kotlin.jvm.internal.PropertyReference1Impl
import kotlin.jvm.internal.PropertyReference2Impl

enum class ReflectType {
    FUNCTION,
    PROPERTY
}

private val REFLECT_CLASSES = mapOf(
    FunctionReferenceImpl::class to FUNCTION,
    PropertyReference0Impl::class to PROPERTY,
    PropertyReference1Impl::class to PROPERTY,
    PropertyReference2Impl::class to PROPERTY,
    MutablePropertyReference0Impl::class to PROPERTY,
    MutablePropertyReference1Impl::class to PROPERTY,
    MutablePropertyReference2Impl::class to PROPERTY
).mapKeys { it.key.internalName }

private val TYPE_DESC_PATTERN = Regex("""^\[*L\S+;$""")

class FileRemapper(
    private val mappings: Mappings,
    private val goal: RemapGoal
) {
    
    /**
     * Remaps a class read from the given [File] and returns whether the class was changed.
     */
    fun remap(file: File): Boolean {
        val bin = remap(file.inputStream())
        if (bin != null) {
            file.writeBytes(bin)
            return true
        }
        
        return false
    }
    
    /**
     * Remaps a class read from the given [InputStream] and returns the remapped class
     * as a [ByteArray] or null if the class was not changed.
     */
    fun remap(inp: InputStream): ByteArray? {
        val hasChanged = AtomicBoolean()
        val reader = ClassReader(inp)
        val clazz = ClassNode().also { reader.accept(it, 0) }
        val writer = ClassWriter(0)
        
        if (goal == RemapGoal.SPIGOT) {
            // remap kotlin reflect classes
            if (clazz.outerClass != null && clazz.superName in REFLECT_CLASSES) {
                remapReflectClass(clazz, REFLECT_CLASSES[clazz.superName]!!, hasChanged)
            }
            
            // remap kotlin metadata annotation
            val metadataAnnotation = clazz.visibleAnnotations?.find { it.desc == "Lkotlin/Metadata;" }
            if (metadataAnnotation != null) {
                for (i in 0 until metadataAnnotation.values.size step 2) {
                    val key = metadataAnnotation.values[i] as String
                    if (key == "d2") {
                        val value = metadataAnnotation.values[i + 1] as ArrayList<String>
                        value.replaceAll { str ->
                            if (str.matches(TYPE_DESC_PATTERN)) {
                                val type = Type.getType(str)
                                val newType = remapType(type)
                                if (newType != str) {
                                    hasChanged.set(true)
                                    return@replaceAll newType
                                }
                            }
                            
                            return@replaceAll str
                        }
                    }
                }
            }
        }
        
        // remap SRC()/SRF()/SRM() format
        val visitor = ClassRemapVisitor(writer, mappings, goal, hasChanged)
        clazz.accept(visitor)
        
        if (hasChanged.get()) {
            return writer.toByteArray()
        }
        
        return null
    }
    
    private fun remapReflectClass(clazz: ClassNode, reflectType: ReflectType, hasChanged: AtomicBoolean) {
        val insns = clazz.methods.find { it.name == "<init>" }?.instructions ?: return
        val superCallNode = insns.firstOrNull { it.opcode == Opcodes.INVOKESPECIAL } ?: return
        val signatureNode = superCallNode.previous(2) as? LdcInsnNode ?: return
        val nameNode = superCallNode.previous(3) as? LdcInsnNode ?: return
        val ownerClassName = ((superCallNode.previous(4) as? LdcInsnNode)?.cst as? Type)?.internalName ?: return
        
        val name = nameNode.cst.toString()
        if (reflectType == PROPERTY) {
            val desc = signatureNode.cst.toString().substringAfter(')')
            
            val newName = mappings.fieldMappings["$ownerClassName.$name.$desc"] ?: name
            val newDesc = remapType(Type.getType(desc))
            
            if (newName != name || newDesc != desc) {
                nameNode.cst = newName
                signatureNode.cst = "get${newName.capitalize()}()$newDesc"
                hasChanged.set(true)
                return
            }
        } else {
            val desc = "(" + signatureNode.cst.toString().substringAfter('(')
            
            val newName = mappings.methodMappings["$ownerClassName.$name$desc"] ?: name
            val type = Type.getType(desc)
            val newDesc = "(" + type.argumentTypes.joinToString("") { remapType(it) } + ")" + remapType(type.returnType)
            
            if (newName != name || newDesc != desc) {
                nameNode.cst = newName
                signatureNode.cst = newName + newDesc
                hasChanged.set(true)
                return
            }
        }
        
        return
    }
    
    private fun remapType(type: Type): String {
        return when (type.sort) {
            Type.OBJECT -> {
                val typeClass = type.internalName
                mappings.classMappings[typeClass]?.let { "L$it;" } ?: type.descriptor
            }
            
            Type.ARRAY -> {
                val typeClass = type.elementType.internalName
                mappings.classMappings[typeClass]?.let { "[".repeat(type.dimensions) + "L$it;" } ?: type.descriptor
            }
            
            else -> type.descriptor
        }
    }
    
}