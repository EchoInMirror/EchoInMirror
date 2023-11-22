package com.eimsound.daw.commons.actions

abstract class ReversibleAction(private val reversed: Boolean = false) : UndoableAction {
    override suspend fun undo() = perform(reversed)
    override suspend fun execute() = perform(!reversed)
    protected abstract suspend fun perform(isForward: Boolean): Boolean
}
