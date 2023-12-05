package com.eimsound.daw.window.dialogs.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SettingsInputComponent
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.eimsound.audioprocessor.AudioPlayerManager
import com.eimsound.daw.Configuration
import com.eimsound.daw.api.EchoInMirror
import com.eimsound.daw.components.*
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.apache.commons.lang3.SystemUtils
import kotlin.math.roundToInt

@Composable
private fun Latency(title: String, latency: Int) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(title, Modifier.weight(1f))
        Text(
            "${(latency * 1000.0 / EchoInMirror.currentPosition.sampleRate).roundToInt()} 毫秒 / $latency 个采样",
            Modifier.weight(1f)
        )
    }
}

@OptIn(DelicateCoroutinesApi::class)
private fun reopenAudioDevice() {
    GlobalScope.launch(Dispatchers.IO) {
        Configuration.save()
        EchoInMirror.player = EchoInMirror.createAudioPlayer()
    }
}

internal object AudioSettings : SettingTab {
    @Composable
    override fun label() {
        Text("音频")
    }

    @Composable
    override fun icon() {
        Icon(Icons.Filled.SettingsInputComponent, "Audio Settings")
    }

    @Composable
    override fun content() {
        Column {
            SettingsSection("设备与音频设置") {
                SettingsCard("音频工厂") {
                    SettingsMenu(
                        AudioPlayerManager.instance.factories.keys,
                        Configuration.audioDeviceFactoryName
                    ) {
                        if (Configuration.audioDeviceFactoryName == it) return@SettingsMenu
                        EchoInMirror.player?.close()
                        Configuration.audioDeviceName = ""
                        Configuration.audioDeviceFactoryName = it
                        reopenAudioDevice()
                    }
                }
                SettingsCard("音频设备") {
                    var playerNames by remember { mutableStateOf(emptyList<String>()) }
                    LaunchedEffect(Configuration.audioDeviceName) {
                        playerNames = AudioPlayerManager.instance.factories[Configuration.audioDeviceFactoryName]!!.getPlayers()
                    }
                    SettingsMenu(
                        playerNames,
                        Configuration.audioDeviceName
                    ) {
                        if (Configuration.audioDeviceName == it) return@SettingsMenu
                        EchoInMirror.player?.close()
                        Configuration.audioDeviceName = it
                        reopenAudioDevice()
                    }
                }
                SettingsCard("缓冲区大小") {
                    SettingsMenu(
                        EchoInMirror.player?.availableBufferSizes?.toList(),
                        EchoInMirror.currentPosition.bufferSize,
                        { "$it 个采样" }
                    ) {
                        EchoInMirror.player?.close()
//                        EchoInMirror.currentPosition.setSampleRateAndBufferSize(
//                            EchoInMirror.currentPosition.sampleRate, it
//                        )

                        reopenAudioDevice()
                    }
                }
                SettingsCard("采样率") {
                    var sampleRate by remember { mutableStateOf(EchoInMirror.currentPosition.sampleRate) }
                    SettingsMenu(
                        EchoInMirror.player?.availableSampleRates?.toList(),
                        sampleRate,
                    ) {
                        sampleRate = it
                        EchoInMirror.player?.close()
                        Configuration.preferredSampleRate = it

                        reopenAudioDevice()
                    }
                }
                if (SystemUtils.IS_OS_WINDOWS) SettingsCard("后台共享音频设备") {
                    Switch(
                        Configuration.stopAudioOutputOnBlur,
                        {
                            Configuration.stopAudioOutputOnBlur = it
                            Configuration.save()
                        }
                    )
                }
                SettingsCard("对超过 0db 的音频进行削波") {
                    Switch(
                        Configuration.autoCutOver0db,
                        {
                            Configuration.autoCutOver0db = it
                            Configuration.save()
                        }
                    )
                }
            }
            Gap(8)
            Latency("输入延迟", EchoInMirror.player?.inputLatency ?: 0)
            Latency("输出延迟", EchoInMirror.player?.outputLatency ?: 0)
            Gap(8)
            EchoInMirror.player?.Controls()
        }
    }
}