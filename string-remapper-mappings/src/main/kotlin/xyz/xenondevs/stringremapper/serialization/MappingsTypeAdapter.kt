package xyz.xenondevs.stringremapper.serialization

import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import com.google.gson.stream.JsonWriter
import xyz.xenondevs.stringremapper.Mappings

object MappingsTypeAdapter : TypeAdapter<Mappings?>() {
    
    override fun write(writer: JsonWriter, mappings: Mappings?) {
        if (mappings == null) {
            writer.nullValue()
            return
        }
        
        fun writeMap(map: Map<String, String>) {
            writer.beginObject()
            for ((key, value) in map) {
                writer.name(key)
                writer.value(value)
            }
            writer.endObject()
        }
        
        writer.beginArray()
        writeMap(mappings.classMappings)
        writeMap(mappings.methodMappings)
        writeMap(mappings.fieldMappings)
        writer.endArray()
    }
    
    override fun read(reader: JsonReader): Mappings? {
        if (reader.peek() == JsonToken.NULL) {
            reader.nextNull()
            return null
        }
        
        val classMappings = HashMap<String, String>(10_000) // rounded up 5460 
        val methodMappings = HashMap<String, String>(50_000) // rounded up from 41350
        val fieldMappings = HashMap<String, String>(30_000) // rounded up from 24089
        
        fun readMap(map: MutableMap<String, String>) {
            reader.beginObject()
            while (reader.peek() != JsonToken.END_OBJECT) {
                map[reader.nextName()] = reader.nextString()
            }
            reader.endObject()
        }
        
        reader.beginArray()
        readMap(classMappings)
        readMap(methodMappings)
        readMap(fieldMappings)
        reader.endArray()
        
        return Mappings(classMappings, methodMappings, fieldMappings)
    }
    
}