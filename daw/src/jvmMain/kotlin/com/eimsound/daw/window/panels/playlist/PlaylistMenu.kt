package com.eimsound.daw.window.panels.playlist

import androidx.compose.material3.*
import androidx.compose.ui.geometry.Offset
import com.eimsound.daw.api.clips.TrackClip
import com.eimsound.daw.components.CommandMenuItem
import com.eimsound.daw.components.FloatingLayerProvider
import com.eimsound.daw.components.MenuHeader
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
    }) { close ->
        val clip = clips.firstOrNull() ?: return@openEditorMenu
        val color = clip.color ?: clip.track?.color ?: MaterialTheme.colorScheme.primary
        MenuHeader(clip.clip.name, !clip.isDisabled, clip.clip.icon, color) {
            clip.clip.factory.MenuContent(clips, close)
        }
    }
}