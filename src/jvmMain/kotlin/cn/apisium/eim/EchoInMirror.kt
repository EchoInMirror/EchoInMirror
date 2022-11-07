package cn.apisium.eim

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import cn.apisium.eim.api.AudioPlayer
import cn.apisium.eim.api.Bus
import cn.apisium.eim.api.CurrentPosition
import cn.apisium.eim.api.processor.AudioProcessorManager
import cn.apisium.eim.api.window.WindowManager
import cn.apisium.eim.impl.BusImpl
import cn.apisium.eim.impl.WindowManagerImpl
import cn.apisium.eim.impl.processor.AudioProcessorManagerImpl
import cn.apisium.eim.impl.processor.players.JvmAudioPlayer
import cn.apisium.eim.plugin.EIMPluginManager
import org.pf4j.PluginManager

object EchoInMirror {
    @Suppress("MemberVisibilityCanBePrivate")
    val currentPosition = CurrentPosition()
    val bus: Bus = BusImpl()
    private var player_: AudioPlayer = JvmAudioPlayer(currentPosition, bus)
    var sampleRate by mutableStateOf(44800F)
    var bufferSize by mutableStateOf(1024)
    var timeSigNumerator by mutableStateOf(4)
    var timeSigDenominator by mutableStateOf(4)
    var player: AudioPlayer
        get() = player_
        set(value) { player_ = value }

    val pluginManager: PluginManager = EIMPluginManager()
    val windowManager: WindowManager = WindowManagerImpl()
    val audioProcessorManager: AudioProcessorManager = AudioProcessorManagerImpl()
}
