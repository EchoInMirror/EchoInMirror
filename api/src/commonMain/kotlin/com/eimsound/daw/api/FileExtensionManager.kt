package com.eimsound.daw.api

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import com.eimsound.daw.api.clips.Clip
import java.nio.file.Path
import java.util.*
import kotlin.io.path.name

/**
 * @see com.eimsound.daw.impl.clips.audio.AudioFileExtensionHandler
 * @see com.eimsound.daw.impl.clips.midi.MidiFileExtensionHandler
 */
interface FileExtensionHandler {
    val icon: ImageVector
    val extensions: Regex
    val isCustomFileBrowserNode: Boolean

    suspend fun createClip(file: Path, data: Any?): Clip
    @Composable
    fun FileBrowserNode(file: Path, depth: Int)
}

abstract class AbstractFileExtensionHandler : FileExtensionHandler {
    override val isCustomFileBrowserNode = false
    @Composable
    override fun FileBrowserNode(file: Path, depth: Int) { }

    override fun toString() = "${this::class.simpleName}(${extensions.pattern})"
}

object FileExtensionManager {
    val handlers by lazy { ServiceLoader.load(FileExtensionHandler::class.java).toList() }

    fun getHandler(file: Path): FileExtensionHandler? {
        val name = file.name
        return handlers.firstOrNull { it.extensions.containsMatchIn(name) }
    }
}
