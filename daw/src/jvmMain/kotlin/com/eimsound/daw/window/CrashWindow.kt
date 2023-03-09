package com.eimsound.daw.window

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.ApplicationScope
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.rememberWindowState
import com.eimsound.daw.VERSION
import com.eimsound.daw.api.EchoInMirror
import com.eimsound.daw.components.ClickableText
import com.eimsound.daw.components.Gap
import com.eimsound.daw.components.Scrollable
import com.eimsound.daw.dawutils.Logo
import java.awt.Dimension

@Composable
fun ApplicationScope.CrashWindow() {
    val windowState = rememberWindowState()
    Window({
        EchoInMirror.windowManager.closeMainWindow(true)
        exitApplication()
    }, windowState, icon = Logo, title = "Echo In Mirror (v$VERSION)") {
        window.minimumSize = Dimension(600, 400)
        val exception = EchoInMirror.windowManager.globalException ?: return@Window
        Surface(tonalElevation = 1.dp) {
            Column(Modifier.fillMaxSize().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                MaterialTheme.typography.apply {
                    Text("发生致命异常!", style = headlineMedium, fontWeight = FontWeight.Bold)
                    Gap(16)
                    Text(exception.exception.toString(), style = bodyLarge, fontWeight = FontWeight.Bold)
                    Gap(16)
                    Text("ID: ${exception.id}", style = bodySmall)
                    Row {
                        Text("您可以将以下信息发送给开发者以帮助我们改进软件, 或", style = bodySmall)
                        ClickableText("点击这里", style = bodySmall) {
                            EchoInMirror.windowManager.globalException = null
                        }
                        Text("尝试重载窗口.", style = bodySmall)
                    }
                    Gap(16)
                    val clipboardManager = LocalClipboardManager.current
                    Button(onClick = {
                        clipboardManager.setText(AnnotatedString("${exception.id} (${exception.exception})"))
                    }) {
                        Text("复制错误 ID")
                    }
                    Box(Modifier.weight(1F).widthIn(max = 700.dp)) {
                        Scrollable(horizontal = false) {
                            Text(exception.exception.stackTraceToString().replace("\t", "    "), style = bodySmall)
                        }
                    }
                }
            }
        }
    }
}
