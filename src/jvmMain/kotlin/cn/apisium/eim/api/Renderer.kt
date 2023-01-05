package cn.apisium.eim.api

import java.io.File
import javax.sound.sampled.AudioFileFormat

interface Renderable {
    val isRendering: Boolean
    suspend fun processBlock(buffers: Array<FloatArray>, position: CurrentPosition, midiBuffer: ArrayList<Int>)
    fun onRenderStart()
    fun onRenderEnd()
}

enum class RenderFormat(val extend: String, val isLossLess: Boolean = true, val format: AudioFileFormat.Type? = null) {
    WAV("wav", format = AudioFileFormat.Type.WAVE),
    MP3("mp3", false),
    FLAC("flac"),
    OGG("ogg"),
    AU("au", format = AudioFileFormat.Type.AU),
    AIFF("aiff", format = AudioFileFormat.Type.AIFF),
    AIFC("aifc", format = AudioFileFormat.Type.AIFC),
    SND("snd", format = AudioFileFormat.Type.SND),
}

interface Renderer {
    suspend fun start(
        range: IntRange,
        sampleRate: Int,
        ppq: Int,
        bpm: Double,
        file: File,
        format: RenderFormat,
        bits: Int = 16,
        bitRate: Int = 320,
        compressionLevel: Int = 5,
        callback: (Float) -> Unit
    )
}