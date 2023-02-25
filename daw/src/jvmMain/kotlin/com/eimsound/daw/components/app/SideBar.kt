@file:Suppress("DuplicatedCode")

package com.eimsound.daw.components.app

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEach
import com.eimsound.daw.EchoInMirror
import com.eimsound.daw.api.window.Panel
import com.eimsound.daw.api.window.PanelDirection
import com.eimsound.daw.components.LocalFloatingDialogProvider
import com.eimsound.daw.components.icons.EIMLogo
import com.eimsound.daw.components.splitpane.SplitPaneState
import com.eimsound.daw.impl.WindowManagerImpl
import com.eimsound.daw.dawutils.Border
import com.eimsound.daw.dawutils.border
import com.eimsound.daw.window.dialogs.openQuickLoadDialog

//    SideBarItem("Favorite", "收藏") { Icon(Icons.Default.Favorite, "Favorite") },
//    SideBarItem("Plugins", "插件") { Icon(Icons.Default.SettingsInputHdmi, "Plugins") },
//    SideBarItem("Topic", "文件") { Icon(Icons.Default.Topic, "Topic") },

internal var sideBarSelectedItem by mutableStateOf<Panel?>(null)
internal var bottomBarSelectedItem by mutableStateOf<Panel?>(EchoInMirror.windowManager.panels[1])
private var sideBarLastSelected: Panel? = sideBarSelectedItem
private var bottomBarLastSelected: Panel? = bottomBarSelectedItem

internal val sideBarWidthState = object : SplitPaneState() {
    override fun dispatchRawMovement(delta: Float) {
        val movableArea = maxPosition - minPosition
        if (movableArea <= 0) return
        val width = (position + delta).coerceIn(minPosition, maxPosition)
        if (sideBarSelectedItem == null) {
            if (width >= 240 && sideBarLastSelected != null) {
                sideBarSelectedItem = sideBarLastSelected
                position = width
                return
            }
            if (position != 0F) position = 0F
            return
        }
        if (width < 240) {
            if (width < 80) {
                position = 0F
                sideBarSelectedItem = null
            }
            return
        }
        position = width
    }
}

internal val bottomBarHeightState = object : SplitPaneState(0.5F) {
    override fun dispatchRawMovement(delta: Float) {
        val movableArea = maxPosition - minPosition
        if (movableArea <= 0) return
        val height = (position - delta).coerceIn(minPosition, maxPosition)
        if (bottomBarSelectedItem == null) {
            if (height >= 240 && bottomBarLastSelected != null) {
                bottomBarSelectedItem = bottomBarLastSelected
                position = height
                return
            }
            if (position != 0F) position = 0F
            return
        }
        if (height < 240) {
            if (height < 80) {
                position = 0F
                bottomBarSelectedItem = null
            }
            return
        }
        position = height
    }

    override fun calcPosition(constraint: Float): Float {
        super.calcPosition(constraint)
        return maxPosition - position
    }
}

@Composable
internal fun SideBar() {
    val lineColor = MaterialTheme.colorScheme.surfaceVariant
    Surface(Modifier.drawWithContent {
        drawContent()
        drawLine(lineColor, Offset(size.width - 0.3f, 0F), Offset(size.width - 0.3f, size.height), 0.6f)
    }, tonalElevation = 2.dp) {
        NavigationRail {
            val floatingDialogProvider = LocalFloatingDialogProvider.current
            NavigationRailItem(
                icon = { Icon(EIMLogo, "QuickLand") },
                label = { Text("快速加载") },
                selected = false,
                onClick = { floatingDialogProvider.openQuickLoadDialog() }
            )
            (EchoInMirror.windowManager as WindowManagerImpl).panels.filter { it.direction != PanelDirection.Horizontal }.fastForEach {
                key(it) {
                    NavigationRailItem(
                        icon = { it.Icon() },
                        label = { Text(it.name) },
                        selected = sideBarSelectedItem == it,
                        onClick = {
                            if (sideBarSelectedItem == it) {
                                sideBarSelectedItem = null
                                sideBarWidthState.position = 0F
                            } else {
                                sideBarSelectedItem = it
                                if (sideBarWidthState.position == 0F) sideBarWidthState.position = 240F
                                sideBarLastSelected = sideBarSelectedItem
                            }
                        }
                    )
                }
            }
            Box(Modifier.weight(2F)) {}
            EchoInMirror.windowManager.panels.filter { it.direction == PanelDirection.Horizontal }.fastForEach {
                key(it) {
                    NavigationRailItem(
                        icon = { it.Icon() },
                        label = { Text(it.name) },
                        selected = bottomBarSelectedItem == it,
                        onClick = {
                            if (bottomBarSelectedItem == it) {
                                bottomBarSelectedItem = null
                                bottomBarHeightState.position = 0F
                            } else {
                                bottomBarSelectedItem = it
                                if (bottomBarHeightState.position == 0F) bottomBarHeightState.position = 240F
                                bottomBarLastSelected = bottomBarSelectedItem
                            }
                        }
                    )
                }
            }
        }
    }
}

val contentWindowColor @Composable get() = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.2F)

@Composable
internal fun SideBarContent() {
    Surface(tonalElevation = 2.dp, shadowElevation = 2.dp) {
        Box(Modifier.fillMaxSize().border(start = Border(0.6.dp, contentWindowColor))) {
            sideBarSelectedItem?.Content()
        }
    }
}
