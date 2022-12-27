package cn.apisium.eim.components.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.ButtonDefaults.textButtonColors
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import cn.apisium.eim.EchoInMirror
import cn.apisium.eim.components.FloatingDialog
import cn.apisium.eim.components.MenuItem

@Composable
fun AppBarButton(onClick: (() -> Unit)? = null, content: @Composable RowScope.() -> Unit) {
    TextButton(onClick ?: { }, enabled = onClick != null,
        colors = textButtonColors(contentColor = MaterialTheme.colorScheme.onSurface,
            disabledContentColor = MaterialTheme.colorScheme.onSurface), content = content)
}

@Composable
private fun AppBarIcons() {
    AppBarButton({ EchoInMirror.currentPosition.isPlaying = !EchoInMirror.currentPosition.isPlaying }) {
        Icon(
            imageVector = if (EchoInMirror.currentPosition.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
            contentDescription = "PlayOrPause"
        )
    }
    AppBarButton({
        EchoInMirror.currentPosition.isPlaying = false
        EchoInMirror.currentPosition.setPPQPosition(0.0)
    }) {
        Icon(
            imageVector = Icons.Filled.Stop,
            contentDescription = "Stop"
        )
    }
    AppBarButton({ }) {
        Icon(
            imageVector = Icons.Filled.FiberManualRecord,
            contentDescription = "Record"
        )
    }
}

@Composable
private fun TimeText() {
    AppBarButton {
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
    }
}

@Composable
private fun PPQText() {
    AppBarButton {
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
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RightSide() {
    FloatingDialog({
        /*
        <MenuItem value={state.ppq * state.timeSigNumerator}><em>小节</em></MenuItem>
 <Divider />
 <MenuItem value={state.ppq}><em>节拍</em></MenuItem>
 <MenuItem value={state.ppq / 2}>1/2 拍</MenuItem>
 <MenuItem value={state.ppq / 3 | 0}>1/3 拍</MenuItem>
 <MenuItem value={state.ppq / 4}>1/4 拍</MenuItem>
 <MenuItem value={state.ppq / 6 | 0}>1/6 拍</MenuItem>
 <Divider />
 <MenuItem value={steps}><em>步进</em></MenuItem>
 <MenuItem value={steps / 2}>1/2 步</MenuItem>
 <MenuItem value={steps / 3 | 0}>1/3 步</MenuItem>
 <MenuItem value={steps / 4}>1/4 步</MenuItem>
 <MenuItem value={steps / 6 | 0}>1/6 步</MenuItem>
 <Divider />
 <MenuItem value={1}><em>无</em></MenuItem>
         */
        Surface(Modifier.widthIn(min = it.width.dp), MaterialTheme.shapes.extraSmall, shadowElevation = 6.dp, tonalElevation = 1.dp) {
            Column {
                MenuItem(false, { }) {
                    Text("小节", fontStyle = FontStyle.Italic)
                }
                Divider()
                MenuItem(false, { }) {
                    Text("节拍", fontStyle = FontStyle.Italic)
                }
                MenuItem(false, { }) {
                    Text("1/2 拍")
                }
            }
        }
    }) {
        BasicTextField("步进", { }, Modifier.width(50.dp), readOnly = true, singleLine = true)
    }
}

val APP_BAR_HEIGHT = 54.dp

@Composable
internal fun EimAppBar() {
    Surface(modifier = Modifier.fillMaxWidth().height(APP_BAR_HEIGHT), shadowElevation = 2.dp) {
        Row(
            Modifier.background(MaterialTheme.colorScheme.surfaceColorAtElevation(5.dp)),
            horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically
        ) {
            Row { }
            Row {
                TimeText()
                AppBarIcons()
                PPQText()
            }
            Row { RightSide() }
        }
    }
}
