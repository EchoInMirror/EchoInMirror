package cn.apisium.eim.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerIconDefaults
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import cn.apisium.eim.EchoInMirror

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun FloatingDialog(dialogContent: @Composable (size: Size, closeDialog: () -> Unit) -> Unit,
                   modifier: Modifier = Modifier, enabled: Boolean = true,
                   hasOverlay: Boolean = false, isCentral: Boolean = false,
                   content: @Composable BoxScope.() -> Unit) {
    val id = remember { Any() }
    val closeDialog = remember { { EchoInMirror.windowManager.closeFloatingDialog(id) } }
    val offset = remember { arrayOf(Offset.Zero) }
    val size = remember { arrayOf(Size.Zero) }
    Box((if (isCentral) modifier else modifier.onGloballyPositioned {
        offset[0] = it.positionInRoot()
        size[0] = it.size.toSize()
    }).let { if (enabled) it.pointerHoverIcon(PointerIconDefaults.Hand) else it }
        .onPointerEvent(PointerEventType.Press) {
            EchoInMirror.windowManager.openFloatingDialog({ closeDialog() },
                if (isCentral) null else offset[0] + Offset(0f, size[0].height),
                id, hasOverlay
            ) { dialogContent(size[0], closeDialog) }
        }
    ) { content() }
}

@Composable
fun Dialog(onOk: (() -> Unit)? = null, onCancel: (() -> Unit)? = null,
           modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Surface(modifier.widthIn(min = 250.dp).width(IntrinsicSize.Max),
        shape = MaterialTheme.shapes.extraSmall, tonalElevation = 5.dp, shadowElevation = 5.dp) {
        val flag = onOk != null || onCancel != null
        Column(Modifier.padding(16.dp, 16.dp, 16.dp, if (flag) 0.dp else 16.dp)) {
            content()
            if (flag) Row {
                Filled()
                if (onCancel != null) TextButton(onCancel) { Text("取消") }
                if (onOk != null) TextButton(onOk) { Text("确认") }
            }
        }
    }
}
