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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cn.apisium.eim.data.midi.KEY_NAMES
import cn.apisium.eim.utils.toOnSurface

val scales = arrayOf(true, false, true, false, true, false, true, true, false, true, false, true)

@OptIn(ExperimentalComposeUiApi::class)
@Preview
@Composable
fun Keyboard(
    onNoteOn: (key: Int) -> Unit,
    onNoteOff: (key: Int) -> Unit,
    keyWidth: Int = 68,
    keyHeight: Int = 16,
    whiteKeyColor: Color = MaterialTheme.colorScheme.background,
    backKeyColor: Color = MaterialTheme.colorScheme.secondary
) {
    Column(Modifier.width(keyWidth.dp)) {
        for (i in 131 downTo 0) {
            val name = KEY_NAMES[i % 12]
            val isWhite = scales[i % 12]
            val color = if (isWhite) whiteKeyColor else backKeyColor
            var modifier = Modifier.fillMaxWidth().height(keyHeight.dp).background(color)
            if (i <= 0x7F) modifier = modifier.clickable {  }
                .onPointerEvent(PointerEventType.Press) { onNoteOn(i) }
                .onPointerEvent(PointerEventType.Release) { onNoteOff(i) }
            Box(modifier) {
                Text(name + (i / 12), Modifier.align(Alignment.CenterEnd).padding(horizontal = 5.dp),
                    fontSize = 11.sp,
                    letterSpacing = (-0.3).sp,
                    lineHeight = 11.sp,
                    fontWeight = if (i % 12 == 0) FontWeight.ExtraBold else null,
                    color = color.toOnSurface,
                    fontStyle = if (isWhite) null else FontStyle.Italic,
                )
            }
            if (i != 0) Divider(color = if (i % 12 == 0) MaterialTheme.colorScheme.outline
            else MaterialTheme.colorScheme.outlineVariant)
        }
    }
}