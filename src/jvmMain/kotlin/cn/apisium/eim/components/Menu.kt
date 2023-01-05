package cn.apisium.eim.components

import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerIconDefaults
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalComposeUiApi::class, ExperimentalFoundationApi::class)
@Composable
fun MenuItem(
    selected: Boolean = false,
    onClick: () -> Unit,
    onDoubleClick: (() -> Unit)? = null,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
    minHeight: Dp = 38.dp,
    padding: PaddingValues = PaddingValues(horizontal = 12.dp),
    content: @Composable RowScope.() -> Unit
) {
    Row(
        modifier = modifier
            .combinedClickable(
                enabled,
                onDoubleClick = onDoubleClick,
                onClick = onClick
            )
            .run { if (selected) background(MaterialTheme.colorScheme.secondary.copy(0.2F)) else this }
            .sizeIn(
                minWidth = 100.dp,
                maxWidth = 280.dp,
                minHeight = minHeight
            )
            .padding(padding)
            .pointerHoverIcon(if (enabled) PointerIconDefaults.Hand else PointerIconDefaults.Default),
        verticalAlignment = Alignment.CenterVertically,
        content = content
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Menu(
    menuItems: @Composable (closeDialog: () -> Unit) -> Unit,
    content: @Composable () -> Unit
) {
    FloatingDialog({ size, close ->
        Surface(Modifier.widthIn(size.width.dp).width(IntrinsicSize.Min), MaterialTheme.shapes.extraSmall,
            shadowElevation = 5.dp, tonalElevation = 5.dp
        ) {
            Box {
                val stateVertical = rememberScrollState(0)
                Column(Modifier.verticalScroll(stateVertical)) {
                    menuItems(close)
                }
            }
        }
    }) {
        TextFieldDefaults.TextFieldDecorationBox("", content, true,
            singleLine = true,
            visualTransformation = VisualTransformation.None,
            interactionSource = remember { MutableInteractionSource() },
            trailingIcon = { Icon(Icons.Filled.ExpandMore, null, Modifier.padding(horizontal = 8.dp)) })
    }
}
