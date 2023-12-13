package com.eimsound.daw.components.app

import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.window.WindowDraggableArea
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.FrameWindowScope
import androidx.compose.ui.window.WindowPlacement
import com.eimsound.daw.api.EchoInMirror
import com.eimsound.daw.components.*
import com.eimsound.daw.window.mainWindowState
import kotlinx.coroutines.*
import org.apache.commons.lang3.SystemUtils

@Composable
private fun TitleButtons(modifier: Modifier) {
    Row(modifier) {
        RectIconButton({ mainWindowState.isMinimized = !mainWindowState.isMinimized }, 32.dp, 26.dp) {
            Icon(
                imageVector = Icons.Filled.Minimize,
                contentDescription = "Exit",
                Modifier.size(18.dp).offset(y = (-2).dp)
            )
        }
        RectIconButton({
            mainWindowState.placement = if (mainWindowState.placement == WindowPlacement.Floating) WindowPlacement.Maximized
            else WindowPlacement.Floating
        }, 32.dp, 26.dp) {
            Icon(
                imageVector = if (mainWindowState.placement == WindowPlacement.Floating) Icons.Filled.CheckBoxOutlineBlank
                else Icons.Filled.FilterNone,
                contentDescription = "Exit",
                Modifier.size(16.dp)
            )
        }
        RectIconButton({ EchoInMirror.windowManager.closeMainWindow() }, 32.dp, 26.dp) {
            Row {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = "Exit",
                    Modifier.size(18.dp)
                )
                Spacer(Modifier.width(4.dp))
            }
        }
    }
}

var searchContent by mutableStateOf("")

@Composable
private fun Search(color: Color) {
    CustomOutlinedTextField(
        searchContent,
        { searchContent = it },
        Modifier.size(260.dp, 22.dp),
        singleLine = true,
        textStyle = MaterialTheme.typography.labelSmall.copy(LocalContentColor.current),
        placeholder = {
            Box(Modifier.fillMaxWidth()) {
                Row(Modifier.align(Alignment.Center), verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.Search,
                        contentDescription = "Search",
                        Modifier.size(16.dp),
                        color
                    )
                    Text("搜索...", style = MaterialTheme.typography.labelSmall.copy(color))
                }
            }
        },
        colors = textFieldGrayColors(),
        paddingValues = PaddingValues(8.dp, 0.dp, 8.dp, 1.dp)
    )
}

@OptIn(DelicateCoroutinesApi::class)
@Composable
private fun UndoRedoButtons() {
    Row {
        val manager = EchoInMirror.undoManager
        IconButton({
            GlobalScope.launch { manager.undo() }
        }, 26.dp, enabled = manager.canUndo) {
            Icon(
                imageVector = Icons.Filled.ArrowBack,
                contentDescription = "Undo",
                Modifier.size(15.dp)
            )
        }
        IconButton({
            GlobalScope.launch { manager.redo() }
        }, 26.dp, enabled = manager.canRedo) {
            Icon(
                imageVector = Icons.Filled.ArrowForward,
                contentDescription = "Redo",
                Modifier.size(15.dp)
            )
        }
        Gap(6)
    }
}

private var lock by mutableStateOf(false)
@OptIn(DelicateCoroutinesApi::class)
@Composable
private fun TitleBarContent() {
    Box(Modifier.fillMaxWidth()
        .pointerInput(Unit) { // Double click
            awaitPointerEventScope {
                var time = 0L
                var lastPos: Offset? = null
                while (true) {
                    val event = awaitFirstDown(false)
                    if (lastPos != null && (event.position - lastPos).getDistanceSquared() > 20 * density * density) {
                        time = 0
                        lastPos = null
                        continue
                    }
                    time = if (event.previousUptimeMillis - time < viewConfiguration.longPressTimeoutMillis) {
                        GlobalScope.launch {
                            lock = true
                            delay(120)
                            mainWindowState.placement = if (mainWindowState.placement == WindowPlacement.Floating) WindowPlacement.Maximized
                            else WindowPlacement.Floating
                            lock = false
                        }
                        lastPos = null
                        0L
                    } else {
                        lastPos = event.position
                        event.previousUptimeMillis
                    }
                }
            }
        }) {
        val color = LocalContentColor.current.copy(0.4F)
        Row(Modifier.align(Alignment.Center).padding(12.dp, 4.dp, 12.dp).offset((-30).dp), verticalAlignment = Alignment.CenterVertically) {
            UndoRedoButtons()
            Search(color)
        }
        CompositionLocalProvider(LocalContentColor.provides(color)) {
            if (SystemUtils.IS_OS_WINDOWS) TitleButtons(Modifier.align(Alignment.CenterEnd))
        }
    }
}

@Composable
internal fun FrameWindowScope.TitleBar() {
    if (SystemUtils.IS_OS_WINDOWS && mainWindowState.placement == WindowPlacement.Floating && !lock) WindowDraggableArea {
        TitleBarContent()
    } else TitleBarContent()
}
