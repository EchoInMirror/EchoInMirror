package cn.apisium.eim.components.icons

import androidx.compose.material.icons.materialIcon
import androidx.compose.material.icons.materialPath
import androidx.compose.ui.graphics.vector.ImageVector

val MetronomeTick: ImageVector
    get() {
        if (mtronomeTick != null) { return mtronomeTick!! }
        mtronomeTick = materialIcon(name = "EIM.MetronomeTick") {
            materialPath {
                moveTo(12.00000F, 1.75000F)
                lineTo(8.57000F, 2.67000F)
                lineTo(4.07000F, 19.50000F)
                curveTo(4.06000F, 19.50000F, 4.00000F, 19.84000F, 4.00000F, 20.00000F)
                curveTo(4.00000F, 21.11000F, 4.89000F, 22.00000F, 6.00000F, 22.00000F)
                horizontalLineTo(18.00000F)
                curveTo(19.11000F, 22.00000F, 20.00000F, 21.11000F, 20.00000F, 20.00000F)
                curveTo(20.00000F, 19.84000F, 19.94000F, 19.50000F, 19.93000F, 19.50000F)
                lineTo(15.43000F, 2.67000F)
                lineTo(12.00000F, 1.75000F)
                moveTo(10.29000F, 4.00000F)
                horizontalLineTo(13.71000F)
                lineTo(17.20000F, 17.00000F)
                horizontalLineTo(13.00000F)
                verticalLineTo(12.00000F)
                horizontalLineTo(11.00000F)
                verticalLineTo(17.00000F)
                horizontalLineTo(6.80000F)
                lineTo(10.29000F, 4.00000F)
                moveTo(11.00000F, 5.00000F)
                verticalLineTo(9.00000F)
                horizontalLineTo(10.00000F)
                verticalLineTo(11.00000F)
                horizontalLineTo(14.00000F)
                verticalLineTo(9.00000F)
                horizontalLineTo(13.00000F)
                verticalLineTo(5.00000F)
                horizontalLineTo(11.00000F)
                close()
            }
        }
        return mtronomeTick!!
    }

private var mtronomeTick: ImageVector? = null
