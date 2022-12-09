package cn.apisium.eim.api.processor

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

interface LevelPeak {
    var left: Float
    var right: Float
}

class LevelPeakImpl : LevelPeak {
    override var left by mutableStateOf(0F)
    override var right by mutableStateOf(0F)
}
