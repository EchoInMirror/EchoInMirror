package cn.apisium.eim.impl

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.ComposeWindow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.unit.IntOffset
import cn.apisium.eim.api.window.Panel
import cn.apisium.eim.api.window.WindowManager
import cn.apisium.eim.window.editor.Editor
import cn.apisium.eim.window.Mixer
import cn.apisium.eim.window.dialogs.QuickLoadDialog
import cn.apisium.eim.window.dialogs.settings.SettingsWindow
import java.lang.ref.WeakReference
import cn.apisium.eim.window.UndoListPanel

private data class FloatingDialog(val onClose: (() -> Unit)?, val position: Offset?,
    val hasOverlay: Boolean, val content: @Composable () -> Unit) {
    var isClosed by mutableStateOf(false)
}

class WindowManagerImpl: WindowManager {
    private val floatingDialogs = mutableStateMapOf<Any, FloatingDialog>()
    override val dialogs = mutableStateMapOf<@Composable () -> Unit, Boolean>()
    override val panels = mutableStateListOf(Mixer, Editor, UndoListPanel)
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

    @Suppress("UNCHECKED_CAST")
    override fun <T> openFloatingDialog(onClose: (() -> Unit)?, position: Offset?, key: T?,
                                        hasOverlay: Boolean, content: @Composable () -> Unit): T {
        val k = key ?: Any()
        floatingDialogs[k] = FloatingDialog(onClose, position, hasOverlay, content)
        return k as T
    }

    override fun closeFloatingDialog(key: Any) {
        val dialog = floatingDialogs[key] ?: return
        if (dialog.hasOverlay) dialog.isClosed = true
        else floatingDialogs.remove(key)
    }

    @OptIn(ExperimentalComposeUiApi::class)
    @Composable
    fun FloatingDialogs() {
        floatingDialogs.forEach { (key, dialog) ->
            Box {
                var modifier: Modifier = Modifier
                if (dialog.hasOverlay) {
                    modifier = Modifier.fillMaxSize()
                    var isOpened by remember { mutableStateOf(false) }
                    val backgroundAnimation = animateColorAsState(if (dialog.isClosed || !isOpened)
                        Color.Transparent else Color.Black.copy(0.36F), tween(150)) {
                        if (dialog.isClosed) floatingDialogs.remove(key)
                    }
                    remember { isOpened = true }
                    modifier = modifier.background(backgroundAnimation.value)
                }
                if (dialog.onClose != null) modifier = modifier.fillMaxSize().onPointerEvent(PointerEventType.Press) { dialog.onClose.invoke() }
                Box(modifier) {
                    if (dialog.position == null) dialog.content()
                    else Box(Modifier.absoluteOffset { IntOffset(dialog.position.x.toInt(), dialog.position.y.toInt()) }) { dialog.content() }
                }
            }
        }
    }
}
