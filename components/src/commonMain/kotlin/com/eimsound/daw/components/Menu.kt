package com.eimsound.daw.components

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.UiComposable
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.eimsound.daw.commons.DividerAbove
import com.eimsound.daw.commons.displayName
import com.eimsound.daw.components.utils.clickableWithIcon
import com.eimsound.daw.utils.ifNotNull

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

@Composable
fun <T : Any> OutlinedDropdownSelector(
    onSelected: (T) -> Unit,
    items: Collection<T>,
    selected: T?,
    boxModifier: Modifier = Modifier,
    enabled: Boolean = true,
    isSelected: ((T) -> Boolean)? = null,
    itemContent: (@Composable (T) -> Unit)? = null,
    label: String? = null,
    readonly: Boolean = false,
    colors: TextFieldColors? = null,
    content: (@Composable () -> Unit)? = null,
) {
    @OptIn(ExperimentalFoundationApi::class)
    DropdownSelector(
        onSelected, items, selected, boxModifier, enabled, PointerMatcher.Primary,
        isSelected, itemContent, label, true, readonly, colors, content
    )
}

@Composable
fun <T : Any> AutoWidthOutlinedDropdownSelector(
    onSelected: (T) -> Unit,
    items: Collection<T>,
    selected: T?,
    boxModifier: Modifier = Modifier,
    enabled: Boolean = true,
    isSelected: ((T) -> Boolean)? = null,
    itemContent: (@Composable (T) -> Unit)? = null,
    label: String? = null,
    colors: TextFieldColors? = null
) {
    var modifier = boxModifier
    val textMeasurer = rememberTextMeasurer()
    val textWidth = remember(items, items.size) {
        (items.maxOfOrNull { textMeasurer.measure(it.displayName).size.width.dp } ?: 0.dp) + 32.dp
    }
    val maxWidth = if (textWidth > 256.dp) 256.dp else textWidth
    modifier = modifier.width(maxWidth)
    OutlinedDropdownSelector(
        onSelected,
        items,
        selected,
        modifier,
        enabled,
        isSelected,
        itemContent,
        label,
        readonly = true,
        colors
    )
}

@Composable
fun <T : Any> DropdownSelector(
    onSelected: (T) -> Unit,
    items: Collection<T>,
    selected: T?,
    boxModifier: Modifier = Modifier,
    enabled: Boolean = true,
    isSelected: ((T) -> Boolean)? = null,
    itemContent: (@Composable (T) -> Unit)? = null,
    label: String? = null,
    readOnly: Boolean = false,
    colors: TextFieldColors? = null,
    content: (@Composable () -> Unit)? = null,
) {
    @OptIn(ExperimentalFoundationApi::class)
    DropdownSelector(
        onSelected, items, selected, boxModifier, enabled, PointerMatcher.Primary,
        isSelected, itemContent, label, false, readOnly, colors, content
    )
}

