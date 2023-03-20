package com.eimsound.audioprocessor.data

import androidx.compose.runtime.mutableStateOf
import com.eimsound.daw.utils.IManualState
import com.eimsound.daw.utils.binarySearch
import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlin.math.absoluteValue

enum class EnvelopeType {
    SMOOTH,
    EXPONENTIAL,
    SQUARE;

    fun getValue(tension: Float, t: Float, value0: Float, value1: Float) =
        if (value0 == value1) value0 else when (this) {
            SMOOTH -> {
                val controlPoint1: Float
                val controlPoint2: Float
                if (tension > 0) {
                    controlPoint1 = value0
                    controlPoint2 = value1
                } else {
                    val dy = (value1 - value0).absoluteValue * -tension
                    if (value0 > value1) {
                        controlPoint1 = value0 - dy
                        controlPoint2 = value1 + dy
                    } else {
                        controlPoint1 = value0 + dy
                        controlPoint2 = value1 - dy
                    }
                }
                val tmp = 1 - t
                value0 * tmp * tmp * tmp + 3 * controlPoint1 * tmp * tmp * t +
                        3 * tmp * t * t * controlPoint2 + value1 * t * t * t
            }
            EXPONENTIAL -> {
                val controlPoint1: Float
                val controlPoint2: Float
                if (tension > 0) {
                    controlPoint1 = value0 + (value1 - value0) * tension
                    controlPoint2 = value1
                } else {
                    controlPoint1 = value0
                    controlPoint2 = value1 + (value1 - value0) * tension
                }
                val tmp = 1 - t
                value0 * tmp * tmp * tmp + 3 * controlPoint1 * tmp * tmp * t +
                        3 * tmp * t * t * controlPoint2 + value1 * t * t * t
            }
            SQUARE -> value0
        }
}

@Serializable
data class EnvelopePoint(
    var time: Int,
    var value: Float,
    var tension: Float = 0F,
    var type: EnvelopeType = EnvelopeType.SMOOTH
)

@Serializable
data class SerializableEnvelopePointList(val ppq: Int, val points: List<EnvelopePoint>) {
    @OptIn(ExperimentalSerializationApi::class)
    @Suppress("unused")
    @EncodeDefault
    val className = "EnvelopePointList"
}

@Serializable
sealed interface EnvelopePointList : MutableList<EnvelopePoint>, IManualState {
    fun sort()
    fun getValue(position: Int, defaultValue: Float = 0F): Float
}

val VOLUME_RANGE = 0..2
val MIDI_CC_RANGE = 0..127

@Serializable
class DefaultEnvelopePointList : EnvelopePointList, ArrayList<EnvelopePoint>() {
    @Transient
    private var modification = mutableStateOf(0)
    @Transient
    private var currentIndex = -1

    override fun sort() = sortBy { it.time }
    override fun getValue(position: Int, defaultValue: Float): Float {
        if (size == 0) return defaultValue
        if (size == 1) return this[0].value
        if (position < this[0].time) return this[0].value
        if (position > this[size - 1].time) return this[size - 1].value
        if (currentIndex == -1 || currentIndex >= size || this[currentIndex].time > position)
            currentIndex = (binarySearch { it.time <= position } - 1).coerceAtLeast(0)
        while (currentIndex < size - 1 && this[currentIndex + 1].time <= position) currentIndex++
        val cur = this[currentIndex]
        if (currentIndex >= size - 1) return cur.value
        val next = this[currentIndex + 1]
        return cur.type.getValue(
            cur.tension,
            (position - cur.time).toFloat() / (next.time - cur.time),
            cur.value,
            next.value
        )
    }

    override fun update() { modification.value++ }
    override fun read() { modification.value }
}


