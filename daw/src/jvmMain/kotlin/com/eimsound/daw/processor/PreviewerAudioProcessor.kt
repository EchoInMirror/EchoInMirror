package com.eimsound.daw.processor

import com.eimsound.audioprocessor.*
import com.eimsound.audioprocessor.data.midi.*
import com.eimsound.daw.impl.CurrentPositionImpl
import com.eimsound.daw.impl.processor.eimAudioProcessorFactory
import com.eimsound.daw.processor.synthesizer.SineWaveSynthesizer
import com.eimsound.daw.utils.binarySearch

val PreviewerDescription = DefaultAudioProcessorDescription(
    "Previewer",
    "Previewer",
    manufacturerName = "EchoInMirror",
    isInstrument = true
)

class PreviewerAudioProcessor(factory: AudioProcessorFactory<*>) : AbstractAudioProcessor(PreviewerDescription, factory) {
    private var midiPreviewTarget: List<NoteMessage>? = null
    private var currentIndex = -1
    private val noteRecorder = MidiNoteRecorder()
    private val pendingNoteOns = LongArray(128)
    private val sineWaveSynthesizer =
        SineWaveSynthesizer(AudioProcessorManager.instance.eimAudioProcessorFactory, true, 0.5F)
    private var audioPreviewTarget: ResampledAudioSource? = null
    private var tempBuffers = Array(2) { FloatArray(1024) }

    var volume by sineWaveSynthesizer::volume
    val position: CurrentPosition = CurrentPositionImpl(this).apply {
        isPlaying = true
        isProjectLooping = false
    }
    var playPosition
        get() = position.timeInPPQ.toDouble() / position.projectRange.last
        set(value) { position.setCurrentTime((value * position.projectRange.last).toInt()) }

    override suspend fun processBlock(buffers: Array<FloatArray>, position: CurrentPosition, midiBuffer: ArrayList<Int>) {
        if (position.bpm != this.position.bpm) this.position.bpm = position.bpm
        if (!this.position.isPlaying) return
        val notes = midiPreviewTarget
        val audio = audioPreviewTarget
        if (notes != null) {
            val midiBuffer2 = ArrayList<Int>()
            val bufferSize = this.position.bufferSize.toLong()
            noteRecorder.forEachNotes {
                pendingNoteOns[it] -= bufferSize
                if (pendingNoteOns[it] <= 0) {
                    noteRecorder.unmarkNote(it)
                    midiBuffer2.add(noteOff(0, it).rawData)
                    midiBuffer2.add(pendingNoteOns[it].toInt().coerceAtLeast(0))
                }
            }
            val timeInSamples = this.position.timeInSamples
            val blockEndSample = timeInSamples + bufferSize
            if (currentIndex == -1) {
                val startPPQ = this.position.timeInPPQ
                currentIndex = notes.binarySearch { it.time <= startPPQ }
            }
            if (currentIndex > 0) currentIndex--
            for (i in currentIndex..notes.lastIndex) {
                val note = notes[i]
                val startTimeInSamples = this.position.convertPPQToSamples(note.time)
                if (startTimeInSamples > blockEndSample) break
                currentIndex = i + 1
                if (startTimeInSamples < timeInSamples || note.disabled) continue
                val noteOnTime = (startTimeInSamples - timeInSamples).toInt().coerceAtLeast(0)
                if (noteRecorder.isMarked(note.note)) {
                    noteRecorder.unmarkNote(note.note)
                    midiBuffer2.add(note.toNoteOffRawData())
                    midiBuffer2.add(noteOnTime)
                }
                midiBuffer2.add(note.toNoteOnRawData())
                midiBuffer2.add(noteOnTime)
                val endTimeInSamples = this.position.convertPPQToSamples(note.time + note.duration)
                val endTime = endTimeInSamples - timeInSamples
                if (endTimeInSamples > blockEndSample) {
                    pendingNoteOns[note.note] = endTime
                    noteRecorder.markNote(note.note)
                } else {
                    midiBuffer2.add(note.toNoteOffRawData())
                    midiBuffer2.add((endTimeInSamples - timeInSamples).toInt().coerceAtLeast(0))
                }
            }
            sineWaveSynthesizer.processBlock(buffers, this.position, midiBuffer2)
            this.position.update(blockEndSample)
        } else if (audio != null) {
            audio.factor = position.sampleRate.toDouble() / audio.source!!.sampleRate
            audio.getSamples(this.position.timeInSamples, this.position.bufferSize, tempBuffers)
            buffers.mixWith(tempBuffers, volume)
            this.position.update(this.position.timeInSamples + position.bufferSize)
        }
    }

    override fun onSuddenChange() {
        currentIndex = -1
        pendingNoteOns.fill(0L)
        noteRecorder.reset()
        sineWaveSynthesizer.onSuddenChange()
    }

    override suspend fun prepareToPlay(sampleRate: Int, bufferSize: Int) {
        tempBuffers = Array(2) { FloatArray(bufferSize) }
        position.setSampleRateAndBufferSize(sampleRate, bufferSize)
        position.setCurrentTime(0)
        sineWaveSynthesizer.prepareToPlay(sampleRate, bufferSize)
        val audio = audioPreviewTarget
        if (audio != null) {
            audio.factor = sampleRate.toDouble() / audio.source!!.sampleRate
            position.projectRange = 0..position.convertSamplesToPPQ((audio.length * audio.factor).toLong())
        }
    }

    fun setPreviewTarget(target: List<NoteMessage>) {
        position.setCurrentTime(0)
        audioPreviewTarget?.close()
        audioPreviewTarget = null
        midiPreviewTarget = target
        position.projectRange = 0..target.maxOf { it.time + it.duration }
    }
    fun setPreviewTarget(target: AudioSource) {
        position.setCurrentTime(0)
        midiPreviewTarget = null
        audioPreviewTarget?.close()
        val factor = position.sampleRate.toDouble() / target.sampleRate
        audioPreviewTarget = AudioSourceManager.instance.createResampledSource(target).apply {
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