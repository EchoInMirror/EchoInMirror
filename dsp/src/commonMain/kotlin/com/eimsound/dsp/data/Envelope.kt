package com.eimsound.dsp.data

import androidx.compose.runtime.mutableStateOf
import com.eimsound.daw.commons.IManualState
import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.json.*
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
                    @Suppress("DuplicatedCode")
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
): Comparable<EnvelopePoint> {
    override fun compareTo(other: EnvelopePoint) = time.compareTo(other.time)
}

typealias BaseEnvelopePointList = List<EnvelopePoint>
typealias MutableBaseEnvelopePointList = MutableList<EnvelopePoint>

@Serializable
data class SerializableEnvelopePointList(val ppq: Int, val points: BaseEnvelopePointList) {
    @OptIn(ExperimentalSerializationApi::class)
    @Suppress("unused")
    @EncodeDefault
    val className = "EnvelopePointList"
}

fun JsonObjectBuilder.put(key: String, value: BaseEnvelopePointList?) {
    put(key, Json.encodeToJsonElement(value))
}
fun JsonObjectBuilder.putNotDefault(key: String, value: BaseEnvelopePointList?) {
    if (!value.isNullOrEmpty()) put(key, Json.encodeToJsonElement(value))
}

@Serializable
sealed interface EnvelopePointList : MutableBaseEnvelopePointList, IManualState, BaseEnvelopePointList {
    fun copy(): EnvelopePointList
    fun split(time: Int, offsetStart: Int = 0): BaseEnvelopePointList
    fun getValue(position: Int, defaultValue: Float = 0F): Float
}

fun EnvelopePointList.fromJson(json: JsonElement?) {
    clear()
    if (json != null) addAll(Json.decodeFromJsonElement<BaseEnvelopePointList>(json))
    update()
}

fun BaseEnvelopePointList.toMutableEnvelopePointList() = DefaultEnvelopePointList().apply { addAll(this@toMutableEnvelopePointList) }

val PAN_RANGE = -1F..1F
val VOLUME_RANGE = 0F..1.96F
val MIDI_CC_RANGE = 0F..127F

private inline fun <T> List<T>.lowerBound(comparator: (T) -> Boolean): Int {
    var l = 0
    var r = size - 1
    while (l < r) {
        val mid = (l + r) ushr 1
        if (comparator(this[mid])) l = mid + 1
        else r = mid
    }
    return l
}

@Serializable
class DefaultEnvelopePointList : EnvelopePointList, ArrayList<EnvelopePoint>() {
    @Transient
    private var modification = mutableStateOf(0)
    @Transient
    private var currentIndex = -1

    override fun copy() = DefaultEnvelopePointList().apply { this@DefaultEnvelopePointList.forEach { add(it.copy()) } }
    override fun getValue(position: Int, defaultValue: Float): Float {
        if (size == 0) return defaultValue
        if (size == 1) return this[0].value
        if (position < this[0].time) return this[0].value
        if (position > this[size - 1].time) return this[size - 1].value
        if (currentIndex == -1 || currentIndex >= size || this[currentIndex].time > position)
            currentIndex = (lowerBound { it.time <= position } - 1).coerceAtLeast(0)
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

    override fun split(time: Int, offsetStart: Int): BaseEnvelopePointList {
        val newEnvelope = mutableListOf<EnvelopePoint>()
        var isFirst = true
        removeIf {
            if (it.time + offsetStart <= time) return@removeIf false
            if (isFirst) {
                newEnvelope.add(it.copy(time = it.time - time))
                isFirst = false
                return@removeIf false
            } else {
                it.time -= time
                newEnvelope.add(it)
                return@removeIf true
            }
        }
        getOrNull(size - 2)?.let {
            newEnvelope.add(0, it.copy(time = it.time - time))
        }
        update()
        return newEnvelope
    }

    override fun update() { modification.value++ }
    override fun read() { modification.value }
}
