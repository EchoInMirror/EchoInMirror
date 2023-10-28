package com.eimsound.daw.components.menus

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Divider
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.eimsound.audioprocessor.AudioProcessorManager
import com.eimsound.daw.actions.doAddOrRemoveAudioProcessorAction
import com.eimsound.daw.api.processor.TrackAudioProcessorWrapper
import com.eimsound.daw.components.CustomCheckbox
import com.eimsound.daw.components.FloatingLayerProvider
import com.eimsound.daw.components.SnackbarProvider
import com.eimsound.daw.components.openEditorMenu
import com.eimsound.daw.utils.BasicEditor
import com.eimsound.daw.utils.createTempDirectory
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.*
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.absolutePathString

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
                    p.store(dir.absolutePathString())
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
                    val ap = AudioProcessorManager.instance.createAudioProcessor(dir.absolutePathString())
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
        Row(Modifier.padding(start = 12.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(p.name, Modifier.weight(1F), style = MaterialTheme.typography.titleSmall,
                maxLines = 1, overflow = TextOverflow.Ellipsis,
                textDecoration = if (p.isBypassed) TextDecoration.LineThrough else TextDecoration.None,
                color = LocalContentColor.current.copy(alpha = if (p.isBypassed) 0.7F else 1F))
            CustomCheckbox(!p.isBypassed, { p.isBypassed = !it }, Modifier.padding(start = 8.dp))
        }
        Divider()
    }
}