package cn.apisium.eim.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EventNote
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import cn.apisium.eim.currentPosition
import cn.apisium.eim.icons.MetronomeTick
import cn.apisium.eim.timeSigDenominator
import cn.apisium.eim.timeSigNumerator

@Composable
fun StatusBarItem(id: String, icon: ImageVector? = null, modifier: Modifier = Modifier, child: @Composable () -> Unit) {
    Row(modifier.padding(horizontal = 4.dp).height(24.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
        if (icon != null) Icon(icon, id, modifier = Modifier.size(15.dp))
        Box(Modifier.padding(end = 1.dp))
        child()
    }
}

@Composable
fun statusBar() {
    Surface(tonalElevation = 2.dp) {
        Row(modifier = Modifier.height(24.dp).fillMaxWidth()) {
            Surface(modifier = Modifier.fillMaxHeight().width(1.dp), tonalElevation = 5.dp) { }
            CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onSurfaceVariant) {
                ProvideTextStyle(MaterialTheme.typography.labelSmall) {
                    CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onSurfaceVariant) {
                        StatusBarItem("Project", Icons.Default.Folder) {
                            Text("临时工程")
                        }
                        StatusBarItem("TimeCost", Icons.Default.EventNote) {
                            Text("大约19小时")
                        }
                        Box(Modifier.weight(2F))
                        StatusBarItem("Pai", MetronomeTick) {
                            Text("$timeSigNumerator/$timeSigDenominator")
                        }
                        StatusBarItem("BPM") {
                            Text("%.2f".format(currentPosition.bpm))
                        }
                    }
                }
            }
        }
    }
}
