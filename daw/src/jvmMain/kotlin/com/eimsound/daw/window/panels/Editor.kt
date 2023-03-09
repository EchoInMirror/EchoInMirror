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
import com.eimsound.daw.api.EchoInMirror
import com.eimsound.daw.api.ClipEditor
import com.eimsound.daw.api.window.Panel
import com.eimsound.daw.api.window.PanelDirection
import com.eimsound.daw.commands.*
import com.eimsound.daw.components.Gap
import com.eimsound.daw.utils.BasicEditor

object Editor: Panel, BasicEditor {
    override val name = "编辑器"
    override val direction = PanelDirection.Horizontal
    private var editor: ClipEditor? = null

    @Composable
    override fun Icon() {
        Icon(Icons.Default.Piano, name)
    }

    @OptIn(ExperimentalComposeUiApi::class)
    @Composable
    override fun Content() {
        Box(Modifier.fillMaxSize().onPointerEvent(PointerEventType.Press) {
            EchoInMirror.windowManager.activePanel = Editor
        }) {
            val clip = EchoInMirror.selectedClip
            val track = EchoInMirror.selectedTrack
            val editor = remember(track, clip) {
                @Suppress("TYPE_MISMATCH")
                val res = if (clip != null && track != null) clip.clip.factory.getEditor(clip, track)
                else null
                editor = res
                res
            }
            if (editor == null) Row(Modifier.fillMaxSize(), Arrangement.Center, Alignment.CenterVertically) {
                Icon(Icons.Outlined.Report, null, Modifier.size(22.dp))
                Gap(4)
                Text("请先选择一个 Clip.", fontWeight = FontWeight.Bold)
            } else editor.Editor()
        }
    }

    override fun copy() { editor?.copy() }
    override fun paste() { editor?.paste() }
    override fun cut() { editor?.cut() }
    override fun delete() { editor?.delete() }
    override fun selectAll() { editor?.selectAll() }
}
