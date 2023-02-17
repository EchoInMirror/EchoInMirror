package com.eimsound.daw.window.dialogs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.eimsound.audioprocessor.data.midi.ParsedMidiMessages
import com.eimsound.daw.components.FloatingDialogProvider
import com.eimsound.daw.components.Dialog
import com.eimsound.daw.components.MenuTitle
import java.io.File

fun FloatingDialogProvider.openMidiImportDialog(file: File, onClose: (List<ParsedMidiMessages>) -> Unit) {
    val key = Any()
    openFloatingDialog({
        closeFloatingDialog(key)
    }, key = key, hasOverlay = true) {
        Dialog {
            MenuTitle("导入 MIDI 文件")
            // TODO: 添加相关的UI, 选择不同的track(可多选), 同时调用 MidiViewer 进行预览
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