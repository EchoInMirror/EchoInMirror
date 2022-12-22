package cn.apisium.eim.api

import java.io.File
import javax.sound.sampled.AudioFileFormat

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