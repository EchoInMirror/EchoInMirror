package com.eimsound.daw

import android.app.Application
import androidx.compose.foundation.LocalScrollbarStyle
import androidx.compose.foundation.ScrollbarStyle
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.application
import com.eimsound.audioprocessor.oneBarPPQ
import com.eimsound.daw.actions.doClipsAmountAction
import com.eimsound.daw.api.clips.ClipManager
import com.eimsound.daw.api.EchoInMirror
import com.eimsound.daw.api.controllers.DefaultParameterControllerFactory
import com.eimsound.daw.api.clips.defaultEnvelopeClipFactory
import com.eimsound.daw.commons.ExperimentalEIMApi
import com.eimsound.daw.components.app.EIMTray
import com.eimsound.daw.components.controllers.parameterControllerCreateClipHandler
import com.eimsound.daw.components.icons.EIMLogo
import com.eimsound.daw.impl.WindowManagerImpl
import com.eimsound.daw.window.CrashWindow
import com.eimsound.daw.window.MainWindow
import com.eimsound.daw.window.ProjectWindow
import com.microsoft.appcenter.AppCenter
import com.microsoft.appcenter.analytics.Analytics
import com.microsoft.appcenter.crashes.Crashes
import org.apache.commons.lang3.SystemUtils
import java.awt.Taskbar
import java.awt.Toolkit
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

    if (SystemUtils.IS_OS_MAC) {
        System.setProperty("apple.laf.useScreenMenuBar", "true")
        System.setProperty("apple.awt.application.name", "EchoInMirror")
        System.setProperty("apple.awt.application.appearance", "system")
        System.setProperty("apple.awt.enableTemplateImages", "true")
        System.setProperty("com.apple.mrj.application.apple.menu.about.name", "EchoInMirror")
    }

    UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName())
    if (Taskbar.isTaskbarSupported() && Taskbar.getTaskbar().isSupported(Taskbar.Feature.ICON_IMAGE)) {
        Taskbar.getTaskbar().iconImage = Toolkit.getDefaultToolkit().getImage(
            EIMLogo::class.java.getResource("/logo@2x.png")
        )
    }
    val windowManager = EchoInMirror.windowManager
    Runtime.getRuntime().addShutdownHook(thread(false) {
        androidApplication?.close()
        EchoInMirror.close()
    })

    if (!File("test_project").exists()) File("test_project").mkdir()
    windowManager.openProject(Paths.get("test_project"))

    @OptIn(ExperimentalEIMApi::class)
    parameterControllerCreateClipHandler = { p, id -> // TODO: remove this
        if (EchoInMirror.selectedTrack != null && id != null) {
            val clip = ClipManager.instance.createTrackClip(
                ClipManager.instance.defaultEnvelopeClipFactory.createClip().apply {
                    controllers.add(DefaultParameterControllerFactory.createAudioProcessorParameterController(p, id))
                },
                0,
                EchoInMirror.currentPosition.oneBarPPQ,
                0,
                EchoInMirror.selectedTrack
            )
            listOf(clip).doClipsAmountAction(false)
        }
    }

    application {
        (windowManager as WindowManagerImpl)._exitApplication = ::exitApplication
        EIMTray()
        MaterialTheme(if (windowManager.isDarkTheme) darkColorScheme() else lightColorScheme()) {
            val color = MaterialTheme.colorScheme.onSurface
            CompositionLocalProvider(
                LocalScrollbarStyle provides ScrollbarStyle(16.dp, 8.dp, RoundedCornerShape(4.dp),
                    300, color.copy(0.26f), color.copy(0.60f))
            ) {
                if (windowManager.globalException != null) CrashWindow()
                else if (windowManager.isMainWindowOpened) MainWindow()
                else ProjectWindow()
            }
        }
    }
}
