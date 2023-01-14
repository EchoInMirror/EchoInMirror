package com.eimsound.dsp.native

import com.eimsound.audioprocessor.CurrentPosition
import javax.sound.sampled.AudioFormat

fun CurrentPosition.getAudioFormat(bits: Int = 2, channels: Int = 2) = AudioFormat(
    AudioFormat.Encoding.PCM_SIGNED, sampleRate.toFloat(), bits * 8, channels,
    bits * channels, sampleRate.toFloat(), false)

fun getSampleBits(bits: Int) = (1 shl (8 * bits - 1)) - 1

