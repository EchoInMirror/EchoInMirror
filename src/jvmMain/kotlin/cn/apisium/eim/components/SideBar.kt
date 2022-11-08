package cn.apisium.eim.components

import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cn.apisium.eim.Border
import cn.apisium.eim.border
import cn.apisium.eim.components.splitpane.SplitPaneState
import cn.apisium.eim.icons.EIMLogo

data class SideBarItem(val id: String, val name: String? = null, val icon: @Composable () -> Unit)

val mainItems = mutableStateListOf(
    SideBarItem("EIM", "EIM") { Icon(EIMLogo, "QuickLand") },
    SideBarItem("Favorite", "收藏") { Icon(Icons.Default.Favorite, "Favorite") },
    SideBarItem("Plugins", "插件") { Icon(Icons.Default.SettingsInputHdmi, "Plugins") },
    SideBarItem("Topic", "文件") { Icon(Icons.Default.Topic, "Topic") },
)

val bottomItems = mutableStateListOf(
    SideBarItem("Editor", "钢琴窗") { Icon(Icons.Default.Piano, "Editor") },
    SideBarItem("Mixer", "混音台") { Icon(Icons.Default.Tune, "Mixer") },
)

internal var sideBarSelectedItem by mutableStateOf<String?>(null)
internal var bottomBarSelectedItem by mutableStateOf<String?>(null)
private var sideBarLastSelected: String? = null
private var bottomBarLastSelected: String? = null

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

internal val bottomBarHeightState = object : SplitPaneState() {
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
fun sideBar() {
    Surface(tonalElevation = 2.dp) {
        NavigationRail {
            mainItems.forEach {
                NavigationRailItem(
                    icon = { it.icon() },
                    label = if (it.name == null) null else ({ Text(it.name) }),
                    selected = sideBarSelectedItem == it.id,
                    onClick = {
                        if (sideBarSelectedItem == it.id) {
                            sideBarSelectedItem = null
                            sideBarWidthState.position = 0F
                        } else {
                            sideBarSelectedItem = it.id
                            if (sideBarWidthState.position == 0F) sideBarWidthState.position = 240F
                            sideBarLastSelected = sideBarSelectedItem
                        }
                    }
                )
            }
            Box(Modifier.weight(2F)) {}
            bottomItems.forEach {
                NavigationRailItem(
                    icon = { it.icon() },
                    label = if (it.name == null) null else ({ Text(it.name) }),
                    selected = bottomBarSelectedItem == it.id,
                    onClick = {
                        if (bottomBarSelectedItem == it.id) {
                            bottomBarSelectedItem = null
                            bottomBarHeightState.position = 0F
                        } else {
                            bottomBarSelectedItem = it.id
                            if (bottomBarHeightState.position == 0F) bottomBarHeightState.position = 240F
                            bottomBarLastSelected = bottomBarSelectedItem
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun sideBarContent() {
    Surface(
        tonalElevation = 2.dp,
        shadowElevation = 2.dp,
        shape = RoundedCornerShape(0.dp, 16.dp, if (bottomBarSelectedItem == null) 16.dp else 0.dp, 0.dp)
    ) {
        Box(Modifier.fillMaxSize().border(start = Border(0.6.dp, MaterialTheme.colorScheme.onSurfaceVariant.copy(0.2F)))) {
            val stateVertical = rememberScrollState(0)
            Box(Modifier.fillMaxSize().verticalScroll(stateVertical)) {
                Box(Modifier.padding(14.dp)) { }
            }
            VerticalScrollbar(
                modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight(),
                adapter = rememberScrollbarAdapter(stateVertical)
            )
        }
    }
}
