package com.eimsound.dsp.native.players

import com.eimsound.audioprocessor.AbstractAudioPlayer
import com.eimsound.audioprocessor.AudioPlayerFactory
import com.eimsound.audioprocessor.AudioProcessor
import com.eimsound.audioprocessor.CurrentPosition
import com.eimsound.daw.utils.ByteBufInputStream
import com.eimsound.daw.utils.ByteBufOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.util.*

class NativeAudioPlayer(
    type: String, name: String,
    currentPosition: CurrentPosition,
    processor: AudioProcessor,
    private val execFile: String,
    private vararg val commands: String,
) : AbstractAudioPlayer("[$type] $name", currentPosition, processor), Runnable {
    private var thread: Thread? = null
    private var process: Process? = null
    private var inputStream: ByteBufInputStream? = null
    private var outputStream: ByteBufOutputStream? = null
    private var bufferSize = 0
    private var channels = 2
    private var buffers = arrayOf(FloatArray(bufferSize), FloatArray(bufferSize))
    override var inputLatency: Int = 0
    override var outputLatency: Int = 0

    @Suppress("DuplicatedCode")
    override fun open(sampleRate: Int, bufferSize: Int, bits: Int) {
        this.bufferSize = bufferSize
        buffers = arrayOf(FloatArray(bufferSize), FloatArray(bufferSize))

        val pb = ProcessBuilder(execFile, *commands, "-O", "-B", bufferSize.toString(), "-R", sampleRate.toString())
        pb.redirectError(ProcessBuilder.Redirect.INHERIT)
        val p = pb.start()

        val flag1 = p.inputStream.read() == 1
        val flag2 = p.inputStream.read() == 2
        val isBigEndian = flag1 && flag2
        val input = ByteBufInputStream(isBigEndian, p.inputStream)
        inputStream = input
        outputStream = ByteBufOutputStream(isBigEndian, p.outputStream)

        if (input.read() != 1) throw RuntimeException("Failed to open audio player")

        println(input.readString())
        this.inputLatency = input.readInt()
        this.outputLatency = input.readInt()
        println("Input latency: ${this.inputLatency}, output latency: ${this.outputLatency}, sample rate: ${input.readFloat()}, buffer size: ${input.readInt()}")

        p.onExit().thenAccept { process = null }
        process = p

        processor.prepareToPlay(sampleRate, bufferSize)

        thread = Thread(this)
        thread!!.start()
    }

    override fun close() {
        if (thread != null) {
            thread!!.interrupt()
            thread = null
        }
        if (process != null) {
            process!!.destroy()
            process = null
        }
    }

    @Suppress("DuplicatedCode")
    override fun run() {
        while (thread?.isAlive == true) {
            if (process == null) {
                Thread.sleep(10)
                continue
            }

            enterProcessBlock()
            buffers.forEach { it.fill(0F) }

            runBlocking {
                try {
                    processor.processBlock(buffers, currentPosition, ArrayList(0))
                } catch (e: Throwable) {
                    e.printStackTrace()
                }
            }
            exitProcessBlock()

            if (inputStream!!.read() != 0) {
                continue
            }

            val output = outputStream!!
            output.write(0)
            output.write(channels)
            for (j in 0 until channels) {
                for (i in 0 until bufferSize) output.writeFloat(buffers[j][i])
            }
            output.flush()

            if (currentPosition.isPlaying) currentPosition.update(currentPosition.timeInSamples + bufferSize)
        }
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
    override fun create(name: String, currentPosition: CurrentPosition, processor: AudioProcessor): NativeAudioPlayer {
        val res = "[(.+?)] ".toRegex().find(name) ?: throw IllegalArgumentException("No such player: $name")
        return NativeAudioPlayer(
            res.groupValues[0].trimStart('[').trimEnd(']'),
            name.substring(res.range.last + 1),
            currentPosition, processor, execFile, *commands
        )
    }
}
