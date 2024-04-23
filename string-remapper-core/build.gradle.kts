plugins {
    alias(libs.plugins.kotlin)
    `maven-publish`
}

dependencies {
    api(project(":string-remapper-mappings"))
    implementation("xyz.xenondevs.bytebase:ByteBase:0.4.7")
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