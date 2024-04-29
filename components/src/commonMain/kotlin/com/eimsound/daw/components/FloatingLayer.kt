package com.eimsound.daw.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.PointerMatcher
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.unit.*
import androidx.compose.ui.util.fastForEach
import com.eimsound.daw.commons.BasicEditor
import com.eimsound.daw.language.langs

private data class FloatingLayer(
    val key: Any, val onClose: ((Any) -> Unit)?, val position: Offset?,
    val hasOverlay: Boolean, val overflow: Boolean, val afterClose: (() -> Unit)? = null,
    val content: @Composable () -> Unit
) {
    var isClosed by mutableStateOf(false)
    var isShow by mutableStateOf(false)
}

class FloatingLayerProvider {
    private var floatingLayers by mutableStateOf(listOf<FloatingLayer>())

    fun openFloatingLayer(
        onClose: ((Any) -> Unit)? = null, position: Offset? = null, key: Any? = null,
        hasOverlay: Boolean = false, overflow: Boolean = false, afterClose: (() -> Unit)? = null,
        content: @Composable () -> Unit
    ): Any {
        val k = key ?: Any()
        floatingLayers += FloatingLayer(k, onClose, position, hasOverlay, overflow, afterClose, content)
        return k
    }

    fun closeFloatingLayer(key: Any): Boolean {
        var closed = false
        floatingLayers.fastForEach {
            if (it.key != key) return@fastForEach
            if (it.isShow) it.isClosed = true
            else floatingLayers = floatingLayers.filter { f -> it != f }
            closed = true
        }
        return closed
    }

    fun setFloatingLayerShow(key: Any, show: Boolean) {
        floatingLayers.fastForEach { if (it.key == key) it.isShow = show }
    }

    @OptIn(ExperimentalComposeUiApi::class)
    @Composable
    private fun FloatingSingleLayer(layer: FloatingLayer) {
        Box {
            val alpha by animateFloatAsState(if (layer.isClosed || !layer.isShow) 0F else 1F, tween(300)) {
                if (layer.isClosed) {
                    floatingLayers = floatingLayers.filter { it.key != layer.key }
                    layer.afterClose?.invoke()
                }
            }
            var modifier = if (layer.hasOverlay) Modifier.fillMaxSize().background(Color.Black.copy(alpha * 0.26F))
            else Modifier
            if (layer.onClose != null) modifier = modifier.fillMaxSize()
                .onPointerEvent(PointerEventType.Press) { layer.onClose.invoke(layer.key) }
            Box(modifier)
            if (layer.position == null)
                Box(Modifier.fillMaxSize().graphicsLayer(alpha = alpha), contentAlignment = Alignment.Center) { layer.content() }
            else Layout({
                Box(Modifier.graphicsLayer(alpha = alpha, shadowElevation = if (alpha == 1F) 0F else 5F)) { layer.content() }
            }) { measurables, constraints ->
                if (layer.overflow) {
                    val width = constraints.maxWidth - (12 * density).toInt()
                    val height = constraints.maxHeight - (26 * density).toInt()
                    val placeable = measurables.firstOrNull()?.measure(Constraints(0, width, 0, height))
                    val pw = placeable?.width ?: 0
                    val ph = placeable?.height ?: 0
                    layout(pw, ph) {
                        placeable?.place(if (layer.position.x + pw > width) width - pw else layer.position.x.toInt(),
                            if (layer.position.y + ph > height) height - ph else layer.position.y.toInt(), 5F)
                    }
                } else {
                    var height = (constraints.maxHeight - layer.position.y - 25 * density).toInt()
                    var width = (constraints.maxWidth - layer.position.x - 12 * density).toInt()
                    val isTooSmall = height < 40 * density
                    val isTooRight = width < 100 * density
                    if (isTooSmall) height = constraints.maxHeight - (50 * density).toInt()
                    if (isTooRight) width = constraints.maxWidth - (100 * density).toInt()
                    val c = Constraints(0, width, 0, height)
                    val placeable = measurables.firstOrNull()?.measure(c)
                    layout(c.maxWidth, c.maxHeight) {
                        placeable?.place(layer.position.x.toInt() - (if (isTooRight) placeable.width else 0),
                            layer.position.y.toInt() - (if (isTooSmall) placeable.height + 25 * density else 0).toInt(), 5F)
                    }
                }
            }
            remember { layer.isShow = true }
        }
    }

