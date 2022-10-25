package cn.apisium.eim.components

import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

data class SideBarItem(val id: String, val name: String, val icon: @Composable () -> Unit)

val mainItems = mutableStateListOf(
    SideBarItem("Favorite", "收藏") { Icon(Icons.Default.Favorite, "Favorite") },
    SideBarItem("Plugins", "插件") { Icon(Icons.Default.SettingsInputHdmi, "Plugins") },
    SideBarItem("Topic", "文件") { Icon(Icons.Default.Topic, "Topic") },
)

val bottomItems = mutableStateListOf(
    SideBarItem("Editor", "钢琴窗") { Icon(Icons.Default.Piano, "Editor") },
    SideBarItem("Mixer", "混音台") { Icon(Icons.Default.Tune, "Mixer") },
)

@Composable
fun sideBar() {
    var selectedItem by remember { mutableStateOf("") }
    Surface(tonalElevation = 2.dp) {
        NavigationRail {
            mainItems.forEach {
                NavigationRailItem(
                    icon = { it.icon() },
                    label = { Text(it.name) },
                    selected = selectedItem == it.id,
                    onClick = { selectedItem = it.id }
                )
            }
            Box(Modifier.weight(2F)) {}
            bottomItems.forEach {
                NavigationRailItem(
                    icon = { it.icon() },
                    label = { Text(it.name) },
                    selected = selectedItem == it.id,
                    onClick = { selectedItem = it.id }
                )
            }
        }
    }
}
