package com.eimsound.daw.actions

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Tune
import com.eimsound.audioprocessor.data.EnvelopePoint
import com.eimsound.audioprocessor.data.EnvelopePointList
import com.eimsound.audioprocessor.data.EnvelopeType
import com.eimsound.daw.api.EchoInMirror
import com.eimsound.daw.components.EnvelopeEditor
import com.eimsound.daw.components.EnvelopeEditorEventHandler
import com.eimsound.daw.components.icons.PencilMinus
import com.eimsound.daw.components.icons.PencilPlus
import com.eimsound.daw.utils.*
import kotlinx.coroutines.runBlocking

fun EnvelopePointList.doEnvelopePointsAmountAction(points: Collection<EnvelopePoint>, isDelete: Boolean = false) {
    runBlocking { EchoInMirror.undoManager.execute(EnvelopePointsAmountAction(this@doEnvelopePointsAmountAction,
        points.toList(), isDelete)) }
}

class EnvelopePointsAmountAction(private val list: EnvelopePointList, private val points: Collection<EnvelopePoint>, isDelete: Boolean) :
    ReversibleAction(isDelete) {
    override val name = (if (isDelete) "包络节点删除 (" else "包络节点添加 (") + points.size + "个)"
    override val icon = if (isDelete) PencilMinus else PencilPlus
    override suspend fun perform(isForward: Boolean): Boolean {
        if (isForward) {
            list.addAll(points)
            list.sort()
        } else list.removeAll(points)
        list.update()
        return true
    }
}

fun EnvelopePointList.doEnvelopePointsEditAction(points: List<EnvelopePoint>, deltaX: Int, deltaY: Float, valueRange: IntRange? = null) {
    if (deltaX == 0 && deltaY == 0F) return
    runBlocking {
        EchoInMirror.undoManager.execute(
            EnvelopePointsEditAction(this@doEnvelopePointsEditAction, points.toList(), deltaX, deltaY, valueRange)
        )
    }
}

class EnvelopePointsEditAction(
    private val list: EnvelopePointList, private val points: Collection<EnvelopePoint>,
    private val deltaX: Int, private val deltaY: Float, private val valueRange: IntRange? = null
) : UndoableAction {
    private val oldValues = points.map { it.value }
    override val name = "包络节点编辑 (${points.size}个)"
    override val icon = Icons.Default.Edit

    override suspend fun undo(): Boolean {
        points.forEachIndexed { index, noteMessage ->
            noteMessage.time -= deltaX
            noteMessage.value = oldValues[index]
        }
        if (deltaX != 0) list.sort()
        list.update()
        return true
    }

    override suspend fun execute(): Boolean {
        points.forEach {
            it.time += deltaX
            if (valueRange == null) it.value += deltaY
            else it.value = (it.value + deltaY).coerceIn(valueRange)
        }
        if (deltaX != 0) list.sort()
        list.update()
        return true
    }
}

fun EnvelopePointList.doEnvelopePointsTensionAction(points: Collection<EnvelopePoint>, deltaTension: Float) {
    if (deltaTension == 0F) return
    runBlocking {
        EchoInMirror.undoManager.execute(
            EnvelopePointsTensionAction(this@doEnvelopePointsTensionAction, points.toList(), deltaTension)
        )
    }
}

class EnvelopePointsTensionAction(
    private val list: EnvelopePointList, private val points: Collection<EnvelopePoint>,
    private val deltaTension: Float
) : UndoableAction {
    private val oldTensions = points.map { it.tension }
    override val name = "包络节点张力编辑 (${points.size}个)"
    override val icon = Icons.Default.Tune
    override suspend fun undo(): Boolean {
        points.forEachIndexed { index, p -> p.tension = oldTensions[index] }
        list.update()
        return true
    }

    override suspend fun execute(): Boolean {
        points.forEach { it.tension += deltaTension }
        list.update()
        return true
    }
}

fun EnvelopePointList.doEnvelopePointsTypeAction(points: Collection<EnvelopePoint>, type: EnvelopeType) {
    runBlocking {
        EchoInMirror.undoManager.execute(
            EnvelopePointsTypeAction(this@doEnvelopePointsTypeAction, points.toList(), type)
        )
    }
}

class EnvelopePointsTypeAction(
    private val list: EnvelopePointList, private val points: Collection<EnvelopePoint>,
    private val type: EnvelopeType
) : UndoableAction {
    private val oldTypes = points.map { it.type }
    override val name = "包络节点类型编辑 (${points.size}个)"
    override val icon = Icons.Default.Tune
    override suspend fun undo(): Boolean {
        points.forEachIndexed { index, p -> p.type = oldTypes[index] }
        list.update()
        return true
    }

    override suspend fun execute(): Boolean {
        points.forEach { it.type = type }
        list.update()
        return true
    }
}

object GlobalEnvelopeEditorEventHandler : EnvelopeEditorEventHandler {
    override fun onAddPoints(editor: EnvelopeEditor, points: List<EnvelopePoint>) {
        editor.points.doEnvelopePointsAmountAction(points)
    }

    override fun onPastePoints(editor: EnvelopeEditor, points: List<EnvelopePoint>): List<EnvelopePoint> {
        val startTime = EchoInMirror.currentPosition.timeInPPQ.fitInUnitCeil(EchoInMirror.editUnit)
        val notes = points.map { it.copy(it.time + startTime, it.value * editor.valueRange.range + editor.valueRange.first) }
        editor.points.doEnvelopePointsAmountAction(notes)
        return notes
    }

    override fun onRemovePoints(editor: EnvelopeEditor, points: List<EnvelopePoint>) {
        editor.points.doEnvelopePointsAmountAction(points, true)
    }

    override fun onMovePoints(editor: EnvelopeEditor, points: List<EnvelopePoint>, offsetTime: Int, offsetValue: Float) {
        editor.points.doEnvelopePointsEditAction(points, offsetTime, offsetValue, editor.valueRange)
    }

    override fun onTensionChanged(editor: EnvelopeEditor, points: List<EnvelopePoint>, tension: Float) {
        editor.points.doEnvelopePointsTensionAction(points, tension)
    }

    override fun onTypeChanged(editor: EnvelopeEditor, points: List<EnvelopePoint>, type: EnvelopeType) {
        editor.points.doEnvelopePointsTypeAction(points, type)
    }
}
