package cn.apisium.eim.components

import androidx.compose.foundation.clickable
import androidx.compose.runtime.Composable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import cn.apisium.eim.EchoInMirror
import androidx.compose.ui.unit.dp
import cn.apisium.eim.impl.UndoManagerImpl
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.text.font.FontStyle

@Composable
fun UndoList() {
    Column {
        (EchoInMirror.undoManager as UndoManagerImpl).read()
        EchoInMirror.undoManager.actions.forEachIndexed { index, it ->
            val color = if (index < EchoInMirror.undoManager.cursor) Color(0, 0, 0) else Color(0, 0, 0, 100)
            val fontStyle = if (index < EchoInMirror.undoManager.cursor) FontStyle.Normal else FontStyle.Italic
            Row(Modifier
                .clickable {
                    if (index < EchoInMirror.undoManager.cursor) {
                        GlobalScope.launch { EchoInMirror.undoManager.undo(EchoInMirror.undoManager.cursor - index - 1) }
                    } else {
                        GlobalScope.launch { EchoInMirror.undoManager.redo(index - EchoInMirror.undoManager.cursor + 1) }
                    }
                }) {
                Icon(Icons.Default.RestartAlt, "redo", Modifier.scale(0.8f), color)
                Spacer(Modifier.width(2.dp))
                Text(
                    EchoInMirror.undoManager.actions[index].name,
                    Modifier
                        .fillMaxWidth(),
                    color = color,
                    style = MaterialTheme.typography.labelLarge,
                    fontStyle = fontStyle
                )
            }
        }

    }
}