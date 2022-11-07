package cn.apisium.eim.window

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.SettingsInputComponent
import androidx.compose.material.icons.filled.SettingsInputHdmi
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import cn.apisium.eim.EchoInMirror
import cn.apisium.eim.api.window.SettingsTab
import cn.apisium.eim.components.Filled
import cn.apisium.eim.impl.processor.nativeAudioPluginManager
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.awt.Dimension

private class AudioSettings: SettingsTab {
    @Composable
    override fun label() {
        Text("音频")
    }

    @Composable
    override fun icon() {
        Icon(
            Icons.Filled.SettingsInputComponent,
            contentDescription = "Audio Settings",
        )
    }

    @Composable
    override fun content() {
        BasicText("音频设置")
    }
}

private class NativeAudioPluginSettings: SettingsTab {
    @Composable
    override fun label() {
        Text("VST")
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
            GlobalScope.launch { EchoInMirror.audioProcessorManager.nativeAudioPluginManager.scan() }
        }) {
            Text("搜索")
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun content() {
        Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            CompositionLocalProvider(
                LocalContentColor provides MaterialTheme.colorScheme.surface,
                LocalAbsoluteTonalElevation provides 0.dp
            ) {
                Column(Modifier.width(300.dp)) {
                    Row {
                        Text(
                            text = "搜索路径",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(bottom = 8.dp).weight(1F),
                        )
                        IconButton(
                            onClick = { /*TODO*/ },
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
                        EchoInMirror.audioProcessorManager.nativeAudioPluginManager.scanPaths.forEach {
                            ListItem(
                                modifier = Modifier.background(MaterialTheme.colorScheme.surface),
                                headlineText = { Text(it) },
                                trailingContent = {
                                    IconButton(
                                        { },
                                        modifier = Modifier.size(24.dp).weight(1F)
                                    ) {
                                        Icon(Icons.Filled.Delete, contentDescription = "Delete")
                                    }
                                }
                            )
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
                            onClick = { /*TODO*/ },
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
                        EchoInMirror.audioProcessorManager.nativeAudioPluginManager.skipList.forEach {
                            ListItem(
                                modifier = Modifier.background(MaterialTheme.colorScheme.surface),
                                headlineText = { Text(it) },
                                trailingContent = {
                                    IconButton(
                                        { },
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

val settingsTabs = mutableStateListOf(NativeAudioPluginSettings(), AudioSettings())

@Composable
fun settingsWindow() {
    Dialog({ EchoInMirror.windowManager.settingsDialogOpen = false }, title = "设置") {
        window.minimumSize = Dimension(800, 600)
        Surface(Modifier.fillMaxSize(), tonalElevation = 2.dp) {
            Row {
                var selected by remember { mutableStateOf(settingsTabs.getOrNull(0)?.run { this::class.java.name } ?: "") }
                val selectedTab = settingsTabs.find { it::class.java.name == selected }
                NavigationRail {
                    settingsTabs.forEach {
                        key(it::class.java.name) {
                            NavigationRailItem(
                                icon = { it.icon() },
                                label = { it.label() },
                                selected = selected == it::class.java.name,
                                onClick = { selected = it::class.java.name }
                            )
                        }
                    }
                }
                Column(Modifier.fillMaxSize()) {
                    val stateVertical = rememberScrollState(0)
                    Box(Modifier.weight(1F)) {
                        Box(Modifier.fillMaxSize().verticalScroll(stateVertical)) {
                            Box(Modifier.padding(14.dp)) { selectedTab?.content() }
                        }
                        VerticalScrollbar(
                            modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight(),
                            adapter = rememberScrollbarAdapter(stateVertical)
                        )
                    }
                    Row(Modifier.padding(14.dp, 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Filled()
                        selectedTab?.buttons()
                        Button({ EchoInMirror.windowManager.settingsDialogOpen = false }) { Text("确认") }
                    }
                }
            }
        }
    }
}
