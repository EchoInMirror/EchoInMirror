package cn.apisium.eim.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import cn.apisium.eim.EchoInMirror
import cn.apisium.eim.impl.WindowManagerImpl
import org.jetbrains.compose.splitpane.ExperimentalSplitPaneApi
import org.jetbrains.compose.splitpane.HorizontalSplitPane
import org.jetbrains.compose.splitpane.VerticalSplitPane
import org.jetbrains.compose.splitpane.rememberSplitPaneState
import org.jetbrains.skiko.Cursor

@Composable
fun checkSampleRateAndBufferSize(): Array<Any> {
    SideEffect {
        println("Changed: ${EchoInMirror.sampleRate} ${EchoInMirror.bufferSize}")
    }
    return arrayOf(EchoInMirror.sampleRate, EchoInMirror.bufferSize)
}
private fun Modifier.cursorForHorizontalResize() = pointerHoverIcon(PointerIcon(Cursor(Cursor.E_RESIZE_CURSOR)))

@OptIn(ExperimentalMaterial3Api::class, ExperimentalSplitPaneApi::class)
fun eimApp() {
    application {
        val icon = painterResource("logo.png")
        checkSampleRateAndBufferSize()
        Window(onCloseRequest = ::exitApplication, icon = icon, title = "Echo In Mirror") {
            MaterialTheme {
                Row {
                    sideBar()
                    Scaffold(
                        topBar = { eimAppBar() },
                        content = {
                            HorizontalSplitPane(splitPaneState = rememberSplitPaneState()) {
                                first(20.dp) {
                                    Box(Modifier.fillMaxSize())
                                }
                                second(50.dp) {
                                    VerticalSplitPane(splitPaneState = rememberSplitPaneState()) {
                                        first(50.dp) {
                                            Box(Modifier.fillMaxSize())
                                        }
                                        second(20.dp) {
                                            Box(Modifier.fillMaxSize())
                                        }
                                    }
                                }
                            }
                        },
                        bottomBar = { statusBar() }
                    )
                }

                if (EchoInMirror.windowManager is WindowManagerImpl) EchoInMirror.windowManager.dialogs()
            }
        }
    }
}
