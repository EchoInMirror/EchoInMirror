package com.eimsound.daw.api

import com.eimsound.audioprocessor.AudioPlayer
import com.eimsound.audioprocessor.CurrentPosition
import com.eimsound.audioprocessor.data.AudioThumbnailCache
import com.eimsound.audioprocessor.data.Quantification
import com.eimsound.daw.api.processor.Bus
import com.eimsound.daw.api.processor.Track
import com.eimsound.daw.api.window.WindowManager
import com.eimsound.daw.utils.UndoManager
import org.pf4j.PluginManager
import java.util.*

val EchoInMirror = ServiceLoader.load(IEchoInMirror::class.java).first()!!

/**
 * @see com.eimsound.daw.impl.EchoInMirrorImpl
 */
interface IEchoInMirror {
    val currentPosition: CurrentPosition
    var bus: Bus?
    var player: AudioPlayer?
//    var player: AudioPlayer = JvmAudioPlayer(currentPosition, bus)

    val commandManager: CommandManager
    val pluginManager: PluginManager
    val windowManager: WindowManager
    val undoManager: UndoManager
    val audioThumbnailCache: AudioThumbnailCache
    var quantification: Quantification

    var selectedTrack: Track?
    var selectedClip: TrackClip<*>?

    fun createAudioPlayer(): AudioPlayer

    fun reloadServices()

    val editUnit: Int
}