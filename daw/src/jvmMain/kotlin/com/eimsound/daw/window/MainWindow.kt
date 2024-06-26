package com.eimsound.daw.window

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.ComposeWindow
import androidx.compose.ui.focus.*
import androidx.compose.ui.input.key.*
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.*
import com.eimsound.daw.VERSION
import com.eimsound.daw.api.EchoInMirror
import com.eimsound.daw.api.window.GlobalException
import com.eimsound.daw.components.*
import com.eimsound.daw.components.app.*
import com.eimsound.daw.components.dragdrop.LocalGlobalDragAndDrop
import com.eimsound.daw.components.dragdrop.PlatformDropTargetModifier
import com.eimsound.daw.components.splitpane.HorizontalSplitPane
import com.eimsound.daw.components.splitpane.VerticalSplitPane
import com.eimsound.daw.components.utils.FPSMeasurer
import com.eimsound.daw.dawutils.*
import com.eimsound.daw.impl.WindowManagerImpl
import com.eimsound.daw.language.langs
import com.eimsound.daw.utils.isCrossPlatformAltPressed
import com.eimsound.daw.utils.isCrossPlatformCtrlPressed
import com.eimsound.daw.window.panels.playlist.mainPlaylist
import com.microsoft.appcenter.crashes.Crashes
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.runBlocking
import org.apache.commons.lang3.SystemUtils

@Composable
private fun FrameWindowScope.MainWindowContent(window: ComposeWindow) {
    Row {
        SideBar()
        val density = LocalDensity.current.density
        val dropParent = remember(density) { PlatformDropTargetModifier(density, window) }
        Scaffold(
            Modifier.then(dropParent),
            snackbarHost = { SnackbarHost(LocalSnackbarHost.current) },
            content = {
                Column {
                    EimAppBar()
                    Box(Modifier.weight(1F)) {
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
}

@Composable
private fun SaveProjectWarningDialog() {
    val windowState = rememberDialogState(width = 360.dp, height = 200.dp)
    val windowManager = EchoInMirror.windowManager
    if (windowManager.isSaveProjectWarningDialogOpened) DialogWindow({
        windowManager.isSaveProjectWarningDialogOpened = false
    }, windowState, title = langs.saveProjectTitle
    ) {
        Surface(Modifier.fillMaxSize(), tonalElevation = 4.dp) {
            Column(Modifier.padding(20.dp)) {
                Text(langs.saveProjectPrompt, Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
                Gap(20)
                Row {
                    TextButton(
                        windowManager::exitApplication,
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text(langs.dontSave)
                    }
                    Filled()
                    TextButton({ windowManager.isSaveProjectWarningDialogOpened = false }) {
                        Text(langs.cancel)
                    }
                    Gap(4)
                    Button({
                        runBlocking {
                            EchoInMirror.bus?.save()
                            windowManager.exitApplication()
                        }
                    }) {
                        Text(langs.saveAndExit)
                    }
                }
            }
        }
    }
}

val mainWindowState = WindowState()
var isFPSMeasurerEnabled by mutableStateOf(false)
private var checkHasFocus = { false }

private val logger = KotlinLogging.logger("MainWindow")
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun MainWindow() {
    Window(
        { EchoInMirror.windowManager.closeMainWindow() },
        mainWindowState, icon = Logo, title = "Echo In Mirror (v$VERSION)",
        onKeyEvent = {
            if (it.type != KeyEventType.KeyUp || checkHasFocus()) return@Window false
            var keys = (if (it.key == Key.Backspace) Key.Delete.keyCode else it.key.keyCode).toString()
            if (it.isCrossPlatformCtrlPressed) keys = "${Key.CtrlLeft.keyCode} $keys"
            if (it.isShiftPressed) keys = "${Key.ShiftLeft.keyCode} $keys"
            if (it.isCrossPlatformAltPressed) keys = "${Key.AltLeft.keyCode} $keys"
            EchoInMirror.commandManager.executeCommand(keys)
            false
        }
    ) {
        val focusManager = LocalFocusManager.current
        remember(focusManager) {
            checkHasFocus = {
                @Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")
                (focusManager as? FocusOwnerImpl)?.rootFocusNode?.focusState?.isFocused == false
            }
        }
        InitEditorTools()
        initWindowDecoration()
        if (SystemUtils.IS_OS_MAC) {
            window.rootPane.putClientProperty("apple.awt.fullWindowContent", true)
            window.rootPane.putClientProperty("apple.awt.transparentTitleBar", true)
            window.rootPane.putClientProperty("apple.awt.windowTitleVisible", false)
        }

        CLIPBOARD_MANAGER = LocalClipboardManager.current
        System.setProperty("eim.window.handler", window.windowHandle.toString())
        val windowManager = EchoInMirror.windowManager as WindowManagerImpl
        windowManager.mainWindow = window
        windowManager.floatingLayerProvider = LocalFloatingLayerProvider.current
        window.exceptionHandler = WindowExceptionHandler {
            logger.error(it) { "Uncaught compose exception" }
            windowManager.globalException = GlobalException(it, Crashes.trackCrash(it, Thread.currentThread(), null))
        }

        SaveProjectWarningDialog()

        Box {
            MainWindowContent(window)
            if (isFPSMeasurerEnabled) FPSMeasurer(Modifier.align(Alignment.TopEnd).padding(end = 30.dp, top = 30.dp))

            LocalFloatingLayerProvider.current.FloatingLayers()
            LocalGlobalDragAndDrop.current.DraggingComponent()
            LocalSnackbarProvider.current.SnackbarsContainer(Modifier.align(Alignment.BottomEnd).padding(bottom = 24.dp))
        }

        windowManager.Dialogs()
    }
}