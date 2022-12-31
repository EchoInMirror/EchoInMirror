package cn.apisium.eim.impl

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.ComposeWindow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.Constraints
import cn.apisium.eim.*
import cn.apisium.eim.api.DefaultProjectInformation
import cn.apisium.eim.api.processor.Track
import cn.apisium.eim.api.window.Panel
import cn.apisium.eim.api.window.WindowManager
import cn.apisium.eim.data.midi.getMidiEvents
import cn.apisium.eim.data.midi.getNoteMessages
import cn.apisium.eim.impl.processor.BusImpl
import cn.apisium.eim.impl.processor.TrackImpl
import cn.apisium.eim.impl.processor.players.NativeAudioPlayer
import cn.apisium.eim.processor.synthesizer.KarplusStrongSynthesizer
import cn.apisium.eim.window.panels.editor.Editor
import cn.apisium.eim.window.panels.Mixer
import cn.apisium.eim.window.dialogs.QuickLoadDialog
import cn.apisium.eim.window.dialogs.settings.SettingsWindow
import java.lang.ref.WeakReference
import cn.apisium.eim.window.panels.UndoList
import cn.apisium.eim.window.panels.editor.backingTracks
import java.io.File
import java.nio.file.Path
import javax.sound.midi.MidiSystem
import kotlin.io.path.absolutePathString

private data class FloatingDialog(val onClose: ((Any) -> Unit)?, val position: Offset?,
    val hasOverlay: Boolean, val content: @Composable () -> Unit) {
    var isClosed by mutableStateOf(false)
}

class WindowManagerImpl: WindowManager {
    private val floatingDialogs = mutableStateMapOf<Any, FloatingDialog>()
    override val dialogs = mutableStateMapOf<@Composable () -> Unit, Boolean>()
    override val panels = mutableStateListOf(Mixer, Editor, UndoList)
    override var mainWindow: WeakReference<ComposeWindow> = WeakReference(null)
    override var isDarkTheme by mutableStateOf(false)
    override var activePanel: Panel? = null
    override var isMainWindowOpened by mutableStateOf(false)

    init {
        dialogs[SettingsWindow] = false
        dialogs[QuickLoadDialog] = false
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

    override fun openFloatingDialog(onClose: ((Any) -> Unit)?, position: Offset?, key: Any?,
                                        hasOverlay: Boolean, content: @Composable () -> Unit): Any {
        val k = key ?: Any()
        floatingDialogs[k] = FloatingDialog(onClose, position, hasOverlay, content)
        return k
    }

    override fun closeFloatingDialog(key: Any) {
        val dialog = floatingDialogs[key] ?: return
        dialog.isClosed = true
    }

    @OptIn(ExperimentalComposeUiApi::class)
    @Composable
    fun FloatingDialogs() {
        floatingDialogs.forEach { (key, dialog) ->
            Box {
                var isOpened by remember { mutableStateOf(false) }
                val alpha by animateFloatAsState(if (dialog.isClosed || !isOpened) 0F else 1F, tween(300)) {
                    if (dialog.isClosed) floatingDialogs.remove(key)
                }
                var modifier = if (dialog.hasOverlay) Modifier.fillMaxSize().background(Color.Black.copy(alpha * 0.26F))
                    else Modifier
                if (dialog.onClose != null) modifier = modifier.fillMaxSize()
                    .onPointerEvent(PointerEventType.Press) { dialog.onClose.invoke(key) }
                Box(modifier)
                if (dialog.position == null)
                    Box(Modifier.fillMaxSize().graphicsLayer(alpha = alpha), contentAlignment = Alignment.Center) { dialog.content() }
                else Layout({
                    Box(Modifier.graphicsLayer(alpha = alpha, shadowElevation = if (alpha == 1F) 0F else 5F)) { dialog.content() }
                }) { measurables, constraints ->
                    var height = constraints.maxHeight - dialog.position.y.toInt() - 25
                    var width = constraints.maxWidth - dialog.position.x.toInt() - 25
                    val isTooSmall = height < 40
                    val isTooRight = width < 100
                    if (isTooSmall) height = constraints.maxHeight - 50
                    if (isTooRight) width = constraints.maxWidth - 100
                    val c = Constraints(0, width, 0, height)
                    val placeable = measurables.firstOrNull()?.measure(c)
                    layout(c.maxWidth, c.maxHeight) {
                        placeable?.place(dialog.position.x.toInt() - (if (isTooRight) placeable.width else 0),
                            dialog.position.y.toInt() - (if (isTooSmall) placeable.height + 25 else 0), 5F)
                    }
                }
                remember { isOpened = true }
            }
        }
    }

    override fun clearTrackUIState(track: Track) {
        if (EchoInMirror.selectedTrack == track) EchoInMirror.selectedTrack = null
        backingTracks.remove(track)
    }

    override fun closeMainWindow() {
        isMainWindowOpened = false
        mainWindow.get()?.dispose()
        EchoInMirror.player?.close()
        EchoInMirror.bus?.close()
        EchoInMirror.player = null
        EchoInMirror.bus = null
    }

    override fun openProject(path: Path) {
        if (isMainWindowOpened) return

        val absolutePath = path.absolutePathString()
        recentProjects.remove(absolutePath)
        recentProjects.add(0, absolutePath)
        saveRecentProjects()

        val track = TrackImpl("Track 1")
        val subTrack1 = TrackImpl("SubTrack 1")
        val subTrack2 = TrackImpl("SubTrack 2")
        subTrack1.preProcessorsChain.add(KarplusStrongSynthesizer())
        EchoInMirror.selectedTrack = track
        if (IS_DEBUG) {
            val midi =
                MidiSystem.getSequence(File("E:\\Midis\\UTMR&C VOL 1-14 [MIDI FILES] for other DAWs FINAL by Hunter UT\\VOL 13\\13.Darren Porter - To Feel Again LD.mid"))
            track.notes.addAll(getNoteMessages(midi.getMidiEvents(1)))
        }

        track.subTracks.add(subTrack1)
        track.subTracks.add(subTrack2)
        val bus = BusImpl(DefaultProjectInformation(path))
        EchoInMirror.bus = bus
        bus.subTracks.add(track)
        bus.prepareToPlay(EchoInMirror.currentPosition.sampleRate, EchoInMirror.currentPosition.bufferSize)
        val player = NativeAudioPlayer(EchoInMirror.currentPosition, bus, Configuration.nativeHostPath)
        player.open(EchoInMirror.currentPosition.sampleRate, EchoInMirror.currentPosition.bufferSize, 2)
        EchoInMirror.player = player

//        if (IS_DEBUG) Thread {
//            runBlocking {
//                launch {
//                    delay(2000)
//                    var proQ: NativeAudioPluginImpl? = null
//                    var spire: NativeAudioPluginImpl? = null
//                    EchoInMirror.audioProcessorManager.nativeAudioPluginManager.descriptions.forEach {
//                        if (it.name == "FabFilter Pro-Q 3") proQ = NativeAudioPluginImpl(it)
//                        if (it.name == "Spire-1.5") spire = NativeAudioPluginImpl(it)
//                    }
//                    proQ!!.launch()
//                    spire!!.launch()
//                    subTrack2.preProcessorsChain.add(spire!!)
//                    track.postProcessorsChain.add(proQ!!)
//
//                }
//            }
//        }.start()
        isMainWindowOpened = true
    }
}
