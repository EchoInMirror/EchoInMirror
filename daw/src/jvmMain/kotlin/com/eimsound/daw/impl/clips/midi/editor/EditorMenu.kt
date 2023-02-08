package com.eimsound.daw.impl.clips.midi.editor

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.dp
import com.eimsound.daw.components.Filled
import com.eimsound.daw.components.FloatingDialogProvider
import com.eimsound.daw.components.MenuDialog
import com.eimsound.daw.components.MenuItem

internal var offsetOfRoot: Offset = Offset.Zero
internal fun openEditorMenu(provider: FloatingDialogProvider, position: Offset, editor: DefaultMidiClipEditor) {
    val key = Any()
    val close = { provider.closeFloatingDialog(key) }

    val iconModifier = Modifier.size(18.dp)
    val leadingIconModifier = iconModifier.padding(top = 2.dp)
    val cmdIconModifier = iconModifier.padding(top = 1.dp)

    val textModifier = Modifier.padding(start = 4.dp)
    provider.openFloatingDialog({ close() }, offsetOfRoot + position, key) {
        MenuDialog {
            MenuItem(true, {
                editor.cut()
                close()
            }, modifier = Modifier.fillMaxWidth()) {
                Icon(
                    Icons.Filled.ContentCut,
                    contentDescription = null,
                    modifier = leadingIconModifier
                )
                Text("剪切", modifier = textModifier)
                Filled()
                Icon(
                    Icons.Filled.KeyboardCommandKey,
                    contentDescription = null,
                    modifier = cmdIconModifier
                )
                Text("X")
            }
            MenuItem(true, {
                editor.copy()
                close()
            }, modifier = Modifier.fillMaxWidth()) {
                Icon(
                    Icons.Filled.ContentCopy,
                    contentDescription = null,
                    modifier = leadingIconModifier
                )
                Text("复制", modifier = textModifier)
                Filled()
                Icon(
                    Icons.Filled.KeyboardCommandKey,
                    contentDescription = null,
                    modifier = cmdIconModifier
                )
                Text("C")
            }
            MenuItem(true, {
                editor.paste()
                close()
            }, modifier = Modifier.fillMaxWidth()) {
                Icon(
                    Icons.Filled.ContentPaste,
                    contentDescription = null,
                    modifier = leadingIconModifier
                )
                Text("粘贴", modifier = textModifier)
                Filled()
                Icon(
                    Icons.Filled.KeyboardCommandKey,
                    contentDescription = null,
                    modifier = cmdIconModifier
                )
                Text("V")
            }
            MenuItem(true, {
                editor.copyToClipboardAsString()
            }, modifier = Modifier.fillMaxWidth()) {
                Icon(
                    Icons.Filled.ContentCopy,
                    contentDescription = null,
                    modifier = leadingIconModifier
                )
                Text("复制到剪贴板", modifier = textModifier)
            }
            MenuItem(true, {
                editor.pasteFromClipboard()
            }, modifier = Modifier.fillMaxWidth()) {
                Icon(
                    Icons.Filled.ContentPaste,
                    contentDescription = null,
                    modifier = leadingIconModifier
                )
                Text("从剪贴板粘贴", modifier = textModifier)
            }
        }
    }
}
