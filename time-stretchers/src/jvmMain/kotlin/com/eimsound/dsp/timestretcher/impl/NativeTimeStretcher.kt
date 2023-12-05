package com.eimsound.dsp.timestretcher.impl

import com.eimsound.dsp.timestretcher.AbstractTimeStretcher
import com.eimsound.dsp.timestretcher.TimeStretcher
import com.eimsound.dsp.timestretcher.TimeStretcherFactory
import java.lang.foreign.*
import java.lang.invoke.MethodHandle
import java.lang.ref.Cleaner
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.exists

private lateinit var get_all_time_stretchers: MethodHandle // () -> char*
private lateinit var create_time_stretcher: MethodHandle // (char*) -> void*
private lateinit var destroy_time_stretcher: MethodHandle // (void*) -> void
private lateinit var time_stretcher_process: MethodHandle // (void*, float**, float**, int) -> int
private lateinit var time_stretcher_reset: MethodHandle // (void*) -> void
private lateinit var time_stretcher_flush: MethodHandle // (void*, float**) -> int
private lateinit var time_stretcher_set_speed_ratio: MethodHandle // (void*, float) -> void
private lateinit var time_stretcher_set_semitones: MethodHandle // (void*, float) -> void
private lateinit var time_stretcher_get_max_frames_needed: MethodHandle // (void*) -> int
private lateinit var time_stretcher_get_frames_needed: MethodHandle // (void*) -> int
private lateinit var time_stretcher_is_initialized: MethodHandle // (void*) -> bool
private lateinit var time_stretcher_initialise: MethodHandle // (void*, float, int, int, bool) -> void
private lateinit var time_stretcher_is_planar: MethodHandle // (void*) -> bool

private fun init(file: Path) {
    if (::get_all_time_stretchers.isInitialized) return
    val session = MemorySession.openImplicit()
    val linker = Linker.nativeLinker()
    val lib = SymbolLookup.libraryLookup(file, session)
    get_all_time_stretchers = linker.downcallHandle(
        lib.lookup("get_all_time_stretchers").orElseThrow(),
        FunctionDescriptor.of(
            ValueLayout.ADDRESS
        )
    )
    create_time_stretcher = linker.downcallHandle(
        lib.lookup("create_time_stretcher").orElseThrow(),
        FunctionDescriptor.of(
            ValueLayout.ADDRESS,
            ValueLayout.ADDRESS
        )
    )
    destroy_time_stretcher = linker.downcallHandle(
        lib.lookup("destroy_time_stretcher").orElseThrow(),
        FunctionDescriptor.ofVoid(
            ValueLayout.ADDRESS
        )
    )
    time_stretcher_process = linker.downcallHandle(
        lib.lookup("time_stretcher_process").orElseThrow(),
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT,
            ValueLayout.ADDRESS,
            ValueLayout.ADDRESS,
            ValueLayout.ADDRESS,
            ValueLayout.JAVA_INT
        )
    )
    time_stretcher_reset = linker.downcallHandle(
        lib.lookup("time_stretcher_reset").orElseThrow(),
        FunctionDescriptor.ofVoid(
            ValueLayout.ADDRESS
        )
    )
    time_stretcher_flush = linker.downcallHandle(
        lib.lookup("time_stretcher_flush").orElseThrow(),
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT,
            ValueLayout.ADDRESS,
            ValueLayout.ADDRESS
        )
    )
    time_stretcher_set_speed_ratio = linker.downcallHandle(
        lib.lookup("time_stretcher_set_speed_ratio").orElseThrow(),
        FunctionDescriptor.ofVoid(
            ValueLayout.ADDRESS,
            ValueLayout.JAVA_FLOAT
        )
    )
    time_stretcher_set_semitones = linker.downcallHandle(
        lib.lookup("time_stretcher_set_semitones").orElseThrow(),
        FunctionDescriptor.ofVoid(
            ValueLayout.ADDRESS,
            ValueLayout.JAVA_FLOAT
        )
    )
    time_stretcher_get_max_frames_needed = linker.downcallHandle(
        lib.lookup("time_stretcher_get_max_frames_needed").orElseThrow(),
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT,
            ValueLayout.ADDRESS
        )
    )
    time_stretcher_get_frames_needed = linker.downcallHandle(
        lib.lookup("time_stretcher_get_frames_needed").orElseThrow(),
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT,
            ValueLayout.ADDRESS
        )
    )
    time_stretcher_is_initialized = linker.downcallHandle(
        lib.lookup("time_stretcher_is_initialized").orElseThrow(),
        FunctionDescriptor.of(
            ValueLayout.JAVA_BOOLEAN,
            ValueLayout.ADDRESS
        )
    )
    time_stretcher_initialise = linker.downcallHandle(
        lib.lookup("time_stretcher_initialise").orElseThrow(),
        FunctionDescriptor.ofVoid(
            ValueLayout.ADDRESS,
            ValueLayout.JAVA_FLOAT,
            ValueLayout.JAVA_INT,
            ValueLayout.JAVA_INT,
            ValueLayout.JAVA_BOOLEAN
        )
    )
    time_stretcher_is_planar = linker.downcallHandle(
        lib.lookup("time_stretcher_is_planar").orElseThrow(),
        FunctionDescriptor.of(
            ValueLayout.JAVA_BOOLEAN,
            ValueLayout.ADDRESS
        )
    )
}

