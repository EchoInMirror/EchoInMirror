package cn.apisium.eim.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.unit.toSize
import cn.apisium.eim.EchoInMirror

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun FloatingDialog(dialogContent: @Composable (size: Size) -> Unit, modifier: Modifier = Modifier,
                   hasOverlay: Boolean = false, isCentral: Boolean = false,
                   content: @Composable BoxScope.(closeDialog: () -> Unit) -> Unit) {
    val id = remember { Any() }
    val closeDialog = remember { { EchoInMirror.windowManager.closeFloatingDialog(id) } }
    val offset = remember { arrayOf(Offset.Zero) }
    val size = remember { arrayOf(Size.Zero) }
    Box((if (isCentral) modifier else modifier.onGloballyPositioned {
        offset[0] = it.positionInRoot()
        size[0] = it.size.toSize()
    })
        .onPointerEvent(PointerEventType.Press) {
            EchoInMirror.windowManager.openFloatingDialog(closeDialog,
                if (isCentral) null else offset[0] + Offset(0f, size[0].height),
                id, hasOverlay
            ) { dialogContent(size[0]) }
        }
    ) { content(closeDialog) }
}
