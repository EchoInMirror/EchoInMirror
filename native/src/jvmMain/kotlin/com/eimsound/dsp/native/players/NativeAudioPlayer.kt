package com.eimsound.dsp.native.players

import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.eimsound.audioprocessor.AbstractAudioPlayer
import com.eimsound.audioprocessor.AudioPlayerFactory
import com.eimsound.audioprocessor.AudioProcessor
import com.eimsound.audioprocessor.CurrentPosition
import com.eimsound.daw.utils.ByteBufInputStream
import com.eimsound.daw.utils.ByteBufOutputStream
import com.fasterxml.jackson.databind.ObjectMapper
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.EOFException
import java.util.*

class NativeAudioPlayer(
    factory: NativeAudioPlayerFactory,
    type: String, name: String,
    currentPosition: CurrentPosition,
    processor: AudioProcessor,
    execFile: String,
    vararg commands: String,
) : AbstractAudioPlayer(factory, "[$type] $name", currentPosition, processor), Runnable {
    private var thread: Thread? = null
    private var process: Process? = null
    private var inputStream: ByteBufInputStream
    private var outputStream: ByteBufOutputStream
    private val mutex = Mutex()
    private var channels = 2
    private var hasControls = false
    private lateinit var buffers: Array<FloatArray>
    override var inputLatency = 0
    override var outputLatency = 0
    override lateinit var availableSampleRates: IntArray
    override lateinit var availableBufferSizes: IntArray
    override var name = super.name

    init {
        try {
            val pb = ProcessBuilder(arrayListOf(execFile).apply {
                addAll(commands)
                add("-O")
                add("#")
                if (type.isNotEmpty()) {
                    add("-T")
                    add(ObjectMapper().writeValueAsString(type))
                }
                add("-B")
                add(currentPosition.bufferSize.toString())
                add("-R")
                add(currentPosition.sampleRate.toString())
            })
            pb.redirectOutput(ProcessBuilder.Redirect.INHERIT)
            val p = pb.start()
            process = p

            val flag1 = p.errorStream.read() == 1
            val flag2 = p.errorStream.read() == 2
            val isBigEndian = flag1 && flag2
            inputStream = ByteBufInputStream(isBigEndian, p.errorStream)
            outputStream = ByteBufOutputStream(isBigEndian, p.outputStream)

            outputStream.writeString(name)
            outputStream.flush()

            if (inputStream.read() != 1) throw RuntimeException("Failed to open audio player")

            p.onExit().thenAccept { close() }

            readDeviceInfo()

            thread = Thread(this)
            thread!!.start()
        } catch (e: Throwable) {
            close()
            throw e
        }
    }

    override fun close() {
        if (process != null) {
            process!!.destroy()
            process = null
        }
        if (thread != null) {
            thread!!.interrupt()
            thread = null
        }
        closeCallback?.invoke()
        closeCallback = null
    }

    @Composable
    override fun controls() {
        if (hasControls) {
            Button({
                runBlocking {
                    mutex.withLock {
                        if (process != null) {
                            outputStream.write(1)
                            outputStream.flush()
                        }
                    }
                }
            }) {
                Text("打开控制面板")
            }
        }
    }

    @Suppress("DuplicatedCode")
    override fun run() {
        val bufferSize = buffers[0].size
        try {
            while (thread?.isAlive == true && process != null) {
                enterProcessBlock()
                buffers.forEach { it.fill(0F) }

                runBlocking {
                    try {
                        processor.processBlock(buffers, currentPosition, ArrayList(0))
                    } catch (e: Throwable) {
                        e.printStackTrace()
                    }

                    exitProcessBlock()

                    mutex.withLock {
                        when (inputStream.read()) {
                            0 -> {
                                outputStream.write(0)
                                outputStream.write(channels)
                                for (j in 0 until channels) {
                                    for (i in 0 until bufferSize) outputStream.writeFloat(buffers[j][i])
                                }
                                outputStream.flush()
                            }
                            1 -> readDeviceInfo()
                            else -> throw EOFException()
                        }
                    }
                }

                if (currentPosition.isPlaying) currentPosition.update(currentPosition.timeInSamples + bufferSize)
            }
        } catch (ignored: EOFException) { } catch (ignored: InterruptedException) { } finally {
            close()
        }
    }

    private fun readDeviceInfo() {
        name = inputStream.readString()
        inputLatency = inputStream.readInt()
        outputLatency = inputStream.readInt()
        val sampleRate = inputStream.readInt()
        val bufferSize = inputStream.readInt()
        if (sampleRate != currentPosition.sampleRate || bufferSize != currentPosition.bufferSize) {
            currentPosition.setSampleRateAndBufferSize(sampleRate, bufferSize)
        }
        availableSampleRates = IntArray(inputStream.readInt()) { inputStream.readInt() }
        availableBufferSizes = IntArray(inputStream.readInt()) { inputStream.readInt() }
        hasControls = inputStream.readBoolean()

        buffers = arrayOf(FloatArray(bufferSize), FloatArray(bufferSize))
        processor.prepareToPlay(sampleRate, bufferSize)
    }
}

class NativeAudioPlayerFactory(
    private val execFile: String,
    private vararg val commands: String,
) : AudioPlayerFactory {
    override val name = "Native"
    override suspend fun getPlayers() = withContext(Dispatchers.IO) {
        ProcessBuilder(execFile, "-O", "-A").start().inputStream
            .readAllBytes().decodeToString().split("\$EIM\$").filter { it.isNotEmpty() }
    }
    override fun create(name: String, currentPosition: CurrentPosition, processor: AudioProcessor) = try {
        "\\[(.+?)] ".toRegex().find(name)?.let {
            NativeAudioPlayer(this,
                it.groupValues[1],
                name.substring(it.range.last + 1),
                currentPosition, processor, execFile, *commands
            )
        }
    } catch (e: Throwable) {
        e.printStackTrace()
        null
    } ?: NativeAudioPlayer(this, "", "",
        currentPosition, processor, execFile, *commands
    )
}
