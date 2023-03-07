package com.eimsound.daw.window.dialogs

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEachIndexed
import com.eimsound.audioprocessor.data.midi.MidiTrack
import com.eimsound.audioprocessor.data.midi.toMidiTracks
import com.eimsound.daw.components.*
import com.eimsound.daw.utils.mutableStateSetOf
import java.io.File
import javax.sound.midi.MidiSystem

fun FloatingDialogProvider.openMidiImportDialog(file: File, onClose: (List<MidiTrack>) -> Unit) {
    val midiTracks = MidiSystem.getSequence(file).toMidiTracks()
    val midiTracksHasEvent = midiTracks.filter { it.hasMidiEvent }
    val key = Any()
    openFloatingDialog({
        closeFloatingDialog(key)
    }, key = key, hasOverlay = true) {
        Dialog {
            MenuTitle("导入 MIDI 文件", modifier = Modifier.padding(top = 10.dp, bottom = 10.dp))
            // TODO: 添加相关的UI, 选择不同的track(可多选), 同时调用 MidiViewer 进行预览
            Row(Modifier.sizeIn(400.dp, 300.dp, 600.dp, 400.dp).padding(horizontal = 10.dp)) {
                Column(Modifier.weight(1F)) {
                    val checkboxes = remember { mutableStateSetOf<Int>() }
                    val stateVertical = rememberScrollState(0)
                    Row {
                        Text(
                            "轨道",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(bottom = 4.dp),
                        )
                    }
                    AbsoluteElevationCard {
                        Box(Modifier.fillMaxSize()) {
                            Column(Modifier.verticalScroll(stateVertical)) {
                                midiTracksHasEvent.fastForEachIndexed { index, midiTrack ->
                                    MenuItem {
                                        Text(midiTrack.name ?: "轨道${index + 1}", Modifier.weight(1F))
                                        Checkbox(checkboxes.contains(index), {
                                            if (!checkboxes.remove(index)) checkboxes.add(index)
                                        })
                                    }
                                }
                            }
                            VerticalScrollbar(
                                rememberScrollbarAdapter(stateVertical),
                                Modifier.align(Alignment.CenterEnd).fillMaxHeight()
                            )
                        }
                    }
                }
                Column(Modifier.weight(1F)) { }
//                Column(Modifier.weight(12f)) {
//                    ListItem(
//                        headlineText = { Text("One line list item with 24x24 icon") },
//                        leadingContent = {
//                            Checkbox(
//                                checked = false, onCheckedChange = {}
//                            )
//                        }
//                    )
//                }
            }

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

//fun List<MidiTrack>.getNoteTracks(
//    separateByTrack: Boolean = false,
//    separateByMidiTrack: Boolean = false
//): List<Pair<String, List<EimMidiEvent>>> {
//    if (separateByTrack and separateByMidiTrack)
//}
