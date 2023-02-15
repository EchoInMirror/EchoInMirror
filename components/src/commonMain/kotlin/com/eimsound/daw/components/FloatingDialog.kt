package com.eimsound.daw.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.PointerMatcher
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.onClick
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerIconDefaults
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import com.eimsound.daw.components.utils.Zero

private data class FloatingDialog(val onClose: ((Any) -> Unit)?, val position: Offset?,
                                  val hasOverlay: Boolean, val overflow: Boolean, val content: @Composable () -> Unit) {
    var isClosed by mutableStateOf(false)
}

class FloatingDialogProvider {
    private val floatingDialogs = mutableStateMapOf<Any, FloatingDialog>()
    fun openFloatingDialog(onClose: ((Any) -> Unit)? = null, position: Offset? = null, key: Any? = null,
                           hasOverlay: Boolean = false, overflow: Boolean = false, content: @Composable () -> Unit): Any {
        val k = key ?: Any()
        floatingDialogs[k] = FloatingDialog(onClose, position, hasOverlay, overflow, content)
        return k
    }
    fun closeFloatingDialog(key: Any) {
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
                    if (dialog.overflow) {
                        val width = constraints.maxWidth - 12
                        val height = constraints.maxHeight - 25
                        val placeable = measurables.firstOrNull()?.measure(Constraints(0, width, 0, height))
                        val pw = placeable?.width ?: 0
                        val ph = placeable?.height ?: 0
                        layout(pw, ph) {
                            placeable?.place(if (dialog.position.x + pw > width) width - pw else dialog.position.x.toInt(),
                                if (dialog.position.y + ph > height) height - ph else dialog.position.y.toInt(), 5F)
                        }
                    } else {
                        var height = constraints.maxHeight - dialog.position.y.toInt() - 25
                        var width = constraints.maxWidth - dialog.position.x.toInt() - 12
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
                }
                remember { isOpened = true }
            }
        }
    }
}

val LocalFloatingDialogProvider = staticCompositionLocalOf { FloatingDialogProvider() }

@Composable
fun FloatingDialog(dialogContent: @Composable (size: Size, closeDialog: () -> Unit) -> Unit,
                   modifier: Modifier = Modifier, enabled: Boolean = true,
                   hasOverlay: Boolean = false, isCentral: Boolean = false,
                   content: @Composable BoxScope.() -> Unit) {
    @OptIn(ExperimentalFoundationApi::class)
    FloatingDialog(dialogContent, modifier, enabled, hasOverlay, isCentral, PointerMatcher.Primary, content)
}

@OptIn(ExperimentalComposeUiApi::class, ExperimentalFoundationApi::class)
@Composable
fun FloatingDialog(dialogContent: @Composable (size: Size, closeDialog: () -> Unit) -> Unit,
                   modifier: Modifier = Modifier, enabled: Boolean = true,
                   hasOverlay: Boolean = false, isCentral: Boolean = false,
                   matcher: PointerMatcher = PointerMatcher.Primary,
                   content: @Composable BoxScope.() -> Unit) {
    val id = remember { Any() }
    val localFloatingDialogProvider = LocalFloatingDialogProvider.current
    val closeDialog = remember { { localFloatingDialogProvider.closeFloatingDialog(id) } }
    val offset = remember { arrayOf(Offset.Zero) }
    val size = remember { arrayOf(Size.Zero) }
    Box((if (isCentral) modifier else modifier.onGloballyPositioned {
        offset[0] = it.positionInRoot()
        size[0] = it.size.toSize()
    }).let { if (enabled) it.pointerHoverIcon(PointerIconDefaults.Hand) else it }
        .onClick(matcher = matcher) {
            localFloatingDialogProvider.openFloatingDialog({ closeDialog() },
                if (isCentral) null else offset[0] + Offset(0f, size[0].height),
                id, hasOverlay
            ) { dialogContent(size[0], closeDialog) }
        }
    ) { content() }
}

@Composable
fun Dialog(onOk: (() -> Unit)? = null, onCancel: (() -> Unit)? = null, hasPadding: Boolean = true, minWidth: Dp = 250.dp,
           modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Surface(modifier.widthIn(min = minWidth).width(IntrinsicSize.Max),
        shape = MaterialTheme.shapes.extraSmall, tonalElevation = 5.dp, shadowElevation = 5.dp) {
        val flag = onOk != null || onCancel != null
        Column(if (hasPadding) Modifier.padding(16.dp, 16.dp, 16.dp, if (flag) Dp.Zero else 16.dp) else Modifier) {
            content()
            if (flag) Row {
                Filled()
                if (onCancel != null) TextButton(onCancel) { Text("取消") }
                if (onOk != null) TextButton(onOk) { Text("确认") }
            }
        }
    }
}

@Composable
fun MenuDialog(content: @Composable ColumnScope.() -> Unit) {
    Surface(Modifier.width(IntrinsicSize.Min),
        shape = MaterialTheme.shapes.extraSmall, tonalElevation = 5.dp, shadowElevation = 5.dp) {
        Column(content = content)
    }
}
