package com.eimsound.daw.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eimsound.daw.language.langs


private val EXPANDER_PADDING_VERTICAL = 8.dp
private val EXPANDER_CARD_GAP = 4.dp
private val LIST_HEIGHT = 36.dp
private val CARD_HEIGHT = 64.dp


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
            modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                .padding(16.dp, EXPANDER_PADDING_VERTICAL)
        ) {
            Text(header)
            Gap(16)
            Filled()
            content()
        }
    }
}

@Composable
fun <T> SettingsListManager(
    list: Collection<T>,
    addButtonText: String? = langs.addPath,
    onAddButtonClick: (() -> Unit)? = null,
    onDelete: ((T) -> Unit)? = null,
) {
    Surface(shape = MaterialTheme.shapes.small) {
        val modifier = Modifier.background(MaterialTheme.colorScheme.surface).fillMaxWidth()
        Column {
            if (list.isEmpty()) {
                MenuItem(modifier = modifier) {
                    Filled()
                    Text(langs.none + "...",
                        color = MaterialTheme.colorScheme.onSurface.copy(0.5F),
                        style = LocalTextStyle.current.copy(fontSize = 14.sp, fontStyle = FontStyle.Italic)
                    )
                    Filled()
                }
            } else {
                list.forEach {
                    key(it) {
                        MenuItem(modifier = modifier) {
                            Row(Modifier.padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text(it.toString(), Modifier.weight(1F))
                                Gap(16)
                                onDelete?.let { delete ->
                                    IconButton(onClick = { delete(it) }) {
                                        Icon(Icons.Filled.Delete, langs.delete)

                                    }
                                }
                            }
                        }
                        Divider()
                    }
                }
            }
            onAddButtonClick?.let {
                Button(it, Modifier.fillMaxWidth().height(LIST_HEIGHT), shape = MaterialTheme.shapes.extraSmall) {
                    Text(addButtonText!!)
                }
            }
        }
    }
}