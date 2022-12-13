package cn.apisium.eim.components.app

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import cn.apisium.eim.EchoInMirror

@Composable
private fun RowScope.appBarIcons() {
    NavigationBarItem(false, { EchoInMirror.currentPosition.isPlaying = !EchoInMirror.currentPosition.isPlaying }, {
        Icon(
            imageVector = if (EchoInMirror.currentPosition.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
            contentDescription = "PlayOrPause"
        )
    }, modifier = Modifier.weight(0.4F))
    NavigationBarItem(false, {
        EchoInMirror.currentPosition.isPlaying = false
        EchoInMirror.currentPosition.setPPQPosition(0.0)
    }, {
        Icon(
            imageVector = Icons.Filled.Stop,
            contentDescription = "Stop"
        )
    }, modifier = Modifier.weight(0.4F))
    NavigationBarItem(false, { }, {
        Icon(
            imageVector = Icons.Filled.FiberManualRecord,
            contentDescription = "Record"
        )
    }, modifier = Modifier.weight(0.4F))
}

@Composable
private fun RowScope.timeText() {
    NavigationBarItem(false, { }, {
        Row(verticalAlignment = Alignment.Bottom, modifier = Modifier.padding(start = 4.dp)) {
            Text(
                text = "${(EchoInMirror.currentPosition.timeInSeconds.toInt() / 60).toString().padStart(2, '0')}:${(EchoInMirror.currentPosition.timeInSeconds.toInt() % 60).toString().padStart(2, '0')}:",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.7F).sp,
                lineHeight = 0.sp
            )
            Text(
                text = ((EchoInMirror.currentPosition.timeInSeconds * 1000).toInt() % 1000).toString().padStart(3, '0'),
                fontSize = 14.sp,
                letterSpacing = (-1.5).sp,
                lineHeight = 0.sp
            )
        }
    })
}

@Composable
private fun RowScope.ppqText() {
    NavigationBarItem(false, { }, {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = "${(1 + EchoInMirror.currentPosition.ppqPosition / EchoInMirror.currentPosition.timeSigNumerator)
                    .toInt().toString().padStart(2, '0')}:${(1 + EchoInMirror.currentPosition.ppqPosition
                    .toInt() % EchoInMirror.currentPosition.timeSigNumerator).toString().padStart(2, '0')}:",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.7F).sp,
                lineHeight = 0.sp
            )
            Text(
                text = (1 + (EchoInMirror.currentPosition.ppqPosition - EchoInMirror.currentPosition.ppqPosition.toInt()) *
                        (16 / EchoInMirror.currentPosition.timeSigDenominator)).toInt().toString(),
                fontSize = 14.sp,
                letterSpacing = (-1.5).sp,
                lineHeight = 0.sp
            )
        }
    })
}

@Composable
fun eimAppBar() {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
        Surface(modifier = Modifier.size(300.dp, 34.dp), shadowElevation = 3.dp, shape = ShapeDefaults.Large) {
            NavigationBar {
                timeText()
                appBarIcons()
                ppqText()
            }
        }
    }
}
