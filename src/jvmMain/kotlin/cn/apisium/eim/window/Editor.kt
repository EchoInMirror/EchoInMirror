package cn.apisium.eim.window

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Piano
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
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
import cn.apisium.eim.data.midi.midiOff
import cn.apisium.eim.data.midi.midiOn

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
    Canvas(modifier) {
        val noteHeightPx = noteHeight.toPx()
        val noteWidthPx = noteWidth.toPx()
        val verticalScrollValue = verticalScrollState.value
        val horizontalScrollValue = horizontalScrollState.value
        val startYNote = (verticalScrollValue / noteHeightPx).toInt()
        val endYNote = ((verticalScrollValue + size.height) / noteHeightPx).toInt()
        val beatsWidth = noteWidthPx * 16 / timeSigDenominator
//        val startXNote = (horizontalScrollValue / noteWidthPx).toInt()
//        val endXNote = ((horizontalScrollValue + size.width) / noteWidthPx).toInt()
        for (i in startYNote..endYNote) {
            drawLine(
                color = outlineColor,
                start = Offset(0f, i * noteHeightPx - verticalScrollValue),
                end = Offset(size.width, i * noteHeightPx - verticalScrollValue),
                strokeWidth = 1F
            )
            if (cn.apisium.eim.components.scales[i % 12]) {
                drawRect(
                    color = highlightNoteColor,
                    topLeft = Offset(0f, i * noteHeightPx - verticalScrollValue),
                    size = Size(size.width, noteHeightPx)
                )
            }
        }
        val drawBeats = noteWidthPx > 9.6F
        val horizontalDrawWidth = if (drawBeats) beatsWidth else beatsWidth * timeSigNumerator
        val highlightWidth = if (drawBeats) timeSigDenominator * timeSigNumerator else timeSigDenominator
        for (i in (horizontalScrollValue / horizontalDrawWidth).toInt()..((horizontalScrollValue + horizontalScrollState.maxValue) / horizontalDrawWidth).toInt()) {
            drawLine(
                color = if (i % highlightWidth == 0) barsOutlineColor else outlineColor,
                start = Offset(i * horizontalDrawWidth - horizontalScrollState.value, 0f),
                end = Offset(i * horizontalDrawWidth - horizontalScrollState.value, size.height),
                strokeWidth = 1F
            )
        }
    }
}

@Composable
private fun editorContent(horizontalScrollState: ScrollState) {
    VerticalSplitPane(splitPaneState = rememberSplitPaneState(100F)) {
        first(0.dp) {
            val verticalScrollState = rememberScrollState()
            val noteHeight by remember { mutableStateOf(16.dp) }
            val noteWidth by remember { mutableStateOf(0.6.dp) }
            Column(Modifier.fillMaxSize()) {
                Timeline(Modifier.zIndex(3F), noteWidth, horizontalScrollState, 68.dp)
                Box(Modifier.weight(1F)) {
                    Row(Modifier.fillMaxSize().zIndex(-1F)) {
                        Surface(Modifier.verticalScroll(verticalScrollState).zIndex(5f), shadowElevation = 4.dp) {
                            Keyboard(
                                { EchoInMirror.selectedTrack?.playMidiEvent(midiOn(0, it)) },
                                { EchoInMirror.selectedTrack?.playMidiEvent(midiOff(0, it)) },
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
                    PlayHead(noteWidth, horizontalScrollState, 68.dp)
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
