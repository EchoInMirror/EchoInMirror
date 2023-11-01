package com.eimsound.audioprocessor

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.eimsound.daw.utils.ExperimentalEIMApi
import com.eimsound.daw.utils.FloatRange
import kotlin.reflect.KProperty

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
    @OptIn(ExperimentalEIMApi::class) (globalChangeHandler(this, newValue, emitEvent))
}
