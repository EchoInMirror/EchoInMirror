package com.eimsound.daw.commons.actions

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.nio.file.Files
import java.nio.file.Path

internal fun createTempDirectory(): Path = Files.createTempDirectory(
    System.getProperty("eim.tempfiles.prefix", "EchoInMirror") + "-undo"
).apply { Files.createDirectories(this) }

abstract class ListReplaceAction<T>(private val target: T, private val list: MutableList<T>,
                                    private val index: Int): UndoableAction {
    private var old: T? = null
    private var isReplaced = false
    private var restorablePath: Path? = null

    override suspend fun execute(): Boolean {
        old = list.getOrNull(index)?.apply { store(this) }
        list[index] = target
        if (isReplaced) {
            target.let { t ->
                if (t is Restorable) restorablePath?.let { t.restore(it) }
            }
        } else isReplaced = true
        return true
    }

    override suspend fun undo(): Boolean {
        if (list.getOrNull(index) != target) return false
        val o = old ?: return false
        store(target)
        list[index] = o
        if (o is Restorable) restorablePath?.let { o.restore(it) }
        return true
    }

    private suspend fun store(it: T) {
        if (it is Restorable) withContext(Dispatchers.IO) {
            if (restorablePath == null) restorablePath = createTempDirectory()
            it.store(restorablePath!!)
        }
        if (it is AutoCloseable) it.close()
    }
}
