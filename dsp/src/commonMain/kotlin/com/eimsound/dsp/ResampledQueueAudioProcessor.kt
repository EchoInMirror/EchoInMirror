package com.eimsound.dsp

import be.tarsos.dsp.resample.Resampler
import com.eimsound.dsp.data.AudioBufferQueue
import kotlin.math.ceil

class ResampledQueueAudioProcessor(
    private val channels: Int,
    var inputSampleRate: Int = 44100,
    var outputSampleRate: Int = 44100,
    private val getNextBlock: suspend () -> Array<FloatArray>
) {
    private val queue = AudioBufferQueue(channels, 8192)
    private val resamplers = Array(channels) { Resampler(true, 0.1, 4.0) }
    private var outputBuffers = emptyArray<FloatArray>()

    suspend fun process(buffers: Array<FloatArray>) {
        if (inputSampleRate == outputSampleRate && buffers.size == channels) {
            getNextBlock().forEachIndexed { index, floats ->
                floats.copyInto(buffers[index], 0, 0, floats.size)
            }
            return
        }

        val factor = inputSampleRate.toDouble() / outputSampleRate

        val inputSize = ceil(buffers[0].size * factor).toInt()

        while (queue.available < inputSize) {
            queue.push(getNextBlock())
        }

        if (outputBuffers.size != channels || outputBuffers[0].size != inputSize)
            outputBuffers = Array(channels) { FloatArray(inputSize) }

        queue.pop(outputBuffers)
        repeat(channels.coerceAtMost(buffers.size)) {
            val result = resamplers[it].process(factor, outputBuffers[it], 0, inputSize, false,
                buffers[it], 0, buffers[it].size)
            if (result.inputSamplesConsumed != inputSize) {
                queue.seekPopIndex(result.inputSamplesConsumed - inputSize)
            }
        }
    }
}