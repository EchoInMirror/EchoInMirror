package com.eimsound.daw

import androidx.compose.foundation.LocalScrollbarStyle
import androidx.compose.foundation.ScrollbarStyle
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.application
import com.eimsound.daw.window.MainWindow
import com.eimsound.daw.window.ProjectWindow
import java.io.File
import java.nio.file.Paths
import javax.swing.UIManager

fun main() {
    UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName())
    createDirectories()
    Runtime.getRuntime().addShutdownHook(Thread(EchoInMirror.windowManager::closeMainWindow))

    if (!File("test_project").exists()) File("test_project").mkdir()
    EchoInMirror.windowManager.openProject(Paths.get("test_project"))
    application {
        MaterialTheme(if (EchoInMirror.windowManager.isDarkTheme) darkColorScheme() else lightColorScheme()) {
            val color = MaterialTheme.colorScheme.onSurface
            CompositionLocalProvider(
                LocalScrollbarStyle provides ScrollbarStyle(16.dp, 8.dp, RoundedCornerShape(4.dp),
                    300, color.copy(0.26f), color.copy(0.60f))
            ) {
                if (EchoInMirror.windowManager.isMainWindowOpened) MainWindow()
                else ProjectWindow()
            }
        }
    }
}
