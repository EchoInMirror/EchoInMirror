package cn.apisium.eim.api.window

import androidx.compose.runtime.Composable

enum class PanelDirection {
    Both, Horizontal, Vertical
}

interface Panel {
    val name: String
    val direction: PanelDirection
    @Composable fun logo()
    @Composable fun content()
}

interface PanelManager {
    fun registerPanel(panel: Panel)
    fun unregisterPanel(panel: Panel)
    fun getPanel(id: String): Panel?
}
