package com.eimsound.daw.window.dialogs.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import com.eimsound.daw.components.Gap
import com.eimsound.daw.components.SettingTab
import com.eimsound.daw.components.SettingsCard
import com.eimsound.daw.components.SettingsListManager

internal object FileBrowserSettings : SettingTab {
    @Composable
    override fun label() {
        Text("文件")
    }

    @Composable
    override fun icon() {
        Icon(Icons.Filled.Folder, "File Browser")
    }

    @Composable
    @OptIn(ExperimentalMaterial3Api::class)
    override fun content() {
        var showOptionalFormat by remember { mutableStateOf(true) }
        var folderPathList by remember { mutableStateOf(listOf<String>(
            "C:\\Users\\Administrator\\Desktop\\test",
            "C:\\Users\\Administrator\\Desktop\\test2"
        ))}

        Column {
            SettingSection("文件浏览器的自定义文件夹") {
                SettingsListManager(
                    folderPathList,
                    addButtonText = "添加文件夹",
                    onAddButtonClick = {
                        folderPathList = folderPathList + "C:\\Users\\Administrator\\Desktop\\test111"
                    }
                )
            }
            Gap(16)
            SettingSection("文件浏览器的个性化配置项") {
                SettingsCard("只显示可导入格式的文件") {
                    Switch(checked = showOptionalFormat, onCheckedChange = {
                        showOptionalFormat = it
                    })
                }
                SettingsCard("不显示可导入的格式") {
                    Switch(checked = showOptionalFormat, onCheckedChange = {
                        showOptionalFormat = it
                    })
                }
                SettingsCard("只显示不可导入的格式") {
                    Switch(checked = showOptionalFormat, onCheckedChange = {
                        showOptionalFormat = it
                    })
                }
            }
        }
    }
}

@Composable
private fun SettingSection(
    title: String? = null,
    content: @Composable () -> Unit
){
    Column {
        if (title != null){
            Text(title, style = MaterialTheme.typography.bodyMedium)
            Gap(8)
        }
        content()
    }
}