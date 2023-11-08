package com.eimsound.audioprocessor

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.eimsound.daw.commons.Reloadable
import java.util.*

interface AudioPlayer : AutoCloseable {
    val factory: AudioPlayerFactory
    val name: String
    val cpuLoad: Float
    val inputLatency: Int
    val outputLatency: Int
    val availableSampleRates: IntArray
    val availableBufferSizes: IntArray
    @Composable fun controls()
    fun onClose(callback: () -> Unit)
}

abstract class AbstractAudioPlayer(
    override val factory: AudioPlayerFactory,
    override val name: String,
    val currentPosition: CurrentPosition,
    var processor: AudioProcessor,
) : AudioPlayer {
    final override var cpuLoad by mutableStateOf(0F)
        private set
    private var lastTime = 0L
    private var time = 0L
    private var times = 0
    protected var closeCallback: (() -> Unit)? = null
    override val availableSampleRates = intArrayOf(44100, 48000, 88200, 96000)
    override val availableBufferSizes = intArrayOf(256, 512, 1024, 2048, 4096)

    protected fun enterProcessBlock() {
        lastTime = System.nanoTime()
    }

    protected fun exitProcessBlock() {
        time += System.nanoTime() - lastTime
        if (currentPosition.bufferSize * times++ > currentPosition.sampleRate) {
            cpuLoad = time / 1_000_000_1000F
            time = 0
            times = 0
        }
    }

    @Composable
    override fun controls() { }

    override fun onClose(callback: () -> Unit) { closeCallback = callback }
}

interface AudioPlayerFactory {
    val name: String
    suspend fun getPlayers(): List<String>
    fun create(name: String, currentPosition: CurrentPosition, processor: AudioProcessor): AudioPlayer
}

/**
 * @see com.eimsound.audioprocessor.impl.AudioPlayerManagerImpl
 */
interface AudioPlayerManager : Reloadable {
    companion object {
        val instance by lazy { ServiceLoader.load(AudioPlayerManager::class.java).first()!! }
    }
    val factories: Map<String, AudioPlayerFactory>

    fun create(factory: String, name: String, currentPosition: CurrentPosition, processor: AudioProcessor): AudioPlayer
    fun createDefaultPlayer(currentPosition: CurrentPosition, processor: AudioProcessor): AudioPlayer
}
