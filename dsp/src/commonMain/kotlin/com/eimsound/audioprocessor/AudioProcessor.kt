package com.eimsound.audioprocessor

import com.eimsound.daw.utils.IDisplayName
import com.eimsound.daw.utils.randomId
import com.fasterxml.jackson.annotation.JsonIgnore
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
import java.util.*

/**
 * @see com.eimsound.daw.impl.processor.EIMAudioProcessorDescription
 * @see com.eimsound.audioprocessor.NativeAudioPluginDescription
 */
interface AudioProcessorDescription : Comparable<AudioProcessorDescription>, IDisplayName {
    val name: String
    val category: String?
    val manufacturerName: String?
    val version: String?
    val isInstrument: Boolean
    val identifier: String
    val descriptiveName: String?
    @get:JsonIgnore
    val isDeprecated: Boolean?
    @get:JsonIgnore
    override val displayName: String
    override fun compareTo(other: AudioProcessorDescription) =
        if (other.isDeprecated != isDeprecated) {
            if (isDeprecated == true) 1 else -1
        } else {
            val n = name.compareTo(other.name)
            if (n == 0) version?.compareTo(other.version ?: "") ?: 0 else n
        }
}

interface AudioProcessorListener
interface SuddenChangeListener {
    fun onSuddenChange()
}

interface AudioProcessor: AutoCloseable, SuddenChangeListener {
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
    fun onClick() { }
    fun addListener(listener: AudioProcessorListener)
    fun removeListener(listener: AudioProcessorListener)
    suspend fun save(path: String)
    suspend fun load(path: String, json: JsonNode)
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
    private val _listeners = WeakHashMap<AudioProcessorListener, Unit>()
    protected val listeners: Set<AudioProcessorListener> get() = _listeners.keys

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

    override fun addListener(listener: AudioProcessorListener) { _listeners[listener] = Unit }
    override fun removeListener(listener: AudioProcessorListener) { _listeners.remove(listener) }

    override fun close() { }
    override fun onSuddenChange() { }
}

@JsonInclude(JsonInclude.Include.NON_NULL)
open class DefaultAudioProcessorDescription(
    override val name: String,
    override val identifier: String,
    override val category: String? = null,
    override val manufacturerName: String? = null,
    override val version: String? = null,
    override val isInstrument: Boolean = false,
    override val descriptiveName: String? = null,
    override val isDeprecated: Boolean? = false,
): AudioProcessorDescription {
    override val displayName get() = name
}

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
