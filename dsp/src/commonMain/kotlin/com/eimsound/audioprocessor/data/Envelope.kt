package com.eimsound.audioprocessor.data

import androidx.compose.runtime.mutableStateOf
import com.eimsound.daw.utils.IManualState
import com.eimsound.daw.utils.binarySearch
import com.fasterxml.jackson.annotation.JsonEnumDefaultValue
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonInclude
import kotlin.math.absoluteValue
import kotlin.math.min

enum class EnvelopeType {
    @JsonEnumDefaultValue
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
                    val y0 = value0 + value1 - dy
                    val y1 = min(value0, value1) + dy
                    controlPoint1 = if (value0 > value1) y0 else y1
                    controlPoint2 = if (value0 > value1) y1 else y0
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

class EnvelopePoint(
    val time: Int,
    val value: Float,
    tension: Float = 0F,
    @get:JsonInclude(value = JsonInclude.Include.NON_DEFAULT)
    val type: EnvelopeType = EnvelopeType.SMOOTH
) {
    var tension = tension.coerceIn(-1F, 1F)
        set(value) { field = value.coerceIn(-1F, 1F) }
}

interface EnvelopePointList : MutableList<EnvelopePoint>, IManualState {
    fun sort()
    fun getValue(position: Int): Float
}

class DefaultEnvelopePointList : EnvelopePointList, ArrayList<EnvelopePoint>() {
    @JsonIgnore
    private var modification = mutableStateOf(0)
    @JsonIgnore
    private var currentIndex = -1
    override fun sort() = sortBy { it.time }
    override fun getValue(position: Int): Float {
        if (size == 0) return 0F
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


