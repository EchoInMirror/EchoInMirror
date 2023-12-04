package com.eimsound.dsp.timestretcher.dsp

import kotlin.math.max
import kotlin.math.pow

class WaveformSimilarityBasedOverlapAdd {
    private var seekWindowLength = 0
    private var seekLength = 0
    private var overlapLength = 0

    private var pMidBuffer = FloatArray(0)
    private var pRefMidBuffer = FloatArray(0)
    private var outputFloatBuffer = FloatArray(0)

    private var sampleReq = 0

    fun setSpeedRatio(value: Double) {
        val nominalSkip = value * (seekWindowLength - overlapLength)
        val intskip = (nominalSkip + 0.5).toInt()
        sampleReq = (max((intskip + overlapLength).toDouble(), seekWindowLength.toDouble()) + seekLength).toInt()
    }

    fun applyNewParameters(sampleRate: Float, overlapMs: Int = 12, sequenceMs: Int = 82, seekWindowMs: Int = 28) {
        val oldOverlapLength = overlapLength
        overlapLength = (sampleRate * overlapMs / 1000).toInt()
        seekWindowLength = (sampleRate * sequenceMs / 1000).toInt()
        seekLength = (sampleRate * seekWindowMs / 1000).toInt()

        //pMidBuffer and pRefBuffer are initialized with 8 times the needed length to prevent a reset
        //of the arrays when overlapLength changes.
        if (overlapLength > oldOverlapLength * 8 && pMidBuffer.isEmpty()) {
            pMidBuffer = FloatArray(overlapLength * 8) //overlapLengthx2?
            pRefMidBuffer = FloatArray(overlapLength * 8) //overlapLengthx2?
        }
        val prevOutputBuffer = outputFloatBuffer
        outputFloatBuffer = FloatArray(outputBufferSize).apply {
            if (prevOutputBuffer.isNotEmpty()) {
                var i = 0
                while (i < prevOutputBuffer.size && i < size) {
                    this[i] = prevOutputBuffer[i]
                    i++
                }
            }
        }
    }

    val inputBufferSize get() = sampleReq

    val outputBufferSize get() = seekWindowLength - overlapLength


    /**
     * Overlaps the sample in output with the samples in input.
     * @param output The output buffer.
     * @param input The input buffer.
     */
    private fun overlap(output: FloatArray, input: FloatArray, inputOffset: Int) {
        for (i in 0 until overlapLength) {
            val itemp = overlapLength - i
            output[i] = (input[i + inputOffset] * i + pMidBuffer[i] * itemp) / overlapLength
        }
    }


    /**
     * Seeks for the optimal overlap-mixing position.
     *
     * The best position is determined as the position where the two overlapped
     * sample sequences are 'most alike', in terms of the highest
     * cross-correlation value over the overlapping period
     *
     * @param inputBuffer The input buffer
     * @return The best position.
     */
    private fun seekBestOverlapPosition(inputBuffer: FloatArray): Int {
        var bestOffset: Int
        var bestCorrelation: Double
        var currentCorrelation: Double
        var comparePosition: Int

        // Slopes the amplitude of the 'midBuffer' samples
        precalcCorrReferenceMono()
        bestCorrelation = -10.0
        bestOffset = 0

        // Scans for the best correlation value by testing each possible
        // position
        // over the permitted range.
        var tempOffset = 0
        while (tempOffset < seekLength) {
            comparePosition = tempOffset

            // Calculates correlation value for the mixing position
            // corresponding
            // to 'tempOffset'
            currentCorrelation = calcCrossCorr(pRefMidBuffer, inputBuffer, comparePosition)
            // heuristic rule to slightly favor values close to mid of the
            // range
            val tmp = (2 * tempOffset - seekLength).toDouble() / seekLength
            currentCorrelation = (currentCorrelation + 0.1) * (1.0 - 0.25 * tmp * tmp)

            // Checks for the highest correlation value
            if (currentCorrelation > bestCorrelation) {
                bestCorrelation = currentCorrelation
                bestOffset = tempOffset
            }
            tempOffset++
        }
        return bestOffset
    }

    /**
     * Slopes the amplitude of the 'midBuffer' samples so that cross correlation
     * is faster to calculate. Why is this faster?
     */
    fun precalcCorrReferenceMono() {
        for (i in 0 until overlapLength) {
            val temp = (i * (overlapLength - i)).toFloat()
            pRefMidBuffer[i] = pMidBuffer[i] * temp
        }
    }


    fun calcCrossCorr(mixingPos: FloatArray, compare: FloatArray, offset: Int): Double {
        var corr = 0.0
        var norm = 0.0
        for (i in 1 until overlapLength) {
            corr += (mixingPos[i] * compare[i + offset]).toDouble()
            norm += (mixingPos[i] * mixingPos[i]).toDouble()
        }
        // To avoid division by zero.
        if (norm < 1e-8) {
            norm = 1.0
        }
        return corr / norm.pow(0.5)
    }


