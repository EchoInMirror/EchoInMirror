@file:Suppress("unused")

package com.eimsound.audioprocessor.data.midi

import java.nio.charset.Charset
import javax.sound.midi.MidiMessage
import javax.sound.midi.Sequence
import javax.sound.midi.Track
import kotlin.experimental.and
import javax.sound.midi.MidiEvent as JMidiEvent
import kotlin.math.pow
import org.mozilla.universalchardet.UniversalDetector

val KEY_NAMES = arrayOf("C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B")

class MidiTrack {
    // 元信息
    var sequenceNumber: Int? = null
    val texts: MutableList<String> = mutableListOf()
    val copyrights: MutableList<String> = mutableListOf()
    var trackName: String? = null
    var instrumentName: String? = null
    val lyrics: MutableList<Pair<String, Long>> = mutableListOf()
    val markers: MutableList<Pair<String, Long>> = mutableListOf()
    val cuePoints: MutableList<Pair<String, Long>> = mutableListOf()
    val setTempos: MutableList<Pair<Long, Long>> = mutableListOf()
    var smpteOffset: Double? = null  // 单位 秒
    var timeSignature: Pair<Int, Int> = Pair(4, 4)  // 拍号
    var keySignature: JMidiEvent? = null  // 调号 先不处理
    val sequencerSpecifics: MutableList<JMidiEvent> = mutableListOf()  // 自定义信息 先不处理

    // 非元信息
    val noteEvents: MutableList<JMidiEvent> = mutableListOf()
    val keyPressureEvents: MutableList<JMidiEvent> = mutableListOf()
    val controlChangeEvents: MutableList<JMidiEvent> = mutableListOf()
    val programChangeEvents: MutableList<JMidiEvent> = mutableListOf()
    val channelPressureEvents: MutableList<JMidiEvent> = mutableListOf()
    val pitchBendEvents: MutableList<JMidiEvent> = mutableListOf()

    val hasNoteOrCtrlrolEvents get() = (noteEvents.size > 0) or (keyPressureEvents.size > 0) or
            (controlChangeEvents.size > 0) or (programChangeEvents.size > 0) or (channelPressureEvents.size > 0) or
            (pitchBendEvents.size > 0)
    val name get() = trackName ?: instrumentName
}
fun JMidiEvent.byte1() = this.message.message[0]
fun JMidiEvent.byte2() = this.message.message[1]
fun JMidiEvent.getMetaContent() = this.message.message.drop(3).toByteArray()
fun JMidiEvent.getMetaStringWithTick() = Pair(this.getMetaString(), this.tick)
fun JMidiEvent.getMetaString(): String {
    val content = this.getMetaContent()
    return String(content, Charset.forName(guessEncoding(content)))
}

fun JMidiEvent.getSequenceNumber() = byteToInt(this.getMetaContent())
fun JMidiEvent.getSetTempo() = Pair(byteToLong(this.getMetaContent()), this.tick)

private fun byteToInt(bytes: ByteArray): Int {
    var result = 0
    var shift = 0
    for (byte in bytes) {
        result = result or (byte.toInt() shl shift)
        shift += 8
    }
    return result
}

private fun byteToLong(bytes: ByteArray): Long {
    var result = 0L
    var shift = 0
    for (byte in bytes) {
        result = result or (byte.toLong() shl shift)
        shift += 8
    }
    return result
}

private fun guessEncoding(bytes: ByteArray): String {
    val detector = UniversalDetector()
    detector.handleData(bytes, 0, bytes.size)
    detector.dataEnd()
    val encoding = detector.detectedCharset
    return encoding ?: "UTF-8"
}

fun JMidiEvent.getSmpteOffset(): Double? {
    if (this.message.length != 8) return null
    val frameRate = when ((this.message.message[3].toInt() ushr 5) and 0b11) {
        0 -> 24.0
        1 -> 25.0
        2 -> 29.97
        3 -> 30.0
        else -> 30.0
    }

    return (this.message.message[3] and 0x1F).toDouble() * 60 * 60 +
            this.message.message[4].toDouble() * 60 +
            this.message.message[5].toDouble() +
            this.message.message[6].toUInt().toDouble() / frameRate +
            this.message.message[7].toUInt().toDouble() / frameRate / 100
}

