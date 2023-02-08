package com.eimsound.daw.window.panels

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SettingsBackupRestore
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.eimsound.daw.EchoInMirror
import com.eimsound.daw.api.window.Panel
import com.eimsound.daw.api.window.PanelDirection
import com.eimsound.daw.components.utils.clickableWithIcon
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

@Suppress("PrivatePropertyName")
private val ICON_SIZE = Modifier.scale(0.8f).padding(4.dp, end = 2.dp)

object UndoList: Panel {
    override val name = "历史操作"
    override val direction = PanelDirection.Vertical

    @Composable
    override fun Icon() {
        Icon(Icons.Default.SettingsBackupRestore, "undo")
    }

    @OptIn(DelicateCoroutinesApi::class)
    @Composable
    override fun Content() {
        Column(Modifier.padding(vertical = 4.dp)) {
            Row(
                Modifier.fillMaxWidth().clickableWithIcon {
                    GlobalScope.launch { EchoInMirror.undoManager.reset() }
                }) {
                Icon(Icons.Outlined.RestartAlt, "redo", ICON_SIZE)
                Text("初始状态", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
            }
            val cursor = EchoInMirror.undoManager.cursor
            EchoInMirror.undoManager.actions.forEachIndexed { index, it ->
                var color = MaterialTheme.colorScheme.onSurface
                val fontStyle = if (index < cursor) FontStyle.Normal else FontStyle.Italic
                if (index >= cursor) color = color.copy(0.38F)
                Row(
                    Modifier.padding(horizontal = 4.dp).fillMaxWidth().clickableWithIcon {
                        val flag = index < cursor
                        GlobalScope.launch {
                            if (flag) EchoInMirror.undoManager.undo(cursor - index - 1)
                            else EchoInMirror.undoManager.redo(index - cursor + 1)
                        }
                    }) {
                    Icon(it.icon, "redo", ICON_SIZE, color)
                    Text(
                        EchoInMirror.undoManager.actions[index].name,
                        color = color,
                        style = MaterialTheme.typography.labelLarge,
                        fontStyle = fontStyle
                    )
                }
            }
        }
    }
}