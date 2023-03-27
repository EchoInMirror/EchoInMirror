package com.eimsound.daw.utils

import androidx.compose.ui.graphics.vector.ImageVector

interface UndoableAction {
    suspend fun undo(): Boolean
    suspend fun execute(): Boolean
    val name: String
    val icon: ImageVector
    fun merge(other: UndoableAction): UndoableAction? = null
}

interface Recoverable {
    fun recover(path: String)
}

abstract class ReversibleAction(private val reversed: Boolean = false) : UndoableAction {
    override suspend fun undo() = perform(reversed)
    override suspend fun execute() = perform(!reversed)
    protected abstract suspend fun perform(isForward: Boolean): Boolean
}

abstract class ListAddOrRemoveAction<T>(private val target: T, private val list: MutableList<T>,
                             private val isDelete: Boolean, private var index: Int = -1): ReversibleAction(isDelete) {
    override suspend fun perform(isForward: Boolean): Boolean {
        if (isForward) {
            if (isDelete) {
                index = list.indexOf(target)
                list.remove(target)
                if (target is AutoCloseable) target.close()
            } else if (index == -1) list.add(target) else list.add(index, target)
        } else {
            if (isDelete) {
                list.add(index, target)
                if (target is Recoverable) target.recover("") // TODO: recover path
            } else {
                list.remove(target)
                if (target is AutoCloseable) target.close()
            }
        }
        return true
    }
}

abstract class ListReplaceAction<T>(private val target: T, private val list: MutableList<T>,
                                        private val index: Int): UndoableAction {
    private var old: T? = null
    private var isReplaced = false

    override suspend fun execute(): Boolean {
        old = list.getOrNull(index)?.apply { if (this is AutoCloseable) this.close() }
        list[index] = target
        if (isReplaced) {
            target.let { if (it is Recoverable) it.recover("") } // TODO: recover path
        } else isReplaced = true
        return true
    }

    override suspend fun undo(): Boolean {
        if (list.getOrNull(index) != target) return false
        val o = old ?: return false
        target.let { if (it is AutoCloseable) it.close() }
        list[index] = o
        if (o is Recoverable) o.recover("") // TODO: recover path
        return true
    }
}

class UndoableActionExecuteException(message: String, cause: Throwable? = null) : Exception(message, cause)

interface UndoManager {
    val actions: List<UndoableAction>
    val limit: Int
    val cursor: Int
    val errorHandlers: MutableSet<(UndoableActionExecuteException) -> Unit>
    suspend fun undo(steps: Int = 1): Boolean
    suspend fun redo(steps: Int = 1): Boolean
    suspend fun execute(action: UndoableAction): Boolean
    suspend fun reset(): Boolean
    fun clear()
}
