package cn.apisium.eim.api

import java.io.File
import javax.sound.sampled.AudioFileFormat

interface Renderable {
    val isRendering: Boolean
    suspend fun processBlock(buffers: Array<FloatArray>, position: CurrentPosition, midiBuffer: ArrayList<Int>)
    fun onRenderStart()
    fun onRenderEnd()
}

interface Renderer {
    suspend fun start(
        startPosition: Int,
        endPosition: Int,
        sampleRate: Int,
        ppq: Int,
        bpm: Double,
        file: File,
        audioType: AudioFileFormat.Type,
        callback: (Float) -> Unit
    )
}