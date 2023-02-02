package com.eimsound.audioprocessor.data

import androidx.compose.runtime.mutableStateOf
import com.eimsound.daw.utils.IManualState
import com.fasterxml.jackson.annotation.JsonEnumDefaultValue
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonInclude

enum class EnvelopeType {
    @JsonEnumDefaultValue
    SMOOTH,
    EXPONENTIAL,
    SINE,
    COSINE,
    LOGARITHMIC,
    SQUARE,
    SAWTOOTH,
    INVERSE_SAWTOOTH,
    TRIANGLE,
    RANDOM;
}

//fun EnvelopeType.getValue(tension: Float, t: Float, value: Float, nextValue: Float) = when (this) {
//    EnvelopeType.SMOOTH -> value + (nextValue - value) * t
//    EnvelopeType.EXPONENTIAL -> value * (nextValue / value).pow(t)
//    EnvelopeType.SINE -> value + (nextValue - value) * sin(t * PI / 2)
//    EnvelopeType.COSINE -> value + (nextValue - value) * cos(t * PI / 2)
//    EnvelopeType.LOGARITHMIC -> value * (nextValue / value).pow(t)
//    EnvelopeType.SQUARE -> if (t < 0.5) value else nextValue
//    EnvelopeType.SAWTOOTH -> value + (nextValue - value) * t
//    EnvelopeType.INVERSE_SAWTOOTH -> value + (nextValue - value) * (1 - t)
//    EnvelopeType.TRIANGLE -> value + (nextValue - value) * if (t < 0.5) t * 2 else (1 - t) * 2
//    EnvelopeType.RANDOM -> value + (nextValue - value) * random()
//}

//fun EnvelopeType.draw(tension: Float, x: Float, value: Float, nextX: Float, nextValue: Float) {
//    when (this) {
//        EnvelopeType.SMOOTH -> {
//
//        }
//    }
//}

data class EnvelopePoint(
    val time: Int,
    val value: Float,
    val tension: Float = 0F,
    @get:JsonInclude(value = JsonInclude.Include.NON_DEFAULT)
    val type: EnvelopeType = EnvelopeType.SMOOTH
)

interface EnvelopePointList : MutableList<EnvelopePoint>, IManualState {
    fun sort()
}

class DefaultEnvelopePointList : EnvelopePointList, ArrayList<EnvelopePoint>() {
    @JsonIgnore
    private var modification = mutableStateOf(0)
    override fun sort() = sortBy { it.time }
    override fun update() { modification.value++ }
    override fun read() { modification.value }
}


