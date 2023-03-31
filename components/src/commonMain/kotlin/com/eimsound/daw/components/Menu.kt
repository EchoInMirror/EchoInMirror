package com.eimsound.daw.components

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalFoundationApi::class)
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
                .pointerHoverIcon(if (enabled && onClick != null) PointerIcon.Hand else PointerIcon.Default)
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
                VerticalScrollbar(rememberScrollbarAdapter(stateVertical), Modifier.align(Alignment.CenterEnd).fillMaxHeight())
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun Selector(
    items: List<String>,
    selected: String,
    boxModifier: Modifier = Modifier,
    enabled: Boolean = true,
    matcher: PointerMatcher = PointerMatcher.Primary,
    onSelected: (String) -> Unit,
) {
    val filter = remember { mutableStateOf<String?>(null) }
    FloatingLayer({ size, close ->
        Surface(
            Modifier.width(size.width.dp).heightIn(max = 300.dp), MaterialTheme.shapes.extraSmall,
            shadowElevation = 5.dp, tonalElevation = 5.dp
        ) {
            Box {
                val state = rememberLazyListState()
                val currentValue = filter.value
                val items0 = remember(items, currentValue) { (if (currentValue == null) items else items
                    .filter { it.contains(currentValue, true) }).distinct() }
                LazyColumn(state = state) {
                    items(items0) {
                        MenuItem({
                            close()
                            filter.value = null
                            onSelected(it)
                        }, selected == it, modifier = Modifier.fillMaxWidth()) {
                            Text(it)
                        }
                    }
                }
                VerticalScrollbar(rememberScrollbarAdapter(state), Modifier.align(Alignment.CenterEnd).fillMaxHeight())
            }
        }
    }, modifier = boxModifier, enabled = enabled, matcher = matcher, pass = PointerEventPass.Initial) {
        CustomTextField(filter.value ?: selected, {
            filter.value = it.ifEmpty { null }
        }, Modifier.fillMaxWidth().pointerHoverIcon(PointerIcon.Hand))
    }
}
