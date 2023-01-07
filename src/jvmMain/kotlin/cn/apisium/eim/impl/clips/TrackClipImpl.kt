package cn.apisium.eim.impl.clips

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import cn.apisium.eim.api.Clip
import cn.apisium.eim.api.TrackClip

class TrackClipImpl <T: Clip> (override val clip: T) : TrackClip<T> {
    override var time by mutableStateOf(0)
    override var currentIndex = 0
}
