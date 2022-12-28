package cn.apisium.eim

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import cn.apisium.eim.api.*
import cn.apisium.eim.api.processor.AudioProcessorManager
import cn.apisium.eim.api.window.WindowManager
import cn.apisium.eim.data.defaultQuantification
import cn.apisium.eim.impl.*
import cn.apisium.eim.impl.processor.AudioProcessorManagerImpl
import cn.apisium.eim.impl.processor.players.NativeAudioPlayer
import cn.apisium.eim.plugin.EIMPluginManager
import org.pf4j.PluginManager

object EchoInMirror {
    @Suppress("MemberVisibilityCanBePrivate")
    val currentPosition: CurrentPosition = CurrentPositionImpl()
    val bus: Track = TrackImpl("Bus")
    var player: AudioPlayer = NativeAudioPlayer(currentPosition, bus, Configuration.nativeHostPath)
//    var player: AudioPlayer = JvmAudioPlayer(currentPosition, bus)

    val commandManager: CommandManager = CommandManagerImpl()
    @Suppress("unused")
    val pluginManager: PluginManager = EIMPluginManager()
    val windowManager: WindowManager = WindowManagerImpl()
    val audioProcessorManager: AudioProcessorManager = AudioProcessorManagerImpl()
    val undoManager: UndoManager = UndoManagerImpl()
    var quantification by mutableStateOf(defaultQuantification)

    var selectedTrack by mutableStateOf<Track?>(null)
}
