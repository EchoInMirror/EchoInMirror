package cn.apisium.eim.components

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.splitpane.ExperimentalSplitPaneApi
import org.jetbrains.compose.splitpane.HorizontalSplitPane
import org.jetbrains.compose.splitpane.VerticalSplitPane
import org.jetbrains.compose.splitpane.rememberSplitPaneState
import org.jetbrains.skiko.Cursor


private fun Modifier.cursorForHorizontalResize(): Modifier =
    pointerHoverIcon(PointerIcon(Cursor(Cursor.E_RESIZE_CURSOR)))

@Composable
@Preview
@OptIn(ExperimentalSplitPaneApi::class)
fun eimApp(innerPadding: PaddingValues) {
    Row {
    }
    val splitterState = rememberSplitPaneState()
    val hSplitterState = rememberSplitPaneState()
    HorizontalSplitPane(
        splitPaneState = splitterState
    ) {
        first(20.dp) {
            Box(Modifier.background(Color.Red).fillMaxSize())
        }
        second(50.dp) {
            VerticalSplitPane(splitPaneState = hSplitterState) {
                first(50.dp) {
                    Box(Modifier.background(Color.Blue).fillMaxSize())
                }
                second(20.dp) {
                    Box(Modifier.background(Color.Green).fillMaxSize())
                }
            }
        }
        splitter {
            visiblePart {
                Box(
                    Modifier
                        .width(1.dp)
                        .fillMaxHeight()
                        .background(MaterialTheme.colorScheme.background)
                )
            }
            handle {
                Box(
                    Modifier
                        .markAsHandle()
                        .cursorForHorizontalResize()
                        .background(SolidColor(Color.Gray), alpha = 0.50f)
                        .width(9.dp)
                        .fillMaxHeight()
                )
            }
        }
    }
}
