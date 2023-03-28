package xyz.xenondevs.stringremapper.util

object TypeUtils {
    
    private val ARRAY_MATCH = Regex("""\[]""")
    
    fun getMojangDescriptor(type: String): String =
        when (type) {
            "void" -> "V"
            "boolean" -> "Z"
            "byte" -> "B"
            "char" -> "C"
            "short" -> "S"
            "int" -> "I"
            "long" -> "J"
            "float" -> "F"
            "double" -> "D"
            else -> {
                val arrayDims = ARRAY_MATCH.findAll(type).count()
                if (arrayDims > 0) {
                    val baseType = type.substringBefore('[')
                    "[".repeat(arrayDims) + getMojangDescriptor(baseType)
                } else {
                    "L${type.replace('.', '/')};"
                }
            }
        }
    
}