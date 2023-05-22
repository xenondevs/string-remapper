rootProject.name = "string-remapper"

include("string-remapper-core")
include("string-remapper-plugin")
include("string-remapper-gradle-plugin")

dependencyResolutionManagement { 
    versionCatalogs { 
        create("libs") {
            version("kotlin", "1.8.20")
        
            plugin("kotlin", "org.jetbrains.kotlin.jvm").versionRef("kotlin")
            
            library("kotlin-stdlib", "org.jetbrains.kotlin", "kotlin-stdlib").versionRef("kotlin")
            library("kotlin-reflect", "org.jetbrains.kotlin", "kotlin-reflect").versionRef("kotlin")
            
            bundle("kotlin", listOf("kotlin-stdlib", "kotlin-reflect"))
        }
    }
}