fun JMidiEvent.getTimeSignatures() = Pair(
    this.message.message[3].toInt(),
    2.0.pow(this.message.message[4].toDouble()).toInt()
)
fun Sequence.getMidiTracks(): List<MidiTrack> {
    val midiTrackList = mutableListOf<MidiTrack>()
    tracks.forEach {
        val track = MidiTrack()
        midiTrackList.add(track)

        for (i in 0 until it.size()) {
            val midiEvent = it[i]
            when {
                // NOTE_ON
                midiEvent.byte1() and 0xF0.toByte() == 0x80.toByte() -> track.noteEvents.add(midiEvent)
                // NOTE_OFF
                midiEvent.byte1() and 0xF0.toByte() == 0x90.toByte() -> track.noteEvents.add(midiEvent)
                // KEY_PRESSURE
                midiEvent.byte1() and 0xF0.toByte() == 0xA0.toByte() -> track.keyPressureEvents.add(midiEvent)
                // CONTROL_CHANGE
                midiEvent.byte1() and 0xF0.toByte() == 0xB0.toByte() -> track.controlChangeEvents.add(midiEvent)
                // PROGRAM_CHANGE
                midiEvent.byte1() and 0xF0.toByte() == 0xC0.toByte() -> track.programChangeEvents.add(midiEvent)
                // CHANNEL_PRESSURE
                midiEvent.byte1() and 0xF0.toByte() == 0xD0.toByte() -> track.channelPressureEvents.add(midiEvent)
                // PITCH_BEND
                midiEvent.byte1() and 0xF0.toByte() == 0xE0.toByte() -> track.pitchBendEvents.add(midiEvent)
                // META_EVENT
                midiEvent.byte1() == 0xFF.toByte() -> {
                    when {
                        // https://www.recordingblogs.com/wiki/midi-meta-messages
                        midiEvent.byte2() == 0x00.toByte() -> {
                            if (track.sequenceNumber == null)
                                track.sequenceNumber = midiEvent.getSequenceNumber()
                        }

                        midiEvent.byte2() == 0x01.toByte() -> track.texts.add(midiEvent.getMetaString())
                        midiEvent.byte2() == 0x02.toByte() -> track.copyrights.add(midiEvent.getMetaString())
                        midiEvent.byte2() == 0x03.toByte() -> {
                            if (track.trackName == null)
                                track.trackName = midiEvent.getMetaString()
                        }

                        midiEvent.byte2() == 0x04.toByte() -> {
                            if (track.instrumentName == null)
                                track.instrumentName = midiEvent.getMetaString()
                        }

                        midiEvent.byte2() == 0x05.toByte() -> track.lyrics.add(midiEvent.getMetaStringWithTick())
                        midiEvent.byte2() == 0x06.toByte() -> track.markers.add(midiEvent.getMetaStringWithTick())
                        midiEvent.byte2() == 0x07.toByte() -> track.cuePoints.add(midiEvent.getMetaStringWithTick())
                        midiEvent.byte2() == 0x2F.toByte() -> {}  // end of track
                        midiEvent.byte2() == 0x51.toByte() -> track.setTempos.add(midiEvent.getSetTempo())
                        midiEvent.byte2() == 0x54.toByte() -> track.smpteOffset = midiEvent.getSmpteOffset()
                        midiEvent.byte2() == 0x58.toByte() -> track.timeSignature = midiEvent.getTimeSignatures()
                        midiEvent.byte2() == 0x59.toByte() -> track.keySignature = midiEvent
                        midiEvent.byte2() == 0x70.toByte() -> track.sequencerSpecifics.add(midiEvent)
                    }
                }
            }
        }
    }
    return midiTrackList
}

data class MidiEventWithTime(val event: MidiEvent, val time: Int)

fun Track.getMidiEvents(scale: Double = 1.0, sorted: Boolean = true): List<MidiEventWithTime> {
    val list = ArrayList<MidiEventWithTime>()
    for (i in 0 until size()) get(i).apply {
        list.add(MidiEventWithTime(MidiEvent(message), (tick * scale).toInt()))
    }
    if (sorted) list.sortBy { it.time }
    return list
}

fun Sequence.toMidiEvents(track: Int? = null, destPPQ: Int? = null): List<MidiEventWithTime> {
    val scale = if (destPPQ == null) 1.0 else destPPQ.toDouble() / resolution
    return if (track == null) {
        val list = ArrayList<MidiEventWithTime>()
        tracks.forEach { list.addAll(it.getMidiEvents(scale, false)) }
        list.sortBy { it.time }
        list
    } else tracks[track].getMidiEvents(scale)
}

fun Int.toMidiEvent() = MidiEvent(this)

