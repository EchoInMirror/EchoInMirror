package com.eimsound.daw.components.app

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.WindowPlacement
import com.eimsound.daw.api.EchoInMirror
import com.eimsound.daw.components.*
import com.eimsound.daw.dawutils.maximize
import com.eimsound.daw.dawutils.minimize
import com.eimsound.daw.dawutils.restore
import com.eimsound.daw.language.langs
import com.eimsound.daw.window.mainWindowState
import kotlinx.coroutines.*
import org.apache.commons.lang3.SystemUtils

@Composable
private fun TitleButtons(modifier: Modifier) {
    Row(modifier) {
        RectIconButton({
            if (SystemUtils.IS_OS_WINDOWS) EchoInMirror.windowManager.mainWindow?.let {
                if (mainWindowState.isMinimized) it.restore() else it.minimize()
                return@RectIconButton
            }
            mainWindowState.isMinimized = !mainWindowState.isMinimized
        }, 32.dp, 26.dp) {
            Icon(
                imageVector = Icons.Filled.Minimize,
                contentDescription = if (mainWindowState.isMinimized) "Restore" else "Minimize",
                Modifier.size(18.dp).offset(y = (-2).dp)
            )
        }
        RectIconButton({
            if (SystemUtils.IS_OS_WINDOWS) EchoInMirror.windowManager.mainWindow?.let {
                if (mainWindowState.placement == WindowPlacement.Floating) it.maximize() else it.restore()
                return@RectIconButton
            }
            mainWindowState.placement = if (mainWindowState.placement == WindowPlacement.Floating) WindowPlacement.Maximized
            else WindowPlacement.Floating
        }, 32.dp, 26.dp) {
            Icon(
                imageVector = if (mainWindowState.placement == WindowPlacement.Floating) Icons.Filled.CheckBoxOutlineBlank
                else Icons.Filled.FilterNone,
                contentDescription = if (mainWindowState.placement == WindowPlacement.Floating) "Maximize" else "Restore",
                Modifier.size(14.dp)
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
                    Text("${langs.search}...", style = MaterialTheme.typography.labelSmall.copy(color))
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

@OptIn(DelicateCoroutinesApi::class)
@Composable
private fun SaveButton() {
    val bus = EchoInMirror.bus
    IconButton({
        GlobalScope.launch { bus?.save() }
    }, 26.dp, enabled = bus?.project?.saved == false) {
        Icon(
            imageVector = Icons.Filled.Save,
            contentDescription = "Save",
            Modifier.size(15.dp)
        )
    }
}

var actionButtonsSize = IntSize.Zero
var titleBarContentSize = IntSize.Zero

@Composable
fun TitleBar() {
    Box(Modifier.fillMaxWidth()
        .padding(top = if (SystemUtils.IS_OS_WINDOWS && mainWindowState.placement != WindowPlacement.Floating) 8.dp else 0.dp)
    ) {
        val color = LocalContentColor.current.copy(0.4F)
        Row(
            Modifier.align(Alignment.Center).padding(12.dp, 4.dp, 12.dp).offset(-SIDE_BAR_WIDTH / 2).onPlaced { titleBarContentSize = it.size },
            verticalAlignment = Alignment.CenterVertically
        ) {
            UndoRedoButtons()
            Search(color)
            Gap(4)
            SaveButton()
        }
        if (SystemUtils.IS_OS_WINDOWS) CompositionLocalProvider(LocalContentColor.provides(color)) {
            TitleButtons(Modifier.align(Alignment.CenterEnd).onPlaced { actionButtonsSize = it.size })
        }
    }
}
