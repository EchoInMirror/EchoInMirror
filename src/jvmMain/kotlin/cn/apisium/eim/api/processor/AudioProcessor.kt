package cn.apisium.eim.api.processor

import cn.apisium.eim.api.CurrentPosition
import cn.apisium.eim.utils.randomId
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.ser.std.StdSerializer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

interface AudioProcessorDescription {
    val name: String
    val category: String?
    val manufacturerName: String?
    val version: String?
    val isInstrument: Boolean?
    val identifier: String?
}

interface AudioProcessor: AutoCloseable {
    @get:JsonProperty
    var name: String
    val description: AudioProcessorDescription
    val inputChannelsCount: Int
    val outputChannelsCount: Int
    @get:JsonProperty(access = JsonProperty.Access.READ_ONLY)
    val factory: AudioProcessorFactory<*>
    @get:JsonProperty
    val id: String
    suspend fun processBlock(buffers: Array<FloatArray>, position: CurrentPosition, midiBuffer: ArrayList<Int>) { }
    fun prepareToPlay(sampleRate: Int, bufferSize: Int) { }
    fun onSuddenChange() { }
    fun onClick() { }
    suspend fun save(path: String)
    suspend fun load(path: String, json: JsonNode)
    override fun close() { }
}

abstract class AbstractAudioProcessor(
    description: AudioProcessorDescription,
    override val factory: AudioProcessorFactory<*>
): AudioProcessor {
    override val inputChannelsCount = 2
    override val outputChannelsCount = 2
    override var id = randomId()
        protected set
    @Suppress("CanBePrimaryConstructorProperty")
    override val description = description
    override var name = description.name

    override suspend fun save(path: String) {
        withContext(Dispatchers.IO) {
            ObjectMapper().writerWithDefaultPrettyPrinter().writeValue(File("$path.json"), mapOf(
                "factory" to factory.name,
                "name" to name,
                "id" to id,
                "identifier" to description.name
            ))
        }
    }

    override suspend fun load(path: String, json: JsonNode) {
        name = json["name"]?.asText() ?: ""
        json["id"]?.asText()?.let { if (it.isNotEmpty()) id = it }
    }
}

@JsonInclude(JsonInclude.Include.NON_NULL)
open class DefaultAudioProcessorDescription(
    override val name: String,
    override val category: String? = null,
    override val manufacturerName: String? = null,
    override val version: String? = null,
    override val isInstrument: Boolean? = null,
    override val identifier: String? = null,
): AudioProcessorDescription

class AudioProcessorIDSerializer @JvmOverloads constructor(t: Class<AudioProcessor>? = null) : StdSerializer<AudioProcessor>(t) {
    override fun serialize(value: AudioProcessor, jgen: JsonGenerator, provider: SerializerProvider?) {
        jgen.writeString(value.id)
    }
}
class AudioProcessorCollectionIDSerializer @JvmOverloads constructor(t: Class<Collection<AudioProcessor>>? = null) :
    StdSerializer<Collection<AudioProcessor>>(t) {
    override fun serialize(value: Collection<AudioProcessor>, jgen: JsonGenerator, provider: SerializerProvider?) {
        jgen.writeStartArray()
        value.forEach { jgen.writeString(it.id) }
        jgen.writeEndArray()
    }
}
