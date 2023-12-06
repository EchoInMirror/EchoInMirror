package com.eimsound.daw.components.app

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.window.ApplicationScope
import androidx.compose.ui.window.Tray
import com.eimsound.daw.api.EchoInMirror
import com.eimsound.daw.components.icons.EIMLogo
import com.eimsound.daw.window.mainWindowState

@Composable
fun ApplicationScope.EIMTray() {
    Tray(
        rememberVectorPainter(EIMLogo),
        tooltip = "Echo In Mirror",
        onAction = { mainWindowState.isMinimized = !mainWindowState.isMinimized },
    ) {
        Separator()
        Item("退出") {
            EchoInMirror.windowManager.closeMainWindow()
            exitApplication()
        }
    }
}