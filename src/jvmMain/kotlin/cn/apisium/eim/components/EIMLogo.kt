package cn.apisium.eim.components

import androidx.compose.material.icons.materialIcon
import androidx.compose.material.icons.materialPath
import androidx.compose.ui.graphics.vector.ImageVector

val EIMLogo: ImageVector
    get() {
        if (logo != null) { return logo!! }
        logo = materialIcon(name = "EIM.Logo") {
            materialPath {
                moveTo(10.504F, 9.96F)
                lineTo(9.402F, 8.2F)
                arcToRelative(7.746F, 7.746F, 0.0F,
                    isMoreThanHalf = false,
                    isPositiveArc = false,
                    dx1 = -0.528F,
                    dy1 = 0.621F
                )
                lineToRelative(-0.005F, 0.007F)
                lineToRelative(1.141F, 1.823F)
                curveToRelative(0.105F, -0.17F, 0.44F, -0.626F, 0.494F, -0.69F)
                close()
                moveToRelative(-0.575F, -2.267F)
                lineToRelative(1.084F, 1.733F)
                curveToRelative(0.196F, -0.18F, 0.404F, -0.344F, 0.621F, -0.492F)
                lineToRelative(-1.08F, -1.728F)
                arcToRelative(7.21F, 7.21F, 0.0F,
                    isMoreThanHalf = false,
                    isPositiveArc = false,
                    dx1 = -0.625F,
                    dy1 = 0.487F
                )
                close()
                moveToRelative(1.233F, -0.864F)
                lineToRelative(1.092F, 1.747F)
                arcToRelative(4.91F, 4.91F, 0.0F,
                    isMoreThanHalf = false,
                    isPositiveArc = true,
                    dx1 = 4.208F,
                    dy1 = -0.063F
                )
                lineToRelative(1.266F, -1.661F)
                arcToRelative(6.694F, 6.694F, 0.0F,
                    isMoreThanHalf = false,
                    isPositiveArc = false,
                    dx1 = -6.566F,
                    dy1 = -0.023F
                )
                close()
                moveTo(18.866F, 10.694F)
                curveToRelative(1.238F, 2.07F, 1.16F, 4.861F, -0.365F, 6.862F)
                lineToRelative(1.473F, 1.337F)
                curveToRelative(2.205F, -2.894F, 2.188F, -7.003F, 0.157F, -9.858F)
                close()
                moveTo(8.406F, 9.436F)
                lineTo(2.232F, 17.54F)
                lineToRelative(1.472F, 1.336F)
                lineToRelative(5.825F, -7.646F)
                close()
                moveTo(7.562F, 14.859F)
                lineTo(6.297F, 16.52F)
                arcToRelative(6.693F, 6.693F, 0.0F,
                    isMoreThanHalf = false,
                    isPositiveArc = false,
                    dx1 = 6.557F,
                    dy1 = 0.029F
                )
                lineToRelative(-1.093F, -1.748F)
                arcToRelative(4.91F, 4.91F, 0.0F,
                    isMoreThanHalf = false,
                    isPositiveArc = true,
                    dx1 = -4.199F,
                    dy1 = 0.058F
                )
                close()
                moveToRelative(4.82F, -0.415F)
                lineToRelative(1.08F, 1.728F)
                arcToRelative(7.22F, 7.22F, 0.0F,
                    isMoreThanHalf = false,
                    isPositiveArc = false,
                    dx1 = 0.624F,
                    dy1 = -0.483F
                )
                lineToRelative(-1.084F, -1.733F)
                arcToRelative(5.306F, 5.306F, 0.0F,
                    isMoreThanHalf = false,
                    isPositiveArc = true,
                    dx1 = -0.62F,
                    dy1 = 0.488F
                )
                close()
                moveToRelative(1.627F, -1.712F)
                curveToRelative(-0.102F, 0.167F, -0.44F, 0.624F, -0.497F, 0.691F)
                lineToRelative(1.101F, 1.761F)
                curveToRelative(0.189F, -0.199F, 0.368F, -0.409F, 0.536F, -0.629F)
                close()
                moveTo(5.524F, 5.817F)
                lineTo(4.051F, 4.48F)
                curveToRelative(-2.205F, 2.895F, -2.188F, 7.003F, -0.158F, 9.86F)
                lineToRelative(1.266F, -1.662F)
                curveToRelative(-1.238F, -2.07F, -1.16F, -4.86F, 0.365F, -6.861F)
                close()
                moveTo(20.339F, 4.471F)
                lineToRelative(-5.85F, 7.68F)
                lineToRelative(1.123F, 1.795F)
                lineToRelative(6.2F, -8.139F)
                close()
                moveTo(11.366F, 12.53F)
                curveToRelative(-0.435F, -0.395F, -0.495F, -1.1F, -0.133F, -1.574F)
                curveToRelative(0.361F, -0.475F, 1.007F, -0.54F, 1.442F, -0.145F)
                curveToRelative(0.435F, 0.395F, 0.494F, 1.099F, 0.132F, 1.573F)
                curveToRelative(-0.361F, 0.475F, -1.006F, 0.54F, -1.441F, 0.145F)
                close()
            }
        }
        return logo!!
    }

private var logo: ImageVector? = null
