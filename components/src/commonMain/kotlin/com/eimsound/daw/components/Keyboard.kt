package com.eimsound.daw.components

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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eimsound.audioprocessor.data.defaultScale
import com.eimsound.audioprocessor.data.midi.KEY_NAMES
import com.eimsound.daw.components.utils.clickableWithIcon
import com.eimsound.daw.components.utils.toOnSurfaceColor
import com.eimsound.daw.components.utils.x
import com.eimsound.daw.components.utils.y

const val KEYBOARD_KEYS = 128
val KEYBOARD_DEFAULT_WIDTH = 68.dp

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun Keyboard(
    onNoteOn: (key: Int, position: Float) -> Unit,
    onNoteOff: (key: Int, position: Float) -> Unit,
    modifier: Modifier = Modifier,
    keyHeight: Dp = 17.dp,
    keyWidth: Dp = KEYBOARD_DEFAULT_WIDTH,
    whiteKeyColor: Color = MaterialTheme.colorScheme.background,
    blackKeyColor: Color = MaterialTheme.colorScheme.secondary,
    swapColor: Boolean = false
) {
    val whiteKeyColor0 = if (swapColor) blackKeyColor else whiteKeyColor
    val blackKeyColor0 = if (swapColor) whiteKeyColor else blackKeyColor
    val whiteKeyTextColor = whiteKeyColor0.toOnSurfaceColor()
    val blackKeyTextColor = blackKeyColor0.toOnSurfaceColor()
    Column(modifier.size(keyWidth, keyHeight * KEYBOARD_KEYS).graphicsLayer {  }) {
        for (i in (KEYBOARD_KEYS - 1) downTo 0) {
            val name = KEY_NAMES[i % 12]
            val isBlack = defaultScale.scale[i % 12]
            Box(Modifier.fillMaxWidth().weight(1F)
                .background(if (isBlack) blackKeyColor0 else whiteKeyColor0)
                .clickableWithIcon()
                .onPointerEvent(PointerEventType.Press) {
                    onNoteOn(i, it.x / keyWidth.toPx())
                }
                .onPointerEvent(PointerEventType.Release) {
                    onNoteOff(i, it.y / keyWidth.toPx())
                }
            ) {
                if (keyHeight.value < 13) return@Box
                Text(name + (i / 12), Modifier.align(Alignment.CenterEnd).padding(horizontal = 5.dp),
                    fontSize = 11.sp,
                    letterSpacing = (-0.3).sp,
                    lineHeight = 11.sp,
                    fontWeight = if (i % 12 == 0) FontWeight.ExtraBold else null,
                    color = if (isBlack) blackKeyTextColor else whiteKeyTextColor,
                    fontStyle = if (isBlack) FontStyle.Italic else null
                )
            }
            if (i != 0) Divider(color = if (i % 12 == 0) MaterialTheme.colorScheme.outline
            else MaterialTheme.colorScheme.outlineVariant)
        }
    }
}