@Composable
fun <T : Any> DropdownSelector(
    onSelected: (T) -> Unit,
    items: Collection<T>,
    selected: T?,
    boxModifier: Modifier = Modifier,
    enabled: Boolean = true,
    @OptIn(ExperimentalFoundationApi::class) matcher: PointerMatcher = PointerMatcher.Primary,
    isSelected: ((T) -> Boolean)? = null,
    itemContent: (@Composable (T) -> Unit)? = null,
    label: String? = null,
    isOutlined: Boolean = false,
    readonly: Boolean = false,
    colors: TextFieldColors? = null,
    content: (@Composable () -> Unit)? = null,
) {
    val filter = remember { mutableStateOf<String?>(null) }
    @OptIn(ExperimentalFoundationApi::class) FloatingLayer({ size, close ->
        Surface(
            Modifier.width(size.width).heightIn(max = 300.dp), MaterialTheme.shapes.extraSmall,
            shadowElevation = 5.dp, tonalElevation = 5.dp
        ) {
            FlexibleScrollable {
                val state = rememberLazyListState()
                val currentValue = filter.value
                val items0 = remember(items, currentValue) { (if (currentValue == null) items else items
                    .filter { it.displayName.contains(currentValue, true) }).distinct() }
                LazyColumn(state = state) {
                    items(items0) {
                        if ((it as? DividerAbove)?.hasDividerAbove == true) Divider()
                        MenuItem({
                            close()
                            filter.value = null
                            onSelected(it)
                        }, if (isSelected == null) it == selected else isSelected(it), modifier = Modifier.fillMaxWidth()) {
                            if (itemContent == null) Text(it.displayName)
                            else itemContent(it)
                        }
                    }
                }
                VerticalScrollbar(rememberScrollbarAdapter(state), Modifier.fillMaxHeight())
            }
        }
    }, modifier = boxModifier, enabled = enabled, matcher = matcher, pass = PointerEventPass.Initial) {
        if (content == null) {
            if (isOutlined) CustomOutlinedTextField(
                filter.value ?: selected?.displayName ?: "",
                { filter.value = it.ifEmpty { null } },
                boxModifier.pointerHoverIcon(PointerIcon.Hand),
                label = if (label == null) null else ({ Text(label) }),
                singleLine = true,
                readOnly = readonly,
                colors = colors ?: TextFieldDefaults.colors(),
                suffix = {
                    Icon(
                        Icons.Filled.ExpandMore, "Expand",
                        Modifier.size(20.dp).pointerHoverIcon(PointerIcon.Hand).clip(CircleShape).clickable { }
                    )
                }
            ) else CustomTextField(
                filter.value ?: selected?.displayName ?: "",
                { filter.value = it.ifEmpty { null } },
                boxModifier.pointerHoverIcon(PointerIcon.Hand),
                label = if (label == null) null else ({ Text(label) }),
                singleLine = true,
                readOnly = readonly,
                colors = colors ?: TextFieldDefaults.colors(),
                suffix = {
                    Icon(
                        Icons.Filled.ExpandMore, "Expand",
                        Modifier.size(20.dp).pointerHoverIcon(PointerIcon.Hand).clip(CircleShape).clickable { }
                    )
                }
            )
        } else content()
    }
}

@Composable
fun MenuHeader(
    title: String, enable: Boolean = true, icon: ImageVector? = null, color: Color? = null,
    onChange: ((String) -> Unit)? = null, onColorChange: ((Color) -> Unit)? = null,
    titleContent: @Composable (RowScope.() -> Unit)? = null, content: (@Composable ColumnScope.() -> Unit)? = null
) {
    Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min).ifNotNull(color) { background(it.copy(0.1F)) }) {
        if (color != null) {
            val floatingLayerProvider = LocalFloatingLayerProvider.current
            Spacer(Modifier.width(8.dp).fillMaxHeight().background(color).ifNotNull(onColorChange) {
                clickableWithIcon {
                    if (onColorChange != null) floatingLayerProvider.openColorPicker(color, onChange = onColorChange)
                }
            })
        }
        Column {
            Box(
                Modifier.fillMaxWidth().ifNotNull(color) { background(it.copy(0.24F)) }
            ) {
                Row(
                    Modifier.padding(start = if (icon == null) 8.dp else 4.dp).heightIn(32.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (icon != null) {
                        Icon(icon, title, Modifier.size(16.dp))
                        Gap(2)
                    }

                    if (onChange == null) Text(title, Modifier.weight(1F).padding(4.dp, end = 16.dp),
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 1, overflow = TextOverflow.Ellipsis,
                        textDecoration = if (enable) null else TextDecoration.LineThrough,
                        color = LocalContentColor.current.copy(alpha = if (enable) 1F else 0.7F),
                        fontWeight = FontWeight.ExtraBold
                    ) else BasicTextField(title, onChange,
                        Modifier.weight(1F).padding(start = 4.dp), singleLine = true,
                        textStyle = MaterialTheme.typography.titleSmall.copy(
                            LocalContentColor.current.copy(if (enable) 1F else 0.7F),
                            fontWeight = FontWeight.ExtraBold
                        )
                    )

                    titleContent?.invoke(this)
                }
            }
            content?.invoke(this)
        }
    }
    Divider()
}
