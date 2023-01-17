package com.eimsound.daw.impl.clips.midi.editor

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import com.eimsound.daw.components.FloatingDialogProvider
import com.eimsound.daw.components.MenuDialog
import com.eimsound.daw.components.MenuItem

internal var offsetOfRoot: Offset = Offset.Zero
internal fun openEditorMenu(provider: FloatingDialogProvider, position: Offset, editor: DefaultMidiClipEditor) {
    val key = Any()
    val close = { provider.closeFloatingDialog(key) }
    provider.openFloatingDialog({ close() }, offsetOfRoot + position, key) {
        MenuDialog {
            MenuItem(true, {
                editor.copy()
                close()
            }, modifier = Modifier.fillMaxWidth()) {
                Text("复制")
            }
        }
    }
}
