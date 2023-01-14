package com.eimsound.daw.window.dialogs

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.eimsound.daw.components.ColorPicker
import com.eimsound.daw.components.FloatingDialogProvider
import com.eimsound.daw.components.utils.HsvColor
import com.eimsound.daw.components.utils.randomColor

private val KEY = Any()

fun openColorPicker(localFloatingDialogProvider: FloatingDialogProvider, initialColor: Color = randomColor(),
                   onCancel: (() -> Unit)? = null, onClose: (Color) -> Unit) {
    localFloatingDialogProvider.openFloatingDialog({
        localFloatingDialogProvider.closeFloatingDialog(KEY)
        onCancel?.invoke()
    }, key = KEY, hasOverlay = true) {
        Surface(shape = MaterialTheme.shapes.small, shadowElevation = 5.dp, tonalElevation = 5.dp) {
            Column(Modifier.width(IntrinsicSize.Min)) {
                var currentColor by remember { mutableStateOf(HsvColor.from(initialColor)) }
                ColorPicker(currentColor, Modifier.size(200.dp)) { currentColor = it }
                Row(Modifier.fillMaxWidth().padding(end = 10.dp),
                    horizontalArrangement = Arrangement.End) {
                    TextButton({
                        localFloatingDialogProvider.closeFloatingDialog(KEY)
                        onCancel?.invoke()
                    }) { Text("取消") }
                    TextButton({
                        localFloatingDialogProvider.closeFloatingDialog(KEY)
                        onClose(currentColor.toColor())
                    }) { Text("确认") }
                }
            }
        }
    }
}
