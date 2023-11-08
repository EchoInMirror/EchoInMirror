package com.eimsound.daw.components.menus

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Piano
import androidx.compose.material.icons.filled.SettingsInputHdmi
import androidx.compose.material3.*
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.dp
import com.eimsound.audioprocessor.AudioProcessorManager
import com.eimsound.daw.actions.doAddOrRemoveAudioProcessorAction
import com.eimsound.daw.api.processor.TrackAudioProcessorWrapper
import com.eimsound.daw.components.*
import com.eimsound.daw.commons.BasicEditor
import com.eimsound.daw.utils.createTempDirectory
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.*
import java.nio.file.Files
import java.nio.file.Path

private var copiedAudioProcessorPath: Path? = null

private val logger = KotlinLogging.logger { }
@OptIn(DelicateCoroutinesApi::class)
fun FloatingLayerProvider.openAudioProcessorMenu(
    pos: Offset, p: TrackAudioProcessorWrapper, list: MutableList<TrackAudioProcessorWrapper>,
    index: Int = -1, snackbarProvider: SnackbarProvider? = null, isLoading: MutableState<Boolean>? = null
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
                    p.store(dir)
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
    }, false) {
        MenuHeader(p.name, !p.isBypassed,
            if (p.description.isInstrument) Icons.Default.Piano else Icons.Default.SettingsInputHdmi
        ) {
            CustomCheckbox(!p.isBypassed, { p.isBypassed = !it }, Modifier.padding(start = 8.dp))
        }
        Divider()
    }
}
