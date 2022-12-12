package cn.apisium.eim.data.midi

import kotlin.math.pow

val KEY_NAMES = arrayOf("C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B")

@Suppress("MemberVisibilityCanBePrivate", "unused")
@JvmInline
value class MidiEvent(val rawData: Int) {
    constructor(byte1: Byte, byte2: Byte, byte3: Byte) : this(byte1.toInt() or (byte2.toInt() shl 8) or (byte3.toInt() shl 16))
    constructor(byte1: Int, byte2: Int, byte3: Int) : this(byte1 or (byte2 shl 8) or (byte3 shl 16))
    val byte1 get() = byte1Int.toByte()
    val byte2 get() = byte2Int.toByte()
    val byte3 get() = rawData.toByte()
    val byte4 get() = byte4Int.toByte()
    val byte1Int get() = rawData and 0xFF
    val byte2Int get() = rawData ushr 8 and 0xFF
    val byte3Int get() = rawData ushr 16 and 0xFF
    val byte4Int get() = rawData ushr 24 and 0xFF
    val isNoteOn get() = byte1Int == 0x90 && byte3Int != 0
    val isNoteOff get() = byte1Int == 0x80 || (byte1Int == 0x90 && byte3Int == 0)
    val isNote get() = isNoteOn || isNoteOff
    val isController get() = byte1Int == 0xB0
    val isProgramChange get() = byte1Int == 0xC0
    val isPitchBend get() = byte1Int == 0xE0
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
    val noteName get() = KEY_NAMES[note % 12] + (note / 12)
    val velocity get() = byte3Int
    val controller get() = byte2Int
    val program get() = byte2Int
    val pitch get() = byte2Int or (byte3Int shl 7)
    val metaType get() = byte2Int
    val metaLength get() = byte3Int
    val sysexLength get() = byte3Int
    val realTimeType get() = byte1Int and 0xF
}

fun midiOn(channel: Int, note: Int, velocity: Int = 70) = MidiEvent(0x90 or channel, note, velocity)
fun midiOff(channel: Int, note: Int) = MidiEvent(0x80 or channel, note, 70)
