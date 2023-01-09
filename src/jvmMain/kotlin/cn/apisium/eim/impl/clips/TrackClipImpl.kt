package cn.apisium.eim.impl.clips

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import cn.apisium.eim.api.Clip
import cn.apisium.eim.api.TrackClip

class TrackClipImpl <T: Clip> (override val clip: T, time: Int = 0, duration: Int = 0) : TrackClip<T> {
    override var time by mutableStateOf(time)
    override var duration by mutableStateOf(duration)
    override var currentIndex = -1
    override fun reset() {
        currentIndex = -1
    }

    override fun toString(): String {
        return "TrackClipImpl(clip=$clip, time=$time, duration=$duration)"
    }
}
