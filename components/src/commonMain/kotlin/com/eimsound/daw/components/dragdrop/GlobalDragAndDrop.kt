package com.eimsound.daw.components.dragdrop

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.onDrag
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.awt.datatransfer.DataFlavor
import java.io.File

class GlobalDragAndDrop {
    var dataTransfer: Any? by mutableStateOf(null)
    internal var rootPosition = Offset.Zero
    internal var clickPosition = Offset.Zero
    internal var currentPosition by mutableStateOf(Offset.Zero)
    internal var dropCallback: ((Any, Offset) -> Unit)? = null
    internal var draggingComponent: (@Composable () -> Unit)? by mutableStateOf(null)
    internal var contentColor: Color? = null

    @Composable
    fun DraggingComponent() {
        draggingComponent?.let {
            Box(Modifier.graphicsLayer(alpha = 0.9F,
                translationX = currentPosition.x - clickPosition.x,
                translationY = currentPosition.y - clickPosition.y
            )) {
                CompositionLocalProvider(LocalContentColor provides (contentColor ?: LocalContentColor.current), content = it)
            }
        }
    }
}

val LocalGlobalDragAndDrop = staticCompositionLocalOf { GlobalDragAndDrop() }

@OptIn(ExperimentalFoundationApi::class, DelicateCoroutinesApi::class)
@Composable
fun GlobalDraggable(
    onDragStart: suspend () -> Any?,
    onDragEnd: () -> Unit = { },
    onDrag: () -> Unit = { },
    modifier: Modifier = Modifier,
    draggingComponent: (@Composable () -> Unit)? = null,
    content: @Composable () -> Unit
) {
    val currentPos = remember { arrayOf(Offset.Zero) }
    val globalDragAndDrop = LocalGlobalDragAndDrop.current
    var isCurrent by remember { mutableStateOf(false) }
    val dragEnd = {
        if (isCurrent) {
            isCurrent = false
            globalDragAndDrop.draggingComponent = null
            val data = globalDragAndDrop.dataTransfer!!
            globalDragAndDrop.dataTransfer = null
            onDragEnd()
            val fn = globalDragAndDrop.dropCallback
            if (fn != null) {
                fn(data, globalDragAndDrop.currentPosition - globalDragAndDrop.rootPosition)
                globalDragAndDrop.dropCallback = null
            }
        }
    }
    val contentColor = LocalContentColor.current
    Box(modifier.onGloballyPositioned { currentPos[0] = it.positionInRoot() }.onDrag(onDragStart = {
        GlobalScope.launch {
            val data = onDragStart() ?: return@launch
            isCurrent = true
            globalDragAndDrop.dataTransfer = data
            globalDragAndDrop.clickPosition = it
            globalDragAndDrop.draggingComponent = draggingComponent ?: content
            globalDragAndDrop.contentColor = contentColor
            globalDragAndDrop.rootPosition = currentPos[0] + it
            globalDragAndDrop.currentPosition = globalDragAndDrop.rootPosition
        }
    }, onDragEnd = dragEnd, onDragCancel = dragEnd) {
        globalDragAndDrop.currentPosition += it
        onDrag()
    }) { content() }
}

@Composable
fun GlobalDropTarget(onDrop: ((Any, Offset) -> Unit)?, modifier: Modifier = Modifier, content: @Composable (Offset?) -> Unit) {
    var currentPos by remember { mutableStateOf(Rect.Zero) }
    Box(modifier.onGloballyPositioned { currentPos = it.boundsInRoot() }) {
        val globalDragAndDrop = LocalGlobalDragAndDrop.current
        val data = globalDragAndDrop.dataTransfer
        val isInBounds = data != null && currentPos.contains(globalDragAndDrop.currentPosition)
        if (isInBounds) {
            globalDragAndDrop.dropCallback = onDrop
            content(globalDragAndDrop.currentPosition - currentPos.topLeft)
        } else content(null)
    }
}

@Composable
fun FileDraggable(file: File, modifier: Modifier = Modifier, draggingComponent: (@Composable () -> Unit)? = null, content: @Composable () -> Unit) {
    GlobalDraggable({ file }, modifier = modifier, draggingComponent = draggingComponent, content = content)
}

@Composable
fun FileDropTarget(onDrop: (File, Offset) -> Unit, modifier: Modifier = Modifier, content: @Composable (Offset?) -> Unit) {
    var pos by remember { mutableStateOf(Offset.Zero) }
    var isCurrent by remember { mutableStateOf(false) }
    val globalDragAndDrop = LocalGlobalDragAndDrop.current
    GlobalDropTarget({ data, p -> if (data is File) onDrop(data, p) }, modifier.dropTarget(
        onDragStarted = { data, p ->
            if (data.transferable.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
                globalDragAndDrop.dataTransfer = (data.transferable.getTransferData(DataFlavor.javaFileListFlavor) as?
                        List<*>)?.firstOrNull() as File?
                pos = p
                isCurrent = true
                true
            } else false
        },
        onDragMoved = { if (isCurrent && globalDragAndDrop.dataTransfer != null) pos = it },
        onDragEnded = {
            if (isCurrent) {
                isCurrent = false
                globalDragAndDrop.dataTransfer = null
            }
        }
    ) { _, p ->
        if (isCurrent || globalDragAndDrop.dataTransfer == null) false
        else {
            onDrop(globalDragAndDrop.dataTransfer as File, p)
            globalDragAndDrop.dataTransfer = null
            true
        }
    }) { content(if (isCurrent) pos else it) }
}
