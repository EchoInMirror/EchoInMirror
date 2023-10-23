package com.eimsound.daw.components.splitpane

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.Density

open class SplitPaneState(
    private var initialPositionPercentage: Float = -1F,
    moveEnabled: Boolean = true,
) {
    var moveEnabled by mutableStateOf(moveEnabled)

    var position by mutableStateOf(0F)

    var minPosition: Float = 0f

    var maxPosition: Float = Float.POSITIVE_INFINITY

    open fun dispatchRawMovement(delta: Float, density: Density) {
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