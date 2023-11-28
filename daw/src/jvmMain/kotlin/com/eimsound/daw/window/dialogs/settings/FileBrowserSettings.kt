package com.eimsound.daw.window.dialogs.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import com.eimsound.daw.Configuration
import com.eimsound.daw.components.Gap
import com.eimsound.daw.components.SettingTab
import com.eimsound.daw.components.SettingsCard
import com.eimsound.daw.components.SettingsListManager
import com.eimsound.daw.utils.CurrentWindow
import com.eimsound.daw.utils.openFolderBrowser


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
    override fun content() {
        val window = CurrentWindow.current

        Column {
            SettingSection("文件浏览器的自定义文件夹") {
                SettingsListManager(
                    Configuration.fileBrowserCustomRoots,
                    addButtonText = "添加文件夹",
                    onAddButtonClick = {
                        openFolderBrowser(window)?.let {
                            Configuration.fileBrowserCustomRoots.add(it.toPath())
                            Configuration.save()
                        }
                    },
                    onDelete = {
                        Configuration.fileBrowserCustomRoots.remove(it)
                        Configuration.save()
                    }
                )
            }
            Gap(16)
            SettingSection("文件浏览器的个性化配置项") {
                SettingsCard("只显示受支持格式的文件") {
                    Switch(checked = Configuration.fileBrowserShowSupFormatOnly, onCheckedChange = {
                        Configuration.fileBrowserShowSupFormatOnly = it
                        Configuration.save()
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