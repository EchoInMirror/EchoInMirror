package com.eimsound.daw.components.menus

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material3.Divider
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.dp
import com.eimsound.daw.api.processor.Track
import com.eimsound.daw.components.CustomCheckbox
import com.eimsound.daw.components.FloatingLayerProvider
import com.eimsound.daw.components.SnackbarProvider
import com.eimsound.daw.components.openEditorMenu
import com.eimsound.daw.utils.BasicEditor

fun FloatingLayerProvider.openTrackMenu(
    pos: Offset, track: Track, snackbarProvider: SnackbarProvider? = null
) {
    openEditorMenu(pos, object : BasicEditor {

    }, false) {
        MenuHeader(track.name, !track.isBypassed, Icons.Default.ViewList) {
            CustomCheckbox(!track.isBypassed, { track.isBypassed = !it }, Modifier.padding(start = 8.dp))
        }
        Divider()
    }
}