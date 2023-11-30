package com.eimsound.audioprocessor

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.eimsound.daw.commons.Reloadable
import com.eimsound.dsp.ResampledQueueAudioProcessor
import java.util.*

interface AudioPlayer : AutoCloseable {
    val factory: AudioPlayerFactory
    val name: String
    val cpuLoad: Float
    val inputLatency: Int
    val outputLatency: Int
    val channels: Int
    val availableSampleRates: List<Int>
    val availableBufferSizes: List<Int>
    val sampleRate: Int

    @Composable fun Controls()
    fun onClose(callback: () -> Unit)
}

abstract class AbstractAudioPlayer(
    override val factory: AudioPlayerFactory,
    override val name: String,
    final override val channels: Int,
    val currentPosition: MutableCurrentPosition,
    private val processor: AudioProcessor,
    preferredSampleRate: Int? = null,
) : AudioPlayer {
    final override var cpuLoad by mutableStateOf(0F)
        private set
    private var lastTime = 0L
    private var time = 0L
    private var times = 0
    private var closeCallback: (() -> Unit)? = null
    override var sampleRate by mutableStateOf(preferredSampleRate ?: currentPosition.sampleRate)
        protected set
    override val availableSampleRates = listOf(44100, 48000, 88200, 96000)
    override val availableBufferSizes = listOf(256, 512, 1024, 2048, 4096)

    private var processBuffers = Array(channels) { FloatArray(currentPosition.bufferSize) }
    private val midiBuffer = arrayListOf<Int>()
    private val resampler = ResampledQueueAudioProcessor(
        channels,
        currentPosition.sampleRate,
        currentPosition.sampleRate
    ) {
        lastTime = System.nanoTime()
        repeat(channels) { i -> processBuffers[i].fill(0F) }
        processor.processBlock(processBuffers, currentPosition, midiBuffer)
        if (currentPosition.isPlaying) {
            currentPosition.timeInSamples += currentPosition.bufferSize
        }
        midiBuffer.clear()
        exitProcessBlock()
        processBuffers
    }

    private fun exitProcessBlock() {
        time += System.nanoTime() - lastTime
        if (currentPosition.bufferSize * times++ > currentPosition.sampleRate) {
            cpuLoad = time / 1_000_000_1000F
            time = 0
            times = 0
        }
    }

    @Composable
    override fun Controls() { }

    override fun onClose(callback: () -> Unit) { closeCallback = callback }
    override fun close() {
        closeCallback?.invoke()
        closeCallback = null
    }

    protected suspend fun process(): Array<FloatArray> {
        if (processBuffers[0].size != currentPosition.bufferSize)
            processBuffers = Array(channels) { FloatArray(currentPosition.bufferSize) }
        if (currentPosition.sampleRate != resampler.inputSampleRate) resampler.inputSampleRate = currentPosition.sampleRate
        if (sampleRate != resampler.outputSampleRate) resampler.outputSampleRate = sampleRate
        resampler.process(processBuffers)
        return processBuffers
    }

    protected suspend fun prepareToPlay() {
        processor.prepareToPlay(currentPosition.sampleRate, currentPosition.bufferSize)
    }
}

interface AudioPlayerFactory {
    val name: String
    suspend fun getPlayers(): List<String>
    fun create(
        name: String, currentPosition: MutableCurrentPosition, processor: AudioProcessor,
        preferredSampleRate: Int? = null,
    ): AudioPlayer
}

/**
 * @see com.eimsound.audioprocessor.impl.AudioPlayerManagerImpl
 */
interface AudioPlayerManager : Reloadable {
    companion object {
        val instance by lazy { ServiceLoader.load(AudioPlayerManager::class.java).first()!! }
    }
    val factories: Map<String, AudioPlayerFactory>

    fun create(
        factory: String, name: String, currentPosition: MutableCurrentPosition, processor: AudioProcessor,
        preferredSampleRate: Int? = null,
    ): AudioPlayer
    fun createDefaultPlayer(currentPosition: MutableCurrentPosition, processor: AudioProcessor): AudioPlayer
}
