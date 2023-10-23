package com.eimsound.dsp.native.processors

import cn.apisium.shm.SharedMemory
import com.eimsound.audioprocessor.*
import com.eimsound.daw.utils.ByteBufInputStream
import com.eimsound.daw.utils.ByteBufOutputStream
import com.eimsound.dsp.native.IS_SHM_SUPPORTED
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.nio.ByteBuffer
import java.util.*

//val ProcessAudioProcessorDescription = DefaultAudioProcessorDescription("ProcessAudioProcessor",
//    "ProcessAudioProcessor", null, "EIMSound", "0.0.0", true)


private const val PARAMETER_IS_AUTOMATABLE = 0b00001
private const val PARAMETER_IS_DISCRETE = 0b00010
private const val PARAMETER_IS_BOOLEAN = 0b00100
@Suppress("unused")
private const val PARAMETER_IS_META = 0b01000
@Suppress("unused")
private const val PARAMETER_IS_ORIENTATION_INVERTED = 0b10000

@Suppress("MemberVisibilityCanBePrivate")
open class ProcessAudioProcessorImpl(
    description: AudioProcessorDescription,
    factory: AudioProcessorFactory<*>,
    private var enabledSharedMemory: Boolean = IS_SHM_SUPPORTED &&
            System.getProperty("eim.dsp.nativeaudioplugins.sharedmemory", "1") != "false",
) : ProcessAudioProcessor, AbstractAudioProcessor(description, factory, false) {
    override var inputChannelsCount = 0
        protected set
    override var outputChannelsCount = 0
        protected set
    override var isLaunched = false
        protected set
    override var name = "ProcessAudioProcessor"
    final override var parameters = emptyList<AudioProcessorParameter>()
        private set
    private var process: Process? = null
    private var prepared = false
    private var sharedMemory: SharedMemory? = null
    private var byteBuffer: ByteBuffer? = null
    private val shmName = if (enabledSharedMemory) "EIM-NativePlayer-${UUID.randomUUID()}" else ""
    protected var inputStream: ByteBufInputStream? = null
    protected var outputStream: ByteBufOutputStream? = null
    protected val mutex = Mutex()
    private val parametersToId = hashMapOf<IAudioProcessorParameter, Int>()
    private val modifiedParameter = hashSetOf<IAudioProcessorParameter>()

    override suspend fun processBlock(buffers: Array<FloatArray>, position: CurrentPosition, midiBuffer: ArrayList<Int>) {
        val output = outputStream
        if (!isLaunched || output == null) return
        if (!prepared) prepareToPlay(position.sampleRate, position.bufferSize)
        mutex.withLock {
            val bufferSize = position.bufferSize
            output.write(1) // action
            output.write(position.toFlags()) // flags
            output.writeDouble(position.bpm) // bpm
            output.writeShort((midiBuffer.size / 2).toShort()) // midi event count
            output.writeShort(modifiedParameter.size.toShort()) // parameter count
            output.writeVarLong(position.timeInSamples) // time in samples
            val inputChannels = inputChannelsCount.coerceAtMost(buffers.size)
            val outputChannels = outputChannelsCount.coerceAtMost(buffers.size)
            val bf = byteBuffer
            if (bf == null) {
                output.write(inputChannels)
                output.write(outputChannels)
                repeat(inputChannels) { i ->
                    repeat(bufferSize) { j -> output.writeFloat(buffers[i][j]) }
                }
            } else {
                repeat(inputChannels) { i ->
                    repeat(bufferSize) { j -> bf.putFloat(buffers[i][j]) }
                }
                bf.rewind()
            }
            for (i in 0 until midiBuffer.size step 2) {
                output.writeVarInt(midiBuffer[i])
                output.writeShort(midiBuffer[i + 1].toShort())
            }
            modifiedParameter.forEach { p ->
                output.writeVarInt(parametersToId[p] ?: 9999999)
                output.writeFloat(p.value)
            }
            modifiedParameter.clear()
            output.flush()
            midiBuffer.clear()

            val input = inputStream!!
            do {
                val id = input.read()
                handleInput(id, position)
            } while (id != 1)
            if (bf == null) {
                repeat(outputChannels) { i ->
                    repeat(bufferSize) { j -> buffers[i][j] = input.readFloat() }
                }
            } else {
                repeat(outputChannels) { i ->
                    repeat(bufferSize) { j -> buffers[i][j] = bf.float }
                }
                bf.rewind()
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
        var newSize = 0
        if (enabledSharedMemory && IS_SHM_SUPPORTED) {
            val size = bufferSize * inputChannelsCount.coerceAtLeast(outputChannelsCount) * 4
            sharedMemory?.let {
                if (it.size != size) {
                    it.close()
                    try {
                        sharedMemory = SharedMemory.create(shmName, size)
                        byteBuffer = sharedMemory!!.toByteBuffer()
                        newSize = size
                    } catch (ignored: Throwable) {
                        sharedMemory = null
                        byteBuffer = null
                    }
                }
            }
        }
        if (sharedMemory == null) output.write(0)
        else {
            output.write(1)
            output.writeInt(newSize)
        }
        output.flush()
    }

    override suspend fun launch(execFile: String, preset: String?, vararg commands: String): Boolean {
        if (isLaunched) return true
        return withContext(Dispatchers.IO) {
            val args = ArrayList<String>()
            val handler = System.getProperty("eim.window.handler", "")
            if (handler.isNotEmpty()) {
                args.add("-H")
                args.add(handler)
            }
            if (enabledSharedMemory && IS_SHM_SUPPORTED) {
                args.add("-M")
                args.add(shmName)
            }
            args.addAll(commands)
            val pb = if (preset == null) ProcessBuilder(execFile, "-L", "#", *args.toTypedArray())
            else ProcessBuilder(execFile, "-L", "#", "-P", "#", *args.toTypedArray())

            pb.redirectError(ProcessBuilder.Redirect.INHERIT)
            val p = pb.start()

            val flag1 = p.inputStream.read() == 1
            val flag2 = p.inputStream.read() == 2
            val isBigEndian = flag1 && flag2
            val input = ByteBufInputStream(isBigEndian, p.inputStream)
            val output = ByteBufOutputStream(isBigEndian, p.outputStream)

            output.writeString(Json.encodeToString(description))
            if (preset != null) output.writeString(preset)
            output.flush()

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
                readInitInformation(input)

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

    private suspend fun readInitInformation(input: ByteBufInputStream) {
        withContext(Dispatchers.IO) {
            inputChannelsCount = input.read()
            outputChannelsCount = input.read()
            val len = input.readVarInt()

            parametersToId.clear()
            parameters = List(len) {
                val flags = input.read()
                val initialValue = input.readFloat()
                input.readInt() // category
                val name = input.readString()
                val label = input.readString()
                val valueStrings = input.readStringArray()

                val isAutomatable = flags and PARAMETER_IS_AUTOMATABLE != 0
                val id = it.toString()

                if (flags and PARAMETER_IS_BOOLEAN != 0) {
                    audioProcessorParameterOf(id, name, initialValue != 0F, isAutomatable = isAutomatable,
                        onChange = ::handleParameterChange)
                } else if (flags and PARAMETER_IS_DISCRETE != 0) {
                    audioProcessorParameterOf(id, name, valueStrings, label = label, isAutomatable = isAutomatable,
                        onChange = ::handleParameterChange)
                } else {
                    audioProcessorParameterOf(id, name, initialValue = initialValue, label = label,
                        isAutomatable = isAutomatable, onChange = ::handleParameterChange)
                }.apply { parametersToId[this] = it }
            }
        }
    }

    override fun close() {
        if (sharedMemory != null) {
            byteBuffer = null
            sharedMemory!!.close()
            sharedMemory = null
        }
        if (process != null) {
            inputStream?.close()
            outputStream?.close()
            process!!.destroyForcibly()
            process = null
            inputStream = null
            outputStream = null
            isLaunched = false
        }
        super.close()
    }

    private fun handleInput(id: Int, position: CurrentPosition) {
        val input = inputStream!!
        when (id) {
            2 -> position.isPlaying = input.readBoolean() // transport play
            3 -> { // parameter change
                val index = input.readVarInt()
                val value = input.readFloat()
                if (index > 0 && index < parameters.size) {
                    val cur = parameters[index]
                    lastModifiedParameter = cur
                    cur.doChangeAction(value, false)
                }
            }
        }
    }

    private fun handleParameterChange(p: IAudioProcessorParameter) { modifiedParameter.add(p) }
}