package cn.apisium.eim.impl

import androidx.compose.animation.animateColorAsState
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
import cn.apisium.eim.api.Track
import cn.apisium.eim.api.window.Panel
import cn.apisium.eim.api.window.WindowManager
import cn.apisium.eim.window.editor.Editor
import cn.apisium.eim.window.Mixer
import cn.apisium.eim.window.dialogs.QuickLoadDialog
import cn.apisium.eim.window.dialogs.settings.SettingsWindow
import java.lang.ref.WeakReference
import cn.apisium.eim.window.UndoList
import cn.apisium.eim.window.editor.backingTracks

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
                var modifier: Modifier = Modifier
                var isOpened by remember { mutableStateOf(false) }
                if (dialog.hasOverlay) {
                    modifier = Modifier.fillMaxSize()
                    val backgroundAnimation = animateColorAsState(if (dialog.isClosed || !isOpened)
                        Color.Transparent else Color.Black.copy(0.36F), tween(150))
                    modifier = modifier.background(backgroundAnimation.value)
                }
                if (dialog.onClose != null) modifier = modifier.fillMaxSize()
                    .onPointerEvent(PointerEventType.Press) { dialog.onClose.invoke(key) }
                Box(modifier)
                val alpha by animateFloatAsState(if (dialog.isClosed || !isOpened) 0F else 1F) {
                    if (dialog.isClosed) floatingDialogs.remove(key)
                }
                if (dialog.position == null)
                    Box(Modifier.fillMaxSize().graphicsLayer(alpha = alpha, shadowElevation = if (alpha == 1F) 0F else 5F),
                        contentAlignment = Alignment.Center) { dialog.content() }
                else Layout({
                    Box(Modifier.graphicsLayer(alpha = alpha, shadowElevation = if (alpha == 1F) 0F else 5F)) { dialog.content() }
                }) { measurables, constraints ->
                    val c = Constraints(0, constraints.maxWidth - dialog.position.x.toInt(),
                        0, constraints.maxHeight - dialog.position.y.toInt() - 25)
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
}
