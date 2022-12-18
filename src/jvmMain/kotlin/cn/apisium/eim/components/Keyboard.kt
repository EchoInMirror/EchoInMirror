package cn.apisium.eim.components

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cn.apisium.eim.EchoInMirror
import cn.apisium.eim.data.midi.KEY_NAMES
import cn.apisium.eim.utils.toOnSurfaceColor

val scales = arrayOf(true, false, true, false, true, false, true, true, false, true, false, true)

@OptIn(ExperimentalComposeUiApi::class)
@Preview
@Composable
fun Keyboard(
    onNoteOn: (key: Int, position: Float) -> Unit,
    onNoteOff: (key: Int, position: Float) -> Unit,
    modifier: Modifier = Modifier,
    keyHeight: Dp = 17.dp,
    keyWidth: Dp = 68.dp,
    whiteKeyColor: Color = if (EchoInMirror.windowManager.isDarkTheme) MaterialTheme.colorScheme.secondary
        else MaterialTheme.colorScheme.background,
    backKeyColor: Color = if (EchoInMirror.windowManager.isDarkTheme) MaterialTheme.colorScheme.background
        else MaterialTheme.colorScheme.secondary
) {
    Column(modifier.width(keyWidth)) {
        for (i in 131 downTo 0) {
            val name = KEY_NAMES[i % 12]
            val isWhite = scales[i % 12]
            val color = if (isWhite) whiteKeyColor else backKeyColor
            var modifier2 = Modifier.fillMaxWidth().height(keyHeight - 1.dp).background(color)
            if (i <= 0x7F) modifier2 = modifier2.clickable {  }
                .onPointerEvent(PointerEventType.Press) {
                    onNoteOn(i, if (it.changes.isEmpty()) 0F else it.changes.first().position.x / keyWidth.toPx())
                }
                .onPointerEvent(PointerEventType.Release) {
                    onNoteOff(i, if (it.changes.isEmpty()) 0F else it.changes.first().position.x / keyWidth.toPx())
                }
            Box(modifier2) {
                Text(name + (i / 12), Modifier.align(Alignment.CenterEnd).padding(horizontal = 5.dp),
                    fontSize = 11.sp,
                    letterSpacing = (-0.3).sp,
                    lineHeight = 11.sp,
                    fontWeight = if (i % 12 == 0) FontWeight.ExtraBold else null,
                    color = color.toOnSurfaceColor(),
                    fontStyle = if (isWhite) null else FontStyle.Italic,
                )
            }
            if (i != 0) Divider(color = if (i % 12 == 0) MaterialTheme.colorScheme.outline
            else MaterialTheme.colorScheme.outlineVariant)
        }
    }
}