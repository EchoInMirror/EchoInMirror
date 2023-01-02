package cn.apisium.eim.actions

import androidx.compose.ui.graphics.vector.ImageVector
import cn.apisium.eim.api.UndoableAction

class AddOrRemoveTrackAction: UndoableAction {
    override suspend fun undo(): Boolean {
        TODO("Not yet implemented")
    }

    override suspend fun execute(): Boolean {
        TODO("Not yet implemented")
    }

    override val name = "添加或删除轨道"
    override val icon: ImageVector
        get() = TODO("Not yet implemented")
}