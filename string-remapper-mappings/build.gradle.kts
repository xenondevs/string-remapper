plugins {
    alias(libs.plugins.kotlin)
    `maven-publish`
}

dependencies {
    api("com.google.code.gson:gson:2.10.1")
    api("xyz.xenondevs.commons:commons-gson:1.0-SNAPSHOT")
}

publishing {
    repositories {
        maven {
            name = "xenondevs"
            url = uri("https://repo.xenondevs.xyz/releases/")
            credentials(PasswordCredentials::class)
        }
    }
    
    publications {
        create<MavenPublication>("mavenKotlin") {
            from(components["kotlin"])
            artifact(tasks.getByName("sources"))
        }
    }
}