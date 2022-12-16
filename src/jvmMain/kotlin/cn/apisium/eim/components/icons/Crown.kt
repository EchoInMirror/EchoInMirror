package cn.apisium.eim.components.icons

import androidx.compose.material.icons.materialIcon
import androidx.compose.material.icons.materialPath
import androidx.compose.ui.graphics.vector.ImageVector

val Crown: ImageVector
    get() {
        if (crown != null) { return crown!! }
        crown = materialIcon(name = "EIM.Crown") {
            materialPath {
                moveTo(5.00000F, 16.00000F)
                lineTo(3.00000F, 5.00000F)
                lineTo(8.50000F, 10.00000F)
                lineTo(12.00000F, 4.00000F)
                lineTo(15.50000F, 10.00000F)
                lineTo(21.00000F, 5.00000F)
                lineTo(19.00000F, 16.00000F)
                horizontalLineTo(5.00000F)
                moveTo(19.00000F, 19.00000F)
                curveTo(19.00000F, 19.60000F, 18.60000F, 20.00000F, 18.00000F, 20.00000F)
                horizontalLineTo(6.00000F)
                curveTo(5.40000F, 20.00000F, 5.00000F, 19.60000F, 5.00000F, 19.00000F)
                verticalLineTo(18.00000F)
                horizontalLineTo(19.00000F)
                verticalLineTo(19.00000F)
                close()
            }
        }
        return crown!!
    }

private var crown: ImageVector? = null
