package cn.apisium.eim.window

import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import cn.apisium.eim.api.window.Panel
import cn.apisium.eim.api.window.PanelDirection
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SettingsBackupRestore

import cn.apisium.eim.components.UndoList

object UndoListPanel:Panel{
    override val name = "历史操作"
    override val direction = PanelDirection.Vertical
    @Composable
    override fun icon() {
        Icon(Icons.Default.SettingsBackupRestore, "undo")
    }

    @Composable
    override fun content() {
        UndoList()
    }
}