package com.eimsound.audiosources.impl

import com.eimsound.audiosources.FileAudioSource
import com.eimsound.audiosources.MemoryAudioSource
import java.nio.file.Path

class ProxyFileAudioSource (
    override val file: Path, private var memoryAudioSource: MemoryAudioSource?
) : FileAudioSource, MemoryAudioSource {
    override val isClosed get() = memoryAudioSource?.isClosed ?: true
    override fun copy() = ProxyFileAudioSource(file, memoryAudioSource)

    override val sampleRate = memoryAudioSource?.sampleRate ?: 0f
    override val channels = memoryAudioSource?.channels ?: 0
    override val length = memoryAudioSource?.length ?: 0L
    override val isRandomAccessible = true
    override var position = 0L

    override fun getSamples(buffers: Array<FloatArray>, start: Int, length: Int, offset: Int) =
        memoryAudioSource?.getSamples(buffers, start, length, offset) ?: 0

    override fun close() { memoryAudioSource = null }
}