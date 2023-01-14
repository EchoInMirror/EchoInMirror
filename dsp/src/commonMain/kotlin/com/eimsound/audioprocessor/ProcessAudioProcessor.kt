package com.eimsound.audioprocessor

interface ProcessAudioProcessor: AudioProcessor {
    val isLaunched: Boolean
    suspend fun launch(execFile: String, vararg commands: String): Boolean
}

fun CurrentPosition.toFlags(): Int {
    var flags = 0
    if (isPlaying) flags = flags or 1
    if (isLooping) flags = flags or 2
    if (isRecording) flags = flags or 4
    if (isRealtime) flags = flags or 8
    return flags
}
