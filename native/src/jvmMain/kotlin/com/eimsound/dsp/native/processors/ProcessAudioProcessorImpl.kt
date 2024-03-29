package com.eimsound.dsp.native.processors

import cn.apisium.shm.SharedMemory
import com.eimsound.audioprocessor.*
import com.eimsound.daw.utils.ByteBufInputStream
import com.eimsound.daw.utils.ByteBufOutputStream
import com.eimsound.daw.utils.randomId
import com.eimsound.dsp.native.IS_SHM_SUPPORTED
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.*
import kotlinx.coroutines.selects.onTimeout
import kotlinx.coroutines.selects.select
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.nio.ByteBuffer
import java.nio.file.Path
import java.util.*
import kotlin.io.path.absolutePathString

//val ProcessAudioProcessorDescription = DefaultAudioProcessorDescription("ProcessAudioProcessor",
//    "ProcessAudioProcessor", null, "EIMSound", "0.0.0", true)


private const val PARAMETER_IS_AUTOMATABLE = 0b00001
private const val PARAMETER_IS_DISCRETE = 0b00010
private const val PARAMETER_IS_BOOLEAN = 0b00100
@Suppress("unused")
private const val PARAMETER_IS_META = 0b01000
@Suppress("unused")
private const val PARAMETER_IS_ORIENTATION_INVERTED = 0b10000

