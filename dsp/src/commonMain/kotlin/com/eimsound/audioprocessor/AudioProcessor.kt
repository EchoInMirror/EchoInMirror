package com.eimsound.audioprocessor

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.eimsound.daw.utils.*
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.*
import java.io.File
import java.util.*
import kotlin.reflect.KProperty

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

interface IAudioProcessorParameter: Comparable<AudioProcessorParameter> {
    val id: String
    val name: String
    val label: String
    val isSuggestion: Boolean
    val isFloat: Boolean
    val isAutomatable: Boolean
    var value: Float
    val range: FloatRange
    val initialValue: Float
    val valueStrings: Array<String>

    operator fun getValue(thisRef: Any?, property: KProperty<*>) = value

    operator fun setValue(thisRef: Any?, property: KProperty<*>, value: Float) { this.value = value }
    fun setValue(value: Float, emitEvent: Boolean = true) { this.value = value }
}

open class AudioProcessorParameter(
    override val id: String,
    override val name: String,
    final override val range: FloatRange = 0F..1F,
    final override val initialValue: Float = range.start,
    override val label: String = "",
    override val isFloat: Boolean = true,
    override val isSuggestion: Boolean = false,
    override val isAutomatable: Boolean = true,
    override val valueStrings: Array<String> = emptyArray(),
    private val onChange: ((IAudioProcessorParameter) -> Unit)? = null
): IAudioProcessorParameter {
    // first compare by isSuggestion, then by isAutomatable, then by name
    override fun compareTo(other: AudioProcessorParameter) =
        if (other.isSuggestion != isSuggestion) {
            if (isSuggestion) -1 else 1
        } else if (other.isAutomatable != isAutomatable) {
            if (isAutomatable) -1 else 1
        } else name.compareTo(other.name)

    private var _value by mutableStateOf(initialValue.coerceIn(range))
    override var value get() = _value
        set(value) {
            val newVal = value.coerceIn(range)
            if (newVal != _value) {
                _value = newVal
                onChange?.invoke(this)
            }
        }

    override fun setValue(value: Float, emitEvent: Boolean) {
        if (emitEvent) this.value = value
        else _value = value
    }

    override fun toString(): String {
        return "AbstractAudioProcessorParameter(name='$name', isSuggestion=$isSuggestion, isFloat=$isFloat, value=$value, range=$range)"
    }
}

@Suppress("NOTHING_TO_INLINE")
inline fun audioProcessorParameterOf(id: String, name: String, range: FloatRange = 0F..1F,
                                     initialValue: Float = range.start, label: String = "", isFloat: Boolean = true,
                                     isSuggestion: Boolean = false, isAutomatable: Boolean = true,
                                     valueStrings: Array<String> = emptyArray(),
                                     noinline onChange: ((IAudioProcessorParameter) -> Unit)? = null
) =
    AudioProcessorParameter(id, name, range, initialValue, label, isFloat, isSuggestion, isAutomatable, valueStrings, onChange)

class AudioProcessorBooleanParameter(id: String, name: String, initialValue: Boolean = false, isSuggestion: Boolean = false,
                                     isAutomatable: Boolean = true, onChange: ((IAudioProcessorParameter) -> Unit)? = null):
    AudioProcessorParameter(id, name, 0F..1F, if (initialValue) 1F else 0F, isFloat = false,
        isSuggestion = isSuggestion, isAutomatable = isAutomatable, onChange = onChange) {
        var booleanValue get() = super.value == 1F
            set(value) { super.value = if (value) 1F else 0F }
    }

@Suppress("NOTHING_TO_INLINE")
inline fun audioProcessorParameterOf(id: String, name: String, initialValue: Boolean = false, isSuggestion: Boolean = false,
                                     isAutomatable: Boolean = true,
                                     noinline onChange: ((IAudioProcessorParameter) -> Unit)? = null) =
    AudioProcessorBooleanParameter(id, name, initialValue, isSuggestion, isAutomatable, onChange)

