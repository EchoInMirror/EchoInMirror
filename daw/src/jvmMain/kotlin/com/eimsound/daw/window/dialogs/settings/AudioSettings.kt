package com.eimsound.daw.window.dialogs.settings

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SettingsInputComponent
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.eimsound.audioprocessor.AudioProcessor
import com.eimsound.daw.Configuration
import com.eimsound.daw.EchoInMirror
import com.eimsound.daw.components.*

internal object AudioSettings : Tab {
    @Composable
    override fun label() {
        Text("音频")
    }

    @Composable
    override fun icon() {
        Icon(Icons.Filled.SettingsInputComponent, "Audio Settings")
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun content() {
        Column {
            var selectedFactoryName by remember { mutableStateOf(Configuration.audioFactoryName) }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("音频工厂:", Modifier.weight(1f))
                Menu({ close ->
                    EchoInMirror.audioPlayerManager.factories.forEach { (name, _) ->
                        MenuItem(
                            selectedFactoryName == name,
                            {
                                selectedFactoryName = name
                                Configuration.audioFactoryName = name
                                Configuration.save()
                                close()
                            }
                        ) {
                            Text(name)
                        }
                    }
                }, boxModifier = Modifier.weight(1f)) {
                    Text(selectedFactoryName, Modifier.fillMaxWidth())
                }
                Spacer(Modifier.weight(.5f))
            }
            Spacer(Modifier.height(10.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                var selectedPlayerName by remember { mutableStateOf(EchoInMirror.player?.name ?: "未选定") }
                Text("音频设备:", Modifier.weight(1f))
                Menu({ close ->
                    var playerNams by remember { mutableStateOf(emptyList<String>()) }
                    LaunchedEffect(selectedFactoryName) {
                        playerNams = EchoInMirror.audioPlayerManager.factories[selectedFactoryName]!!.getPlayers()
                    }
                    playerNams.forEach { playerName ->
                        MenuItem(
                            selectedPlayerName == playerName,
                            {
                                EchoInMirror.player?.close()
                                EchoInMirror.player =
                                    EchoInMirror.audioPlayerManager.factories[selectedFactoryName]!!.create(
                                        playerName,
                                        EchoInMirror.currentPosition,
                                        EchoInMirror.bus as AudioProcessor
                                    )
                                selectedPlayerName = playerName
                                close()
                            }
                        ) {
                            Text(playerName)
                        }

                    }
                }, boxModifier = Modifier.weight(1f)) {
                    Text(selectedPlayerName, Modifier.fillMaxWidth())
                }
                Spacer(Modifier.weight(.5f))
            }
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                var bufferSize by remember { mutableStateOf(EchoInMirror.currentPosition.bufferSize) }
                Text("设备区块大小:", Modifier.weight(1f))
                Menu({ close ->
                    arrayOf(128, 256, 512, 1024).forEach {
                        MenuItem(
                            bufferSize == it,
                            {
                                bufferSize = it
                                EchoInMirror.player?.close()
                                EchoInMirror.currentPosition.bufferSize = it
                                close()
                            }
                        ) {
                            Text("${it}个采样")
                        }
                    }
                }, boxModifier = Modifier.weight(1f)) {
                    Text("${bufferSize}个采样", Modifier.fillMaxWidth())
                }
                Spacer(Modifier.weight(.5f))
            }

            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                var sampleRate by remember { mutableStateOf(EchoInMirror.currentPosition.sampleRate) }
                Text("采样率:", Modifier.weight(1f))
                Menu({ close ->
                    arrayOf(48000, 44100).forEach {
                        MenuItem(
                            sampleRate == it,
                            {
                                sampleRate = it
                                EchoInMirror.player?.close()
                                EchoInMirror.currentPosition.sampleRate = it
                                close()
                            }
                        ) {
                            Text(it.toString())
                        }
                    }
                }, boxModifier = Modifier.weight(1f)) {
                    Text(sampleRate.toString(), Modifier.fillMaxWidth())
                }
                Spacer(Modifier.weight(.5f))
            }

            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                var shareDevice by remember { mutableStateOf(Configuration.stopAudioOutputOnBlur) }
                Text("后台共享音频设备")
                Checkbox(
                    shareDevice,
                    {
                        shareDevice = it
                        Configuration.stopAudioOutputOnBlur = it
                        Configuration.save()
                    }
                )
            }

            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                var autoCutOver0db by remember { mutableStateOf(Configuration.autoCutOver0db) }
                Text("对超过 0db 的音频进行削波")
                Checkbox(
                    autoCutOver0db,
                    {
                        autoCutOver0db = it
                        Configuration.autoCutOver0db = it
                        Configuration.save()
                    }
                )
            }

            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                val inputDelay by mutableStateOf(EchoInMirror.player?.inputLatency ?: 0)
                Text("输入延迟:", Modifier.weight(1f))
                Text(
                    "${inputDelay / EchoInMirror.currentPosition.sampleRate * 1000} 毫秒 / $inputDelay 个采样",
                    Modifier.weight(1f)
                )

                Spacer(Modifier.weight(.5f))
            }

            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                val outputDelay by mutableStateOf(EchoInMirror.player?.outputLatency ?: 0)
                Text("输入延迟:", Modifier.weight(1f))
                Text(
                    "${outputDelay / EchoInMirror.currentPosition.sampleRate * 1000} 毫秒 / $outputDelay 个采样",
                    Modifier.weight(1f)
                )

                Spacer(Modifier.weight(.5f))
            }
        }


    }
}