package com.eimsound.daw.components.menus

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material3.Divider
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.dp
import com.eimsound.daw.actions.doAddOrRemoveTrackAction
import com.eimsound.daw.api.processor.Bus
import com.eimsound.daw.api.processor.Track
import com.eimsound.daw.api.processor.TrackManager
import com.eimsound.daw.components.CustomCheckbox
import com.eimsound.daw.components.FloatingLayerProvider
import com.eimsound.daw.components.SnackbarProvider
import com.eimsound.daw.components.openEditorMenu
import com.eimsound.daw.utils.BasicEditor
import com.eimsound.daw.utils.createTempDirectory
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.nio.file.Files
import java.nio.file.Path

private var copiedTrackPath: Path? = null

private val logger = KotlinLogging.logger { }
@OptIn(DelicateCoroutinesApi::class)
fun FloatingLayerProvider.openTrackMenu(
    pos: Offset, track: Track, list: MutableList<Track>?, index: Int, snackbarProvider: SnackbarProvider? = null
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
//                .invokeOnCompletion { isLoading?.value = false }
        }

        override val canPaste get() = copiedTrackPath != null
        override val hasSelected get() = list != null && track !is Bus
    }, false) {
        MenuHeader(track.name, !track.isBypassed, Icons.Default.ViewList) {
            CustomCheckbox(!track.isBypassed, { track.isBypassed = !it }, Modifier.padding(start = 8.dp))
        }
        Divider()
    }
}
