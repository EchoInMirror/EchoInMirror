package com.eimsound.daw

import android.app.Application
import androidx.compose.foundation.LocalScrollbarStyle
import androidx.compose.foundation.ScrollbarStyle
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.application
import com.eimsound.daw.api.EchoInMirror
import com.eimsound.daw.window.CrashWindow
import com.eimsound.daw.window.MainWindow
import com.eimsound.daw.window.ProjectWindow
import com.microsoft.appcenter.AppCenter
import com.microsoft.appcenter.analytics.Analytics
import com.microsoft.appcenter.crashes.Crashes
import java.io.File
import java.nio.file.Paths
import javax.swing.UIManager
import kotlin.concurrent.thread

fun main() {
    Configuration
    var androidApplication: Application? = null
    if (APP_CENTER_SECRET.isNotEmpty()) {
        androidApplication = Application.getApplication(
            EchoInMirror::class.java.packageName,
            VERSION.split(".")[0].toInt(),
            VERSION
        )
        androidApplication.start()
//        System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", "trace")
//        AppCenter.setLogLevel(2)
        AppCenter.start(androidApplication, APP_CENTER_SECRET, Analytics::class.java, Crashes::class.java)
        AppCenter.setUserId(Configuration.userId)
    }
    UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName())
    Runtime.getRuntime().addShutdownHook(thread(false) {
        androidApplication?.close()
        EchoInMirror.windowManager.closeMainWindow()
    })

    if (!File("test_project").exists()) File("test_project").mkdir()
    EchoInMirror.windowManager.openProject(Paths.get("test_project"))
    application {
        MaterialTheme(if (EchoInMirror.windowManager.isDarkTheme) darkColorScheme() else lightColorScheme()) {
            val color = MaterialTheme.colorScheme.onSurface
            CompositionLocalProvider(
                LocalScrollbarStyle provides ScrollbarStyle(16.dp, 8.dp, RoundedCornerShape(4.dp),
                    300, color.copy(0.26f), color.copy(0.60f))
            ) {
                if (EchoInMirror.windowManager.globalException != null) CrashWindow()
                else if (EchoInMirror.windowManager.isMainWindowOpened) MainWindow()
                else ProjectWindow()
            }
        }
    }
}
