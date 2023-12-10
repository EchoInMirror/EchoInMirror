package com.eimsound.dsp.native.processors

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SettingsInputHdmi
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.eimsound.audioprocessor.NativeAudioPluginFactory
import com.eimsound.daw.components.Gap
import com.eimsound.daw.components.SettingTab
import com.eimsound.daw.components.SettingsListManager
import com.eimsound.daw.components.SettingsSection
import com.eimsound.daw.utils.CurrentWindow
import com.eimsound.daw.utils.openFolderBrowser
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import javax.swing.JFileChooser
import javax.swing.filechooser.FileNameExtensionFilter

@OptIn(DelicateCoroutinesApi::class)
private fun NativeAudioPluginFactory.saveAsync() = GlobalScope.launch {
    save()
}

private var scanningJob by mutableStateOf<Job?>(null)
class NativeAudioPluginSettings: SettingTab {
    @Composable
    override fun label() {
        Text("原生音频插件")
    }

    @Composable
    override fun icon() {
        Icon(
            Icons.Filled.SettingsInputHdmi,
            contentDescription = "Native Audio Plugin",
        )
    }

    @OptIn(DelicateCoroutinesApi::class)
    @Composable
    override fun buttons() {
        TextButton({
            if (scanningJob == null) {
                scanningJob = GlobalScope.launch {
                    NativeAudioPluginFactoryImpl.instance!!.scan()
                    scanningJob = null
                }
            } else {
                scanningJob!!.cancel()
                scanningJob = null
                val list = NativeAudioPluginFactoryImpl.instance!!.scanningPlugins
                list.forEach { it.value.destroy() }
                list.clear()
            }
        }) { Text(if (scanningJob == null) "搜索" else "取消") }
    }

    @Composable
    override fun content() {
        Column {
            val window = CurrentWindow.current
            val apm = NativeAudioPluginFactoryImpl.instance!!

            SettingsSection("搜索路径") {
                SettingsListManager(
                    list = apm.scanPaths,
                    onAddButtonClick = {
                        openFolderBrowser(window) { file ->
                            if (file == null) return@openFolderBrowser
                            apm.scanPaths.add(file.absolutePath)
                            apm.saveAsync()
                        }
                    },
                    onDelete = {
                        apm.scanPaths.remove(it)
                        apm.saveAsync()
                    }
                )
            }

            Gap(8)

            SettingsSection("排除路径") {
                SettingsListManager(
                    list = apm.skipList,
                    onAddButtonClick = {
                        val fileChooser = JFileChooser().apply {
                            isAcceptAllFileFilterUsed = false
                            fileFilter = FileNameExtensionFilter("Native Audio Plugin Format (${
                                apm.pluginExtensions.joinToString(", ") { ".$it" }
                            })", *apm.pluginExtensions.toTypedArray())
                        }
                        fileChooser.showOpenDialog(window)
                        fileChooser.selectedFile?.let {
                            apm.skipList.add(it.absolutePath)
                            apm.saveAsync()
                        }
                    },
                    onDelete = {
                        apm.skipList.remove(it)
                        apm.saveAsync()
                    }
                )
            }

            if (scanningJob != null) {
                Gap(8)
                SettingsSection("正在搜索... (${apm.scannedCount}/${apm.allScanCount})") {
                    if (apm.allScanCount != 0) LinearProgressIndicator(
                        modifier = Modifier.padding(vertical = 4.dp).fillMaxWidth(),
                        progress = apm.scannedCount.toFloat() / apm.allScanCount,
                    )
                    apm.scanningPlugins.keys.let {
                        if (it.isNotEmpty()) {
                            SettingsListManager(
                                list = it
                            )
                        }
                    }
                }
            }
        }
    }
}
