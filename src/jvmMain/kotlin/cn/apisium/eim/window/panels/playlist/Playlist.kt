package cn.apisium.eim.window.panels.playlist

import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.gestures.forEachGesture
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastAll
import androidx.compose.ui.zIndex
import cn.apisium.eim.EchoInMirror
import cn.apisium.eim.api.processor.Track
import cn.apisium.eim.api.projectDisplayPPQ
import cn.apisium.eim.components.*
import cn.apisium.eim.utils.openMaxValue
import cn.apisium.eim.window.panels.editor.calcScroll
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

var noteWidth = mutableStateOf(0.2.dp)
var trackHeight by mutableStateOf(70.dp)
val verticalScrollState = ScrollState(0)
val horizontalScrollState = ScrollState(0).apply {
    openMaxValue = (noteWidth.value * EchoInMirror.currentPosition.projectDisplayPPQ).value.toInt()
}

@Composable
private fun TrackContent(track: Track, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().height(trackHeight)) {
        track.clips.forEach {
            Box(Modifier
                .size(noteWidth.value * it.duration, trackHeight)
                .absoluteOffset(noteWidth.value * it.time)
                .background(track.color.copy(0.1F))
                .border(0.5.dp, track.color, MaterialTheme.shapes.extraSmall)
                .clip(MaterialTheme.shapes.extraSmall)
            ) {
                @Suppress("TYPE_MISMATCH")
                it.clip.factory.playlistContent(it.clip, track, trackHeight, noteWidth)
            }
        }
    }
    Divider()
    track.subTracks.forEach { key(it.id) { TrackContent(it, modifier) } }
}

@Suppress("DuplicatedCode")
private suspend fun PointerInputScope.handleMouseEvent(coroutineScope: CoroutineScope) {
    forEachGesture {
        awaitPointerEventScope {
            var event: PointerEvent
            do {
                event = awaitPointerEvent(PointerEventPass.Initial)
                when (event.type) {
                    PointerEventType.Scroll -> {
                        calcScroll(event, noteWidth, horizontalScrollState, coroutineScope) {
                            val newValue = (trackHeight.value + (if (it.scrollDelta.y > 0) -2 else 2)).coerceIn(28F, 100F)
                            if (newValue == trackHeight.value) return@calcScroll
                            val y = it.position.x
                            val oldY = (y + verticalScrollState.value) / trackHeight.toPx()
                            trackHeight = newValue.dp
                            coroutineScope.launch {
                                val trackHeightPx = trackHeight.toPx()
                                verticalScrollState.scrollBy(
                                    (oldY - (y + verticalScrollState.value) / trackHeightPx) * trackHeightPx
                                )
                            }
                        }
                        continue
                    }
                    else -> {}
                }
            } while (!event.changes.fastAll(PointerInputChange::changedToDownIgnoreConsumed))
            val down = event.changes[0]

            var drag: PointerInputChange?
            do {
                @Suppress("INVISIBLE_MEMBER")
                drag = awaitPointerSlopOrCancellation(down.id, down.type,
                    triggerOnMainAxisSlop = false) { change, _ -> change.consume() }
            } while (drag != null && !drag.isConsumed)
            if (drag == null) return@awaitPointerEventScope
            drag(drag.id) {
                it.consume()
            }
        }
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
                        cn.apisium.eim.components.silder.Slider(
                            noteWidth.value.value / 0.4f,
                            { noteWidth.value = 0.4.dp * it },
                            valueRange = 0.15f..8f
                        )
                    }
                }
                TrackItems()
            }
        }
        Box(Modifier.fillMaxSize()) {
            Column {
                var contentWidth by remember { mutableStateOf(0.dp) }
                var cursorOffsetY by remember { mutableStateOf(0.dp) }
                val localDensity = LocalDensity.current
                val coroutineScope = rememberCoroutineScope()
                Timeline(Modifier.zIndex(3f), noteWidth, horizontalScrollState, true)
                Box(Modifier.weight(1f).onGloballyPositioned {
                    with(localDensity) {
                        contentWidth = it.size.width.toDp()
                        cursorOffsetY = it.size.height.toDp()
                    }
                }.pointerInput(Unit) { handleMouseEvent(coroutineScope) }) {
                    EditorGrid(noteWidth, horizontalScrollState)
                    val width = noteWidth.value * EchoInMirror.currentPosition.projectDisplayPPQ
                    remember(width, localDensity) {
                        with (localDensity) { horizontalScrollState.openMaxValue = width.toPx().toInt() }
                    }
                    Column(Modifier.horizontalScroll(horizontalScrollState).verticalScroll(verticalScrollState)
                        .width(width)) {
                        Divider()
                        EchoInMirror.bus!!.subTracks.forEach {
                            key(it.id) { TrackContent(it) }
                        }
                        TextButton({ }, Modifier.width(0.dp)) { }
                    }
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