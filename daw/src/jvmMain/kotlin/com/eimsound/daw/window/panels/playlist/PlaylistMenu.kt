package com.eimsound.daw.window.panels.playlist

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.eimsound.daw.api.clips.TrackClip
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
    }) { close ->
        val clip = clips.firstOrNull() ?: return@openEditorMenu
        val color = clip.color ?: clip.track?.color ?: MaterialTheme.colorScheme.primary
        Surface(
            Modifier.fillMaxWidth().drawBehind {
                drawRect(color, Offset.Zero, Size(8 * density, size.height))
            }, color = color.copy(0.1F)
        ) {
            Column(Modifier.padding(start = 8.dp)) {
                Box(Modifier.fillMaxWidth().background(color.copy(0.3F))) {
                    Row(Modifier.padding(8.dp, 5.dp), verticalAlignment = Alignment.CenterVertically) {
                        clip.clip.icon?.let {
                            Icon(it, it.name, Modifier.size(20.dp).padding(end = 6.dp))
                        }
                        Text(
                            clip.clip.name,
                            style = MaterialTheme.typography.labelLarge,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                clip.clip.factory.MenuContent(clips, close)
            }
        }
        Divider()
    }
}