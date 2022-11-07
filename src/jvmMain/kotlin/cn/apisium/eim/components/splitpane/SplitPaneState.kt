package cn.apisium.eim.components.splitpane

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

open class SplitPaneState(
    initialPositionPercentage: Float,
    moveEnabled: Boolean,
) {

    var moveEnabled by mutableStateOf(moveEnabled)
        internal set

    var position by mutableStateOf(initialPositionPercentage)
        internal set

    internal var minPosition: Float = 0f

    internal var maxPosition: Float = Float.POSITIVE_INFINITY

    open fun dispatchRawMovement(delta: Float) {
        val movableArea = maxPosition - minPosition
        if (movableArea > 0) {
            position = (position + delta).coerceIn(minPosition, maxPosition)
        }
    }

}