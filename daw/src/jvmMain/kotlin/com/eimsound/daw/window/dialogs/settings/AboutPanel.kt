package com.eimsound.daw.window.dialogs.settings

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Help
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.eimsound.daw.RELEASE_TIME
import com.eimsound.daw.VERSION
import com.eimsound.daw.api.window.EditorExtension
import com.eimsound.daw.components.ClickableText
import com.eimsound.daw.components.Gap
import com.eimsound.daw.components.SettingTab
import com.eimsound.daw.dawutils.EIMChan
import com.eimsound.daw.impl.clips.midi.editor.notesEditorExtensions
import com.eimsound.daw.utils.openInBrowser
import com.eimsound.daw.window.panels.playlist.playListExtensions
import org.apache.commons.lang3.SystemUtils
import java.net.URI
import java.text.DateFormat

object EditorEIMChan : EditorExtension {
    override val key = this
    override val isBackground = true
    var alpha by mutableStateOf(0F)

    @Composable
    override fun Content() {
        Box(Modifier.fillMaxSize()) {
            Image(EIMChan, "EIM Chan", Modifier.widthIn(max = 400.dp).align(Alignment.BottomEnd), alpha = alpha)
        }
    }
}

internal object AboutPanel: SettingTab {
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
            Image(EIMChan, "EIM Chan", Modifier.size(400.dp))
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
                Gap(20)
                Text("系统版本: ${SystemUtils.OS_NAME} (${SystemUtils.OS_VERSION}, ${SystemUtils.OS_ARCH})", style = bodySmall)
                Text("Java 版本: ${SystemUtils.JAVA_VM_NAME} (${SystemUtils.JAVA_VM_VERSION}, ${SystemUtils.JAVA_VENDOR})", style = bodySmall)
                Gap(16)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("编辑器中 EIM 娘的不透明度: ", style = bodySmall)
                    Slider(EditorEIMChan.alpha, {
                        val prev = EditorEIMChan.alpha
                        if (prev == it) return@Slider
                        EditorEIMChan.alpha = it
                        if (it == 0F) {
                            playListExtensions.remove(EditorEIMChan)
                            notesEditorExtensions.remove(EditorEIMChan)
                        } else if (prev == 0F) {
                            playListExtensions.add(EditorEIMChan)
                            notesEditorExtensions.add(EditorEIMChan)
                        }
                    }, Modifier.width(160.dp))
                }
            }
        }
    }
}
