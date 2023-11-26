package com.eimsound.daw.window.panels.playlist

import androidx.compose.ui.geometry.Offset
import com.eimsound.daw.api.TrackClip
import com.eimsound.daw.components.FloatingLayerProvider
import com.eimsound.daw.components.openEditorMenu

internal fun FloatingLayerProvider.openPlaylistMenu(pos: Offset, clips: List<TrackClip<*>>, playlist: Playlist) {
    openEditorMenu(pos, playlist, footer = { close ->
        ((clips.firstOrNull()?.clip?.factory ?: return@openEditorMenu))
            .MenuContent(clips, close)
    })
}