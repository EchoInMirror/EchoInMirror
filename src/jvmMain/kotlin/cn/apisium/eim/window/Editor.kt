package cn.apisium.eim.window

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Piano
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import cn.apisium.eim.api.window.Panel
import cn.apisium.eim.api.window.PanelDirection

class Editor: Panel {
    override val name = "编辑器"
    override val direction = PanelDirection.Horizontal

    @Composable
    override fun icon() {
        Icon(Icons.Default.Piano, "Editor")
    }

    @Composable
    override fun content() {

    }
}