class AudioProcessorIntParameter(id: String, name: String, valueStrings: Array<String>, initialValue: Int = 0,
                                      label: String = "", isSuggestion: Boolean = false,
                                     isAutomatable: Boolean = true, onChange: ((IAudioProcessorParameter) -> Unit)? = null):
    AudioProcessorParameter(id, name, 0F..valueStrings.size.toFloat(), initialValue.toFloat(), label, false,
        isSuggestion, isAutomatable, valueStrings, onChange) {
        var intValue get() = super.value.toInt()
            set(value) { super.value = value.toFloat() }
    }

@Suppress("NOTHING_TO_INLINE")
inline fun audioProcessorParameterOf(id: String, name: String, valueStrings: Array<String>, initialValue: Int = 0,
                                     label: String = "", isSuggestion: Boolean = false,
                                     isAutomatable: Boolean = true,
                                     noinline onChange: ((IAudioProcessorParameter) -> Unit)? = null) =
    AudioProcessorIntParameter(id, name, valueStrings, initialValue, label, isSuggestion, isAutomatable, onChange)

@ExperimentalEIMApi
var globalChangeHandler: (IAudioProcessorParameter, Float, Boolean) -> Unit = { p, v, e -> p.setValue(v, e) }
fun IAudioProcessorParameter.doChangeAction(newValue: Float, emitEvent: Boolean = true) {
    if (newValue == value) return
    @OptIn(ExperimentalEIMApi::class) globalChangeHandler(this, newValue, emitEvent)
}

interface AudioProcessor: Restorable, AutoCloseable, SuddenChangeListener {
    var name: String
    @Transient
    val description: AudioProcessorDescription
    @Transient
    val inputChannelsCount: Int
    @Transient
    val outputChannelsCount: Int
    val factory: AudioProcessorFactory<*>
    val id: String
    val parameters: List<IAudioProcessorParameter>
    val lastModifiedParameter: IAudioProcessorParameter?
    var isBypassed: Boolean
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
    private val saveParameters: Boolean = true
): AudioProcessor, JsonSerializable {
    override val inputChannelsCount = 2
    override val outputChannelsCount = 2
    override var id = randomId()
        protected set
    @Suppress("CanBePrimaryConstructorProperty")
    override val description = description
    override var name = description.name
    override val parameters = emptyList<IAudioProcessorParameter>()
    override var lastModifiedParameter: IAudioProcessorParameter? = null
        protected set
    override var isBypassed by mutableStateOf(false)
    private val _listeners = WeakHashMap<AudioProcessorListener, Unit>()
    protected val listeners get() = _listeners.keys
    protected open val storeFileName = "processor.json"

    @Suppress("MemberVisibilityCanBePrivate")
    protected fun JsonObjectBuilder.buildBaseJson() {
        put("factory", factory.name)
        put("name", name)
        put("id", id)
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
        isBypassed = json["isBypassed"]?.jsonPrimitive?.boolean ?: false
        if (saveParameters) json["parameters"]?.jsonObject?.let { obj ->
            val params = parameters.associateBy { it.id }
            obj.forEach { (id, value) ->
                val parameter = params[id] ?: return@forEach
                parameter.value = value.jsonPrimitive.float.coerceIn(parameter.range)
            }
        }
    }

    override suspend fun store(path: String) {
        encodeJsonFile(File("$path/$storeFileName"), true)
    }
    override suspend fun restore(path: String) {
        fromJsonFile(File("$path/$storeFileName"))
    }

    override fun addListener(listener: AudioProcessorListener) { _listeners[listener] = Unit }
    override fun removeListener(listener: AudioProcessorListener) { _listeners.remove(listener) }

    override fun close() { }
    override fun onSuddenChange() { }
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

object AudioProcessorIDSerializer : KSerializer<AudioProcessor> {
    override val descriptor = String.serializer().descriptor
    override fun serialize(encoder: Encoder, value: AudioProcessor) { encoder.encodeString(value.id) }
    override fun deserialize(decoder: Decoder) = throw UnsupportedOperationException()
}
