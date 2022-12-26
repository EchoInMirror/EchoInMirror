package cn.apisium.eim.api

interface UndoableAction {
    suspend fun undo(): Boolean
    suspend fun execute(): Boolean
    val name: String
    fun merge(other: UndoableAction): UndoableAction? = null
}

interface UndoManager {
    val actions: List<UndoableAction>
    val limit: Int
    val cursor: Int
    suspend fun undo(steps: Int = 1): Boolean
    suspend fun redo(steps: Int = 1): Boolean
    suspend fun execute(action: UndoableAction): Boolean
    fun clear()
}
