package cn.apisium.eim.impl.clips

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import cn.apisium.eim.api.Clip
import cn.apisium.eim.api.TrackClip
import cn.apisium.eim.api.processor.Track
import org.slf4j.LoggerFactory

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

    override fun toString(): String {
        return "TrackClipImpl(clip=$clip, time=$time, duration=$duration)"
    }
}
