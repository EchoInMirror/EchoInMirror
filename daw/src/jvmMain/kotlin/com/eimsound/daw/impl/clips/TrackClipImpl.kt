package com.eimsound.daw.impl.clips

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.eimsound.daw.api.Clip
import com.eimsound.daw.api.TrackClip
import com.eimsound.daw.api.processor.Track
import com.eimsound.daw.utils.asInt
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.slf4j.LoggerFactory

@Suppress("PrivatePropertyName")
private val LOGGER = LoggerFactory.getLogger(TrackClipImpl::class.java)

class TrackClipImpl <T: Clip> (override val clip: T, time: Int = 0, duration: Int = 0, start: Int = 0,
                               override var track: Track? = null) : TrackClip<T> {
    override var time by mutableStateOf(time)
    override var duration by mutableStateOf(duration)
    private var _start by mutableStateOf(start)
    override var start: Int
        get() = _start
        set(value) {
            if (value < 0 && !clip.isExpandable) {
                _start = 0
                LOGGER.warn("Start of a clip cannot be negative: $this")
            } else _start = value
        }
    override var currentIndex = -1
    override fun reset() {
        currentIndex = -1
    }

    override fun toJson() = buildJsonObject {
        put("time", time)
        put("duration", duration)
        put("start", start)
        put("clip", clip.toJson())
    }

    override fun fromJson(json: JsonElement) {
        json as JsonObject
        json["time"]?.asInt()?.let { time = it }
        json["duration"]?.asInt()?.let { duration = it }
        json["start"]?.asInt()?.let { start = it }
    }

    override fun copy(time: Int, duration: Int, start: Int, clip: T, currentIndex: Int, track: Track?) =
        TrackClipImpl(clip, time, duration, start, track)

    override fun toString(): String {
        return "TrackClipImpl(clip=$clip, time=$time, duration=$duration)"
    }
}
