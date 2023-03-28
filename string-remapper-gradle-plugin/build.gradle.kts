plugins {
    @Suppress("DSL_SCOPE_VIOLATION")
    id("org.jetbrains.kotlin.jvm") version libs.versions.kotlin
    `java-gradle-plugin`
    `kotlin-dsl`
    `maven-publish`
}

dependencies {
    implementation(project(":string-remapper-core"))
}

gradlePlugin {
    plugins {
        create("string-remapper-gradle-plugin") {
            id = "xyz.xenondevs.string-remapper-gradle-plugin"
            description = "String remapper plugin for Gradle"
            implementationClass = "xyz.xenondevs.stringremapper.StringRemapperGradlePlugin"
        }
    }
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