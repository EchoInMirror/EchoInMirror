@file:Suppress("DuplicatedCode")

package cn.apisium.eim.components.app

import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.dp
import cn.apisium.eim.utils.Border
import cn.apisium.eim.EchoInMirror
import cn.apisium.eim.api.window.Panel
import cn.apisium.eim.api.window.PanelDirection
import cn.apisium.eim.utils.border
import cn.apisium.eim.components.splitpane.SplitPaneState
import cn.apisium.eim.icons.EIMLogo
import cn.apisium.eim.impl.WindowManagerImpl

//    SideBarItem("Favorite", "收藏") { Icon(Icons.Default.Favorite, "Favorite") },
//    SideBarItem("Plugins", "插件") { Icon(Icons.Default.SettingsInputHdmi, "Plugins") },
//    SideBarItem("Topic", "文件") { Icon(Icons.Default.Topic, "Topic") },

internal var sideBarSelectedItem by mutableStateOf<Panel?>(null)
internal var bottomBarSelectedItem by mutableStateOf<Panel?>(EchoInMirror.windowManager.panels[1])
private var sideBarLastSelected: Panel? = null
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

internal val bottomBarHeightState = object : SplitPaneState(100F) {
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
            NavigationRailItem(
                icon = { Icon(EIMLogo, "QuickLand") },
                label = { Text("快速加载") },
                selected = false,
                onClick = { }
            )
            (EchoInMirror.windowManager as WindowManagerImpl).panels.filter { it.direction != PanelDirection.Horizontal }.forEach {
                key(it) {
                    NavigationRailItem(
                        icon = { it.icon() },
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
            EchoInMirror.windowManager.panels.filter { it.direction == PanelDirection.Horizontal }.forEach {
                key(it) {
                    NavigationRailItem(
                        icon = { it.icon() },
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
    Surface(
        tonalElevation = 2.dp,
        shadowElevation = 2.dp,
        shape = RoundedCornerShape(0.dp, 16.dp, if (bottomBarSelectedItem == null) 16.dp else 0.dp, 0.dp)
    ) {
        Box(Modifier.fillMaxSize().border(start = Border(0.6.dp, contentWindowColor))) {
            val stateVertical = rememberScrollState(0)
            Box(Modifier.fillMaxSize().verticalScroll(stateVertical)) {
                sideBarSelectedItem?.content()
            }
            VerticalScrollbar(
                modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight(),
                adapter = rememberScrollbarAdapter(stateVertical)
            )
        }
    }
}