private class NativeTimeStretcher(name: String) : AbstractTimeStretcher(name) {
    private val cleaner = Cleaner.create().apply { register(this, ::close) }
    private val session = MemorySession.openShared(cleaner)

    private val pointer: Addressable =
        create_time_stretcher.invokeExact(session.allocateUtf8String(name) as Addressable) as MemoryAddress
    private lateinit var inputBuffersPtr: Addressable
    private lateinit var outputBuffersPtr: Addressable
    private lateinit var inputBuffers: FloatBuffer
    private lateinit var outputBuffers: FloatBuffer
    private var inputBufferSize = 0
    private var isPlanar = false

    override var speedRatio = 1F
        set(value) {
            if (value == field) return
            time_stretcher_set_speed_ratio.invokeExact(pointer, value)
            field = value
        }
    override var semitones = 0F
        set(value) {
            if (value == field) return
            time_stretcher_set_semitones.invokeExact(pointer, value)
            field = value
        }
    override val maxFramesNeeded
        get() = time_stretcher_get_max_frames_needed.invokeExact(pointer) as Int
    override val framesNeeded
        get() = time_stretcher_get_frames_needed.invokeExact(pointer) as Int

    override fun initialise(sourceSampleRate: Float, samplesPerBlock: Int, numChannels: Int, isRealtime: Boolean) {
        super.initialise(sourceSampleRate, samplesPerBlock, numChannels, isRealtime)
        time_stretcher_initialise.invokeExact(pointer, sourceSampleRate, samplesPerBlock, numChannels, isRealtime)
        outputBuffersPtr = session.allocateArray(ValueLayout.JAVA_FLOAT, samplesPerBlock.toLong() * numChannels)
            .apply { outputBuffers = asByteBuffer().order(ByteOrder.nativeOrder()).asFloatBuffer() }
        isPlanar = time_stretcher_is_planar.invokeExact(pointer) as Boolean
    }

    override fun process(input: Array<FloatArray>, output: Array<FloatArray>, numSamples: Int): Int {
        if (!isInitialised) return 0
        if (inputBufferSize < numSamples * numChannels) {
            inputBufferSize = numSamples.coerceAtLeast(maxFramesNeeded) * numChannels
            inputBuffersPtr = session.allocateArray(ValueLayout.JAVA_FLOAT, inputBufferSize.toLong())
                .apply { inputBuffers = asByteBuffer().order(ByteOrder.nativeOrder()).asFloatBuffer() }
        }

        inputBuffers.rewind()
        if (isPlanar) repeat(numChannels) { i -> inputBuffers.put(input[i], 0, numSamples) }
        else repeat(numSamples) { i -> repeat(numChannels) { j -> inputBuffers.put(input[j][i]) } }

        return (time_stretcher_process.invokeExact(
            pointer, inputBuffersPtr, outputBuffersPtr, numSamples
        ) as Int).apply { readOutput(output, this) }
    }

    override fun flush(output: Array<FloatArray>): Int {
        if (!isInitialised) return 0
        return (time_stretcher_flush.invokeExact(pointer, outputBuffersPtr) as Int).apply {
            readOutput(output, this)
        }
    }

    override fun reset() {
        if (!isInitialised) return
        time_stretcher_reset.invokeExact(pointer)
    }

    override fun close() {
        destroy_time_stretcher.invokeExact(pointer)
        session.close()
    }

    private fun readOutput(output: Array<FloatArray>, numSamples: Int) {
        outputBuffers.rewind()
        if (isPlanar) repeat(numChannels) { i -> outputBuffers.get(output[i], 0, numSamples) }
        else repeat(numSamples) { i -> repeat(numChannels) { j -> output[j][i] = outputBuffers.get() } }
    }

    override fun toString() = "NativeTimeStretcher(name=$name, speedRatio=$speedRatio, semitones=$semitones)"
}

class NativeTimeStretcherFactory : TimeStretcherFactory {
    override val timeStretchers: List<String>

    init {
        val str = System.getProperty("eim.dsp.timestretchers.library.file")
        var list: List<String>? = null
        if (str?.isNotEmpty() == true) {
            val file = Path(str)
            if (file.exists()) {
                init(file)
                list = (get_all_time_stretchers.invokeExact() as MemoryAddress)
                    .getUtf8String(0L).split(",")
            }
        }
        timeStretchers = list ?: emptyList()
    }

    override fun createTimeStretcher(name: String): TimeStretcher? =
        if (name in timeStretchers) NativeTimeStretcher(name) else null
}
