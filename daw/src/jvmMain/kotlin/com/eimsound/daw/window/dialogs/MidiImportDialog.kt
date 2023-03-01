package com.eimsound.daw.window.dialogs

import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEachIndexed
import com.eimsound.audioprocessor.data.midi.ParsedMidiMessages
import com.eimsound.audioprocessor.data.midi.getMidiTracks
import com.eimsound.daw.components.*
import java.io.File
import java.nio.charset.Charset
import javax.sound.midi.MidiEvent
import javax.sound.midi.MidiSystem
import javax.sound.midi.Sequence
import javax.sound.midi.Track
import kotlin.experimental.and
import kotlin.math.pow

fun FloatingDialogProvider.openMidiImportDialog(file: File, onClose: (List<ParsedMidiMessages>) -> Unit) {
    val list = MidiSystem.getSequence(file).getMidiTracks()

    val key = Any()
    openFloatingDialog({
        closeFloatingDialog(key)
    }, key = key, hasOverlay = true) {
        Dialog {
            MenuTitle("导入 MIDI 文件", modifier = Modifier.padding(top = 10.dp, bottom = 10.dp))
            // TODO: 添加相关的UI, 选择不同的track(可多选), 同时调用 MidiViewer 进行预览
            Row(Modifier.sizeIn(400.dp, 300.dp, 600.dp, 400.dp).padding(horizontal = 10.dp)) {
                Column(Modifier.weight(1F)) {
                    val checkboxes = remember { mutableSetOf<Int>() }
                    val stateVertical = rememberScrollState(0)

                    Row {
                        Text("轨道",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(bottom = 4.dp),
                        )
                    }
                    AbsoluteElevationCard {
                        Box(Modifier.fillMaxSize()) {
                            Column(Modifier.verticalScroll(stateVertical)) {
                                list.fastForEachIndexed { index, midiTrack ->
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
