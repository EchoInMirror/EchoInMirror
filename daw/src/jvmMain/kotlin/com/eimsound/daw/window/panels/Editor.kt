package com.eimsound.daw.window.panels

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Piano
import androidx.compose.material.icons.outlined.Report
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.eimsound.daw.EchoInMirror
import com.eimsound.daw.api.ClipEditor
import com.eimsound.daw.api.window.Panel
import com.eimsound.daw.api.window.PanelDirection
import com.eimsound.daw.commands.*
import com.eimsound.daw.components.Gap

object Editor: Panel {
    override val name = "编辑器"
    override val direction = PanelDirection.Horizontal
    private var editor: ClipEditor? = null

    init {
        EchoInMirror.commandManager.apply {
            registerCommandHandler(DeleteCommand) {
                if (EchoInMirror.windowManager.activePanel == Editor) editor?.delete()
            }
            registerCommandHandler(CopyCommand) {
                if (EchoInMirror.windowManager.activePanel == Editor) editor?.copy()
            }
            registerCommandHandler(CutCommand) {
                if (EchoInMirror.windowManager.activePanel == Editor) editor?.cut()
            }
            registerCommandHandler(PasteCommand) {
                if (EchoInMirror.windowManager.activePanel == Editor) editor?.paste()
            }
            registerCommandHandler(SelectAllCommand) {
                if (EchoInMirror.windowManager.activePanel == Editor) editor?.selectAll()
            }
        }
    }

    @Composable
    override fun icon() {
        Icon(Icons.Default.Piano, "Editor")
    }

    @OptIn(ExperimentalComposeUiApi::class)
    @Composable
    override fun content() {
        Box(Modifier.fillMaxSize().onPointerEvent(PointerEventType.Press) {
            EchoInMirror.windowManager.activePanel = Editor
        }) {
            val clip = EchoInMirror.selectedClip
            val track = EchoInMirror.selectedTrack
            val editor = remember(track, clip) {
                @Suppress("TYPE_MISMATCH")
                val res = if (clip != null && track != null) clip.clip.factory.getEditor(clip, track)
                else null
                this@Editor.editor = res
                res
            }
            if (editor == null) Row(Modifier.fillMaxSize(), Arrangement.Center, Alignment.CenterVertically) {
                Icon(Icons.Outlined.Report, null, Modifier.size(22.dp))
                Gap(4)
                Text("请先选择一个 Clip.", fontWeight = FontWeight.Bold)
            } else editor.Content()
        }
    }
}
