package cn.apisium.eim.impl.processor.players

import cn.apisium.eim.api.AudioPlayer
import cn.apisium.eim.api.CurrentPosition
import cn.apisium.eim.api.processor.AudioProcessor
import cn.apisium.eim.utils.EIMInputStream
import cn.apisium.eim.utils.EIMOutputStream
import kotlinx.coroutines.runBlocking
import java.util.*

class NativeAudioPlayer(currentPosition: CurrentPosition, processor: AudioProcessor?, private val execFile: String,
                        private vararg val commands: String) : AudioPlayer(currentPosition, processor), Runnable {
    private var thread: Thread? = null
    private var process: Process? = null
    private var inputStream: EIMInputStream? = null
    private var outputStream: EIMOutputStream? = null
    private var bufferSize = 0
    private var channels = 2
    private var buffers = arrayOf(FloatArray(bufferSize), FloatArray(bufferSize))

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
        inputStream = EIMInputStream(isBigEndian, p.inputStream)
        outputStream = EIMOutputStream(isBigEndian, p.outputStream)

        p.onExit().thenAccept { process = null }
        process = p

        if (processor != null) processor!!.prepareToPlay()

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
            if (processor == null || process == null) {
                Thread.sleep(10)
                continue
            }

            buffers.forEach { Arrays.fill(it, 0F) }

            runBlocking {
                try {
                    processor?.processBlock(buffers, currentPosition)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

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