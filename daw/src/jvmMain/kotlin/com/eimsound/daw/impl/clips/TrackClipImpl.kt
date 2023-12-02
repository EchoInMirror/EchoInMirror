package com.eimsound.daw.impl.clips

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.eimsound.daw.api.Clip
import com.eimsound.daw.api.ClipFactory
import com.eimsound.daw.api.TrackClip
import com.eimsound.daw.api.processor.Track
import com.eimsound.daw.commons.json.asBoolean
import com.eimsound.daw.commons.json.asInt
import com.eimsound.daw.commons.json.putNotDefault
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject

private val logger = KotlinLogging.logger {  }
class TrackClipImpl <T: Clip> (override val clip: T, time: Int = 0, duration: Int = 0, start: Int = 0,
                               track: Track? = null) : TrackClip<T> {
    override var time by mutableStateOf(time)
    override var duration by mutableStateOf(duration)
    override var isDisabled by mutableStateOf(false)
    private var _start by mutableStateOf(start)
    override var track by mutableStateOf(track)

    override var start: Int
        get() = _start
        set(value) {
            if (value < 0 && !clip.isExpandable) {
                _start = 0
                logger.warn { "Start of a clip cannot be negative: $this" }
            } else _start = value
        }
    override var currentIndex = -1
    override fun reset() {
        currentIndex = -1
    }

    override fun toJson() = buildJsonObject {
        putNotDefault("time", time)
        putNotDefault("duration", duration)
        putNotDefault("start", start)
        putNotDefault("isDisabled", isDisabled)
        put("clip", clip.toJson())
    }

    override fun fromJson(json: JsonElement) {
        json as JsonObject
        json["time"]?.asInt()?.let { time = it }
        json["duration"]?.asInt()?.let { duration = it }
        json["start"]?.asInt()?.let { start = it }
        json["isDisabled"]?.asBoolean()?.let { isDisabled = it }
    }

    @Suppress("UNCHECKED_CAST")
    override fun copy(time: Int, duration: Int, start: Int, clip: T, currentIndex: Int, track: Track?) =
        TrackClipImpl((clip.factory as ClipFactory<T>).copy(clip), time, duration, start, track)

    override fun toString(): String {
        return "TrackClipImpl(clip=$clip, time=$time, duration=$duration)"
    }

    override fun compareTo(other: TrackClip<*>) =
        if (time == other.time) duration - other.duration
        else time - other.time
}
