package com.eimsound.audioprocessor

/**
 * @see com.eimsound.dsp.native.processors.ProcessAudioProcessorImpl
 */
interface ProcessAudioProcessor: AudioProcessor {
    val isLaunched: Boolean
    suspend fun launch(execFile: String, preset: String?, vararg commands: String): Boolean
}

fun CurrentPosition.toFlags(): Int {
    var flags = 0
    if (isPlaying) flags = flags or 1
    if (isLooping) flags = flags or 2
    if (isRecording) flags = flags or 4
    if (isRealtime) flags = flags or 8
    return flags
}
