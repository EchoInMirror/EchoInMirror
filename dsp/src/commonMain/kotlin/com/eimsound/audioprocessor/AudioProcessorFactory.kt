package com.eimsound.audioprocessor

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.fasterxml.jackson.databind.ser.std.StdSerializer

@JsonSerialize(using = AudioProcessorFactoryNameSerializer::class)
interface AudioProcessorFactory<T: AudioProcessor> {
    val name: String
    val descriptions: Set<AudioProcessorDescription>
    suspend fun createAudioProcessor(description: AudioProcessorDescription): T
    suspend fun createAudioProcessor(path: String, json: JsonNode): T
}

class AudioProcessorFactoryNameSerializer @JvmOverloads constructor(t: Class<AudioProcessorFactory<*>>? = null) :
    StdSerializer<AudioProcessorFactory<*>>(t) {
    override fun serialize(value: AudioProcessorFactory<*>, jgen: JsonGenerator, provider: SerializerProvider?) {
        jgen.writeString(value.name)
    }
}
