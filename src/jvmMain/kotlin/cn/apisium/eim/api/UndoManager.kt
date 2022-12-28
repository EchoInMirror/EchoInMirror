package cn.apisium.eim.api
import androidx.compose.ui.graphics.vector.ImageVector

interface UndoableAction {
    suspend fun undo(): Boolean
    suspend fun execute(): Boolean
    val name: String
    val icon: ImageVector
    fun merge(other: UndoableAction): UndoableAction? = null
}

abstract class ReversibleAction(private val reversed: Boolean = false) : UndoableAction {
    override suspend fun undo() = perform(reversed)
    override suspend fun execute() = perform(!reversed)
    protected abstract suspend fun perform(isForward: Boolean): Boolean
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
