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
import com.eimsound.daw.language.langs
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
            "${(latency * 1000.0 / EchoInMirror.currentPosition.sampleRate).roundToInt()} ${langs.ms} / $latency ${langs.sample}",
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
        Text(langs.audio)
    }

    @Composable
    override fun icon() {
        Icon(Icons.Filled.SettingsInputComponent, langs.audioSettingsLang.name)
    }

    @Composable
    override fun content() {
        val colors = textFieldGrayColors()
        Column {
            SettingsSection(langs.audioSettingsLang.name) {
                SettingsCard(langs.audioSettingsLang.audioFactory) {
                    AutoWidthOutlinedDropdownSelector(
                        {
                            if (Configuration.audioDeviceFactoryName == it) return@AutoWidthOutlinedDropdownSelector
                            EchoInMirror.player?.close()
                            Configuration.audioDeviceName = ""
                            Configuration.audioDeviceFactoryName = it
                            reopenAudioDevice()
                        },
                        AudioPlayerManager.instance.factories.keys,
                        Configuration.audioDeviceFactoryName,
                        colors = colors
                    )
                }
                SettingsCard(langs.audioSettingsLang.audioDevice) {
                    var playerNames by remember { mutableStateOf(emptyList<String>()) }
                    LaunchedEffect(Configuration.audioDeviceName) {
                        playerNames = AudioPlayerManager.instance.factories[Configuration.audioDeviceFactoryName]!!.getPlayers()
                    }
                    AutoWidthOutlinedDropdownSelector(
                        {
                            if (Configuration.audioDeviceName == it) return@AutoWidthOutlinedDropdownSelector
                            EchoInMirror.player?.close()
                            Configuration.audioDeviceName = it
                            reopenAudioDevice()
                        },
                        playerNames,
                        Configuration.audioDeviceName,
                        colors = colors
                    )
                }
                SettingsCard(langs.audioSettingsLang.bufferSize) {
                    AutoWidthOutlinedDropdownSelector(
                        {
                            EchoInMirror.player?.close()
                            Configuration.preferredBufferSize = it

                            reopenAudioDevice()
                        },
                        EchoInMirror.player?.availableBufferSizes ?: emptyList(),
                        EchoInMirror.player?.bufferSize,
                        colors = colors
                    )
                }
                SettingsCard(langs.sampleRate) {
                    AutoWidthOutlinedDropdownSelector(
                        {
                            EchoInMirror.player?.close()
                            Configuration.preferredSampleRate = it

                            reopenAudioDevice()
                        },
                        EchoInMirror.player?.availableSampleRates ?: emptyList(),
                        EchoInMirror.player?.sampleRate,
                        colors = colors
                    )
                }
                if (SystemUtils.IS_OS_WINDOWS) SettingsCard(langs.audioSettingsLang.sharedAudioDevice) {
                    Switch(
                        Configuration.stopAudioOutputOnBlur,
                        {
                            Configuration.stopAudioOutputOnBlur = it
                            Configuration.save()
                        }
                    )
                }
                SettingsCard(langs.audioSettingsLang.clipAudio) {
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
            Latency(langs.audioSettingsLang.inputLatency, EchoInMirror.player?.inputLatency ?: 0)
            Latency(langs.audioSettingsLang.outputLatency, EchoInMirror.player?.outputLatency ?: 0)
            Gap(8)
            EchoInMirror.player?.Controls()
        }
    }
}