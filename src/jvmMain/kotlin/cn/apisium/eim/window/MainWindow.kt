package cn.apisium.eim.window

import androidx.compose.foundation.layout.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.*
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.ApplicationScope
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowExceptionHandler
import cn.apisium.eim.EchoInMirror
import cn.apisium.eim.components.app.*
import cn.apisium.eim.components.app.EimAppBar
import cn.apisium.eim.components.app.SideBar
import cn.apisium.eim.components.app.SideBarContent
import cn.apisium.eim.components.app.StatusBar
import cn.apisium.eim.components.app.bottomBarHeightState
import cn.apisium.eim.components.app.bottomBarSelectedItem
import cn.apisium.eim.components.app.sideBarWidthState
import cn.apisium.eim.components.splitpane.HorizontalSplitPane
import cn.apisium.eim.components.splitpane.VerticalSplitPane
import cn.apisium.eim.impl.WindowManagerImpl
import cn.apisium.eim.utils.Border
import cn.apisium.eim.utils.CLIPBOARD_MANAGER
import cn.apisium.eim.utils.Logo
import cn.apisium.eim.utils.border
import cn.apisium.eim.window.panels.playlist.Playlist
import java.lang.ref.WeakReference

@OptIn(ExperimentalComposeUiApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ApplicationScope.MainWindow() {
    if (!EchoInMirror.windowManager.isMainWindowOpened) return
    Window(onCloseRequest = ::exitApplication, icon = Logo, title = "Echo In Mirror", onKeyEvent = {
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