@Suppress("unused")
fun MidiMessage.toMidiEvent() = MidiEvent(this)
fun MidiMessage.toInt(): Int {
    var data = message[0].toUByte().toInt()
    if (length > 1) data = data or (message[1].toUByte().toInt() shl 8)
    if (length > 2) data = data or (message[2].toUByte().toInt() shl 16)
    return data
}

@Suppress("MemberVisibilityCanBePrivate")
@JvmInline
value class MidiEvent(val rawData: Int) {
    constructor(byte1: Byte, byte2: Byte, byte3: Byte) : this(byte1.toUByte().toInt() or (byte2.toUByte().toInt() shl 8) or (byte3.toUByte().toInt() shl 16))
    constructor(byte1: Int, byte2: Int, byte3: Int) : this(byte1 or (byte2 shl 8) or (byte3 shl 16))
    constructor(message: MidiMessage) : this(message.message[0].toUByte().toInt() and 0xFF,
        if (message.length > 1) message.message[1].toUByte().toInt() and 0xFF else 0,
        if (message.length > 2) message.message[2].toUByte().toInt() and 0xFF else 0)
    val byte1 get() = byte1Int.toByte()
    val byte2 get() = byte2Int.toByte()
    val byte3 get() = byte3Int.toByte()
    val byte4 get() = byte4Int.toByte()
    val byte1Int get() = rawData and 0xFF
    val byte2Int get() = rawData ushr 8 and 0xFF
    val byte3Int get() = rawData ushr 16 and 0xFF
    val byte4Int get() = rawData ushr 24 and 0xFF
    val isNoteOn get() = byte1Int and 0xF0 == 0x90
    val isNoteOff get() = byte1Int and 0xF0 == 0x80 || (byte1Int and 0xF0 == 0x90 && byte3Int == 0)
    val isNote get() = isNoteOn || isNoteOff
    val isController get() = byte1Int and 0xF0 == 0xB0
    val isProgramChange get() = byte1Int and 0xF0 == 0xC0
    val isPitchBend get() = byte1Int and 0xF0 == 0xE0
    val isMeta get() = byte1Int == 0xFF
    val isSysex get() = byte1Int == 0xF0
    val isSysexEnd get() = byte1Int == 0xF7
    val isSystem get() = byte1Int in 0xF0..0xFF
    val isChannel get() = byte1Int in 0x80..0xEF
    val isRealTime get() = byte1Int in 0xF8..0xFF
    val isChannelVoice get() = byte1Int in 0x80..0xEF && byte1Int !in 0xF0..0xFF
    val isChannelMode get() = byte1Int == 0xB0 && byte2Int in 0x78..0x7F
    val channel get() = byte1Int and 0xF
    val note get() = byte2Int
    val noteFrequency get() = 440.0 * 2.0.pow((note - 69) / 12.0)
    val noteName get() = getNoteName(note)
    val velocity get() = byte3Int
    val controller get() = byte2Int
    val value get() = byte3Int
    val program get() = byte2Int
    val pitch get() = byte2Int or (byte3Int shl 7)
    val metaType get() = byte2Int
    val metaLength get() = byte3Int
    val sysexLength get() = byte3Int
    val realTimeType get() = byte1Int and 0xF

    fun toNoteOn() = MidiEvent(channel or 0x90, byte2Int, byte3Int)
    fun toNoteOff() = MidiEvent(channel or 0x80, byte2Int, byte3Int)
}

fun getNoteName(note: Int) = KEY_NAMES[note % 12] + (note / 12)

fun noteOn(channel: Int = 0, note: Int, velocity: Int = 70): MidiEvent = MidiEvent(0x90 or channel, note, velocity)
fun noteOff(channel: Int = 0, note: Int) = MidiEvent(0x80 or channel, note, 70)
fun controllerEvent(channel: Int, controller: Int, value: Int) = MidiEvent(0xB0 or channel, controller, value)
fun allNotesOff(channel: Int) = controllerEvent(channel, 123, 0)
fun allSoundOff(channel: Int) = controllerEvent(channel, 120, 0)
fun programChange(channel: Int, program: Int) = MidiEvent(0xC0 or channel, program, 0)
fun pitchBend(channel: Int, value: Int) = MidiEvent(0xE0 or channel, value and 0x7F, value ushr 7 and 0x7F)
fun metaEvent(type: Int, length: Int) = MidiEvent(0xFF, type, length)
fun sysexEvent(length: Int) = MidiEvent(0xF0, 0, length)
fun realTimeEvent(type: Int) = MidiEvent(0xF8 or type, 0, 0)
