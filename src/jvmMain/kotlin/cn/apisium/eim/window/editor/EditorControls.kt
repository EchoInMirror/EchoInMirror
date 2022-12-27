package cn.apisium.eim.window.editor

import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cn.apisium.eim.EchoInMirror
import cn.apisium.eim.components.FloatingDialog
import cn.apisium.eim.components.silder.Slider

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun EditorControls() {
    Slider(noteWidth.value / 0.4f, { noteWidth = 0.4.dp * it }, Modifier.padding(12.dp), valueRange = 0.5f..8f)
    Row {
        Text("当前轨道: ", fontWeight = FontWeight.Bold)
        Text(EchoInMirror.selectedTrack?.name ?: "未选择")
    }
    Row {
        FloatingDialog({  }, true) {
            TextField("", { })
        }
    }
}
