plugins {
    @Suppress("DSL_SCOPE_VIOLATION")
    kotlin("jvm") version libs.versions.kotlin
    `maven-publish`
}

dependencies {
    implementation("xyz.xenondevs.bytebase:ByteBase:0.4.4")
    implementation("com.google.code.gson:gson:2.10.1")
    implementation("xyz.xenondevs.commons:commons-gson:1.0-SNAPSHOT")
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