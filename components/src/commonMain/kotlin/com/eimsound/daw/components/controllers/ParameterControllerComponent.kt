package com.eimsound.daw.components.controllers

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.eimsound.audioprocessor.AudioProcessorParameter
import com.eimsound.daw.commons.BasicEditor
import com.eimsound.daw.components.*
import com.eimsound.daw.components.silder.Slider
import com.eimsound.daw.components.utils.onRightClickOrLongPress
import com.eimsound.daw.utils.range

var copiedValue = 0F
fun FloatingLayerProvider.openParameterControllerMenu(
    pos: Offset, p: AudioProcessorParameter, clipboardManager: ClipboardManager? = null
) {
    openEditorMenu(pos, object : BasicEditor {
        override fun copy() {
            copiedValue = p.value
            clipboardManager?.setText(AnnotatedString(p.value.toString()))
        }

        override fun paste() {
            if (clipboardManager == null) {
                p.value = copiedValue
                return
            }
            p.value = clipboardManager.getText()?.text?.toFloatOrNull() ?: return
        }

        override val canPaste
            get() = clipboardManager == null || clipboardManager.getText()?.text?.toFloatOrNull() != null

        override val canDelete = false
    }, false) {
        MenuHeader(p.name)
        Divider()
    }
}

@Composable
fun ParameterControllerComponent(p: AudioProcessorParameter) {
    val floatingLayerProvider = LocalFloatingLayerProvider.current
    val clipboardManager = LocalClipboardManager.current
    val audioProcessorParameterValue by rememberUpdatedState(p)
    Column(
        Modifier.width(80.dp).padding(vertical = 4.dp).onRightClickOrLongPress {
            floatingLayerProvider.openParameterControllerMenu(it, audioProcessorParameterValue, clipboardManager)
        }
    ) {
        if (p.isFloat) Slider(p.value, { p.value = it }, valueRange = p.range, onValueReset = { p.value = p.initialValue })
        else Slider(p.value, { p.value = it }, valueRange = p.range, steps = p.range.range.toInt(), onValueReset = { p.value = p.initialValue })
        Text(p.name, Modifier.fillMaxWidth(),
            style = MaterialTheme.typography.labelSmall,
            maxLines = 2, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center
        )
    }
}
