package com.eimsound.audioprocessor

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

interface AudioPlayer: AutoCloseable {
    val cpuLoad: Float
    fun open(sampleRate: Int, bufferSize: Int, bits: Int)
}
abstract class AbstractAudioPlayer(val currentPosition: CurrentPosition, var processor: AudioProcessor):
    AudioPlayer {
    final override var cpuLoad by mutableStateOf(0F)
        private set
    private var lastTime = 0L
    private var time = 0L
    private var times = 0
    protected fun enterProcessBlock() { lastTime = System.nanoTime() }
    protected fun exitProcessBlock() {
        time += System.nanoTime() - lastTime
        if (currentPosition.bufferSize * times++ > currentPosition.sampleRate) {
            cpuLoad = time / 1_000_000_1000F
            time = 0
            times = 0
        }
    }
}
