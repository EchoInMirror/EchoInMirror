package cn.apisium.eim.components

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import cn.apisium.eim.EchoInMirror

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun FloatingDialog(dialogContent: @Composable () -> Unit, hasOverlay: Boolean = false, isCentral: Boolean = false,
                   content: @Composable (closeDialog: () -> Unit) -> Unit) {
    val id = remember { Any() }
    val closeDialog = remember { { EchoInMirror.windowManager.closeFloatingDialog(id) } }
    val offset = remember { arrayOf(Offset.Zero) }
    Box((if (isCentral) Modifier else Modifier.onGloballyPositioned { offset[0] = it.positionInRoot() })
        .onPointerEvent(PointerEventType.Press) {
            EchoInMirror.windowManager.openFloatingDialog(closeDialog, if (isCentral) null else offset[0], id, hasOverlay, dialogContent)
        }
    ) { content(closeDialog) }
}
