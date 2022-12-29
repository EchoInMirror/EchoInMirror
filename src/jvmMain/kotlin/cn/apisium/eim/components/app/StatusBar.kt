package cn.apisium.eim.components.app

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EventNote
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import cn.apisium.eim.utils.Border
import cn.apisium.eim.EchoInMirror
import cn.apisium.eim.api.ChannelType
import cn.apisium.eim.components.Filled
import cn.apisium.eim.components.FloatingDialog
import cn.apisium.eim.components.MenuItem
import cn.apisium.eim.components.icons.*
import cn.apisium.eim.utils.border
import cn.apisium.eim.utils.onClick
import cn.apisium.eim.window.dialogs.settings.SettingsWindow

@Composable
fun StatusBarItem(id: String, icon: ImageVector? = null, iconColor: Color? = null,
                  modifier: Modifier = Modifier, onClick: (() -> Unit)? = null, child: (@Composable () -> Unit)? = null) {
    Row(
        (if (onClick == null) modifier else modifier.onClick(onClick = onClick)).padding(horizontal = 4.dp).height(24.dp),
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
    FloatingDialog({ _, close ->
        Surface(Modifier.width(IntrinsicSize.Min), shape = MaterialTheme.shapes.extraSmall,
            tonalElevation = 5.dp, shadowElevation = 5.dp) {
            Column {
                ChannelType.values().forEach { type ->
                    MenuItem(type == EchoInMirror.bus.channelType, {
                        close()
                        EchoInMirror.bus.channelType = type
                    }) {
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
        val type = EchoInMirror.bus.channelType
        StatusBarItem("MonoAndStereo", getChannelTypeIcon(type),
            if (type == ChannelType.STEREO) null else MaterialTheme.colorScheme.primary)
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
                    StatusBarItem("Project", Icons.Default.Folder) {
                        Text("临时工程")
                    }
                    StatusBarItem("TimeCost", Icons.Default.EventNote) {
                        Text("大约19小时")
                    }
                    Filled()
                    BusChannelType()
                }
            }
        }
    }
}
