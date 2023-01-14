package com.eimsound.daw.components.icons

import androidx.compose.material.icons.materialIcon
import androidx.compose.material.icons.materialPath
import androidx.compose.ui.graphics.vector.ImageVector

val PencilPlus: ImageVector
    get() {
        if (pencilPlus != null) return pencilPlus!!
        pencilPlus = materialIcon(name = "EIM.PencilPlus") {
            materialPath {
                moveTo(20.70000F, 7.00000F)
                curveTo(21.10000F, 6.60000F, 21.10000F, 6.00000F, 20.70000F, 5.60000F)
                lineTo(18.40000F, 3.30000F)
                curveTo(18.00000F, 2.90000F, 17.40000F, 2.90000F, 17.00000F, 3.30000F)
                lineTo(15.20000F, 5.10000F)
                lineTo(19.00000F, 8.90000F)
                moveTo(3.00000F, 17.20000F)
                verticalLineTo(21.00000F)
                horizontalLineTo(6.80000F)
                lineTo(17.80000F, 9.90000F)
                lineTo(14.10000F, 6.10000F)
                lineTo(3.00000F, 17.20000F)
                moveTo(7.00000F, 2.00000F)
                verticalLineTo(5.00000F)
                horizontalLineTo(10.00000F)
                verticalLineTo(7.00000F)
                horizontalLineTo(7.00000F)
                verticalLineTo(10.00000F)
                horizontalLineTo(5.00000F)
                verticalLineTo(7.00000F)
                horizontalLineTo(2.00000F)
                verticalLineTo(5.00000F)
                horizontalLineTo(5.00000F)
                verticalLineTo(2.00000F)
                horizontalLineTo(7.00000F)
                close()
            }
        }
        return pencilPlus!!
    }


private var pencilPlus: ImageVector? = null