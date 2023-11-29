package com.eimsound.daw.components

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.UiComposable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
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

@Composable
private fun FlexibleScrollable(content: @Composable @UiComposable () -> Unit) {
    Layout(content) { measurables, constraints ->
        val col = measurables[0].measure(constraints)
        val scroll = measurables[1].measure(constraints.copy(0, maxHeight = col.height))
        layout(col.width, col.height) {
            col.placeRelative(0, 0)
            scroll.placeRelative(constraints.maxWidth - scroll.width, 0)
        }
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
            Modifier.widthIn(size.width).width(IntrinsicSize.Min).heightIn(8.dp).height(IntrinsicSize.Min), MaterialTheme.shapes.extraSmall,
            shadowElevation = 5.dp, tonalElevation = 5.dp
        ) {
            FlexibleScrollable {
                val stateVertical = rememberScrollState(0)
                Column(Modifier.verticalScroll(stateVertical)) { menuItems(close) }
                VerticalScrollbar(rememberScrollbarAdapter(stateVertical), Modifier.fillMaxHeight())
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
    items: Collection<String>,
    selected: String,
    boxModifier: Modifier = Modifier,
    enabled: Boolean = true,
    matcher: PointerMatcher = PointerMatcher.Primary,
    content: (@Composable () -> Unit)? = null,
    onSelected: (String) -> Unit,
) {
    val filter = remember { mutableStateOf<String?>(null) }
    FloatingLayer({ size, close ->
        Surface(
            Modifier.width(size.width).heightIn(max = 300.dp), MaterialTheme.shapes.extraSmall,
            shadowElevation = 5.dp, tonalElevation = 5.dp
        ) {
            FlexibleScrollable {
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
                VerticalScrollbar(rememberScrollbarAdapter(state), Modifier.fillMaxHeight())
            }
        }
    }, modifier = boxModifier, enabled = enabled, matcher = matcher, pass = PointerEventPass.Initial) {
        if (content == null) CustomTextField(filter.value ?: selected, {
            filter.value = it.ifEmpty { null }
        }, Modifier.fillMaxWidth().pointerHoverIcon(PointerIcon.Hand))
        else content()
    }
}

@Composable
fun MenuHeader(
    title: String, enable: Boolean = true, icon: ImageVector? = null,
    onChange: ((String) -> Unit)? = null, content: @Composable (RowScope.() -> Unit)? = null
) {
    Row(
        Modifier.padding(start = 12.dp).fillMaxWidth().heightIn(32.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Icon(icon, null, Modifier.size(18.dp))
        }
        if (onChange == null) Text(title, Modifier.weight(1F).padding(4.dp, end = 16.dp),
            style = MaterialTheme.typography.titleSmall,
            maxLines = 1, overflow = TextOverflow.Ellipsis,
            textDecoration = if (enable) null else TextDecoration.LineThrough,
            color = LocalContentColor.current.copy(alpha = if (enable) 1F else 0.7F),
            fontWeight = FontWeight.ExtraBold
        ) else BasicTextField(title, onChange,
            Modifier.weight(1F).padding(start = 4.dp), maxLines = 1,
            textStyle = MaterialTheme.typography.titleSmall.copy(
                LocalContentColor.current.copy(if (enable) 1F else 0.7F),
                fontWeight = FontWeight.ExtraBold
            )
        )
        content?.invoke(this)
    }
}
