package com.eimsound.daw.components.controllers

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.eimsound.audioprocessor.AudioProcessorParameter
import com.eimsound.audioprocessor.doChangeAction
import com.eimsound.daw.api.EchoInMirror
import com.eimsound.daw.commons.BasicEditor
import com.eimsound.daw.commons.ExperimentalEIMApi
import com.eimsound.daw.components.*
import com.eimsound.daw.components.silder.Slider
import com.eimsound.daw.components.utils.onRightClickOrLongPress
import com.eimsound.daw.utils.range
import java.util.UUID

@ExperimentalEIMApi
var parameterControllerCreateClipHandler: (AudioProcessorParameter, UUID?) -> Unit = { _, _ -> }

private var copiedValue = 0F
@OptIn(ExperimentalEIMApi::class)
fun FloatingLayerProvider.openParameterControllerMenu(
    pos: Offset, p: AudioProcessorParameter, uuid: UUID? = null, clipboardManager: ClipboardManager? = null
) {
    openEditorMenu(pos, object : BasicEditor {
        override fun copy() {
            copiedValue = p.value
            clipboardManager?.setText(AnnotatedString(p.value.toString()))
        }

        override fun paste() {
            p.value = if (clipboardManager == null) copiedValue
                else clipboardManager.getText()?.text?.toFloatOrNull() ?: return
        }

        override val canPaste
            get() = clipboardManager == null || clipboardManager.getText()?.text?.toFloatOrNull() != null

        override val canDelete = false
        override val pasteValue
            get() = clipboardManager?.getText()?.text?.toFloatOrNull()?.toString()
    }, false, { close ->
        CommandMenuItem({
            close()
            parameterControllerCreateClipHandler(p, uuid)
        }, enabled = EchoInMirror.selectedTrack != null) {
            Text("创建包络剪辑")
        }
    }) { close ->
        Row(
            Modifier.padding(12.dp).fillMaxWidth().heightIn(32.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(p.name + ":", Modifier.padding(4.dp, end = 8.dp),
                style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Visible,
//                textDecoration = if (enable) null else TextDecoration.LineThrough,
//                color = LocalContentColor.current.copy(alpha = if (enable) 1F else 0.7F),
                fontWeight = FontWeight.ExtraBold
            )
            CustomTextField(
                p.value.toString(),
                { p.value = it.toFloatOrNull() ?: return@CustomTextField },
                singleLine = true,
                maxLines = 1,
            )
        }
        Divider()
        CommandMenuItem({
            close()
            p.doChangeAction(p.initialValue)
        }) {
            Text("重置")
        }
    }
}

@Composable
fun ParameterControllerComponent(p: AudioProcessorParameter, uuid: UUID? = null) {
    val floatingLayerProvider = LocalFloatingLayerProvider.current
    val clipboardManager = LocalClipboardManager.current
    val audioProcessorParameterValue by rememberUpdatedState(p)
    val uuidValue by rememberUpdatedState(uuid)
    Column(
        Modifier.width(80.dp).padding(vertical = 4.dp).onRightClickOrLongPress { it, _ ->
            floatingLayerProvider.openParameterControllerMenu(it, audioProcessorParameterValue, uuidValue, clipboardManager)
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
