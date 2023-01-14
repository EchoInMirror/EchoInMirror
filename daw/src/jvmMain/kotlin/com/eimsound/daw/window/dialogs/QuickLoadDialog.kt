package com.eimsound.daw.window.dialogs

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.eimsound.daw.EchoInMirror
import java.awt.Dimension

private fun closeQuickLoadWindow() {
    EchoInMirror.windowManager.dialogs[QuickLoadDialog] = false
}

val QuickLoadDialog = @Composable {
    Dialog(::closeQuickLoadWindow, title = "快速加载") {
        window.minimumSize = Dimension(860, 700)
        window.isModal = false
        Surface(Modifier.fillMaxSize(), tonalElevation = 4.dp) {
            // TODO:
        }
    }
}
