package cn.apisium.eim.window.dialogs.settings

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Help
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import cn.apisium.eim.RELEASE_TIME
import cn.apisium.eim.VERSION
import cn.apisium.eim.components.ClickableText
import cn.apisium.eim.components.Gap
import cn.apisium.eim.components.Tab
import cn.apisium.eim.utils.openInBrowser
import java.net.URI
import java.text.DateFormat

internal object AboutPanel: Tab {
    @Composable
    override fun label() {
        Text("关于")
    }

    @Composable
    override fun icon() {
        Icon(Icons.Filled.Help, "About")
    }

    @Composable
    override fun content() {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Image(painterResource("eim-chan.png"), "EIM Chan", Modifier.size(400.dp))
            MaterialTheme.typography.apply {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("EchoInMirror ", style = headlineLarge)
                    Text("by EIMSound", style = labelMedium)
                }
                Gap(8)
                Text("版本: $VERSION (${DateFormat.getDateTimeInstance().format(RELEASE_TIME)})", style = bodySmall)
                Gap(16)
                Text("基于 AGPL-3.0 协议开源", style = bodyMedium)
                ClickableText("https://github.com/EchoInMirror/EchoInMirror", style = bodyMedium) {
                    openInBrowser(URI("https://github.com/EchoInMirror/EchoInMirror"))
                }
                Text("未经允许不可二次发布本程序的任何源码, 可执行文件和所包含的资源等文件!", style = bodySmall)
                Text("如有能力可参与代码贡献或直接赞助项目", style = bodyMedium)
                Gap(16)
                Text("贡献者", style = bodyMedium)
                Text("Shirasawa N0I0C0K SuiltaPico Shika", style = bodySmall)
            }
        }
    }
}
