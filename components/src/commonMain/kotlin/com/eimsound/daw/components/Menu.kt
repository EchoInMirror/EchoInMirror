package com.eimsound.daw.components

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerIconDefaults
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalComposeUiApi::class, ExperimentalFoundationApi::class)
@Composable
fun MenuItem(
    onClick: (() -> Unit)? = null,
    selected: Boolean = false,
    onDoubleClick: (() -> Unit)? = null,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
    minHeight: Dp = 38.dp,
    padding: PaddingValues = PaddingValues(horizontal = 12.dp),
    horizontalArrangement: Arrangement.Horizontal = Arrangement.Start,
    content: @Composable RowScope.() -> Unit
) {
    val color = LocalContentColor.current
    CompositionLocalProvider(LocalContentColor.provides(if (enabled) color else color.copy(0.38F))) {
        Row((if (onClick == null) modifier else modifier
                .combinedClickable(
                    enabled,
                    onDoubleClick = onDoubleClick,
                    onClick = onClick
                ))
                .run { if (selected) background(MaterialTheme.colorScheme.secondary.copy(0.2F)) else this }
                .sizeIn(
                    minWidth = 100.dp,
                    maxWidth = 280.dp,
                    minHeight = minHeight
                )
                .pointerHoverIcon(if (enabled && onClick != null) PointerIconDefaults.Hand else PointerIconDefaults.Default)
                .padding(padding),
            horizontalArrangement,
            Alignment.CenterVertically,
            content
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DropdownMenu(
    menuItems: @Composable (() -> Unit) -> Unit,
    boxModifier: Modifier = Modifier,
    enabled: Boolean = true,
    matcher: PointerMatcher = PointerMatcher.Primary,
    content: @Composable BoxScope.() -> Unit,
) {
    FloatingLayer({ size, close ->
        Surface(
            Modifier.widthIn(size.width.dp).width(IntrinsicSize.Min).heightIn(8.dp), MaterialTheme.shapes.extraSmall,
            shadowElevation = 5.dp, tonalElevation = 5.dp
        ) {
            Box {
                val stateVertical = rememberScrollState(0)
                Column(Modifier.verticalScroll(stateVertical)) {
                    menuItems(close)
                }
            }
        }
    }, modifier = boxModifier, enabled = enabled, matcher = matcher, content = content)
}

@Composable
fun Menu(
    menuItems: @Composable (() -> Unit) -> Unit,
    modifier: Modifier = Modifier,
    boxModifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    @OptIn(ExperimentalFoundationApi::class)
    DropdownMenu(menuItems, boxModifier) {
        ReadonlyTextField(content = content, modifier = modifier)
    }
}
