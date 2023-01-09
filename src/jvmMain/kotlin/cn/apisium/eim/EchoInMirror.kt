package cn.apisium.eim

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import cn.apisium.eim.api.*
import cn.apisium.eim.api.processor.AudioProcessorManager
import cn.apisium.eim.api.processor.Bus
import cn.apisium.eim.api.processor.Track
import cn.apisium.eim.api.window.WindowManager
import cn.apisium.eim.data.defaultQuantification
import cn.apisium.eim.impl.*
import cn.apisium.eim.impl.clips.ClipManagerImpl
import cn.apisium.eim.impl.processor.AudioProcessorManagerImpl
import cn.apisium.eim.plugin.EIMPluginManager
import org.pf4j.PluginManager

object EchoInMirror {
    @Suppress("MemberVisibilityCanBePrivate")
    val currentPosition: CurrentPosition = CurrentPositionImpl()
    var bus: Bus? by mutableStateOf(null)
    var player: AudioPlayer? by mutableStateOf(null)
//    var player: AudioPlayer = JvmAudioPlayer(currentPosition, bus)

    val commandManager: CommandManager = CommandManagerImpl()
    @Suppress("unused")
    val pluginManager: PluginManager = EIMPluginManager()
    val windowManager: WindowManager = WindowManagerImpl()
    val clipManager: ClipManager = ClipManagerImpl()
    val audioProcessorManager: AudioProcessorManager = AudioProcessorManagerImpl()
    val undoManager: UndoManager = UndoManagerImpl()
    var quantification by mutableStateOf(defaultQuantification)

    private var selectedTrack_ by mutableStateOf<Track?>(null)
    var selectedTrack
        get() = selectedTrack_
        set(value) {
            if (value != selectedTrack_) {
                selectedClip = null
                selectedTrack_ = value
            }
        }
    var selectedClip by mutableStateOf<TrackClip<*>?>(null)
}
