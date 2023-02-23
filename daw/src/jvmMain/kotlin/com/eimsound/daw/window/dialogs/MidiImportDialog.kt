package com.eimsound.daw.window.dialogs

import javax.sound.midi.*

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.eimsound.audioprocessor.data.midi.*
import com.eimsound.audioprocessor.data.midi.MidiEvent as EimMidiEvent
import com.eimsound.daw.components.Dialog
import com.eimsound.daw.components.FloatingDialogProvider
import com.eimsound.daw.components.MenuTitle
import com.eimsound.daw.components.MidiView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
fun FloatingDialogProvider.openMidiImportDialog(file: File, onClose: (List<ParsedMidiMessages>) -> Unit) {
    val list = {
//        val instruments = MidiSystem.getSynthesizer().defaultSoundbank.instruments
        val sequence = MidiSystem.getSequence(file)

        sequence.tracks.map {
            it.getMidiEvents().parse()
        }
    }

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
//                    list.forEachIndexed() { index, midiTrack ->
//                        Row(Modifier.fillMaxWidth()) {
//                            ListItem(
//                                headlineText = { Text(index.toString()?: "我是一轨") },
//                                leadingContent = {
//
//                                }
//                            )
//                        }
//                    }

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

class PhasedMidi() {
    val midiTracks: Array<MidiTrack> = arrayOf()
}

class MidiTrack() {
    // 元信息
    val sequenceNumber: Int? = null
    val text: MutableList<String> = mutableListOf()
    val copyright: MutableList<String> = mutableListOf()
    val trackName: String? = null
    val instrumentName: String? = null
    val lyrics: MutableList<Pair<String, Int>> = mutableListOf()
    val markers: MutableList<Pair<String, Int>> = mutableListOf()
    val cuePoints: MutableList<Pair<String, Int>> = mutableListOf()
    val setTempos: MutableList<Pair<String, Int>> = mutableListOf()
    val smpteOffsets: Float? = null
    val timeSignatures: Pair<Int, Int> = Pair(0, 0)  // 拍号
    val keySignatures: MidiEvent? = null  // 调号 先不处理
    val sequencerSpecifics: MutableList<MidiEvent> = mutableListOf()  // 自定义信息 先不处理
    // 非元信息
    val noteEventEvents: MutableList<MidiEvent> = mutableListOf()
    val keyPressureEvents: MutableList<MidiEvent> = mutableListOf()
    val controlChangeEvents: MutableList<MidiEvent> = mutableListOf()
    val channelPressureEvents: MutableList<MidiEvent> = mutableListOf()
    val pitchBendEvents: MutableList<MidiEvent> = mutableListOf()
    //    CONTROL_CHANGE,  // 0xB0 to 0xBF
//    PROGRAM_CHANGE,  // 0xC0 to 0xCF
//    CHANNEL_PRESSURE,  // 0xD0 to 0xDF
//    PITCH_BEND, // 0xE0 to 0xEF
}

fun Sequence.getMidiTracks() {
    val midiTrackList = mutableListOf<MidiTrack>()
    tracks.forEach {
        val track = MidiTrack()
        midiTrackList.add(track)

        for ( i in 0..it.size()) {
            val midiEvent = it[i]
            when{
                // NOTE_ON
                midiEvent.byte1() in 0x80..0x8F -> track.noteEvents.add(midiEvent)
                // NOTE_OFF
                midiEvent.byte1() in 0x90..0x9F -> track.noteEvents.add(midiEvent)
                // KEY_PRESSURE
                midiEvent.byte1() in 0xA0..0xAF ->
            }

        }
    }
    val midiTrack = MidiTrack()
}
//enum class MidiEventType() {
//    NOTE_ON, // 0x80 to 0x8F
//    NOTE_OFF, // 0x90 to 0x9F
//    KEY_PRESSURE, // 0xA0 to 0xAF
//    CONTROL_CHANGE,  // 0xB0 to 0xBF
//    PROGRAM_CHANGE,  // 0xC0 to 0xCF
//    CHANNEL_PRESSURE,  // 0xD0 to 0xDF
//    PITCH_BEND, // 0xE0 to 0xEF
//    META_EVENT, // 0xFF
//}
fun MidiEvent.byte1() = this.message.message[0]
fun MidiEvent.byte2() = this.message.message[1]
/*

Message	Meta type	Data length	Contains	Occurs at
Sequence number	0x00	2 bytes	The number of a sequence	At delta time 0
Text	0x01	variable	Some text	Anywhere
Copyright notice	0x02	variable	A copyright notice	At delta time 0 in the first track
Track name	0x03	variable	A track name	At delta time 0
Instrument name	0x04	variable	The name of an instrument in the current track	Anywhere
Lyrics	0x05	variable	Lyrics, usually a syllable per quarter note	Anywhere
Marker	0x06	variable	The text of a marker	Anywhere
Cue point	0x07	variable	The text of a cue, usually to prompt for some action from the user	Anywhere

Channel prefix	0x20	1 byte	A channel number (following meta events will apply to this channel)	Anywhere
End of track	0x2F	0	 	At the end of each track

Set tempo	0x51	3	The number of microseconds per beat	Anywhere, but usually in the first track
SMPTE offset	0x54	5	SMPTE time to denote playback offset from the beginning	At the beginning of a track and in the first track of files with MIDI format type 1
Time signature	0x58	4	Time signature, metronome clicks, and size of a beat in 32nd notes	Anywhere
Key signature	0x59	2	A key signature	Anywhere
Sequencer specific	0x7F	variable	Something specific to the MIDI device manufacturer	Anywhere
 */
//fun Track.parse(): MidiTrack {
//    val midiEvents = this.getMidiEvents()
//    midiEvents.forEach{
//        val event = it.event
//        if (event.isMeta) {
//            when {
//                event.is
//            }
//        }
//    }
//}
