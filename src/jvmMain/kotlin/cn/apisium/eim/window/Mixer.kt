package cn.apisium.eim.window

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
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
        Row(Modifier.padding(14.dp)) {
            Surface(Modifier.size(80.dp, 200.dp), shadowElevation = 1.dp, shape = MaterialTheme.shapes.medium) {
                Column(Modifier.background(MaterialTheme.colorScheme.surface).padding(4.dp)) {
                    // TODO
                }
            }
        }
    }
}
