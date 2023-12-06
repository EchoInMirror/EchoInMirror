package com.eimsound.daw.components.app

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.eimsound.daw.Configuration
import com.eimsound.daw.api.EchoInMirror
import com.eimsound.daw.api.processor.ChannelType
import com.eimsound.daw.components.Filled
import com.eimsound.daw.components.FloatingLayer
import com.eimsound.daw.components.MenuItem
import com.eimsound.daw.components.icons.*
import com.eimsound.daw.components.utils.absoluteError
import com.eimsound.daw.components.utils.clickableWithIcon
import com.eimsound.daw.components.utils.warning
import com.eimsound.daw.dawutils.Border
import com.eimsound.daw.dawutils.border
import com.eimsound.daw.dawutils.formatDuration
import com.eimsound.daw.utils.openInExplorer
import com.eimsound.daw.window.dialogs.ExportDialog
import com.eimsound.daw.window.dialogs.settings.SettingsWindow
import kotlin.math.roundToInt

@Composable
fun StatusBarItem(id: String, icon: ImageVector? = null, iconColor: Color? = null,
                  modifier: Modifier = Modifier, onLongClick: (() -> Unit)? = null,
                  onDoubleClick: (() -> Unit)? = null, onClick: (() -> Unit)? = null,
                  child: (@Composable () -> Unit)? = null) {
    Row(
        (if (onClick == null) modifier else modifier
            .clickableWithIcon(onLongClick = onLongClick, onDoubleClick = onDoubleClick, onClick = onClick))
            .padding(horizontal = 4.dp).height(24.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) Icon(icon, id, Modifier.size(14.dp), iconColor ?: LocalContentColor.current)
        Box(Modifier.padding(horizontal = 1.dp))
        if (child != null) child()
    }
}

fun getChannelTypeIcon(type: ChannelType) = when (type) {
    ChannelType.STEREO -> SetNone
    ChannelType.MONO -> SetCenter
    ChannelType.LEFT -> SetLeftCenter
    ChannelType.RIGHT -> SetCenterRight
    ChannelType.SIDE -> SetLeftRight
}

@Composable
private fun BusChannelType() {
    FloatingLayer({ _, close ->
        Surface(Modifier.width(IntrinsicSize.Min), shape = MaterialTheme.shapes.extraSmall,
            tonalElevation = 5.dp, shadowElevation = 5.dp) {
            Column {
                ChannelType.entries.forEach { type ->
                    MenuItem({
                        close()
                        EchoInMirror.bus!!.channelType = type
                    }, type == EchoInMirror.bus!!.channelType) {
                        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(getChannelTypeIcon(type), type.name)
                            Box(Modifier.padding(horizontal = 4.dp))
                            Text(type.name, style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }
            }
        }
    }) {
        val type = EchoInMirror.bus!!.channelType
        StatusBarItem("MonoAndStereo", getChannelTypeIcon(type),
            if (type == ChannelType.STEREO) null else MaterialTheme.colorScheme.primary)
    }
}

@Composable
private fun ThemeSwitch() {
    StatusBarItem("Theme",
        when (Configuration.themeMode) {
            0 -> Icons.Default.LightMode
            1 -> Icons.Default.DarkMode
            else -> Icons.Default.SettingsBrightness
        },
        onClick = { Configuration.themeMode = (Configuration.themeMode + 1) % 3 },
    )
}

@Composable
private fun ProjectName() {
    StatusBarItem("Project", Icons.Default.Folder,
        onClick = { openInExplorer(EchoInMirror.bus!!.project.root.toFile()) },
        onLongClick = { EchoInMirror.windowManager.closeMainWindow() }
    ) {
        Text(EchoInMirror.bus!!.project.name + (if (EchoInMirror.bus?.project?.saved == true) "" else " (未保存)"))
    }
}

@Composable
private fun AudioDevice() {
    val player = EchoInMirror.player
    val sampleRateEqual = player == null || player.sampleRate == EchoInMirror.currentPosition.sampleRate
    StatusBarItem(
        "AudioDevice",
        if (player == null) Icons.Default.HeadsetOff else Icons.Default.Headset,
        if (player == null) MaterialTheme.colorScheme.absoluteError
            else if (sampleRateEqual) null else MaterialTheme.colorScheme.warning,
        onClick = { EchoInMirror.windowManager.dialogs[SettingsWindow] = true }
    ) {
        if (player != null) Text((if (sampleRateEqual) "" else "采样率不匹配: ") + player.name,
            color = if (sampleRateEqual) Color.Unspecified else MaterialTheme.colorScheme.warning)
    }
}

@Composable
internal fun StatusBar() {
    Surface(tonalElevation = 4.dp) {
        val border = Border(0.6.dp, MaterialTheme.colorScheme.onSurfaceVariant.copy(0.2F))
        Row(modifier = Modifier.height(24.dp).fillMaxWidth().border(top = border, start = if (sideBarSelectedItem == null) null else border)) {
            if (sideBarSelectedItem == null) Surface(modifier = Modifier.fillMaxHeight().width(1.dp), tonalElevation = 7.dp) { }
            ProvideTextStyle(MaterialTheme.typography.labelSmall) {
                CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onSurfaceVariant) {
                    StatusBarItem("Settings", Icons.Default.Settings, onClick = { EchoInMirror.windowManager.dialogs[SettingsWindow] = true })
                    ThemeSwitch()
                    StatusBarItem("Export", Icons.Default.IosShare, onClick = { EchoInMirror.windowManager.dialogs[ExportDialog] = true })
                    ProjectName()
                    StatusBarItem("TimeCost", Icons.Default.EventNote) {
                        Text(formatDuration(EchoInMirror.bus!!.project.timeCost.toLong()))
                    }
                    Filled()
                    AudioDevice()
                    BusChannelType()
                    StatusBarItem("CpuLoad", Icons.Filled.Memory) {
                        Text(((EchoInMirror.player?.cpuLoad ?: 0F) * 100).roundToInt().toString() + "%")
                    }
                }
            }
        }
    }
}
