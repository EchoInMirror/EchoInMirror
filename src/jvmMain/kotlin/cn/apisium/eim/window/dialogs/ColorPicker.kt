package cn.apisium.eim.window.dialogs

import androidx.compose.foundation.layout.*
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
import cn.apisium.eim.EchoInMirror
import cn.apisium.eim.components.ColorPicker
import cn.apisium.eim.utils.HsvColor
import cn.apisium.eim.utils.randomColor

private val KEY = Any()
private fun closeColorPicker() = EchoInMirror.windowManager.closeFloatingDialog(KEY)

fun openColorPicker(initialColor: Color = randomColor(), onCancel: (() -> Unit)? = null,
                    onClose: (Color) -> Unit) {
    EchoInMirror.windowManager.openFloatingDialog({
        closeColorPicker()
        onCancel?.invoke()
    }, key = KEY, hasOverlay = false) {
        Surface(shadowElevation = 5.dp, tonalElevation = 5.dp) {
            Column(Modifier.width(IntrinsicSize.Min)) {
                var currentColor by remember { mutableStateOf(HsvColor.from(initialColor)) }
                ColorPicker(currentColor, Modifier.size(200.dp)) { currentColor = it }
                Row(Modifier.fillMaxWidth().padding(end = 10.dp),
                    horizontalArrangement = Arrangement.End) {
                    TextButton({
                        closeColorPicker()
                        onCancel?.invoke()
                    }) { Text("取消") }
                    TextButton({
                        closeColorPicker()
                        onClose(currentColor.toColor())
                    }) { Text("确认") }
                }
            }
        }
    }
}
