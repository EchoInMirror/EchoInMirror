package com.eimsound.audioprocessor

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.eimsound.daw.commons.Restorable
import com.eimsound.daw.commons.json.*
import com.eimsound.daw.utils.randomId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.*
import java.nio.file.Path
import java.util.*

enum class AudioProcessorState {
    Ready, Loading, Failed, Unloaded
}

interface AudioProcessor: Restorable, AutoCloseable, SuddenChangeListener {
    var name: String
    val description: AudioProcessorDescription
    val inputChannelsCount: Int
    val outputChannelsCount: Int
    val factory: AudioProcessorFactory<*>
    val id: String
    val uuid: UUID
    val parameters: List<AudioProcessorParameter>
    val lastModifiedParameter: AudioProcessorParameter?
    var isBypassed: Boolean
    val state: AudioProcessorState
    val latency: Int
    suspend fun processBlock(buffers: Array<FloatArray>, position: CurrentPosition, midiBuffer: ArrayList<Int>) { }
    fun prepareToPlay(sampleRate: Int, bufferSize: Int) { }
    fun onClick() { }
    fun addListener(listener: AudioProcessorListener)
    fun removeListener(listener: AudioProcessorListener)
}

interface AudioProcessorEditor : AudioProcessor {
    @Composable
    fun Editor()
}

abstract class AbstractAudioProcessor(
    description: AudioProcessorDescription,
    override val factory: AudioProcessorFactory<*>,
    private val saveParameters: Boolean = true,
    private val initialState: AudioProcessorState = AudioProcessorState.Ready
): AudioProcessor, JsonSerializable {
    override val inputChannelsCount = 2
    override val outputChannelsCount = 2
    override var id = randomId()
        protected set
    override var uuid: UUID = UUID.randomUUID()
        protected set
    @Suppress("CanBePrimaryConstructorProperty")
    override val description = description
    override var name = description.name
    override val parameters = emptyList<AudioProcessorParameter>()
    override var lastModifiedParameter: AudioProcessorParameter? = null
        protected set
    override var isBypassed by mutableStateOf(false)
    private val _listeners = WeakHashMap<AudioProcessorListener, Unit>()
    protected val listeners get() = _listeners.keys
    protected open val storeFileName = "processor.json"
    override var state by mutableStateOf(initialState)
        protected set
    override var latency = 0
        protected set

    @Suppress("MemberVisibilityCanBePrivate")
    protected fun JsonObjectBuilder.buildBaseJson() {
        put("factory", factory.name)
        put("name", name)
        put("id", id)
        put("uuid", uuid.toString())
        put("identifier", description.identifier)
        putNotDefault("isBypassed", isBypassed)
        if (saveParameters) put("parameters", buildJsonObject {
            parameters.forEach { if (it.value != it.initialValue) put(it.id, it.value) }
        })
    }
    override fun toJson() = buildJsonObject { buildBaseJson() }

    override fun fromJson(json: JsonElement) {
        json as JsonObject
        name = json["name"]?.asString() ?: ""
        json["id"]?.asString()?.let { if (it.isNotEmpty()) id = it }
        json["uuid"]?.asString()?.let { if (it.isNotEmpty()) uuid = UUID.fromString(it) }
        isBypassed = json["isBypassed"]?.jsonPrimitive?.boolean ?: false
        if (saveParameters) json["parameters"]?.jsonObject?.let { obj ->
            val params = parameters.associateBy { it.id }
            obj.forEach { (id, value) ->
                val parameter = params[id] ?: return@forEach
                parameter.value = value.jsonPrimitive.float.coerceIn(parameter.range)
            }
        }
    }

    override suspend fun store(path: Path) {
        withContext(Dispatchers.IO) {
            encodeJsonFile(path.resolve(storeFileName), true)
        }
    }
    override suspend fun restore(path: Path) {
        withContext(Dispatchers.IO) {
            fromJsonFile(path.resolve(storeFileName))
        }
        if (initialState == AudioProcessorState.Ready) state = AudioProcessorState.Ready
    }

    override fun addListener(listener: AudioProcessorListener) { _listeners[listener] = Unit }
    override fun removeListener(listener: AudioProcessorListener) { _listeners.remove(listener) }

    override fun close() {
        state = AudioProcessorState.Unloaded
    }
    override fun onSuddenChange() { }
}
