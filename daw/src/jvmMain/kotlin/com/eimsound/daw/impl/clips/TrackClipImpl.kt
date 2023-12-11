package com.eimsound.daw.impl.clips

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import com.eimsound.daw.api.clips.Clip
import com.eimsound.daw.api.clips.TrackClip
import com.eimsound.daw.api.processor.Track
import com.eimsound.daw.commons.json.asBoolean
import com.eimsound.daw.commons.json.asColor
import com.eimsound.daw.commons.json.asInt
import com.eimsound.daw.commons.json.putNotDefault
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject

private val logger = KotlinLogging.logger {  }
class TrackClipImpl <T: Clip> (
    override val clip: T, override var time: Int = 0, duration: Int = 0, start: Int = 0, track: Track? = null
) : TrackClip<T> {
    override var duration = duration
        get() = if (field <= 0) clip.defaultDuration else field
    override var isDisabled by mutableStateOf(false)
    override var track by mutableStateOf(track)
    override var color: Color? by mutableStateOf(null)

    override var start = start
        set(value) {
            if (value < 0 && !clip.isExpandable) {
                field = 0
                logger.warn { "Start of a clip cannot be negative: $this" }
            } else field = value
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
        putNotDefault("color", color)
        put("clip", clip.toJson())
    }

    override fun fromJson(json: JsonElement) {
        json as JsonObject
        json["time"]?.asInt()?.let { time = it }
        json["duration"]?.asInt()?.let { duration = it }
        json["start"]?.asInt()?.let { start = it }
        json["isDisabled"]?.asBoolean()?.let { isDisabled = it }
        json["color"]?.asColor().let { color = it }
    }

    @Suppress("UNCHECKED_CAST")
    override fun copy(time: Int, duration: Int, start: Int, clip: T, currentIndex: Int, track: Track?) =
        TrackClipImpl(clip.copy() as T, time, duration, start, track)

    override fun toString(): String {
        return "TrackClipImpl(clip=$clip, time=$time, duration=$duration)"
    }

    override fun compareTo(other: TrackClip<*>) =
        if (time == other.time) duration - other.duration
        else time - other.time
}
