package com.eimsound.daw.impl

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.awt.ComposeWindow
import com.eimsound.daw.*
import com.eimsound.daw.api.DefaultProjectInformation
import com.eimsound.daw.api.processor.Track
import com.eimsound.daw.api.window.Panel
import com.eimsound.daw.api.window.WindowManager
import com.eimsound.daw.impl.clips.midi.editor.backingTracks
import com.eimsound.daw.window.dialogs.ExportDialog
import com.eimsound.daw.window.dialogs.QuickLoadDialog
import com.eimsound.daw.window.dialogs.settings.SettingsWindow
import com.eimsound.daw.window.panels.Editor
import com.eimsound.daw.window.panels.Mixer
import com.eimsound.daw.window.panels.UndoList
import com.eimsound.dsp.native.players.JvmAudioPlayer
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.nio.file.Path
import java.util.*
import kotlin.io.path.absolutePathString

class WindowManagerImpl: WindowManager {
    override val dialogs = mutableStateMapOf<@Composable () -> Unit, Boolean>()
    override val panels = mutableStateListOf(Mixer, Editor, UndoList)
    override var mainWindow: ComposeWindow? = null
    override var isDarkTheme by mutableStateOf(true)
    override var activePanel: Panel? = null
    override var isMainWindowOpened by mutableStateOf(false)

    init {
        dialogs[SettingsWindow] = false
        dialogs[QuickLoadDialog] = false
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

    override fun clearTrackUIState(track: Track) {
        if (EchoInMirror.selectedTrack == track) EchoInMirror.selectedTrack = null
        backingTracks.remove(track)
    }

    override fun closeMainWindow(isExit: Boolean) {
        isMainWindowOpened = false
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
            val (bus, loadBus) = EchoInMirror.trackManager.createBus(DefaultProjectInformation(path))
            EchoInMirror.bus = bus
            bus.prepareToPlay(EchoInMirror.currentPosition.sampleRate, EchoInMirror.currentPosition.bufferSize)
            val player = JvmAudioPlayer("Jvm", EchoInMirror.currentPosition, bus)
            EchoInMirror.player = player

            while (mainWindow == null) delay(25)

            loadBus()

            player.open(EchoInMirror.currentPosition.sampleRate, EchoInMirror.currentPosition.bufferSize, 2)
        }
    }
}
