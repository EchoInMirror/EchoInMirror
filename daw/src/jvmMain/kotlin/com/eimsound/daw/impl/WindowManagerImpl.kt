package com.eimsound.daw.impl

import androidx.compose.runtime.*
import androidx.compose.ui.awt.ComposeWindow
import com.eimsound.daw.*
import com.eimsound.daw.api.DefaultProjectInformation
import com.eimsound.daw.api.EchoInMirror
import com.eimsound.daw.api.processor.TrackManager
import com.eimsound.daw.api.window.GlobalException
import com.eimsound.daw.api.window.Panel
import com.eimsound.daw.api.window.WindowManager
import com.eimsound.daw.components.FloatingLayerProvider
import com.eimsound.daw.window.dialogs.ExportDialog
import com.eimsound.daw.window.dialogs.settings.SettingsWindow
import com.eimsound.daw.window.panels.*
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.nio.file.Path
import kotlin.io.path.absolutePathString

class WindowManagerImpl: WindowManager {
    override val dialogs = mutableStateMapOf<@Composable () -> Unit, Boolean>()
    override val panels = mutableStateListOf(Mixer, Editor, FileSystemBrowser, UndoList, TrackView)
    override var mainWindow: ComposeWindow? = null
    override var activePanel: Panel? by mutableStateOf(null)
    override var isMainWindowOpened by mutableStateOf(false)
    override var globalException: GlobalException? by mutableStateOf(null)
    var floatingLayerProvider: FloatingLayerProvider? = null
    private var _isDarkTheme by mutableStateOf(true)
    override var isDarkTheme
        get() = _isDarkTheme
        set(value) {
            _isDarkTheme = value
            Configuration.save()
        }

    init {
        dialogs[SettingsWindow] = false
        dialogs[ExportDialog] = false
    }

    override fun registerPanel(panel: Panel) {
        panels.add(panel)
    }

    override fun unregisterPanel(panel: Panel) {
        panels.remove(panel)
    }

    @Composable
    fun Dialogs() {
        for ((dialog, visible) in dialogs) {
            if (visible) dialog()
        }
    }

    override fun closeMainWindow(isExit: Boolean) {
        isMainWindowOpened = false
        floatingLayerProvider = null
        try {
            mainWindow?.dispose()
        } catch (ignored: Throwable) { }
        mainWindow = null
        EchoInMirror.player?.close()
        EchoInMirror.bus?.close()
        EchoInMirror.player = null
        EchoInMirror.bus = null
    }

    @OptIn(DelicateCoroutinesApi::class)
    override fun openProject(path: Path) {
        if (isMainWindowOpened) return

        val absolutePath = path.absolutePathString()
        recentProjects.remove(absolutePath)
        recentProjects.add(0, absolutePath)
        saveRecentProjects()

        isMainWindowOpened = true

        GlobalScope.launch {
            val (bus, loadBus) = TrackManager.instance.createBus(DefaultProjectInformation(path))
            EchoInMirror.bus = bus
            bus.prepareToPlay(EchoInMirror.currentPosition.sampleRate, EchoInMirror.currentPosition.bufferSize)

            while (mainWindow == null) delay(25)

            loadBus()

            val player = EchoInMirror.createAudioPlayer()
            EchoInMirror.player = player
        }
    }
}
