package cn.apisium.eim.impl

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import cn.apisium.eim.api.UndoManager
import cn.apisium.eim.api.UndoableAction
import cn.apisium.eim.utils.ManualState

class UndoManagerImpl: UndoManager, ManualState() {
    override val actions = ArrayDeque<UndoableAction>()
    override val limit = 100
    override var cursor by mutableStateOf(0)

    override suspend fun undo(steps: Int): Boolean {
        if (steps <= 0) return true
        for (i in 0 until steps.coerceAtMost(cursor)) {
            if (!actions[cursor - 1].undo()) return false
            cursor--
        }
        update()
        return true
    }

    override suspend fun redo(steps: Int): Boolean {
        if (steps <= 0) return true
        for (i in 0 until steps.coerceAtMost(actions.size - cursor)) {
            if (!actions[cursor].execute()) return false
            cursor++
        }
        update()
        return true
    }

    override suspend fun execute(action: UndoableAction): Boolean {
        if (cursor < actions.size) {
            actions.subList(cursor, actions.size).clear()
        }
        if (actions.isNotEmpty()) {
            val last = actions.last()
            val merged = last.merge(action)
            if (merged != null) {
                if (!merged.execute()) return false
                actions[cursor - 1] = merged
                update()
                return true
            }
        }
        if (!action.execute()) return false
        actions.add(action)
        if (actions.size > limit) {
            actions.removeFirst()
        } else {
            cursor++
        }
        return true
    }

    override fun clear() {
        actions.clear()
        cursor = 0
        update()
    }
}