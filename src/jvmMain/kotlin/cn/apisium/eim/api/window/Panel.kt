package cn.apisium.eim.api.window

import androidx.compose.runtime.Composable

enum class PanelDirection {
    Both, Horizontal, Vertical
}

interface Panel {
    val name: String
    val direction: PanelDirection
    @Composable fun icon()
    @Composable fun content()
}
