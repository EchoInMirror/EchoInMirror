package com.eimsound.daw.components.dragdrop

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.onDrag
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import java.awt.datatransfer.DataFlavor
import java.io.File

class GlobalDragAndDrop {
    var dataTransfer: Any? by mutableStateOf(null)
    internal var rootPosition = Offset.Zero
    internal var clickPosition = Offset.Zero
    internal var currentPosition by mutableStateOf(Offset.Zero)
    internal var dropCallback: ((Any, Offset) -> Unit)? = null
    internal var draggingComponent: (@Composable () -> Unit)? by mutableStateOf(null)

    @Composable
    fun DraggingComponent() {
        draggingComponent?.let {
            Box(Modifier.graphicsLayer(
                alpha = 0.9F,
                translationX = currentPosition.x - clickPosition.x,
                translationY = currentPosition.y - clickPosition.y
            )) { it() }
        }
    }
}

val LocalGlobalDragAndDrop = staticCompositionLocalOf { GlobalDragAndDrop() }

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun GlobalDraggable(
    onDragStart: () -> Any?,
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
    Box(modifier.onGloballyPositioned { currentPos[0] = it.positionInRoot() }.onDrag(onDragStart = {
        val data = onDragStart() ?: return@onDrag
        isCurrent = true
        globalDragAndDrop.dataTransfer = data
        globalDragAndDrop.draggingComponent = draggingComponent ?: content
        globalDragAndDrop.clickPosition = it
        globalDragAndDrop.rootPosition = currentPos[0] + it
        globalDragAndDrop.currentPosition = globalDragAndDrop.rootPosition
    }, onDragEnd = dragEnd, onDragCancel = dragEnd) {
        globalDragAndDrop.currentPosition += it
        onDrag()
    }) { content() }
}

@Composable
fun GlobalDropTarget(onDrop: ((Any, Offset) -> Unit)?, modifier: Modifier = Modifier, content: @Composable (Any?, Offset) -> Unit) {
    var currentPos by remember { mutableStateOf(Rect.Zero) }
    Box(modifier.onGloballyPositioned { currentPos = it.boundsInRoot() }) {
        val globalDragAndDrop = LocalGlobalDragAndDrop.current
        val isInBounds = globalDragAndDrop.dataTransfer != null && currentPos.contains(globalDragAndDrop.currentPosition)
        if (isInBounds) {
            globalDragAndDrop.dropCallback = onDrop
            content(globalDragAndDrop.dataTransfer, globalDragAndDrop.currentPosition - currentPos.topLeft)
        } else content(null, Offset.Zero)
    }
}

@Composable
fun FileDraggable(file: File, modifier: Modifier = Modifier, draggingComponent: (@Composable () -> Unit)? = null, content: @Composable () -> Unit) {
    GlobalDraggable({ file }, modifier = modifier, draggingComponent = draggingComponent, content = content)
}

@Composable
fun FileDropTarget(onDrop: (File, Offset) -> Unit, modifier: Modifier = Modifier, content: @Composable (File?, Offset) -> Unit) {
    var file by remember { mutableStateOf<File?>(null) }
    var pos by remember { mutableStateOf(Offset.Zero) }
    GlobalDropTarget({ data, p -> if (data is File) onDrop(data, p) }, modifier.dropTarget(
        onDragStarted = { data, p ->
            if (data.transferable.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
                file = (data.transferable.getTransferData(DataFlavor.javaFileListFlavor) as? List<*>)?.firstOrNull() as File?
                pos = p
                true
            } else false
        },
        onDragMoved = { if (file != null) pos = it },
        onDragEnded = { file = null }
    ) { _, p ->
        if (file == null) false
        else {
            onDrop(file!!, p)
            file = null
            true
        }
    }) { data, p -> content(data as? File, p) }
}
