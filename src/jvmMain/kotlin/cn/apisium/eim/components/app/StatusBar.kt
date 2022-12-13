package cn.apisium.eim.components.app

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EventNote
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import cn.apisium.eim.utils.Border
import cn.apisium.eim.EchoInMirror
import cn.apisium.eim.utils.border
import cn.apisium.eim.icons.MetronomeTick

@Composable
fun StatusBarItem(id: String, icon: ImageVector? = null, modifier: Modifier = Modifier, onClick: (() -> Unit)? = null, child: (@Composable () -> Unit)? = null) {
    Row(
        modifier.clickable { onClick?.invoke() }.padding(horizontal = 4.dp).height(24.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) Icon(icon, id, modifier = Modifier.size(14.dp))
        Box(Modifier.padding(horizontal = 1.dp))
        if (child != null) child()
    }
}

@Composable
fun statusBar() {
    Surface(tonalElevation = 2.dp) {
        val border = Border(0.6.dp, MaterialTheme.colorScheme.onSurfaceVariant.copy(0.2F))
        Row(modifier = Modifier.height(24.dp).fillMaxWidth().border(top = border, start = if (sideBarSelectedItem == null) null else border)) {
            if (sideBarSelectedItem == null) Surface(modifier = Modifier.fillMaxHeight().width(1.dp), tonalElevation = 5.dp) { }
            CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onSurfaceVariant) {
                ProvideTextStyle(MaterialTheme.typography.labelSmall) {
                    CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onSurfaceVariant) {
                        StatusBarItem("Settings", Icons.Default.Settings, onClick = { EchoInMirror.windowManager.settingsDialogOpen = true })
                        StatusBarItem("Project", Icons.Default.Folder) {
                            Text("临时工程")
                        }
                        StatusBarItem("TimeCost", Icons.Default.EventNote) {
                            Text("大约19小时")
                        }
                        Box(Modifier.weight(2F))
                        StatusBarItem("Pai", MetronomeTick) {
                            Text("${EchoInMirror.currentPosition.timeSigNumerator}/${EchoInMirror.currentPosition.timeSigDenominator}")
                        }
                        StatusBarItem("BPM") {
                            Text("%.2f".format(EchoInMirror.currentPosition.bpm))
                        }
                    }
                }
            }
        }
    }
}
