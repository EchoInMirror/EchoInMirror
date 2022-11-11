package cn.apisium.eim.window

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import cn.apisium.eim.api.window.Panel
import cn.apisium.eim.api.window.PanelDirection

class Mixer: Panel {
    override val name = "混音台"
    override val direction = PanelDirection.Horizontal

    @Composable
    override fun icon() {
        Icon(Icons.Default.Tune, "Mixer")
    }

    @Composable
    override fun content() {

    }
}
