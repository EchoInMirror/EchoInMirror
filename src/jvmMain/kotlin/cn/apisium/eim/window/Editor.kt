package cn.apisium.eim.window

import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Piano
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.*
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import cn.apisium.eim.EchoInMirror
import cn.apisium.eim.api.window.Panel
import cn.apisium.eim.api.window.PanelDirection
import cn.apisium.eim.components.Keyboard
import cn.apisium.eim.components.PlayHead
import cn.apisium.eim.components.Timeline
import cn.apisium.eim.components.splitpane.VerticalSplitPane
import cn.apisium.eim.components.splitpane.rememberSplitPaneState
import cn.apisium.eim.data.midi.noteOff
import cn.apisium.eim.data.midi.noteOn
import cn.apisium.eim.utils.inverts

@Composable
private fun NotesEditorCanvas(
    modifier: Modifier,
    noteHeight: Dp,
    noteWidth: Dp,
    verticalScrollState: ScrollState,
    horizontalScrollState: ScrollState
) {
    val outlineColor = MaterialTheme.colorScheme.surfaceVariant
    val barsOutlineColor = MaterialTheme.colorScheme.outlineVariant
    val highlightNoteColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.03F)
    val timeSigDenominator = EchoInMirror.currentPosition.timeSigDenominator
    val timeSigNumerator = EchoInMirror.currentPosition.timeSigNumerator
    val ppq = EchoInMirror.currentPosition.ppq
    Canvas(modifier.scrollable(verticalScrollState, Orientation.Vertical, reverseDirection = true)) {
        val noteHeightPx = noteHeight.toPx()
        val noteWidthPx = noteWidth.toPx()
        val verticalScrollValue = verticalScrollState.value
        val horizontalScrollValue = horizontalScrollState.value
        val startYNote = (verticalScrollValue / noteHeightPx).toInt()
        val endYNote = ((verticalScrollValue + size.height) / noteHeightPx).toInt()
        val beatsWidth = noteWidthPx * ppq
        for (i in startYNote..endYNote) {
            val y = i * noteHeightPx - verticalScrollValue
            if (y < 0) continue
            drawLine(
                color = outlineColor,
                start = Offset(0f, y),
                end = Offset(size.width, y),
                strokeWidth = 1F
            )
            if (cn.apisium.eim.components.scales[i % 12]) {
                drawRect(
                    color = highlightNoteColor,
                    topLeft = Offset(0f, y),
                    size = Size(size.width, noteHeightPx)
                )
            }
        }
        val drawBeats = noteWidthPx > 0.1F
        val horizontalDrawWidth = if (drawBeats) beatsWidth else beatsWidth * timeSigNumerator
        val highlightWidth = if (drawBeats) timeSigDenominator else timeSigDenominator * timeSigNumerator
        for (i in (horizontalScrollValue / horizontalDrawWidth).toInt()..((horizontalScrollValue + size.width) / horizontalDrawWidth).toInt()) {
            val x = i * horizontalDrawWidth - horizontalScrollState.value
            drawLine(
                color = if (i % highlightWidth == 0) barsOutlineColor else outlineColor,
                start = Offset(x, 0f),
                end = Offset(x, size.height),
                strokeWidth = 1F
            )
        }

        EchoInMirror.selectedTrack?.let { track ->
            val invertsColor = track.color.inverts()
            val currentPPQ = EchoInMirror.currentPosition.timeInPPQ
            val isPlaying = EchoInMirror.currentPosition.isPlaying
            for (it in track.notes) {
                val y = (131 - it.note.note) * noteHeightPx - verticalScrollValue
                val x = it.time * noteWidthPx - horizontalScrollValue
                if (y < -noteHeightPx || y > size.height || x < 0 || x > size.width) continue
                val isPlayingNote = isPlaying && it.time <= currentPPQ && it.time + it.duration >= currentPPQ
                drawRoundRect(
                    if (isPlayingNote) invertsColor else track.color,
                    Offset(x, y),
                    Size(it.duration * noteWidthPx, noteHeightPx),
                    CornerRadius(2f)
                )
            }
        }
    }
}

@Composable
private fun editorContent(horizontalScrollState: ScrollState) {
    VerticalSplitPane(splitPaneState = rememberSplitPaneState(100F)) {
        first(0.dp) {
            val verticalScrollState = rememberScrollState()
            val noteHeight by remember { mutableStateOf(16.dp) }
            val noteWidth by remember { mutableStateOf(0.4.dp) }
            Column(Modifier.fillMaxSize()) {
                val localDensity = LocalDensity.current
                var contentWidth by remember { mutableStateOf(0.dp) }
                Timeline(Modifier.zIndex(3F), noteWidth, horizontalScrollState, 68.dp)
                Box(Modifier.weight(1F).onGloballyPositioned { with(localDensity) { contentWidth = it.size.width.toDp() } }) {
                    Row(Modifier.fillMaxSize().zIndex(-1F)) {
                        Surface(Modifier.verticalScroll(verticalScrollState).zIndex(5f), shadowElevation = 4.dp) {
                            Keyboard(
                                { EchoInMirror.selectedTrack?.playMidiEvent(noteOn(0, it)) },
                                { EchoInMirror.selectedTrack?.playMidiEvent(noteOff(0, it)) },
                                Modifier, noteHeight
                            )
                        }
                        NotesEditorCanvas(
                            Modifier.weight(1F).fillMaxHeight().background(MaterialTheme.colorScheme.background),
                            noteHeight,
                            noteWidth,
                            verticalScrollState,
                            horizontalScrollState
                        )
                    }
                    PlayHead(noteWidth, horizontalScrollState, contentWidth, 68.dp)
                    VerticalScrollbar(
                        rememberScrollbarAdapter(verticalScrollState),
                        Modifier.align(Alignment.CenterEnd).fillMaxHeight()
                    )
                }
            }
        }
        second(0.dp) {
            Row {

            }
        }

        splitter {
            visiblePart {
                Divider()
            }
        }
    }

}

class Editor: Panel {
    override val name = "编辑器"
    override val direction = PanelDirection.Horizontal

    @Composable
    override fun icon() {
        Icon(Icons.Default.Piano, "Editor")
    }

    @Composable
    override fun content() {
        Row(Modifier.fillMaxSize()) {
            Column(Modifier.width(200.dp)) {

            }
            Surface(Modifier.fillMaxSize(), shadowElevation = 2.dp) {
                Column {
                    Box {
                        val stateScroll = rememberSaveable(saver = ScrollState.Saver) {
                            ScrollState(0).apply {
                                @Suppress("INVISIBLE_SETTER")
                                maxValue = 10000
                            }
                        }
                        Column(Modifier.padding(top = 8.dp).fillMaxSize()) {
                            editorContent(stateScroll)
                        }
                        HorizontalScrollbar(
                            modifier = Modifier.align(Alignment.TopStart).fillMaxWidth(),
                            adapter = rememberScrollbarAdapter(stateScroll)
                        )
                    }
                }
            }
        }
    }
}