    fun process(audioFloatBuffer: FloatArray): FloatArray {
//        assert(audioFloatBuffer.size == getInputBufferSize())

        //Search for the best overlapping position.
        val offset = seekBestOverlapPosition(audioFloatBuffer)

        // Mix the samples in the 'inputBuffer' at position of 'offset' with the
        // samples in 'midBuffer' using sliding overlapping
        // ... first partially overlap with the end of the previous sequence
        // (that's in 'midBuffer')
        overlap(outputFloatBuffer, audioFloatBuffer, offset)

        //copy sequence samples from input to output
        val sequenceLength = seekWindowLength - 2 * overlapLength
        System.arraycopy(audioFloatBuffer, offset + overlapLength, outputFloatBuffer, overlapLength, sequenceLength)

        // Copies the end of the current sequence from 'inputBuffer' to
        // 'midBuffer' for being mixed with the beginning of the next
        // processing sequence and so on
        System.arraycopy(audioFloatBuffer, offset + sequenceLength + overlapLength, pMidBuffer, 0, overlapLength)
//        assert(outputFloatBuffer!!.size == getOutputBufferSize())

        return outputFloatBuffer
    }


    /**
     * An object to encapsulate some of the parameters for
     * WSOLA, together with a couple of practical helper functions.
     *
     * @author Joren Six
     */
//    class Parameters
//    /**
//     * @param tempo
//     * The tempo change 1.0 means unchanged, 2.0 is + 100% , 0.5
//     * is half of the speed.
//     * @param sampleRate
//     * The sample rate of the audio 44.1kHz is common.
//     * @param newSequenceMs
//     * Length of a single processing sequence, in milliseconds.
//     * This determines to how long sequences the original sound
//     * is chopped in the time-stretch algorithm.
//     *
//     * The larger this value is, the lesser sequences are used in
//     * processing. In principle a bigger value sounds better when
//     * slowing down tempo, but worse when increasing tempo and
//     * vice versa.
//     *
//     * Increasing this value reduces computational burden and vice
//     * versa.
//     * @param newSeekWindowMs
//     * Seeking window length in milliseconds for algorithm that
//     * finds the best possible overlapping location. This
//     * determines from how wide window the algorithm may look for
//     * an optimal joining location when mixing the sound
//     * sequences back together.
//     *
//     * The bigger this window setting is, the higher the
//     * possibility to find a better mixing position will become,
//     * but at the same time large values may cause a "drifting"
//     * artifact because consequent sequences will be taken at
//     * more uneven intervals.
//     *
//     * If there's a disturbing artifact that sounds as if a
//     * constant frequency was drifting around, try reducing this
//     * setting.
//     *
//     * Increasing this value increases computational burden and
//     * vice versa.
//     * @param newOverlapMs
//     * Overlap length in milliseconds. When the chopped sound
//     * sequences are mixed back together, to form a continuous
//     * sound stream, this parameter defines over how long period
//     * the two consecutive sequences are let to overlap each
//     * other.
//     *
//     * This shouldn't be that critical parameter. If you reduce
//     * the DEFAULT_SEQUENCE_MS setting by a large amount, you
//     * might wish to try a smaller value on this.
//     *
//     * Increasing this value increases computational burden and
//     * vice versa.
//     */(
//        val tempo: Double,
//        val sampleRate: Double,
//        private val sequenceMs: Int,
//        private val seekWindowMs: Int,
//        private val overlapMs: Int
//    ) {
//
//        fun getOverlapMs(): Double {
//            return overlapMs.toDouble()
//        }
//
//        fun getSequenceMs(): Double {
//            return sequenceMs.toDouble()
//        }
//
//        fun getSeekWindowMs(): Double {
//            return seekWindowMs.toDouble()
//        }
//
//        companion object {
//            fun speechDefaults(tempo: Double, sampleRate: Double): Parameters {
//                val sequenceMs = 40
//                val seekWindowMs = 15
//                val overlapMs = 12
//                return Parameters(tempo, sampleRate, sequenceMs, seekWindowMs, overlapMs)
//            }
//
//            fun musicDefaults(tempo: Double, sampleRate: Double): Parameters {
//                val sequenceMs = 82
//                val seekWindowMs = 28
//                val overlapMs = 12
//                return Parameters(tempo, sampleRate, sequenceMs, seekWindowMs, overlapMs)
//            }
//
//            fun slowdownDefaults(tempo: Double, sampleRate: Double): Parameters {
//                val sequenceMs = 100
//                val seekWindowMs = 35
//                val overlapMs = 20
//                return Parameters(tempo, sampleRate, sequenceMs, seekWindowMs, overlapMs)
//            }
//
//            fun automaticDefaults(tempo: Double, sampleRate: Double): Parameters {
//                val tempoLow = 0.5 // -50% speed
//                val tempoHigh = 2.0 // +100% speed
//                val sequenceMsLow = 125.0 //ms
//                val sequenceMsHigh = 50.0 //ms
//                val sequenceK = (sequenceMsHigh - sequenceMsLow) / (tempoHigh - tempoLow)
//                val sequenceC = sequenceMsLow - sequenceK * tempoLow
//                val seekLow = 25.0 // ms
//                val seekHigh = 15.0 // ms
//                val seekK = (seekHigh - seekLow) / (tempoHigh - tempoLow)
//                val seekC = seekLow - seekK * seekLow
//                val sequenceMs = (sequenceC + sequenceK * tempo + 0.5).toInt()
//                val seekWindowMs = (seekC + seekK * tempo + 0.5).toInt()
//                val overlapMs = 12
//                return Parameters(tempo, sampleRate, sequenceMs, seekWindowMs, overlapMs)
//            }
//        }
//    }
}