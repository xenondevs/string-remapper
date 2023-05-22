plugins {
    alias(libs.plugins.kotlin)
    `java-gradle-plugin`
    `kotlin-dsl`
    `maven-publish`
}

dependencies {
    implementation(project(":string-remapper-plugin"))
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