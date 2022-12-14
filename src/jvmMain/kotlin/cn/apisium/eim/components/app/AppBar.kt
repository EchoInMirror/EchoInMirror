package cn.apisium.eim.components.app

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.ButtonDefaults.textButtonColors
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import cn.apisium.eim.EchoInMirror
import cn.apisium.eim.components.FloatingDialog
import cn.apisium.eim.components.MenuItem
import cn.apisium.eim.data.defaultScale
import cn.apisium.eim.data.midi.KEY_NAMES
import cn.apisium.eim.data.quantificationUnits
import cn.apisium.eim.utils.rem

@Composable
fun getAppBarFont() = TextStyle(MaterialTheme.colorScheme.onBackground, 18.sp, fontWeight = FontWeight.Bold,
    letterSpacing = (-0.7F).sp, lineHeight = 0.sp)

@Composable
fun AppBarSubTitle(title: String) {
    Text(title, fontSize = 9.sp, letterSpacing = (-0.5).sp, lineHeight = 0.sp)
}
@Composable
fun AppBarTitle(title: String) {
    Text(title, fontSize = 18.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.7).sp, lineHeight = 0.sp)
}

@Composable
fun AppBarItem(title: String? = null, subTitle: String? = null, modifier: Modifier = Modifier,
               content: @Composable (() -> Unit)? = null) {
    Column(modifier.padding(6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        if (title != null) AppBarTitle(title)
        content?.invoke()
        if (subTitle != null) AppBarSubTitle(subTitle)
    }
}

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
        Row(verticalAlignment = Alignment.Bottom) {
            Text("${(EchoInMirror.currentPosition.timeInSeconds.toInt() / 60).toString().padStart(2, '0')}:${(EchoInMirror.currentPosition.timeInSeconds.toInt() % 60).toString().padStart(2, '0')}:",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.7F).sp,
                lineHeight = 0.sp
            )
            Text(((EchoInMirror.currentPosition.timeInSeconds * 1000).toInt() % 1000).toString().padStart(3, '0'),
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
            Text("${(1 + EchoInMirror.currentPosition.ppqPosition / EchoInMirror.currentPosition.timeSigNumerator)
                    .toInt().toString().padStart(2, '0')}:${(1 + EchoInMirror.currentPosition.ppqPosition
                    .toInt() % EchoInMirror.currentPosition.timeSigNumerator).toString().padStart(2, '0')}:",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.7F).sp,
                lineHeight = 0.sp
            )
            Text((1 + (EchoInMirror.currentPosition.ppqPosition - EchoInMirror.currentPosition.ppqPosition.toInt()) *
                        (16 / EchoInMirror.currentPosition.timeSigDenominator)).toInt().toString(),
                fontSize = 14.sp,
                letterSpacing = (-1.5).sp,
                lineHeight = 0.sp
            )
        }
    }
}

@Composable
private fun Quantification() {
    FloatingDialog({ _, close ->
        Surface(Modifier.width(IntrinsicSize.Min), MaterialTheme.shapes.extraSmall,
            shadowElevation = 6.dp, tonalElevation = 1.dp) {
            Column {
                quantificationUnits.forEach {
                    if (it.hasDividerAbove) Divider()
                    MenuItem(EchoInMirror.quantification == it, {
                        close()
                        EchoInMirror.quantification = it
                    }) {
                        Text(it.name, fontWeight = if (it.isSpecial) FontWeight.Bold else FontWeight.Normal)
                    }
                }
            }
        }
    }) {
        AppBarItem(EchoInMirror.quantification.name, "??????")
    }
}

@Composable
fun RootNote() {
    FloatingDialog({ _, close ->
        Surface(Modifier.width(IntrinsicSize.Min), MaterialTheme.shapes.extraSmall,
            shadowElevation = 6.dp, tonalElevation = 1.dp) {
            Column {
                KEY_NAMES.forEach {
                    MenuItem(false, { close() }) { Text(it) }
                }
            }
        }
    }) {
        AppBarItem("C", "??????")
    }
}

@Composable
fun Scale() {
    AppBarItem(defaultScale.name, "??????")
}

@Composable
private fun TimeSignature() {
    AppBarItem(subTitle = "??????") {
        Row(verticalAlignment = Alignment.Bottom) {
            BasicTextField(EchoInMirror.currentPosition.timeSigNumerator.toString(), {
                EchoInMirror.currentPosition.timeSigNumerator = it.toIntOrNull()?.coerceIn(1, 32) ?: return@BasicTextField
            }, Modifier.width(IntrinsicSize.Min), textStyle = getAppBarFont(), singleLine = true)
            AppBarTitle("/")
            FloatingDialog({ _, close ->
                Surface(shape = MaterialTheme.shapes.extraSmall,
                    shadowElevation = 6.dp, tonalElevation = 1.dp) {
                    Column {
                        arrayOf(2, 4, 8, 16).forEach {
                            MenuItem(EchoInMirror.currentPosition.timeSigDenominator == it, {
                                close()
                                EchoInMirror.currentPosition.timeSigDenominator = it
                            }) {
                                Text(it.toString())
                            }
                        }
                    }
                }
            }) {
                AppBarTitle(EchoInMirror.currentPosition.timeSigDenominator.toString())
            }
        }
    }
}

@Composable
private fun BPM() {
    AppBarItem(subTitle = "BPM") {
        BasicTextField("%.2f" % EchoInMirror.currentPosition.bpm, {
            EchoInMirror.currentPosition.bpm = it.toDoubleOrNull()?.coerceIn(1.0, 600.0) ?: return@BasicTextField
        }, Modifier.width(IntrinsicSize.Min), textStyle = getAppBarFont(), singleLine = true)
    }
}

val APP_BAR_HEIGHT = 54.dp

@Composable
internal fun EimAppBar() {
    Surface(modifier = Modifier.fillMaxWidth().height(APP_BAR_HEIGHT), shadowElevation = 2.dp, tonalElevation = 5.dp) {
        Row(
            Modifier.padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically
        ) {
            Row(Modifier.weight(1f), horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically) { }
            Row(horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                TimeText()
                AppBarIcons()
                PPQText()
            }
            Row(Modifier.weight(1f), horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically) {
                Quantification()
                RootNote()
                Scale()
                TimeSignature()
                BPM()
            }
        }
    }
}
