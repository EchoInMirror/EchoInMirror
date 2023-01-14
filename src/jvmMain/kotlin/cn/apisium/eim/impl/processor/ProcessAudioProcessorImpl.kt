package cn.apisium.eim.impl.processor

import cn.apisium.eim.EchoInMirror
import cn.apisium.eim.api.CurrentPosition
import cn.apisium.eim.api.processor.*
import cn.apisium.eim.utils.EIMInputStream
import cn.apisium.eim.utils.EIMOutputStream
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

val ProcessAudioProcessorDescription = EIMAudioProcessorDescription("ProcessAudioProcessor")

@Suppress("MemberVisibilityCanBePrivate")
open class ProcessAudioProcessorImpl(
    description: AudioProcessorDescription,
    factory: AudioProcessorFactory<*>,
) : ProcessAudioProcessor, AbstractAudioProcessor(description, factory) {
    override var inputChannelsCount = 0
        protected set
    override var outputChannelsCount = 0
        protected set
    override var isLaunched = false
        protected set
    override var name = "ProcessAudioProcessor"
    private var process: Process? = null
    private var prepared = false
    protected var inputStream: EIMInputStream? = null
    protected var outputStream: EIMOutputStream? = null
    protected val mutex = Mutex()

    override suspend fun processBlock(buffers: Array<FloatArray>, position: CurrentPosition, midiBuffer: ArrayList<Int>) {
        val output = outputStream
        if (!isLaunched || output == null) return
        if (!prepared) prepareToPlay(position.sampleRate, position.bufferSize)
        mutex.withLock {
            output.write(1)
            output.write(position.toFlags())
            output.writeDouble(position.bpm)
            output.writeLong(position.timeInSamples)
            val inputChannels = inputChannelsCount.coerceAtMost(buffers.size)
            val outputChannels = outputChannelsCount.coerceAtMost(buffers.size)
            output.write(inputChannels)
            output.write(outputChannels)
            output.writeShort((midiBuffer.size / 2).toShort())
            for (i in 0 until inputChannels) buffers[i].forEach(output::writeFloat)
            for (i in 0 until midiBuffer.size step 2) {
                output.writeInt(midiBuffer[i])
                output.writeShort(midiBuffer[i + 1].toShort())
            }
            output.flush()
            midiBuffer.clear()

            val input = inputStream!!
            do {
                val id = input.read()
                handleInput(id)
            } while (id != 1)
            for (i in 0 until outputChannels) {
                for (j in 0 until buffers[i].size) {
                    buffers[i][j] = input.readFloat()
                }
            }
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    override fun onClick() {
        if (!isLaunched) return
        GlobalScope.launch {
            mutex.withLock {
                outputStream?.apply {
                    write(2)
                    flush()
                }
            }
        }
    }

    override fun prepareToPlay(sampleRate: Int, bufferSize: Int) {
        prepared = true
        val output = outputStream ?: return
        output.write(0)
        output.writeInt(sampleRate)
        output.writeInt(bufferSize)
        output.flush()
    }

    override suspend fun launch(execFile: String, vararg commands: String): Boolean {
        if (isLaunched) return true
        return withContext(Dispatchers.IO) {
            val args = ArrayList<String>()
            EchoInMirror.windowManager.mainWindow?.windowHandle?.let {
                args.add("-H")
                args.add(it.toString())
            }
            args.addAll(commands)
            val pb = ProcessBuilder(execFile, *args.toTypedArray())

            pb.redirectOutput(ProcessBuilder.Redirect.INHERIT)
            val p = pb.start()

            val flag1 = p.errorStream.read() == 1
            val flag2 = p.errorStream.read() == 2
            val isBigEndian = flag1 && flag2
            val input = EIMInputStream(isBigEndian, p.errorStream)

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
            inputStream?.close()
            outputStream?.close()
            process!!.destroyForcibly()
            process = null
            inputStream = null
            outputStream = null
            isLaunched = false
        }
    }

    private fun handleInput(id: Int) {
        val input = inputStream!!
        when (id) {
            2 -> println(input.read()) // transport play
            3 -> {
                val index = input.readInt()
                val value = input.readFloat()
                println("$index $value")
            }
        }
    }
}