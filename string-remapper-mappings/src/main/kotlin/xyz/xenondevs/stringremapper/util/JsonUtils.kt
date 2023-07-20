package xyz.xenondevs.stringremapper.util

import com.google.gson.GsonBuilder
import xyz.xenondevs.commons.gson.registerTypeAdapter
import xyz.xenondevs.stringremapper.serialization.MappingsTypeAdapter

internal val GSON = GsonBuilder()
    .setPrettyPrinting()
    .registerTypeAdapter(MappingsTypeAdapter)
    .create()