package com.eimsound.daw.components

import androidx.compose.ui.geometry.Offset
import com.eimsound.daw.actions.doAddOrRemoveAudioProcessorAction
import com.eimsound.daw.api.processor.TrackAudioProcessorWrapper
import com.eimsound.daw.utils.BasicEditor

fun FloatingLayerProvider.openAudioProcessorMenu(pos: Offset, p: TrackAudioProcessorWrapper, list: MutableList<TrackAudioProcessorWrapper>) {
    openEditorMenu(pos, object : BasicEditor {
        override fun delete() {
            list.doAddOrRemoveAudioProcessorAction(p, true)
        }
    }, false)
}