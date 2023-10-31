package com.eimsound.daw.components
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp


private val EXPANDER_PADDING_HORIZONTAL = 16.dp
private val EXPANDER_PADDING_VERTICAL = 8.dp
private val LIST_HEIGHT = 36.dp

@Composable
fun SettingsCard(
    header: String = "",
    content: @Composable () -> Unit
) {
    Surface(
        shape = MaterialTheme.shapes.small,
        modifier = Modifier.padding(bottom = EXPANDER_PADDING_VERTICAL)
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
    list: List<T>,
//    content: @Composable (T) -> Unit,
    addButtonText: String? = "添加路径",
    onAddButtonClick: (() -> Unit)? = null
) {
    Surface(shape = MaterialTheme.shapes.small,
    ) {
        Column {
            list.forEach {
                MenuItem(modifier=Modifier.background(MaterialTheme.colorScheme.surface).fillMaxWidth()) { Text(it.toString()) }
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