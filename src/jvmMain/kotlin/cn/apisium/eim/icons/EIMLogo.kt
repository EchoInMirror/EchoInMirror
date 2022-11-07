package cn.apisium.eim.icons

import androidx.compose.material.icons.materialIcon
import androidx.compose.material.icons.materialPath
import androidx.compose.ui.graphics.vector.ImageVector

val EIMLogo: ImageVector
    get() {
        if (logo != null) { return logo!! }
        logo = materialIcon(name = "EIM.Logo") {
            materialPath {
                moveTo(11.5544F, 10.956F)
                lineTo(10.3422F, 9.02F)
                arcToRelative(
                    8.5206F, 8.5206F, 0F,
                    isMoreThanHalf = false,
                    isPositiveArc = false,
                    dx1 = -0.5808F,
                    dy1 = 0.6831F
                )
                lineToRelative(-0.0055F, 0.0077F)
                lineToRelative(1.2551F, 2.0053F)
                curveToRelative(0.1155F, -0.187F, 0.484F, -0.6886F, 0.5434F, -0.759F)
                close()
                moveToRelative(-0.6325F, -2.4937F)
                lineToRelative(1.1924F, 1.9063F)
                curveToRelative(0.2156F, -0.198F, 0.4444F, -0.3784F, 0.6831F, -0.5412F)
                lineToRelative(-1.188F, -1.9008F)
                arcToRelative(
                    7.931F, 7.931F, 0F,
                    isMoreThanHalf = false,
                    isPositiveArc = false,
                    dx1 = -0.6875F,
                    dy1 = 0.5357F
                )
                close()
                moveToRelative(1.3563F, -0.9504F)
                lineToRelative(1.2012F, 1.9217F)
                arcToRelative(
                    5.401F, 5.401F, 0F,
                    isMoreThanHalf = false,
                    isPositiveArc = true,
                    dx1 = 4.6288F,
                    dy1 = -0.0693F
                )
                lineToRelative(1.3926F, -1.8271F)
                arcToRelative(7.3634F, 7.3634F, 0F,
                    isMoreThanHalf = false,
                    isPositiveArc = false,
                    dx1 = -7.2226F,
                    dy1 = -0.0253F
                )
                close()
                moveTo(20.7526F, 11.7634F)
                curveToRelative(1.3618F, 2.277F, 1.276F, 5.3471F, -0.4015F, 7.5482F)
                lineToRelative(1.6203F, 1.4707F)
                curveToRelative(2.4255F, -3.1834F, 2.4068F, -7.7033F, 0.1727F, -10.8438F)
                close()
                moveTo(9.2466F, 10.3796F)
                lineTo(2.4552F, 19.294F)
                lineToRelative(1.6192F, 1.4696F)
                lineToRelative(6.4075F, -8.4106F)
                close()
                moveTo(8.3182F, 16.3449F)
                lineTo(6.9267F, 18.172F)
                arcToRelative(7.3623F, 7.3623F, 0F,
                    isMoreThanHalf = false,
                    isPositiveArc = false,
                    dx1 = 7.2127F,
                    dy1 = 0.0319F
                )
                lineToRelative(-1.2023F, -1.9228F)
                arcToRelative(
                    5.401F, 5.401F, 0F,
                    isMoreThanHalf = false,
                    isPositiveArc = true,
                    dx1 = -4.6189F,
                    dy1 = 0.0638F
                )
                close()
                moveToRelative(5.302F, -0.4565F)
                lineToRelative(1.188F, 1.9008F)
                arcToRelative(7.942F, 7.942F, 0F,
                    isMoreThanHalf = false,
                    isPositiveArc = false,
                    dx1 = 0.6864F,
                    dy1 = -0.5313F
                )
                lineToRelative(-1.1924F, -1.9063F)
                arcToRelative(
                    5.8366F, 5.8366F, 0F,
                    isMoreThanHalf = false,
                    isPositiveArc = true,
                    dx1 = -0.682F,
                    dy1 = 0.5368F
                )
                close()
                moveToRelative(1.7897F, -1.8832F)
                curveToRelative(-0.1122F, 0.1837F, -0.484F, 0.6864F, -0.5467F, 0.7601F)
                lineToRelative(1.2111F, 1.9371F)
                curveToRelative(0.2079F, -0.2189F, 0.4048F, -0.4499F, 0.5896F, -0.6919F)
                close()
                moveTo(6.0764F, 6.3987F)
                lineTo(4.4561F, 4.928F)
                curveToRelative(-2.4255F, 3.1845F, -2.4068F, 7.7033F, -0.1738F, 10.846F)
                lineToRelative(1.3926F, -1.8282F)
                curveToRelative(-1.3618F, -2.277F, -1.276F, -5.346F, 0.4015F, -7.5471F)
                close()
                moveTo(22.3729F, 4.9181F)
                lineToRelative(-6.435F, 8.448F)
                lineToRelative(1.2353F, 1.9745F)
                lineToRelative(6.82F, -8.9529F)
                close()
                moveTo(12.5026F, 13.783F)
                curveToRelative(-0.4785F, -0.4345F, -0.5445F, -1.21F, -0.1463F, -1.7314F)
                curveToRelative(0.3971F, -0.5225F, 1.1077F, -0.594F, 1.5862F, -0.1595F)
                curveToRelative(0.4785F, 0.4345F, 0.5434F, 1.2089F, 0.1452F, 1.7303F)
                curveToRelative(-0.3971F, 0.5225F, -1.1066F, 0.594F, -1.5851F, 0.1595F)
                close()
            }
        }
        return logo!!
    }

private var logo: ImageVector? = null
