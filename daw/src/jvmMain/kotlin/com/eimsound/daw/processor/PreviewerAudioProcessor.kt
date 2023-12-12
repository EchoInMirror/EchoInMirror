package com.eimsound.daw.processor

import com.eimsound.audioprocessor.*
import com.eimsound.audiosources.*
import com.eimsound.daw.dawutils.processMIDIBuffer
import com.eimsound.dsp.data.midi.*
import com.eimsound.daw.impl.PlayPositionImpl
import com.eimsound.daw.impl.processor.eimAudioProcessorFactory
import com.eimsound.daw.processor.synthesizer.SineWaveSynthesizer

val PreviewerDescription = DefaultAudioProcessorDescription(
    "Previewer",
    "Previewer",
    manufacturerName = "EchoInMirror",
    isInstrument = true
)

class PreviewerAudioProcessor(factory: AudioProcessorFactory<*>) : AbstractAudioProcessor(PreviewerDescription, factory) {
    private var midiPreviewTarget: List<NoteMessage>? = null
    private var currentIndex = -1
    private val noteRecorder = MidiNoteTimeRecorder()
    private val sineWaveSynthesizer =
        SineWaveSynthesizer(AudioProcessorManager.instance.eimAudioProcessorFactory, true, 0.5F)
    private var audioPreviewTarget: ResampledAudioSource? = null
    private var tempBuffers = Array(2) { FloatArray(1024) }
    private var fileSampleRate = 0F

    var volume by sineWaveSynthesizer::volume
    val position = PlayPositionImpl(this).apply {
        isPlaying = true
        isProjectLooping = false
    }
    
    var playPosition
        get() = position.timeInPPQ.toDouble() / position.projectRange.last
        set(value) {
            position.timeInPPQ = (value * position.projectRange.last).toInt()
            audioPreviewTarget?.position = position.convertPPQToSamples(position.timeInPPQ)
        }

    override suspend fun processBlock(buffers: Array<FloatArray>, position: PlayPosition, midiBuffer: ArrayList<Int>) {
        if (position.bpm != this.position.bpm) this.position.bpm = position.bpm
        if (!this.position.isPlaying) return
        val notes = midiPreviewTarget
        val audio = audioPreviewTarget
        if (notes != null) {
            val midiBuffer2 = ArrayList<Int>()
            noteRecorder.processBlock(this.position.bufferSize, midiBuffer2)
            val timeInSamples = this.position.timeInSamples
            currentIndex = processMIDIBuffer(
                notes, this.position, midiBuffer2, 0, timeInSamples, noteRecorder, currentIndex
            )
            sineWaveSynthesizer.processBlock(buffers, this.position, midiBuffer2)
            this.position.timeInSamples += position.bufferSize
        } else if (audio != null) {
            audio.factor = position.sampleRate.toDouble() / fileSampleRate
            audio.nextBlock(tempBuffers, this.position.bufferSize)
            buffers.mixWith(tempBuffers, volume, 1F)
            this.position.timeInSamples += position.bufferSize
        }
    }

    override fun onSuddenChange() {
        currentIndex = -1
        noteRecorder.reset()
        sineWaveSynthesizer.onSuddenChange()
    }

    override suspend fun prepareToPlay(sampleRate: Int, bufferSize: Int) {
        tempBuffers = Array(2) { FloatArray(bufferSize) }
        position.bufferSize = bufferSize
        position.sampleRate = sampleRate
        position.timeInPPQ = 0
        sineWaveSynthesizer.prepareToPlay(sampleRate, bufferSize)
        val audio = audioPreviewTarget
        if (audio != null) {
            audio.factor = sampleRate.toDouble() / sampleRate
            position.projectRange = 0..position.convertSamplesToPPQ(audio.length * sampleRate)
        }
    }

    fun setPreviewTarget(target: List<NoteMessage>) {
        position.timeInPPQ = 0
        audioPreviewTarget?.close()
        audioPreviewTarget = null
        midiPreviewTarget = target
        position.projectRange = 0..target.maxOf { it.time + it.duration }
    }
    fun setPreviewTarget(target: AudioSource) {
        position.timeInPPQ = 0
        midiPreviewTarget = null
        audioPreviewTarget?.close()
        fileSampleRate = target.sampleRate
        val factor = position.sampleRate.toDouble() / target.sampleRate
        audioPreviewTarget = AudioSourceManager.createResampledSource(target).apply {
            this.factor = factor
        }
        position.projectRange = 0..position.convertSamplesToPPQ((target.length * factor).toLong())
    }
    fun clear() {
        midiPreviewTarget = null
        audioPreviewTarget?.close()
        audioPreviewTarget = null
        position.projectRange = 0..0
    }
}