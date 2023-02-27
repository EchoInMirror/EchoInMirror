package com.eimsound.daw.window.dialogs

import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.material3.*
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.eimsound.audioprocessor.data.midi.*
import com.eimsound.daw.components.Dialog
import com.eimsound.daw.components.FloatingDialogProvider
import com.eimsound.daw.components.MenuTitle
import org.mozilla.universalchardet.UniversalDetector
import java.io.File
import java.nio.charset.Charset
import javax.sound.midi.*
import javax.sound.midi.MidiEvent
import kotlin.experimental.and
import kotlin.math.pow

@OptIn(ExperimentalMaterial3Api::class)
fun FloatingDialogProvider.openMidiImportDialog(file: File, onClose: (List<ParsedMidiMessages>) -> Unit) {
    val list = MidiSystem.getSequence(file).getMidiTracks().filter { it.hasNoteOrCtrlrolEvents }
    val key = Any()
    openFloatingDialog({
        closeFloatingDialog(key)
    }, key = key, hasOverlay = true) {
        Dialog(modifier = Modifier.width(1000.dp)) {
            MenuTitle("导入 MIDI 文件", modifier = Modifier.padding(top = 10.dp, bottom = 10.dp))
            // TODO: 添加相关的UI, 选择不同的track(可多选), 同时调用 MidiViewer 进行预览
            Divider()
            Row {
                Column(Modifier.weight(12f)) {
                    val checkboxes = remember { mutableSetOf<Int>() }
                    val selectAll = mutableStateOf(true)

                    Box(Modifier.heightIn(max = 1000.dp).fillMaxSize()) {
                        val stateVertical = rememberScrollState(0)

                        Column(modifier = Modifier.heightIn(max = 1000.dp)) {
                            ListItem(
                                headlineText = {
                                    Text("全选")
                                },
                                leadingContent = {
                                    Checkbox(
                                        checked = selectAll.value, onCheckedChange = {
                                            selectAll.value = it
                                            if (it) checkboxes.addAll(list.indices)
                                            else checkboxes.clear()
                                        }
                                    )
                                }
                            )
                            list.forEachIndexed { index, midiTrack ->
                                ListItem(
                                    headlineText = {
                                        Text(midiTrack.name ?: "轨道${index + 1}")
                                    },
                                    leadingContent = {
                                        Checkbox(
                                            checked = checkboxes.contains(index), onCheckedChange = {
                                                if (it) checkboxes.add(index)
                                                else checkboxes.remove(index)
                                                selectAll.value = checkboxes.size == list.size
                                            }
                                        )
                                    }
                                )
                            }
                        }
                        VerticalScrollbar(
                            rememberScrollbarAdapter(stateVertical),
                            Modifier.align(Alignment.CenterEnd).fillMaxHeight()
                        )
                    }
                }
                Column(Modifier.weight(12f)) {
                    ListItem(
                        headlineText = { Text("One line list item with 24x24 icon") },
                        leadingContent = {
                            Checkbox(
                                checked = false, onCheckedChange = {}
                            )
                        }
                    )
                }
            }

            Divider()
            Row(Modifier.fillMaxWidth().padding(end = 10.dp), horizontalArrangement = Arrangement.End) {
                TextButton({
                    closeFloatingDialog(key)
                }) { Text("取消") }
                TextButton({
                    closeFloatingDialog(key)
//                  TODO: onClose(currentColor.toColor())
                }) { Text("确认") }
            }
        }
    }
}

class MidiTrack() {
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
    var keySignature: MidiEvent? = null  // 调号 先不处理
    val sequencerSpecifics: MutableList<MidiEvent> = mutableListOf()  // 自定义信息 先不处理

    // 非元信息
    val noteEvents: MutableList<MidiEvent> = mutableListOf()
    val keyPressureEvents: MutableList<MidiEvent> = mutableListOf()
    val controlChangeEvents: MutableList<MidiEvent> = mutableListOf()
    val programChangeEvents: MutableList<MidiEvent> = mutableListOf()
    val channelPressureEvents: MutableList<MidiEvent> = mutableListOf()
    val pitchBendEvents: MutableList<MidiEvent> = mutableListOf()

    val hasNoteOrCtrlrolEvents get() = (noteEvents.size > 0) or (keyPressureEvents.size > 0) or
            (controlChangeEvents.size > 0) or (programChangeEvents.size > 0) or (channelPressureEvents.size > 0) or
            (pitchBendEvents.size > 0)
    val name get() = trackName ?: instrumentName
}

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

//fun List<MidiTrack>.getNoteTracks(
//    separateByTrack: Boolean = false,
//    separateByMidiTrack: Boolean = false
//): List<Pair<String, List<EimMidiEvent>>> {
//    if (separateByTrack and separateByMidiTrack)
//}

fun MidiEvent.byte1() = this.message.message[0]
fun MidiEvent.byte2() = this.message.message[1]
fun MidiEvent.getMetaContent() = this.message.message.drop(3).toByteArray()
fun MidiEvent.getMetaStringWithTick() = Pair(this.getMetaString(), this.tick)
fun MidiEvent.getMetaString(): String {
    val content = this.getMetaContent()
    return String(content, Charset.forName(guessEncoding(content)))
}

fun MidiEvent.getSequenceNumber() = byteToInt(this.getMetaContent())
fun MidiEvent.getSetTempo() = Pair(byteToLong(this.getMetaContent()), this.tick)
fun MidiEvent.getSmpteOffset(): Double? {
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

fun MidiEvent.getTimeSignatures() = Pair(
    this.message.message[3].toInt(),
    2.0.pow(this.message.message[4].toDouble()).toInt()
)

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