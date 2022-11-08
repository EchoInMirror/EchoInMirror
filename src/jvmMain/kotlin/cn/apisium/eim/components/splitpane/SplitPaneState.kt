package cn.apisium.eim.components.splitpane

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

open class SplitPaneState(
    private var initialPositionPercentage: Float = -1F,
    moveEnabled: Boolean = true,
) {
    var moveEnabled by mutableStateOf(moveEnabled)

    var position by mutableStateOf(0F)

    internal var minPosition: Float = 0f

    internal var maxPosition: Float = Float.POSITIVE_INFINITY

    open fun dispatchRawMovement(delta: Float) {
        val movableArea = maxPosition - minPosition
        if (movableArea > 0) {
            position = (position + delta).coerceIn(minPosition, maxPosition)
        }
    }

    open fun calcPosition(constraint: Float): Float {
        if (initialPositionPercentage != -1F) {
            position = initialPositionPercentage * constraint
            initialPositionPercentage = -1F
        }
        return position
    }
}