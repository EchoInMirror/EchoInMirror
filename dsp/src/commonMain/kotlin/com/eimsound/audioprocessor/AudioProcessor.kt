package com.eimsound.audioprocessor

import com.eimsound.daw.utils.*
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.*
import java.util.*

/**
 * @see com.eimsound.daw.impl.processor.EIMAudioProcessorDescription
 * @see com.eimsound.audioprocessor.NativeAudioPluginDescription
 */
@Serializable
sealed interface AudioProcessorDescription : Comparable<AudioProcessorDescription>, IDisplayName {
    val name: String
    val category: String?
    val manufacturerName: String?
    val version: String?
    val isInstrument: Boolean
    val identifier: String
    val descriptiveName: String?
    @Transient
    val isDeprecated: Boolean?
    @Transient
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

interface AudioProcessor: Recoverable, AutoCloseable, SuddenChangeListener {
    var name: String
    @Transient
    val description: AudioProcessorDescription
    @Transient
    val inputChannelsCount: Int
    @Transient
    val outputChannelsCount: Int
    val factory: AudioProcessorFactory<*>
    val id: String
    suspend fun processBlock(buffers: Array<FloatArray>, position: CurrentPosition, midiBuffer: ArrayList<Int>) { }
    fun prepareToPlay(sampleRate: Int, bufferSize: Int) { }
    fun onClick() { }
    fun addListener(listener: AudioProcessorListener)
    fun removeListener(listener: AudioProcessorListener)
    suspend fun save(path: String)
    suspend fun load(path: String, json: JsonObject)
}

abstract class AbstractAudioProcessor(
    description: AudioProcessorDescription,
    override val factory: AudioProcessorFactory<*>
): AudioProcessor, JsonSerializable {
    override val inputChannelsCount = 2
    override val outputChannelsCount = 2
    override var id = randomId()
        protected set
    @Suppress("CanBePrimaryConstructorProperty")
    override val description = description
    override var name = description.name
    private val _listeners = WeakHashMap<AudioProcessorListener, Unit>()
    protected val listeners: Set<AudioProcessorListener> get() = _listeners.keys

    @Suppress("MemberVisibilityCanBePrivate")
    protected fun JsonObjectBuilder.buildBaseJson() {
        put("factory", factory.name)
        put("name", name)
        put("id", id)
        put("identifier", description.name)
    }
    override fun toJson() = buildJsonObject { buildBaseJson() }

    override fun fromJson(json: JsonElement) {
        json as JsonObject
        name = json["name"]?.asString() ?: ""
        json["id"]?.asString()?.let { if (it.isNotEmpty()) id = it }
    }

    override suspend fun save(path: String) { }
    override suspend fun load(path: String, json: JsonObject) { fromJson(json) }

    override fun addListener(listener: AudioProcessorListener) { _listeners[listener] = Unit }
    override fun removeListener(listener: AudioProcessorListener) { _listeners.remove(listener) }

    override fun close() { }
    override fun onSuddenChange() { }
    override fun recover(path: String) { }
}

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

    override fun toString(): String {
        return "DefaultAudioProcessorDescription(name='$name', category=$category, manufacturerName=$manufacturerName, " +
                "version=$version, isInstrument=$isInstrument, identifier='$identifier', " +
                "descriptiveName=$descriptiveName, isDeprecated=$isDeprecated)"
    }
}

// kotlin AudioProcessorIDSerializer
object AudioProcessorIDSerializer : KSerializer<AudioProcessor> {
    override val descriptor = String.serializer().descriptor
    override fun serialize(encoder: Encoder, value: AudioProcessor) { encoder.encodeString(value.id) }
    override fun deserialize(decoder: Decoder) = throw UnsupportedOperationException()
}

//class AudioProcessorIDSerializer @JvmOverloads constructor(t: Class<AudioProcessor>? = null) : StdSerializer<AudioProcessor>(t) {
//    override fun serialize(value: AudioProcessor, jgen: JsonGenerator, provider: SerializerProvider?) {
//        jgen.writeString(value.id)
//    }
//}
//class AudioProcessorCollectionIDSerializer @JvmOverloads constructor(t: Class<Collection<AudioProcessor>>? = null) :
//    StdSerializer<Collection<AudioProcessor>>(t) {
//    override fun serialize(value: Collection<AudioProcessor>, jgen: JsonGenerator, provider: SerializerProvider?) {
//        jgen.writeStartArray()
//        value.forEach { jgen.writeString(it.id) }
//        jgen.writeEndArray()
//    }
//}
