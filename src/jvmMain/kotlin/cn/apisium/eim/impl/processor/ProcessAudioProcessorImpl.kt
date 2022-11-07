package cn.apisium.eim.impl.processor

import cn.apisium.eim.EchoInMirror
import cn.apisium.eim.api.CurrentPosition
import cn.apisium.eim.api.processor.FailedToLoadAudioPluginException
import cn.apisium.eim.api.processor.ProcessAudioProcessor
import cn.apisium.eim.utils.EIMInputStream
import cn.apisium.eim.utils.EIMOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Suppress("MemberVisibilityCanBePrivate")
open class ProcessAudioProcessorImpl(
    protected vararg val commands: String
) : ProcessAudioProcessor {
    override var inputChannelsCount = 0
        protected set
    override var outputChannelsCount = 0
        protected set
    override var isLaunched = false
        protected set
    override var name = "ProcessAudioProcessor"
    private var process: Process? = null
    private var inputStream: EIMInputStream? = null
    private var outputStream: EIMOutputStream? = null

    override suspend fun processBlock(buffers: Array<FloatArray>, position: CurrentPosition, midiBuffer: ArrayList<Byte>?) {
        if (!isLaunched) return
        withContext(Dispatchers.IO) {
            val output = outputStream!!
            output.write(1)
            output.writeBoolean(position.isPlaying)
            output.writeDouble(position.bpm)
            output.writeLong(position.timeInSamples)
            val inputChannels = inputChannelsCount.coerceAtMost(buffers.size)
            val outputChannels = outputChannelsCount.coerceAtMost(buffers.size)
            output.write(inputChannels)
            output.write(outputChannels)
            for (i in 0 until inputChannels) buffers[i].forEach(output::writeFloat)
            output.flush()

            val input = inputStream!!
            input.read()
            for (i in 0 until outputChannels) {
                for (j in 0 until buffers[i].size) {
                    buffers[i][j] = input.readFloat()
                }
            }
        }
    }

    override fun prepareToPlay() {
        val output = outputStream ?: return
        output.write(0)
        output.writeInt(EchoInMirror.sampleRate.toInt())
        output.writeInt(EchoInMirror.bufferSize)
        output.flush()
    }

    override suspend fun launch(): Boolean {
        if (isLaunched) return true
        return withContext(Dispatchers.IO) {
            val pb = ProcessBuilder(*commands)

            pb.redirectError(ProcessBuilder.Redirect.INHERIT)
            val p = pb.start()

            val flag1 = p.inputStream.read() == 1
            val flag2 = p.inputStream.read() == 2
            val isBigEndian = flag1 && flag2
            val input = EIMInputStream(isBigEndian, p.inputStream)

            val id = input.read()
            if (id == 127) {
                throw FailedToLoadAudioPluginException(input.readString())
            }
            if (id != 0) {
                p.destroy()
                throw FailedToLoadAudioPluginException("Failed to load plugin")
            }

            p.onExit().thenAccept {
                isLaunched = false
                process = null
            }

            try {
                val output = EIMOutputStream(isBigEndian, p.outputStream)
                inputChannelsCount = input.readInt()
                outputChannelsCount = input.readInt()

                process = p
                inputStream = input
                outputStream = output
                isLaunched = true
            } catch (e: Throwable) {
                p.destroy()
                throw e
            }
            true
        }
    }

    override fun close() {
        if (process != null) {
            process!!.destroyForcibly()
            process = null
            inputStream = null
            outputStream = null
            isLaunched = false
        }
    }
}