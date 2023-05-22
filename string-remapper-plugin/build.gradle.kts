plugins {
    alias(libs.plugins.kotlin)
    `maven-publish`
}

dependencies {
    api(project(":string-remapper-core"))
    api("org.slf4j:slf4j-api:2.0.7")
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