subprojects { 
    group = "xyz.xenondevs.string-remapper"
    version = "1.6"
    
    repositories { 
        mavenCentral()
        maven("https://repo.xenondevs.xyz/releases/")
    }
    
    tasks {
        register<Jar>("sources") {
            dependsOn(JavaPlugin.CLASSES_TASK_NAME)
            from("src/main/kotlin")
            archiveClassifier.set("sources")
        }
    }
}