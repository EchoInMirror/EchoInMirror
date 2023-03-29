package com.eimsound.daw.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.PointerMatcher
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.*
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
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.unit.*
import com.eimsound.daw.components.utils.Zero
import com.eimsound.daw.utils.BasicEditor

private data class FloatingLayer(val onClose: ((Any) -> Unit)?, val position: Offset?,
                                 val hasOverlay: Boolean, val overflow: Boolean, val content: @Composable () -> Unit) {
    var isClosed by mutableStateOf(false)
    var isShow by mutableStateOf(false)
}

class FloatingLayerProvider {
    private val floatingLayers = mutableStateMapOf<Any, FloatingLayer>()

    fun openFloatingLayer(onClose: ((Any) -> Unit)? = null, position: Offset? = null, key: Any? = null,
                          hasOverlay: Boolean = false, overflow: Boolean = false, content: @Composable () -> Unit): Any {
        val k = key ?: Any()
        floatingLayers[k] = FloatingLayer(onClose, position, hasOverlay, overflow, content)
        return k
    }

    fun closeFloatingLayer(key: Any) {
        val layer = floatingLayers[key] ?: return
        layer.isClosed = true
        if (!layer.isShow) floatingLayers.remove(key)
    }

    fun setFloatingLayerShow(key: Any, show: Boolean) { (floatingLayers[key] ?: return).isShow = show }

    @OptIn(ExperimentalComposeUiApi::class)
    @Composable
    fun FloatingLayers() {
        floatingLayers.forEach { (key, layer) ->
            Box {
                val alpha by animateFloatAsState(if (layer.isClosed || !layer.isShow) 0F else 1F, tween(300)) {
                    if (layer.isClosed) floatingLayers.remove(key)
                }
                var modifier = if (layer.hasOverlay) Modifier.fillMaxSize().background(Color.Black.copy(alpha * 0.26F))
                else Modifier
                if (layer.onClose != null) modifier = modifier.fillMaxSize()
                    .onPointerEvent(PointerEventType.Press) { layer.onClose.invoke(key) }
                Box(modifier)
                if (layer.position == null)
                    Box(Modifier.fillMaxSize().graphicsLayer(alpha = alpha), contentAlignment = Alignment.Center) { layer.content() }
                else Layout({
                    Box(Modifier.graphicsLayer(alpha = alpha, shadowElevation = if (alpha == 1F) 0F else 5F)) { layer.content() }
                }) { measurables, constraints ->
                    if (layer.overflow) {
                        val width = constraints.maxWidth - 12
                        val height = constraints.maxHeight - 25
                        val placeable = measurables.firstOrNull()?.measure(Constraints(0, width, 0, height))
                        val pw = placeable?.width ?: 0
                        val ph = placeable?.height ?: 0
                        layout(pw, ph) {
                            placeable?.place(if (layer.position.x + pw > width) width - pw else layer.position.x.toInt(),
                                if (layer.position.y + ph > height) height - ph else layer.position.y.toInt(), 5F)
                        }
                    } else {
                        var height = constraints.maxHeight - layer.position.y.toInt() - 25
                        var width = constraints.maxWidth - layer.position.x.toInt() - 12
                        val isTooSmall = height < 40
                        val isTooRight = width < 100
                        if (isTooSmall) height = constraints.maxHeight - 50
                        if (isTooRight) width = constraints.maxWidth - 100
                        val c = Constraints(0, width, 0, height)
                        val placeable = measurables.firstOrNull()?.measure(c)
                        layout(c.maxWidth, c.maxHeight) {
                            placeable?.place(layer.position.x.toInt() - (if (isTooRight) placeable.width else 0),
                                layer.position.y.toInt() - (if (isTooSmall) placeable.height + 25 else 0), 5F)
                        }
                    }
                }
                remember { layer.isShow = true }
            }
        }
    }
}

val LocalFloatingLayerProvider = staticCompositionLocalOf { FloatingLayerProvider() }

@Composable
fun FloatingLayer(layerContent: @Composable (Size, () -> Unit) -> Unit,
                  modifier: Modifier = Modifier, enabled: Boolean = true,
                  hasOverlay: Boolean = false, isCentral: Boolean = false,
                  content: @Composable BoxScope.() -> Unit) {
    @OptIn(ExperimentalFoundationApi::class)
    FloatingLayer(layerContent, modifier, enabled, hasOverlay, isCentral, PointerMatcher.Primary, content)
}

@OptIn(ExperimentalComposeUiApi::class, ExperimentalFoundationApi::class)
@Composable
fun FloatingLayer(layerContent: @Composable (Size, () -> Unit) -> Unit,
                  modifier: Modifier = Modifier, enabled: Boolean = true,
                  hasOverlay: Boolean = false, isCentral: Boolean = false,
                  matcher: PointerMatcher = PointerMatcher.Primary,
                  content: @Composable BoxScope.() -> Unit) {
    val id = remember { Any() }
    val localFloatingLayerProvider = LocalFloatingLayerProvider.current
    val closeLayer = remember { { localFloatingLayerProvider.closeFloatingLayer(id) } }
    val offset = remember { arrayOf(Offset.Zero) }
    val size = remember { arrayOf(Size.Zero) }
    Box((if (isCentral) modifier else modifier.onGloballyPositioned {
        offset[0] = it.positionInRoot()
        size[0] = it.size.toSize()
    }).let { if (enabled) it.pointerHoverIcon(PointerIconDefaults.Hand) else it }
        .onClick(matcher = matcher) {
            localFloatingLayerProvider.openFloatingLayer({ closeLayer() },
                if (isCentral) null else offset[0] + Offset(0f, size[0].height),
                id, hasOverlay
            ) { layerContent(size[0], closeLayer) }
        }
    ) { content() }
}

@Composable
fun Dialog(onOk: (() -> Unit)? = null, onCancel: (() -> Unit)? = null,
           hasPadding: Boolean = true, minWidth: Dp = 250.dp,
           modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    val flag = onOk != null || onCancel != null
    Dialog(modifier.widthIn(min = minWidth).width(IntrinsicSize.Max),
        if (hasPadding) Modifier.padding(16.dp, 16.dp, 16.dp, if (flag) Dp.Zero else 16.dp) else Modifier) {
        content()
        if (flag) Row {
            Filled()
            if (onCancel != null) TextButton(onCancel) { Text("取消") }
            if (onOk != null) TextButton(onOk) { Text("确认") }
        }
    }
}

@Composable
fun Dialog(modifier: Modifier = Modifier.width(IntrinsicSize.Min),
           columnModifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Surface(modifier.heightIn(8.dp), MaterialTheme.shapes.extraSmall, tonalElevation = 5.dp, shadowElevation = 5.dp) {
        Column(columnModifier, content = content)
    }
}

/**
 * @see com.eimsound.daw.components.initEditorMenuItems
 */
internal var editorMenuComposable: @Composable (BasicEditor, () -> Unit) -> Unit = { _, _ -> }
fun FloatingLayerProvider.openEditorMenu(position: Offset, editor: BasicEditor, content: @Composable ((() -> Unit) -> Unit)? = null) {
    val key = Any()
    val close = { closeFloatingLayer(key) }

    openFloatingLayer({ close() }, position, key, overflow = true) {
        Dialog {
            content?.invoke(close)
            editorMenuComposable(editor, close)
        }
    }
}
