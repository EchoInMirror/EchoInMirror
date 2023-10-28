package com.eimsound.daw.components.menus

import androidx.compose.ui.geometry.Offset
import com.eimsound.daw.api.processor.Track
import com.eimsound.daw.components.FloatingLayerProvider
import com.eimsound.daw.components.SnackbarProvider
import com.eimsound.daw.components.openEditorMenu
import com.eimsound.daw.utils.BasicEditor

fun FloatingLayerProvider.openAudioProcessorMenu(
    pos: Offset, track: Track, snackbarProvider: SnackbarProvider? = null
) {
    openEditorMenu(pos, object : BasicEditor {

    }, false) {

    }
}