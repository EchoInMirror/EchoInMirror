package cn.apisium.eim.components.app

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import cn.apisium.eim.EchoInMirror
import cn.apisium.eim.window.playlistVerticalScrollState

@Composable
private fun RowScope.AppBarIcons() {
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
private fun RowScope.TimeText() {
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
private fun RowScope.PPQText() {
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

val APP_BAR_HEIGHT = 34.dp
val APP_BAR_PADDING = 10.dp
val APP_BAR_FULL_HEIGHT = APP_BAR_PADDING * 2 + APP_BAR_HEIGHT

@Composable
internal fun EimAppBar() {
    var screenWidth by remember { mutableStateOf(1) }
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth().padding(top = APP_BAR_PADDING)
        .onGloballyPositioned { screenWidth = it.size.width }) {
        val isFloat = playlistVerticalScrollState.value > 8 || screenWidth < 800
        val color by animateColorAsState(
            if (isFloat) MaterialTheme.colorScheme.surfaceColorAtElevation(5.dp) else MaterialTheme.colorScheme.background,
            tween(durationMillis = 250)
        )
        val shadow by animateFloatAsState(if (isFloat) 3F else 0F, tween(durationMillis = 250))
        val width by animateFloatAsState(if (isFloat) 300F else if (screenWidth > 900) 500F else 350F, tween(durationMillis = 250))
        Surface(modifier = Modifier.size(width.dp, APP_BAR_HEIGHT), shape = ShapeDefaults.Large, shadowElevation = shadow.dp) {
            NavigationBar {
                Row(Modifier.background(color)) {
                    TimeText()
                    AppBarIcons()
                    PPQText()
                }
            }
        }
    }
}
