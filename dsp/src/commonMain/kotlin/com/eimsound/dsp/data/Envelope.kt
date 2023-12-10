package com.eimsound.dsp.data

import androidx.compose.runtime.mutableStateOf
import com.eimsound.daw.commons.IManualState
import com.eimsound.daw.commons.json.*
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

class EnvelopePoint(
    var time: Int = 0,
    var value: Float = 0F,
    tension: Float = 0F,
    var type: EnvelopeType = EnvelopeType.SMOOTH
): Comparable<EnvelopePoint>, JsonSerializable {
    override fun compareTo(other: EnvelopePoint) = time.compareTo(other.time)
    var tension = tension
        set(value) { field = value.coerceIn(-1F, 1F) }
    fun copy(time: Int = this.time, value: Float = this.value, tension: Float = this.tension, type: EnvelopeType = this.type) =
        EnvelopePoint(time, value, tension, type)

    override fun toJson() = buildJsonArray {
        add(time)
        add(value)
        add(tension)
        if (type != EnvelopeType.SMOOTH) add(type.ordinal)
    }

    override fun fromJson(json: JsonElement) {
        val array = json.jsonArray
        time = array[0].asInt()
        value = array[1].asFloat()
        tension = array[2].asFloat()
        type = if (array.size > 3) EnvelopeType.entries[array[3].asInt()] else EnvelopeType.SMOOTH
    }

    override fun toString() = "EnvelopePoint(time=$time, value=$value, tension=$tension, type=$type)"
}

typealias BaseEnvelopePointList = List<EnvelopePoint>
typealias MutableBaseEnvelopePointList = MutableList<EnvelopePoint>

class SerializableEnvelopePointList(
    var ppq: Int = 96, var points: BaseEnvelopePointList = emptyList()
): JsonSerializable {
    override fun toJson() = buildJsonObject {
        put("ppq", ppq)
        put("points", points)
        put("className", "EnvelopePointList")
    }
    override fun fromJson(json: JsonElement) {
        val obj = json.jsonObject
        ppq = obj["ppq"]?.asInt() ?: 96
        points = obj["points"]?.let { mutableListOf<EnvelopePoint>().apply { fromJson(it) } } ?: emptyList()
    }
}

fun JsonObjectBuilder.put(key: String, value: BaseEnvelopePointList?) {
    if (value == null) put(key, JsonArray(emptyList()))
    else put(key, value.toJsonArray())
}
fun JsonObjectBuilder.putNotDefault(key: String, value: BaseEnvelopePointList?) {
    if (!value.isNullOrEmpty()) put(key, value.toJsonArray())
}

sealed interface EnvelopePointList : MutableBaseEnvelopePointList, IManualState, JsonSerializable {
    fun copy(): EnvelopePointList
    fun split(time: Int, offsetStart: Int = 0): Pair<BaseEnvelopePointList, BaseEnvelopePointList>
    fun getValue(position: Int, defaultValue: Float = 0F): Float
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

    override fun split(time: Int, offsetStart: Int): Pair<BaseEnvelopePointList, BaseEnvelopePointList> {
        val leftEnvelope = mutableListOf<EnvelopePoint>()
        val rightEnvelope = mutableListOf<EnvelopePoint>()
        var isFirst = true
        var firstIndex = -1
        forEachIndexed { i, it ->
            if (it.time + offsetStart <= time) {
                leftEnvelope.add(it.copy())
                return@forEachIndexed
            }
            rightEnvelope.add(it.copy(time = it.time - time))
            if (isFirst) {
                firstIndex = i
                isFirst = false
                leftEnvelope.add(it.copy())
            }
        }
        if (firstIndex == -1 && leftEnvelope.isNotEmpty()) firstIndex = leftEnvelope.size
        if (firstIndex > 0) rightEnvelope.add(0, this[firstIndex - 1].let { it.copy(time = it.time - time) })
        update()
        return leftEnvelope to rightEnvelope
    }

    override fun update() { modification.value++ }
    override fun read() { modification.value }

    override fun fromJson(json: JsonElement) { fromJson(json.jsonArray) { EnvelopePoint() } }
    override fun toJson() = toJsonArray()
}

@Suppress("unused")
fun Collection<BaseEnvelopePointList>.merge(): BaseEnvelopePointList {
    val result = hashMapOf<Int, EnvelopePoint>()
    forEach {
        repeat(it.size) { i ->
            result[it[i].time] = it[i]
        }
    }
    return result.values.sorted()
}
