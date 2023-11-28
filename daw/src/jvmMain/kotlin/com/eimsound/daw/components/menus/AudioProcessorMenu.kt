package com.eimsound.daw.components.menus

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Piano
import androidx.compose.material.icons.filled.Reply
import androidx.compose.material.icons.filled.SettingsInputHdmi
import androidx.compose.material3.*
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import com.eimsound.audioprocessor.AudioProcessorManager
import com.eimsound.audioprocessor.createAudioProcessorOrNull
import com.eimsound.daw.actions.doAddOrRemoveAudioProcessorAction
import com.eimsound.daw.actions.doReplaceAudioProcessorAction
import com.eimsound.daw.api.EchoInMirror
import com.eimsound.daw.api.processor.TrackAudioProcessorWrapper
import com.eimsound.daw.components.*
import com.eimsound.daw.commons.BasicEditor
import com.eimsound.daw.utils.createTempDirectory
import com.eimsound.daw.window.dialogs.openQuickLoadDialog
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.*
import java.nio.file.Files
import java.nio.file.Path
import kotlin.math.roundToInt

private var copiedAudioProcessorPath: Path? = null

private val logger = KotlinLogging.logger { }
@OptIn(DelicateCoroutinesApi::class)
fun FloatingLayerProvider.openAudioProcessorMenu(
    pos: Offset, p: TrackAudioProcessorWrapper, list: MutableList<TrackAudioProcessorWrapper>,
    index: Int = -1, showExtra: Boolean = false, snackbarProvider: SnackbarProvider? = null,
    isLoading: MutableState<Boolean>? = null
) {
    openEditorMenu(pos, object : BasicEditor {
        override fun delete() {
            list.doAddOrRemoveAudioProcessorAction(p, true)
        }

        override fun copy() {
            copiedAudioProcessorPath = null
            GlobalScope.launch(Dispatchers.IO) {
                val dir = createTempDirectory("copy")
                Files.createDirectories(dir)
                try {
                    p.processor.store(dir)
                    copiedAudioProcessorPath = dir
                } catch (e: Exception) {
                    logger.error(e) { "Failed to copy audio processor: $p" }
                }
            }
        }

        override fun paste() {
            val dir = copiedAudioProcessorPath
            if (isLoading?.value == true || dir == null || !Files.exists(dir)) return
            isLoading?.value = true
            delete()
            GlobalScope.launch {
                try {
                    val ap = AudioProcessorManager.instance.createAudioProcessor(dir)
                    list.doAddOrRemoveAudioProcessorAction(ap, false, index)
                } catch (e: Exception) {
                    logger.error(e) { "Failed to paste audio processor: $dir" }
                    snackbarProvider?.enqueueSnackbar(e)
                    return@launch
                }
            }.invokeOnCompletion { isLoading?.value = false }
        }

        override val canPaste get() = copiedAudioProcessorPath != null
    }, false, { close ->
        CommandMenuItem({
            close()
            openQuickLoadDialog {
                if (it == null) return@openQuickLoadDialog
                isLoading?.value = true
                GlobalScope.launch {
                    val (ap, err) = it.factory.createAudioProcessorOrNull(it.description)
                    if (err != null) {
                        snackbarProvider?.enqueueSnackbar(err)
                        return@launch
                    }
                    if (ap == null) return@launch
                    list.doReplaceAudioProcessorAction(ap, index)
                }.invokeOnCompletion { isLoading?.value = false }
            }
        }, Icons.Default.Reply) {
            Text("替换...", fontStyle = FontStyle.Italic)
        }
        if (showExtra) {
            Divider()
            val latency = p.processor.latency
            Text(
                "延迟: $latency (${(latency * 1000F / EchoInMirror.currentPosition.sampleRate).roundToInt()}毫秒)",
                Modifier.padding(16.dp, 6.dp),
                style = MaterialTheme.typography.bodySmall,
                color = LocalContentColor.current.copy(0.4F)
            )
        }
    }) {
        MenuHeader(
            p.name, !p.processor.isDisabled,
            if (p.processor.description.isInstrument) Icons.Default.Piano else Icons.Default.SettingsInputHdmi,
            { p.name = it }
        ) {
            CustomCheckbox(!p.processor.isDisabled, { p.processor.isDisabled = !it }, Modifier.padding(start = 8.dp))
        }
        Divider()
    }
}
