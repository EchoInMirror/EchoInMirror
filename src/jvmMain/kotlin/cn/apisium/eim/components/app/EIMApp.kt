@file:Suppress("INVISIBLE_SETTER")

package cn.apisium.eim.components.app

import androidx.compose.foundation.LocalScrollbarStyle
import androidx.compose.foundation.ScrollbarStyle
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.*
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowExceptionHandler
import androidx.compose.ui.window.application
import cn.apisium.eim.EchoInMirror
import cn.apisium.eim.components.*
import cn.apisium.eim.components.splitpane.HorizontalSplitPane
import cn.apisium.eim.components.splitpane.VerticalSplitPane
import cn.apisium.eim.impl.WindowManagerImpl
import cn.apisium.eim.utils.Border
import cn.apisium.eim.utils.CLIPBOARD_MANAGER
import cn.apisium.eim.utils.border
import cn.apisium.eim.window.playlist.Playlist
import org.jetbrains.skiko.Cursor
import java.lang.ref.WeakReference

@Composable
fun checkSampleRateAndBufferSize() {
    LaunchedEffect(Unit) {
        snapshotFlow {
            EchoInMirror.currentPosition.sampleRate
            EchoInMirror.currentPosition.bufferSize
        }
            .collect {
                println("Changed: ${EchoInMirror.currentPosition.sampleRate} ${EchoInMirror.currentPosition.bufferSize}")
            }
    }
}
@Suppress("unused")
private fun Modifier.cursorForHorizontalResize() = pointerHoverIcon(PointerIcon(Cursor(Cursor.E_RESIZE_CURSOR)))

@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
fun eimApp() {
    application {
        val icon = painterResource("logo.png")
        checkSampleRateAndBufferSize()
        MaterialTheme(
            if (EchoInMirror.windowManager.isDarkTheme) darkColorScheme() else lightColorScheme()
        ) {
            CompositionLocalProvider(LocalScrollbarStyle provides ScrollbarStyle(
                minimalHeight = 16.dp,
                thickness = 8.dp,
                shape = RoundedCornerShape(4.dp),
                hoverDurationMillis = 300,
                unhoverColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.26f),
                hoverColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.60f)
            )) {
                Window(onCloseRequest = ::exitApplication, icon = icon, title = "Echo In Mirror", onKeyEvent = {
                    if (it.type != KeyEventType.KeyUp) return@Window false
                    var keys = it.key.keyCode.toString()
                    if (it.isCtrlPressed) keys = "${Key.CtrlLeft.keyCode} $keys"
                    if (it.isShiftPressed) keys = "${Key.ShiftLeft.keyCode} $keys"
                    if (it.isAltPressed) keys = "${Key.AltLeft.keyCode} $keys"
                    if (it.isMetaPressed) keys = "${Key.MetaLeft.keyCode} $keys"
                    EchoInMirror.commandManager.executeCommand(keys)
                    false
                }) {
                    CLIPBOARD_MANAGER = LocalClipboardManager.current
                    (EchoInMirror.windowManager as WindowManagerImpl).mainWindow = WeakReference(window)
                    window.exceptionHandler = WindowExceptionHandler {
                        it.printStackTrace()
                    }

                    Box {
                        Row {
                            SideBar()
                            Scaffold(
                                topBar = { EimAppBar() },
                                content = {
                                    Column {
                                        Box(Modifier.weight(1F).padding(top = APP_BAR_HEIGHT)) {
                                            HorizontalSplitPane(splitPaneState = sideBarWidthState) {
                                                first(0.dp) { SideBarContent() }
                                                second(400.dp) {
                                                    VerticalSplitPane(splitPaneState = bottomBarHeightState) {
                                                        first(200.dp) {
                                                            Box(Modifier.fillMaxSize().border(start = Border(0.6.dp, contentWindowColor))) {
                                                                Playlist()
                                                            }
                                                        }
                                                        second(0.dp) {
                                                            Surface(tonalElevation = 2.dp, shadowElevation = 2.dp) {
                                                                bottomBarSelectedItem?.content()
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                        StatusBar()
                                    }
                                }
                            )
                        }

                        EchoInMirror.windowManager.FloatingDialogs()
                    }

                    EchoInMirror.windowManager.Dialogs()
                }
            }
        }
    }
}
