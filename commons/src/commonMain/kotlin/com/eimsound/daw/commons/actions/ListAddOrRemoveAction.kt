package com.eimsound.daw.commons.actions

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.nio.file.Path

abstract class ListAddOrRemoveAction<T>(
    private val target: T, private val list: MutableList<T>,
    private val isDelete: Boolean, private var index: Int = -1
): UndoableAction {
    private var restorablePath: Path? = null

    override suspend fun execute(): Boolean {
        if (isDelete) {
            index = list.indexOf(target)
            remove()
        } else {
            if (index == -1) {
                list.add(target)
                index = list.size - 1
            } else list.add(index, target)
            if (target is Restorable && restorablePath != null) target.restore(restorablePath!!)
        }
        return true
    }

    override suspend fun undo(): Boolean {
        if (isDelete) {
            list.add(index, target)
            if (target is Restorable && restorablePath != null) target.restore(restorablePath!!)
        } else remove()
        return true
    }

    private suspend fun remove() {
        list.remove(target)
        if (target is Restorable) withContext(Dispatchers.IO) {
            if (restorablePath == null) restorablePath = createTempDirectory()
            target.store(restorablePath!!)
        }
        if (target is AutoCloseable) target.close()
    }
}
