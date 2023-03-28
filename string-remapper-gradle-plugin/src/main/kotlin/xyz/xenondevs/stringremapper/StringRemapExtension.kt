package xyz.xenondevs.stringremapper

import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property

abstract class StringRemapExtension {

    abstract val spigotVersion: Property<String>
    
    abstract val inputClasses: ListProperty<String>
    
    abstract val classes: ListProperty<String>
    
    abstract val remapGoal: Property<String>
    
}