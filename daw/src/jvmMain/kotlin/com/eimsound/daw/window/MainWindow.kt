package com.eimsound.daw.window

import androidx.compose.foundation.layout.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.*
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.ApplicationScope
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowExceptionHandler
import com.eimsound.daw.VERSION
import com.eimsound.daw.api.EchoInMirror
import com.eimsound.daw.api.window.GlobalException
import com.eimsound.daw.components.LocalFloatingLayerProvider
import com.eimsound.daw.components.LocalSnackbarHost
import com.eimsound.daw.components.LocalSnackbarProvider
import com.eimsound.daw.components.app.*
import com.eimsound.daw.components.dragdrop.LocalGlobalDragAndDrop
import com.eimsound.daw.components.dragdrop.PlatformDropTargetModifier
import com.eimsound.daw.components.splitpane.HorizontalSplitPane
import com.eimsound.daw.components.splitpane.VerticalSplitPane
import com.eimsound.daw.dawutils.Border
import com.eimsound.daw.dawutils.CLIPBOARD_MANAGER
import com.eimsound.daw.dawutils.Logo
import com.eimsound.daw.dawutils.border
import com.eimsound.daw.impl.WindowManagerImpl
import com.eimsound.daw.window.panels.playlist.mainPlaylist
import com.microsoft.appcenter.crashes.Crashes
import io.github.oshai.kotlinlogging.KotlinLogging

private val logger = KotlinLogging.logger("MainWindow")
@OptIn(ExperimentalComposeUiApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ApplicationScope.MainWindow() {
    Window({
        EchoInMirror.windowManager.closeMainWindow(true)
        exitApplication()
    }, icon = Logo, title = "Echo In Mirror (v$VERSION)", onKeyEvent = {
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
        System.setProperty("eim.window.handler", window.windowHandle.toString())
        val windowManager = EchoInMirror.windowManager as WindowManagerImpl
        windowManager.mainWindow = window
        windowManager.floatingLayerProvider = LocalFloatingLayerProvider.current
        window.exceptionHandler = WindowExceptionHandler {
            logger.error(it) { "Uncaught compose exception" }
            windowManager.globalException = GlobalException(it, Crashes.trackCrash(it, Thread.currentThread(), null))
        }

        Box {
            Row {
                SideBar()
                val density = LocalDensity.current.density
                val dropParent = remember(density) { PlatformDropTargetModifier(density, window) }
                Scaffold(
                    Modifier.then(dropParent),
                    topBar = { EimAppBar() },
                    snackbarHost = { SnackbarHost(LocalSnackbarHost.current) },
                    content = {
                        Column {
                            Box(Modifier.weight(1F).padding(top = APP_BAR_HEIGHT)) {
                                HorizontalSplitPane(splitPaneState = sideBarWidthState) {
                                    first(0.dp) { SideBarContent() }
                                    second(400.dp) {
                                        VerticalSplitPane(splitPaneState = bottomBarHeightState) {
                                            first(0.dp) {
                                                Box(Modifier.fillMaxSize().border(start = Border(0.6.dp, contentWindowColor))) {
                                                    @Suppress("DEPRECATION") mainPlaylist.Content()
                                                }
                                            }
                                            second(0.dp) {
                                                Surface(tonalElevation = 2.dp, shadowElevation = 6.dp) {
                                                    bottomBarSelectedItem?.Content()
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

            LocalFloatingLayerProvider.current.FloatingLayers()
            LocalGlobalDragAndDrop.current.DraggingComponent()
            LocalSnackbarProvider.current.SnackbarsContainer(Modifier.align(Alignment.BottomEnd).padding(bottom = 24.dp))
        }

        windowManager.Dialogs()
    }
}