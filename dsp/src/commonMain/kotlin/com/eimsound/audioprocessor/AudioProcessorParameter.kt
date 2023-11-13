package com.eimsound.audioprocessor

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.eimsound.daw.commons.ExperimentalEIMApi
import com.eimsound.daw.utils.FloatRange
import kotlin.reflect.KProperty

interface AudioProcessorParameter: Comparable<SimpleAudioProcessorParameter> {
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

interface AudioProcessorBooleanParameter: AudioProcessorParameter {
    var booleanValue: Boolean
}

interface AudioProcessorIntParameter : AudioProcessorParameter {
    var intValue: Int
}

abstract class AbstractAudioProcessorParameter(
    private val onChange: ((AudioProcessorParameter) -> Unit)? = null
): AudioProcessorParameter {
    // first compare by isSuggestion, then by isAutomatable, then by name
    override fun compareTo(other: SimpleAudioProcessorParameter) =
        if (other.isSuggestion != isSuggestion) {
            if (isSuggestion) -1 else 1
        } else if (other.isAutomatable != isAutomatable) {
            if (isAutomatable) -1 else 1
        } else name.compareTo(other.name)

    @Suppress("PropertyName")
    protected abstract var _value: Float
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
        return "${this::class.simpleName}(name='$name', isSuggestion=$isSuggestion, isFloat=$isFloat, value=$value, range=$range)"
    }
}

open class SimpleAudioProcessorParameter(
    override val id: String,
    override val name: String,
    final override val range: FloatRange = 0F..1F,
    final override val initialValue: Float = range.start,
    override val label: String = "",
    override val isFloat: Boolean = true,
    override val isSuggestion: Boolean = false,
    override val isAutomatable: Boolean = true,
    override val valueStrings: Array<String> = emptyArray(),
    onChange: ((AudioProcessorParameter) -> Unit)? = null
): AbstractAudioProcessorParameter(onChange) {
    override var _value by mutableStateOf(initialValue.coerceIn(range))
}

@Suppress("NOTHING_TO_INLINE")
inline fun audioProcessorParameterOf(
    id: String, name: String, range: FloatRange = 0F..1F,
    initialValue: Float = range.start, label: String = "", isFloat: Boolean = true,
    isSuggestion: Boolean = false, isAutomatable: Boolean = true,
    valueStrings: Array<String> = emptyArray(),
    noinline onChange: ((AudioProcessorParameter) -> Unit)? = null
) =
    SimpleAudioProcessorParameter(id, name, range, initialValue, label, isFloat, isSuggestion, isAutomatable, valueStrings, onChange)

class DefaultAudioProcessorBooleanParameter(
    id: String, name: String, initialValue: Boolean = false, isSuggestion: Boolean = false,
    isAutomatable: Boolean = true, onChange: ((AudioProcessorParameter) -> Unit)? = null
): SimpleAudioProcessorParameter(
    id, name, 0F..1F, if (initialValue) 1F else 0F, isFloat = false,
    isSuggestion = isSuggestion, isAutomatable = isAutomatable, onChange = onChange
), AudioProcessorBooleanParameter {
    override var booleanValue get() = super.value == 1F
        set(value) { super.value = if (value) 1F else 0F }
}

@Suppress("NOTHING_TO_INLINE")
inline fun audioProcessorParameterOf(
    id: String, name: String, initialValue: Boolean = false, isSuggestion: Boolean = false,
    isAutomatable: Boolean = true, noinline onChange: ((AudioProcessorParameter) -> Unit)? = null
) =
    DefaultAudioProcessorBooleanParameter(id, name, initialValue, isSuggestion, isAutomatable, onChange)

class DefaultAudioProcessorIntParameter(
    id: String, name: String, valueStrings: Array<String>, initialValue: Int = 0,
    label: String = "", isSuggestion: Boolean = false,
    isAutomatable: Boolean = true, onChange: ((AudioProcessorParameter) -> Unit)? = null
): SimpleAudioProcessorParameter(
    id, name, 0F..valueStrings.size.toFloat(), initialValue.toFloat(), label, false,
    isSuggestion, isAutomatable, valueStrings, onChange
), AudioProcessorIntParameter {
    override var intValue get() = super.value.toInt()
        set(value) { super.value = value.toFloat() }
}

@Suppress("NOTHING_TO_INLINE")
inline fun audioProcessorParameterOf(
    id: String, name: String, valueStrings: Array<String>, initialValue: Int = 0,
    label: String = "", isSuggestion: Boolean = false,
    isAutomatable: Boolean = true, noinline onChange: ((AudioProcessorParameter) -> Unit)? = null
) =
    DefaultAudioProcessorIntParameter(id, name, valueStrings, initialValue, label, isSuggestion, isAutomatable, onChange)

@ExperimentalEIMApi
var globalChangeHandler: (List<Pair<AudioProcessorParameter, Float>>, Boolean) -> Unit = { list, e ->
    for (i in list.indices) list[i].first.setValue(list[i].second, e)
}
fun List<Pair<AudioProcessorParameter, Float>>.doChangeAction(emitEvent: Boolean = true) {
    for (i in indices) if (this[i].first.value != this[i].second) {
        @OptIn(ExperimentalEIMApi::class) (globalChangeHandler(this, emitEvent))
        return
    }
}

fun AudioProcessorParameter.doChangeAction(newValue: Float, emitEvent: Boolean = true) {
    if (newValue == value) return
    @OptIn(ExperimentalEIMApi::class) (globalChangeHandler(listOf(this to newValue), emitEvent))
}
