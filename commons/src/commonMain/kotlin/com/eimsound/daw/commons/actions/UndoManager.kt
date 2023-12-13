package com.eimsound.daw.commons.actions

import androidx.compose.ui.graphics.vector.ImageVector
import java.nio.file.Path

interface UndoableAction {
    suspend fun undo(): Boolean
    suspend fun execute(): Boolean
    val name: String
    val icon: ImageVector
    fun merge(other: UndoableAction): UndoableAction? = null
}

interface Restorable {
    suspend fun restore(path: Path)
    suspend fun store(path: Path)
}

class UndoableActionExecuteException(message: String, cause: Throwable? = null) : Exception(message, cause)

/**
 * @see com.eimsound.daw.impl.DefaultUndoManager
 */
interface UndoManager {
    val actions: List<UndoableAction>
    val limit: Int
    val cursor: Int
    val errorHandlers: MutableSet<(UndoableActionExecuteException) -> Unit>
    val cursorChangeHandlers: MutableSet<() -> Unit>
    val canUndo: Boolean
    val canRedo: Boolean
    suspend fun undo(steps: Int = 1): Boolean
    suspend fun redo(steps: Int = 1): Boolean
    suspend fun execute(action: UndoableAction): Boolean
    suspend fun reset(): Boolean
    fun clear()
}
