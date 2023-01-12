package cn.apisium.eim.impl.clips

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import cn.apisium.eim.api.Clip
import cn.apisium.eim.api.TrackClip
import cn.apisium.eim.api.processor.Track

class TrackClipImpl <T: Clip> (override val clip: T, time: Int = 0, duration: Int = 0, start: Int = 0,
                               override var track: Track? = null) : TrackClip<T> {
    override var time by mutableStateOf(time)
    override var duration by mutableStateOf(duration)
    override var start by mutableStateOf(start)
    override var currentIndex = -1
    override fun reset() {
        currentIndex = -1
    }

    override fun toString(): String {
        return "TrackClipImpl(clip=$clip, time=$time, duration=$duration)"
    }
}
