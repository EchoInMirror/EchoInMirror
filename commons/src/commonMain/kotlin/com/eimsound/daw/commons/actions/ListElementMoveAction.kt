package com.eimsound.daw.commons.actions

abstract class ListElementMoveAction<T>(
    private val index: Int, private val from: MutableList<T>,
    private val to: MutableList<T>, private var toIndex: Int = -1
): UndoableAction {
    override suspend fun execute(): Boolean {
        if (index < 0 || index >= from.size) return false
        val elm = from.removeAt(index)
        if (toIndex == -1) {
            to.add(elm)
            toIndex = to.size - 1
        } else to.add(toIndex, elm)
        return true
    }

    override suspend fun undo(): Boolean {
        if (toIndex < 0 || toIndex >= to.size || index < 0 || index >= from.size) return false
        val elm = to.removeAt(toIndex)
        from.add(index, elm)
        return true
    }
}