    @Composable
    fun FloatingLayers() {
        floatingLayers.fastForEach { layer ->
            key(layer) { FloatingSingleLayer(layer) }
        }
    }
}

val LocalFloatingLayerProvider = staticCompositionLocalOf { FloatingLayerProvider() }

@Composable
fun FloatingLayer(
    layerContent: @Composable (DpSize, () -> Unit) -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true,
    hasOverlay: Boolean = false, isCentral: Boolean = false, content: @Composable BoxScope.(Boolean) -> Unit
) {
    @OptIn(ExperimentalFoundationApi::class)
    FloatingLayer(layerContent, modifier, enabled, hasOverlay, isCentral, PointerMatcher.Primary, content = content)
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FloatingLayer(
    layerContent: @Composable (DpSize, () -> Unit) -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true,
    hasOverlay: Boolean = false, isCentral: Boolean = false, matcher: PointerMatcher = PointerMatcher.Primary,
    pass: PointerEventPass = PointerEventPass.Main, content: @Composable BoxScope.(Boolean) -> Unit
) {
    val id = remember { Any() }
    var isOpened by remember { mutableStateOf(false) }
    val localFloatingLayerProvider = LocalFloatingLayerProvider.current
    val closeLayer = remember { {
        isOpened = false
        localFloatingLayerProvider.closeFloatingLayer(id)
    } }
    val offset = remember { arrayOf(Offset.Zero) }
    val size = remember { arrayOf(Size.Zero) }
    Box((if (isCentral) modifier else modifier.onPlaced {
        offset[0] = it.positionInRoot()
        size[0] = it.size.toSize()
    }).let { if (enabled) it.pointerHoverIcon(PointerIcon.Hand) else it }
        .pointerInput(pass) {
            awaitPointerEventScope {
                while (true) {
                    val event = awaitPointerEvent(pass)
                    if (event.type != PointerEventType.Press || !matcher.matches(event)) continue
                    isOpened = true
                    localFloatingLayerProvider.openFloatingLayer({ closeLayer() },
                        if (isCentral) null else offset[0] + Offset(0f, size[0].height), id, hasOverlay
                    ) { layerContent(size[0].toDpSize(), closeLayer) }
                }
            }
        }
    ) { content(isOpened) }
}

@Composable
fun Dialog(
    onOk: (() -> Unit)? = null, onCancel: (() -> Unit)? = null, hasPadding: Boolean = true, minWidth: Dp = 250.dp,
    modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit
) {
    val flag = onOk != null || onCancel != null
    Dialog(modifier.widthIn(min = minWidth).width(IntrinsicSize.Max),
        if (hasPadding) Modifier.padding(16.dp, 16.dp, 16.dp, if (flag) 0.dp else 16.dp) else Modifier) {
        content()
        if (flag) Row {
            Filled()
            if (onCancel != null) {
                TextButton(onCancel,
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.secondary)
                ) { Text(langs.cancel) }
            }
            if (onOk != null) TextButton(onOk) { Text(langs.ok) }
        }
    }
}

@Composable
fun Dialog(
    modifier: Modifier = Modifier.width(IntrinsicSize.Min),
    columnModifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit
) {
    Surface(modifier.heightIn(8.dp), MaterialTheme.shapes.extraSmall, tonalElevation = 5.dp, shadowElevation = 5.dp) {
        Column(columnModifier, content = content)
    }
}

private var lastMenuOpenedTime = 0L
/**
 * @see com.eimsound.daw.components.initEditorMenuItems
 */
internal var editorMenuComposable: @Composable (BasicEditor, Boolean, () -> Unit) -> Unit = { _, _, _ -> }
fun FloatingLayerProvider.openEditorMenu(
    position: Offset, editor: BasicEditor, showIcon: Boolean = true,
    footer: @Composable ((() -> Unit) -> Unit)? = null,
    content: @Composable ((() -> Unit) -> Unit)? = null
) {
    val t = System.currentTimeMillis()
    if (t - lastMenuOpenedTime < 100) return
    lastMenuOpenedTime = t

    val key = Any()
    val close: () -> Unit = { closeFloatingLayer(key) }

    openFloatingLayer({ close() }, position, key, overflow = true) {
        Dialog {
            content?.invoke(close)
            editorMenuComposable(editor, showIcon, close)
            footer?.invoke(close)
        }
    }
}
