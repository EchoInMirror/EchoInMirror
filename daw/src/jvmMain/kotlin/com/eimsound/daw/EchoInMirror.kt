package com.eimsound.daw

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.eimsound.audioprocessor.*
import com.eimsound.audioprocessor.data.AudioThumbnailCache
import com.eimsound.audioprocessor.data.defaultQuantification
import com.eimsound.audioprocessor.data.getEditUnit
import com.eimsound.daw.api.*
import com.eimsound.daw.api.processor.Bus
import com.eimsound.daw.api.processor.Track
import com.eimsound.daw.api.processor.TrackManager
import com.eimsound.daw.api.window.WindowManager
import com.eimsound.daw.impl.*
import com.eimsound.daw.plugin.EIMPluginManager
import com.eimsound.daw.utils.UndoManager
import com.eimsound.daw.utils.impl.DefaultUndoManager
import com.eimsound.daw.window.dialogs.settings.settingsTabsLoader
import org.pf4j.PluginManager

object EchoInMirror {
    @Suppress("MemberVisibilityCanBePrivate")
    val currentPosition: CurrentPosition = CurrentPositionImpl(isMainPosition = true)
    var bus: Bus? by mutableStateOf(null)
    var player: AudioPlayer? by mutableStateOf(null)
//    var player: AudioPlayer = JvmAudioPlayer(currentPosition, bus)

    val commandManager: CommandManager = CommandManagerImpl()
    @Suppress("unused")
    val pluginManager: PluginManager = EIMPluginManager()
    val windowManager: WindowManager = WindowManagerImpl()
    val undoManager: UndoManager = DefaultUndoManager()
    val audioThumbnailCache = AudioThumbnailCache(AUDIO_THUMBNAIL_CACHE_PATH.toFile())
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

    fun createAudioPlayer(): AudioPlayer {
        var ret: AudioPlayer? = null
        if (Configuration.audioDeviceFactoryName.isNotEmpty()) {
            try {
                ret = AudioPlayerManager.instance.create(Configuration.audioDeviceFactoryName,
                    Configuration.audioDeviceName, currentPosition, bus!!)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        if (ret == null) ret = AudioPlayerManager.instance.createDefaultPlayer(currentPosition, bus!!)
        var flag = false
        if (Configuration.audioDeviceFactoryName != ret.factory.name) {
            Configuration.audioDeviceFactoryName = ret.factory.name
            flag = true
        }
        if (Configuration.audioDeviceName != ret.name) {
            Configuration.audioDeviceName = ret.name
            flag = true
        }
        if (flag) Configuration.save()
        return ret
    }

    @Suppress("unused")
    fun reloadServices() {
        AudioPlayerManager.instance.reload()
        AudioProcessorManager.instance.reload()
        AudioSourceManager.instance.reload()
        ClipManager.instance.reload()
        TrackManager.instance.reload()
        settingsTabsLoader.reload()
    }

    val editUnit get() = quantification.getEditUnit(currentPosition)
}
