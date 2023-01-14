package com.eimsound.daw.window.panels.playlist

import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.eimsound.audioprocessor.projectDisplayPPQ
import com.eimsound.daw.EchoInMirror
import com.eimsound.daw.components.*
import com.eimsound.daw.components.utils.HorizontalResize
import com.eimsound.daw.components.utils.Move
import com.eimsound.daw.utils.openMaxValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

enum class EditAction {
    NONE, MOVE, RESIZE, SELECT, DELETE;
    @OptIn(ExperimentalComposeUiApi::class)
    fun toPointerIcon(default: PointerIcon = PointerIconDefaults.Default) = when(this) {
        MOVE -> PointerIconDefaults.Move
        RESIZE -> PointerIconDefaults.HorizontalResize
        else -> default
    }
}

var noteWidth = mutableStateOf(0.2.dp)
var trackHeight by mutableStateOf(70.dp)
val verticalScrollState = ScrollState(0)
var contentWidth by mutableStateOf(0.dp)
val horizontalScrollState = ScrollState(0).apply {
    openMaxValue = (noteWidth.value * EchoInMirror.currentPosition.projectDisplayPPQ).value.toInt()
}

val noteWidthRange = 0.02f..5f
val noteWidthSliderRange = (noteWidthRange.start / 0.4F)..(noteWidthRange.endInclusive / 0.4F)
fun Density.calcScroll(event: PointerEvent, noteWidth: MutableState<Dp>, horizontalScrollState: ScrollState,
                       coroutineScope: CoroutineScope, onVerticalScroll: (PointerInputChange) -> Unit) {
    if (event.keyboardModifiers.isShiftPressed) return
    if (event.keyboardModifiers.isCtrlPressed) {
        val change = event.changes[0]
        if (event.keyboardModifiers.isAltPressed) onVerticalScroll(change)
        else {
            val x = change.position.x
            val oldX = (x + horizontalScrollState.value) / noteWidth.value.toPx()
            val newValue = (noteWidth.value.value +
                    (if (change.scrollDelta.y > 0) -0.05F else 0.05F)).coerceIn(noteWidthRange)
            if (newValue != noteWidth.value.value) {
                horizontalScrollState.openMaxValue =
                    (noteWidth.value * EchoInMirror.currentPosition.projectDisplayPPQ).toPx().toInt()
                noteWidth.value = newValue.dp
                coroutineScope.launch {
                    val noteWidthPx = noteWidth.value.toPx()
                    horizontalScrollState.scrollBy(
                        (oldX - (x + horizontalScrollState.value) / noteWidthPx) * noteWidthPx
                    )
                }
            }
        }
        change.consume()
    }
}

@Composable
fun Playlist() {
    Row {
        Surface(Modifier.width(200.dp).fillMaxHeight().zIndex(5f), shadowElevation = 2.dp, tonalElevation = 2.dp) {
            Column {
                Surface(shadowElevation = 2.dp, tonalElevation = 4.dp) {
                    Row(Modifier.height(TIMELINE_HEIGHT).fillMaxWidth().padding(horizontal = 10.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        com.eimsound.daw.components.silder.Slider(
                            noteWidth.value.value / 0.4f,
                            { noteWidth.value = 0.4.dp * it },
                            valueRange = noteWidthSliderRange
                        )
                    }
                }
                TrackItems()
            }
        }
        Box(Modifier.fillMaxSize()) {
            Column {
                val localDensity = LocalDensity.current
                Timeline(Modifier.zIndex(3f), noteWidth, horizontalScrollState, EchoInMirror.currentPosition.projectRange) {
                    EchoInMirror.currentPosition.projectRange = it
                }
                val coroutineScope = rememberCoroutineScope()
                Box(Modifier.weight(1f).pointerInput(coroutineScope) { handleMouseEvent(coroutineScope) }
                    .onGloballyPositioned { with(localDensity) { contentWidth = it.size.width.toDp() } }
                ) {
                    EditorGrid(noteWidth, horizontalScrollState, EchoInMirror.currentPosition.projectRange)
                    val width = (noteWidth.value * EchoInMirror.currentPosition.projectDisplayPPQ)
                        .coerceAtLeast(contentWidth)
                    remember(width, localDensity) {
                        with (localDensity) { horizontalScrollState.openMaxValue = width.toPx().toInt() }
                    }
                    Column(Modifier.verticalScroll(verticalScrollState).fillMaxSize()) {
                        Divider()
                        var i = 0
                        EchoInMirror.bus!!.subTracks.forEach {
                            key(it.id) { i += TrackContent(it, i, localDensity) }
                        }
                    }
                    TrackSelection(localDensity)
                    PlayHead(noteWidth, horizontalScrollState, contentWidth)
                    VerticalScrollbar(
                        rememberScrollbarAdapter(verticalScrollState),
                        Modifier.align(Alignment.TopEnd).fillMaxHeight()
                    )
                }
            }
            HorizontalScrollbar(
                rememberScrollbarAdapter(horizontalScrollState),
                Modifier.align(Alignment.TopStart).fillMaxWidth()
            )
        }
    }
}