private val logger = KotlinLogging.logger {  }
@Suppress("MemberVisibilityCanBePrivate")
open class ProcessAudioProcessorImpl(
    override val description: NativeAudioPluginDescription,
    override val factory: NativeAudioPluginFactoryImpl,
    private var enabledSharedMemory: Boolean = IS_SHM_SUPPORTED &&
            System.getProperty("eim.dsp.nativeaudioplugins.sharedmemory", "1") != "false",
) : ProcessAudioProcessor, AbstractAudioProcessor(description, factory, false, AudioProcessorState.Loading) {
    override var inputChannelsCount = 0
        protected set
    override var outputChannelsCount = 0
        protected set
    override var isLaunched = false
        protected set
    override var name = "ProcessAudioProcessor"
    final override var parameters = emptyList<SimpleAudioProcessorParameter>()
        private set
    private var process: Process? = null
    private var prepared = false
    private var sharedMemory: SharedMemory? = null
    private var byteBuffer: ByteBuffer? = null
    private val shmId = randomId()
    private val shmName
        get() = if (enabledSharedMemory) "EIM-$shmId-$id" else ""
    protected var inputStream: ByteBufInputStream? = null
    protected var outputStream: ByteBufOutputStream? = null
    protected val writeMutex = Mutex()
    protected val modifiedParameterMutex = Mutex()
    private val modifiedParameter = hashSetOf<Int>()

    override suspend fun processBlock(buffers: Array<FloatArray>, position: PlayPosition, midiBuffer: ArrayList<Int>) {
        withContext(Dispatchers.IO) {
            val output = outputStream
            if (!isLaunched || output == null) return@withContext
            if (!prepared) prepareToPlay(position.sampleRate, position.bufferSize)
            writeMutex.withLock {
                val bufferSize = position.bufferSize
                output.write(1) // action
                output.write(position.toFlags()) // flags
                output.writeDouble(position.bpm) // bpm
                output.writeShort((midiBuffer.size / 2).toShort()) // midi event count
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

                modifiedParameterMutex.withLock {
                    val size = modifiedParameter.size
                    output.writeVarInt(size)
                    if (size > 0) {
                        modifiedParameter.forEach { id ->
                            if (id in parameters.indices) {
                                output.writeVarInt(id)
                                output.writeFloat(parameters[id].value)
                            } else {
                                output.writeVarInt(9999999)
                                output.writeFloat(0F)
                            }
                        }
                        modifiedParameter.clear()
                    }
                }

                output.flush()
                midiBuffer.clear()

                if (handleInputLoop(position)) return@withContext
                if (bf == null) {
                    val input = inputStream!!
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
    }

    @OptIn(DelicateCoroutinesApi::class)
    override fun onClick() {
        if (!isLaunched) return
        GlobalScope.launch(Dispatchers.IO) {
            writeMutex.withLock {
                outputStream?.apply {
                    write(2)
                    flush()
                }
            }
        }
    }

    override suspend fun prepareToPlay(sampleRate: Int, bufferSize: Int) = withContext(Dispatchers.IO) {
        writeMutex.withLock {
            prepared = true
            val output = outputStream ?: return@withLock
            output.write(0)
            output.writeInt(sampleRate)
            output.writeInt(bufferSize)
            var newSize = 0
            if (enabledSharedMemory && IS_SHM_SUPPORTED) {
                val size = bufferSize * inputChannelsCount.coerceAtLeast(outputChannelsCount) * 4
                if (sharedMemory?.size != size) {
                    sharedMemory?.close()
                    try {
                        sharedMemory = SharedMemory.create(shmName, size)
                        byteBuffer = sharedMemory!!.toByteBuffer()
                        newSize = size
                    } catch (e: Throwable) {
                        logger.warn(e) { "Failed to create shared memory: $shmName" }
                        sharedMemory = null
                        byteBuffer = null
                    }
                }
            }
            if (sharedMemory == null) output.write(0)
            else {
                output.write(1)
                output.writeString(shmName)
                output.writeInt(newSize)
            }
            output.flush()
        }
    }

    override suspend fun launch(execFile: String, preset: String?, vararg commands: String): Boolean = coroutineScope {
        if (isLaunched) return@coroutineScope true
        state = AudioProcessorState.Loading
        val res = try {
            select {
                var ret = false
                launch {
                    withContext(Dispatchers.IO) {
                        val args = ArrayList<String>()
                        val handler = System.getProperty("eim.window.handler", "")
                        if (handler.isNotEmpty()) {
                            args.add("-H")
                            args.add(handler)
                        }
                        args.addAll(commands)
                        val pb = if (preset == null) ProcessBuilder(execFile, "-L", "#", *args.toTypedArray())
                        else ProcessBuilder(execFile, "-L", "#", "-P", "#", *args.toTypedArray())

                        pb.redirectError(ProcessBuilder.Redirect.INHERIT)
                        val p = pb.start()

                        val flag1 = p.inputStream.read() == 1
                        val flag2 = p.inputStream.read() == 2
                        val isBigEndian = flag1 && flag2
                        val output = ByteBufOutputStream(isBigEndian, p.outputStream)

                        output.writeString(Json.encodeToString(description))
                        if (preset != null) output.writeString(preset)
                        output.flush()

                        val input = ByteBufInputStream(isBigEndian, p.inputStream)
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
                            prepared = false
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
                        ret = true
                    }
                }.onJoin {
                    state = AudioProcessorState.Ready
                    ret
                }
            }
        } catch (e: Throwable) {
            state = AudioProcessorState.Failed
            close()
            false
        }
        coroutineContext.cancelChildren()
        return@coroutineScope res
    }

    private fun readInitInformation(input: ByteBufInputStream) {
        inputChannelsCount = input.read()
        outputChannelsCount = input.read()
        latency = input.readInt()
        val len = input.readVarInt()

        val newList = List(len) {
            val flags = input.read()
            val value = input.readFloat()
            val initialValue = input.readFloat()
            input.readInt() // category
            val steps = input.readInt().coerceAtMost(65536)
            val name = input.readString()
            val label = input.readString()
            var valueStrings = input.readStringArray()
            if (valueStrings.isEmpty() && flags and PARAMETER_IS_DISCRETE != 0) {
                valueStrings = Array(steps) { s -> "${(s.toFloat() / steps * 100).toInt()}%" }
            }

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
            }.apply {
                setValue(value, false)
            }
        }

        if (parameters.isEmpty()) parameters = newList // TODO: replace this in the future
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
            state = if (prepared) AudioProcessorState.Unloaded else AudioProcessorState.Failed
            prepared = false
        }
    }

    private fun handleInputLoop(position: PlayPosition): Boolean {
        val input = inputStream!!
        do {
            val id = input.read()
            if (id == -1) {
                close()
                logger.warn { "Native audio plugin $description has been closed" }
                return true
            }
            handleInput(id, position)
        } while (id != 1)
        return false
    }
    private fun handleInput(id: Int, position: PlayPosition) {
        val input = inputStream!!
        when (id) {
            0 -> readInitInformation(input) // init
            2 -> position.isPlaying = input.readBoolean() // transport play
            3 -> { // parameter change
                val list = mutableListOf<Pair<AudioProcessorParameter, Float>>()
                repeat(input.readVarInt()) {
                    val index = input.readVarInt()
                    val value = input.readFloat()
                    if (index > 0 && index < parameters.size) {
                        val cur = parameters[index]
                        lastModifiedParameter = cur
                        list.add(Pair(cur, value))
                    }
                }
                list.doChangeAction(false)
            }
            4 -> latency = input.readInt() // latency
            5 -> isDisabled = input.readBoolean() // is disabled
        }
    }

    override suspend fun processBlockBypass(position: PlayPosition) {
        withContext(Dispatchers.IO) {
            val output = outputStream
            if (!isLaunched || output == null) return@withContext
            if (!prepared) prepareToPlay(position.sampleRate, position.bufferSize)
            writeMutex.withLock {
                output.write(5) // action
                output.flush()
                handleInputLoop(position)
            }
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    private fun handleParameterChange(p: AudioProcessorParameter) {
        GlobalScope.launch(Dispatchers.IO) {
            modifiedParameterMutex.withLock {
                modifiedParameter.add(p.id.toInt())
            }
        }
    }

    override suspend fun restore(path: Path) {
        super.restore(path)
        launch(factory.getNativeHostPath(description), path.resolve("state.bin").absolutePathString())
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override suspend fun store(path: Path) = coroutineScope {
        super.store(path)
        if (!isLaunched) return@coroutineScope
        select {
            onTimeout(5000) { close() }
            launch {
                writeMutex.withLock {
                    outputStream?.apply {
                        write(3)
                        writeString("$path/state.bin")
                        flush()
                        if (inputStream?.read() != 1) {
                            logger.warn { "Failed to store native audio plugin $description to $path/state.bin" }
                        }
                    }
                }
            }.onJoin { }
        }
        coroutineContext.cancelChildren()
    }
}
