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
import cn.apisium.eim.EchoInMirror
import cn.apisium.eim.IS_DEBUG
import cn.apisium.eim.api.Track
import cn.apisium.eim.api.window.Panel
import cn.apisium.eim.api.window.WindowManager
import cn.apisium.eim.data.midi.getMidiEvents
import cn.apisium.eim.data.midi.getNoteMessages
import cn.apisium.eim.processor.synthesizer.KarplusStrongSynthesizer
import cn.apisium.eim.window.panels.editor.Editor
import cn.apisium.eim.window.panels.Mixer
import cn.apisium.eim.window.dialogs.QuickLoadDialog
import cn.apisium.eim.window.dialogs.settings.SettingsWindow
import java.lang.ref.WeakReference
import cn.apisium.eim.window.panels.UndoList
import cn.apisium.eim.window.panels.editor.backingTracks
import java.io.File
import javax.sound.midi.MidiSystem

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
                    val c = Constraints(0, (constraints.maxWidth - dialog.position.x.toInt() - 25).coerceAtLeast(0),
                        0, (constraints.maxHeight - dialog.position.y.toInt() - 25).coerceAtLeast(0))
                    val placeable = measurables.firstOrNull()?.measure(c)
                    layout(c.maxWidth, c.maxHeight) {
                        placeable?.place(dialog.position.x.toInt(), dialog.position.y.toInt(), 5F)
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

    override fun openMainWindow() {
        if (isMainWindowOpened) return
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
        EchoInMirror.bus.subTracks.add(track)
        EchoInMirror.bus.prepareToPlay(EchoInMirror.currentPosition.sampleRate, EchoInMirror.currentPosition.bufferSize)
        EchoInMirror.player.open(EchoInMirror.currentPosition.sampleRate, EchoInMirror.currentPosition.bufferSize, 2)
        isMainWindowOpened = true
    }
}
