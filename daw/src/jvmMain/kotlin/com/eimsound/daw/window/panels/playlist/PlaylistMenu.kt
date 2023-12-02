package com.eimsound.daw.window.panels.playlist

import androidx.compose.material3.Text
import androidx.compose.ui.geometry.Offset
import com.eimsound.daw.api.TrackClip
import com.eimsound.daw.components.CommandMenuItem
import com.eimsound.daw.components.FloatingLayerProvider
import com.eimsound.daw.components.openEditorMenu

internal fun FloatingLayerProvider.openPlaylistMenu(pos: Offset, clips: List<TrackClip<*>>, playlist: Playlist) {
    openEditorMenu(pos, playlist, footer = { close ->
        val canBeMergedSelectedClips = playlist.canBeMergedSelectedClips
        CommandMenuItem({
            close()
            playlist.mergeSelectedClips()
        }, enabled = canBeMergedSelectedClips > 1) {
            Text("合并片段${if (canBeMergedSelectedClips > 1) " ($canBeMergedSelectedClips)" else ""}")
        }
        ((clips.firstOrNull()?.clip?.factory ?: return@openEditorMenu))
            .MenuContent(clips, close)
    })
}