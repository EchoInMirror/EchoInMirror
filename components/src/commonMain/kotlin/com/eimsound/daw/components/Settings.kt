package com.eimsound.daw.components
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp


private val EXPANDER_PADDING_HORIZONTAL = 16.dp
private val EXPANDER_PADDING_VERTICAL = 8.dp
private val EXPANDER_CARD_GAP = 4.dp
private val LIST_HEIGHT = 36.dp
private val CARD_HEIGHT = 64.dp
private val EXPAND_ICON_SIZE = 20.dp
private val MENU_MAX_WIDTH = 256.dp

@Composable
fun <T> SettingsMenu(
    items: Collection<T>?,
    selected: T,
    toString: (T) -> String = { it.toString() },
    onSelect: (T) -> Unit
) {
    if (items == null) return
    val textMeasurer = rememberTextMeasurer()
    val itemsMap = items.associateBy { toString(it) }
    val textWidth = (items.maxOfOrNull { textMeasurer.measure(toString(it)).size.width.dp } ?: 0.dp) + EXPANDER_PADDING_HORIZONTAL * 2
    val maxWidth = if (textWidth > MENU_MAX_WIDTH) MENU_MAX_WIDTH else textWidth
    OutlinedDropdownSelector(
        { itemsMap[it]?.let { selected -> onSelect(selected) } },
        itemsMap.keys,
        selected = toString(selected),
    ) {
        CustomOutlinedTextField(
            toString(selected), { },
            Modifier.width(maxWidth).height(LIST_HEIGHT).pointerHoverIcon(PointerIcon.Hand),
            readOnly = true,
            textStyle = MaterialTheme.typography.labelLarge.copy(LocalContentColor.current),
            suffix = {
                Icon(Icons.Filled.ExpandMore, "Expand",
                    Modifier.size(EXPAND_ICON_SIZE).pointerHoverIcon(PointerIcon.Hand).clip(CircleShape)
                        .clickable { }
                )
            },
            colors = TextFieldDefaults.colors(
                unfocusedIndicatorColor = MaterialTheme.colorScheme.outlineVariant,
                focusedIndicatorColor = MaterialTheme.colorScheme.outlineVariant,
            ),
            paddingValues = TextFieldDefaults.contentPaddingWithLabel(8.dp, 4.dp, 3.dp, 4.dp)
        )
    }
}

@Composable
fun SettingsSection(
    title: String? = null,
    content: @Composable () -> Unit
) {
    Column {
        if (title != null){
            Text(title, style = MaterialTheme.typography.bodyMedium)
            Gap(8)
        }
        content()
    }
}

@Composable
fun SettingsCard(
    header: String = "",
    content: @Composable () -> Unit
) {
    Surface(
        shape = MaterialTheme.shapes.small,
        modifier = Modifier.padding(bottom = EXPANDER_CARD_GAP).height(CARD_HEIGHT)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.background(MaterialTheme.colorScheme.surface).padding(EXPANDER_PADDING_HORIZONTAL, EXPANDER_PADDING_VERTICAL)
        ) {
            Text(header)
            Filled()
            content()
        }
    }
}

@Composable
fun <T> SettingsListManager(
    list: Collection<T>,
    addButtonText: String? = "添加路径",
    onAddButtonClick: (() -> Unit)? = null,
    onDelete: ((T) -> Unit)? = null,
) {
    Surface(shape = MaterialTheme.shapes.small,
    ) {
        Column {
            if (list.isEmpty()) {
                MenuItem(modifier=Modifier.background(MaterialTheme.colorScheme.surface).fillMaxWidth()) {
                    Filled()
                    Text("暂无...",
                        color = MaterialTheme.colorScheme.onSurface.copy(0.5F),
                        style = LocalTextStyle.current.copy(fontSize = 14.sp, fontStyle = FontStyle.Italic)
                    )
                    Filled()
                }
            }
            list.forEach {
                MenuItem(modifier=Modifier.background(MaterialTheme.colorScheme.surface).fillMaxWidth()) {
                    Text(it.toString())
                    Filled()
                    onDelete?.let { delete ->
                        IconButton(onClick = { delete(it) }) {
                            Icon(Icons.Filled.Delete, "删除")
                        }
                    }
                }
                Divider()
            }
            onAddButtonClick?.let {
                Button(it, Modifier.fillMaxWidth().height(LIST_HEIGHT), shape = MaterialTheme.shapes.extraSmall) {
                    Text(addButtonText!!)
                }
            }
        }
    }
}