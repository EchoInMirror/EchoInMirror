package cn.apisium.eim.window

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Piano
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import cn.apisium.eim.EchoInMirror
import cn.apisium.eim.api.window.Panel
import cn.apisium.eim.api.window.PanelDirection
import cn.apisium.eim.components.Keyboard
import cn.apisium.eim.components.splitpane.VerticalSplitPane
import cn.apisium.eim.components.splitpane.rememberSplitPaneState
import cn.apisium.eim.data.midi.midiOff
import cn.apisium.eim.data.midi.midiOn

@OptIn(ExperimentalTextApi::class)
@Composable
private fun editorContent(stateScroll: ScrollState) {
    val textMeasure = rememberTextMeasurer()
    VerticalSplitPane(splitPaneState = rememberSplitPaneState(100F)) {
        first(0.dp) {
            Box(Modifier.fillMaxSize()) {
                val scrollState = rememberScrollState()
                Row(Modifier.fillMaxSize()) {
                    Surface(Modifier.verticalScroll(scrollState).zIndex(2F), shadowElevation = 4.dp) {
                        Keyboard(
                            { EchoInMirror.selectedTrack?.playMidiEvent(midiOn(0, it)) },
                            { EchoInMirror.selectedTrack?.playMidiEvent(midiOff(0, it)) }
                        )
                    }
                    Canvas(Modifier.weight(1F).fillMaxHeight().background(MaterialTheme.colorScheme.background)) {
                        drawText(textMeasure, stateScroll.value.toString(), Offset(0F, 0F))
                    }
                }
                VerticalScrollbar(
                    rememberScrollbarAdapter(scrollState),
                    Modifier.align(Alignment.CenterEnd).fillMaxHeight()
                )
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
                                maxValue = 1000
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
