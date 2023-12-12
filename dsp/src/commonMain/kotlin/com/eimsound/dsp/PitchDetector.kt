package com.eimsound.dsp

import be.tarsos.dsp.pitch.FastYin
import com.eimsound.audiosources.AudioSource
import com.eimsound.dsp.data.midi.getNoteName
import kotlin.math.ceil
import kotlin.math.log2

fun detectPitch(source: AudioSource, bufferSize: Int = 2048): String? {
    val len = ceil(source.sampleRate * 5).toLong().coerceAtMost(source.length).toInt()
    val buffers = Array(source.channels) { FloatArray(bufferSize) }
    val detector = FastYin(source.sampleRate, bufferSize)
    var pitch = 0F
    var probability = 0F
    repeat(len / bufferSize) {
        source.nextBlock(buffers, bufferSize)
        if (source.channels == 2) repeat(bufferSize) { i ->
            buffers[0][i] = (buffers[0][i] + buffers[1][i]) / 2
        }
        val result = detector.getPitch(buffers[0])
        if (result.isPitched) {
            if (result.probability > probability) {
                pitch = result.pitch
                probability = result.probability
            }
        }
    }
    return if (pitch > 0) getNoteName(note = (log2(pitch / 440F) * 12 + 69).toInt()) else null
}