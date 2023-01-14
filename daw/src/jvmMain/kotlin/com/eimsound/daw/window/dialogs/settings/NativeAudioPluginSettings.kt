package com.eimsound.daw.window.dialogs.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.eimsound.daw.EchoInMirror
import com.eimsound.daw.components.Gap
import com.eimsound.daw.components.Tab
import com.eimsound.daw.impl.processor.nativeAudioPluginManager
import com.eimsound.daw.utils.*
import com.eimsound.dsp.native.NativeAudioPluginFactoryImpl
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.io.File
import javax.swing.JFileChooser
import javax.swing.filechooser.FileNameExtensionFilter

private var scanningJob by mutableStateOf<Job?>(null)
internal object NativeAudioPluginSettings: Tab {
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
                    EchoInMirror.audioProcessorManager.nativeAudioPluginManager.scan()
                    scanningJob = null
                }
            } else {
                scanningJob!!.cancel()
                scanningJob = null
                val list = (EchoInMirror.audioProcessorManager.nativeAudioPluginManager as NativeAudioPluginFactoryImpl).scanningPlugins
                list.forEach { it.value.destroy() }
                list.clear()
            }
        }) { Text(if (scanningJob == null) "搜索" else "取消") }
    }

    @Suppress("DuplicatedCode")
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun content() {
        Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            CompositionLocalProvider(
                LocalContentColor provides MaterialTheme.colorScheme.surface,
                LocalAbsoluteTonalElevation provides 0.dp
            ) {
                val window = CurrentWindow.current
                val apm = EchoInMirror.audioProcessorManager.nativeAudioPluginManager as NativeAudioPluginFactoryImpl
                Column(Modifier.width(300.dp)) {
                    Row {
                        Text(
                            text = "搜索路径",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(bottom = 8.dp).weight(1F),
                        )
                        IconButton(
                            onClick = {
                                openFolderBrowser(window)?.let {
                                    apm.scanPaths.add(it.absolutePath)
                                    apm.save()
                                }
                            },
                            modifier = Modifier.clip(CircleShape).size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Add",
                                tint = MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                    Card {
                        apm.scanPaths.forEach {
                            ListItem(
                                modifier = Modifier.background(MaterialTheme.colorScheme.surface).clickableWithIcon {
                                    if (apm.pluginIsFile) openInExplorer(File(it))
                                },
                                headlineText = { Text(it) },
                                trailingContent = {
                                    IconButton(
                                        {
                                            apm.scanPaths.remove(it)
                                            apm.save()
                                        },
                                        modifier = Modifier.size(24.dp).weight(1F)
                                    ) {
                                        Icon(Icons.Filled.Delete, contentDescription = "Delete")
                                    }
                                }
                            )
                        }
                    }

                    if (scanningJob != null) {
                        Gap(6)
                        Box {
                            Text(
                                text = "正在搜索... (${apm.scannedCount}/${apm.allScanCount})",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(bottom = 8.dp),
                            )
                        }
                        if (apm.allScanCount != 0) LinearProgressIndicator(
                            modifier = Modifier.padding(vertical = 4.dp).fillMaxWidth(),
                            progress = apm.scannedCount.toFloat() / apm.allScanCount,
                        )
                        Card {
                            apm.scanningPlugins.forEach { (k, v) ->
                                key(k) {
                                    ListItem(
                                        modifier = Modifier.background(MaterialTheme.colorScheme.surface).clickableWithIcon {
                                            if (apm.pluginIsFile) selectInExplorer(File(k))
                                        },
                                        headlineText = { Text(k) },
                                        trailingContent = {
                                            IconButton(
                                                { v.destroy() },
                                                modifier = Modifier.size(24.dp).weight(1F)
                                            ) {
                                                Icon(Icons.Filled.Cancel, contentDescription = "Cancel")
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                Column(Modifier.weight(1F)) {
                    Row {
                        Text(
                            text = "排除路径",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(bottom = 8.dp).weight(1F),
                        )
                        IconButton(
                            onClick = {
                                val fileChooser = JFileChooser().apply {
                                    isAcceptAllFileFilterUsed = false
                                    fileFilter = FileNameExtensionFilter("Native Audio Plugin Format (${
                                        apm.pluginExtensions.joinToString(", ") { ".$it" }
                                    })", *apm.pluginExtensions.toTypedArray())
                                }
                                fileChooser.showOpenDialog(window)
                                fileChooser.selectedFile?.let {
                                    apm.skipList.add(it.absolutePath)
                                    apm.save()
                                }
                            },
                            modifier = Modifier.clip(CircleShape).size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Add",
                                tint = MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                    Card {
                        apm.skipList.forEach {
                            ListItem(
                                modifier = Modifier.background(MaterialTheme.colorScheme.surface).clickableWithIcon {
                                    if (apm.pluginIsFile) selectInExplorer(File(it))
                                },
                                headlineText = { Text(it) },
                                trailingContent = {
                                    IconButton(
                                        {
                                            apm.skipList.remove(it)
                                            apm.save()
                                        },
                                        modifier = Modifier.size(24.dp).weight(1F)
                                    ) {
                                        Icon(Icons.Filled.Delete, contentDescription = "Delete")
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
