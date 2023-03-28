package xyz.xenondevs.stringremapper.util

import com.google.gson.GsonBuilder

val GSON = GsonBuilder()
    .setPrettyPrinting()
    .create()