package cn.apisium.eim.impl

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.awt.ComposeWindow
import cn.apisium.eim.*
import cn.apisium.eim.api.DefaultProjectInformation
import cn.apisium.eim.api.processor.Track
import cn.apisium.eim.api.window.Panel
import cn.apisium.eim.api.window.WindowManager
import cn.apisium.eim.impl.clips.midi.editor.backingTracks
import cn.apisium.eim.impl.processor.players.NativeAudioPlayer
import cn.apisium.eim.window.panels.Editor
import cn.apisium.eim.window.panels.Mixer
import cn.apisium.eim.window.dialogs.QuickLoadDialog
import cn.apisium.eim.window.dialogs.ExportDialog
import cn.apisium.eim.window.dialogs.settings.SettingsWindow
import cn.apisium.eim.window.panels.UndoList
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.nio.file.Path
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
            val (bus, loadBus) = EchoInMirror.audioProcessorManager.createBus(DefaultProjectInformation(path))
            EchoInMirror.bus = bus
            bus.prepareToPlay(EchoInMirror.currentPosition.sampleRate, EchoInMirror.currentPosition.bufferSize)
            val player = NativeAudioPlayer(EchoInMirror.currentPosition, bus, Configuration.nativeHostPath)
            EchoInMirror.player = player

            while (mainWindow == null) delay(25)

            loadBus()

            player.open(EchoInMirror.currentPosition.sampleRate, EchoInMirror.currentPosition.bufferSize, 2)
        }
    }
}
