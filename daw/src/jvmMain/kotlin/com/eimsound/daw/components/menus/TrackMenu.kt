package com.eimsound.daw.components.menus

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material3.Divider
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.dp
import com.eimsound.daw.actions.doAddOrRemoveTrackAction
import com.eimsound.daw.api.EchoInMirror
import com.eimsound.daw.api.processor.Bus
import com.eimsound.daw.api.processor.Track
import com.eimsound.daw.api.processor.TrackManager
import com.eimsound.daw.commons.BasicEditor
import com.eimsound.daw.components.*
import com.eimsound.daw.utils.createTempDirectory
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.nio.file.Files
import java.nio.file.Path
import kotlin.math.roundToInt

private var copiedTrackPath: Path? = null

private val logger = KotlinLogging.logger { }
@OptIn(DelicateCoroutinesApi::class)
fun FloatingLayerProvider.openTrackMenu(
    pos: Offset, track: Track, list: MutableList<Track>?, index: Int,
    showExtra: Boolean = false, snackbarProvider: SnackbarProvider? = null
) {
    openEditorMenu(pos, object : BasicEditor {
        override fun delete() {
            list?.doAddOrRemoveTrackAction(track, true)
        }

        override fun copy() {
            copiedTrackPath = null
            GlobalScope.launch(Dispatchers.IO) {
                val dir = createTempDirectory("copy")
                Files.createDirectories(dir)
                try {
                    track.store(dir)
                    copiedTrackPath = dir
                } catch (e: Exception) {
                    logger.error(e) { "Failed to copy audio processor: $track" }
                }
            }
        }

        override fun paste() {
            val dir = copiedTrackPath
            if (list == null || dir == null || !Files.exists(dir)) return
            delete()
            GlobalScope.launch {
                try {
                    val tr = TrackManager.instance.createTrack(dir)
                    list.doAddOrRemoveTrackAction(tr, false, index)
                } catch (e: Exception) {
                    logger.error(e) { "Failed to paste audio processor: $dir" }
                    snackbarProvider?.enqueueSnackbar(e)
                    return@launch
                }
            }
        }

        override val canPaste get() = copiedTrackPath != null
        override val hasSelected get() = list != null && track !is Bus
    }, false, {
        if (showExtra) {
            Divider()
            val latency = track.latency
            Text(
                "延迟: $latency (${(latency * 1000F / EchoInMirror.currentPosition.sampleRate).roundToInt()}毫秒)",
                Modifier.padding(16.dp, 6.dp),
                style = MaterialTheme.typography.bodySmall,
                color = LocalContentColor.current.copy(0.4F)
            )
        }
    }) {
        MenuHeader(
            track.name, !track.isDisabled, Icons.Default.ViewList, track.color, { track.name = it },
            onColorChange = { track.color = it },
            titleContent = { CustomCheckbox(!track.isDisabled, { track.isDisabled = !it }, Modifier) }
        )
    }
}
