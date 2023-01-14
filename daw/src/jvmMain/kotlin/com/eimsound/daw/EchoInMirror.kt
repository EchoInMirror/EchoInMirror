package com.eimsound.daw

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.eimsound.audioprocessor.AudioPlayer
import com.eimsound.audioprocessor.AudioProcessorManager
import com.eimsound.audioprocessor.CurrentPosition
import com.eimsound.daw.api.*
import com.eimsound.daw.api.processor.Bus
import com.eimsound.daw.api.processor.Track
import com.eimsound.daw.api.processor.TrackManager
import com.eimsound.daw.api.window.WindowManager
import com.eimsound.daw.data.defaultQuantification
import com.eimsound.daw.impl.*
import com.eimsound.daw.impl.clips.ClipManagerImpl
import com.eimsound.daw.impl.processor.AudioProcessorManagerImpl
import com.eimsound.daw.impl.processor.TrackManagerImpl
import com.eimsound.daw.plugin.EIMPluginManager
import com.eimsound.daw.utils.UndoManager
import com.eimsound.daw.utils.impl.DefaultUndoManager
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
    val trackManager: TrackManager = TrackManagerImpl()
    val undoManager: UndoManager = DefaultUndoManager()
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
