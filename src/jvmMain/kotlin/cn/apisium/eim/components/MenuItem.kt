package cn.apisium.eim.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun MenuItem(
    selected: Boolean = false,
    onClick: () -> Unit,
    content: @Composable RowScope.() -> Unit
) {
    Row(
        modifier = Modifier
            .clickable(
                onClick = onClick,
            )
            .run { if (selected) background(MaterialTheme.colorScheme.secondary.copy(0.2F)) else this }
            .sizeIn(
                minWidth = 100.dp,
                maxWidth = 280.dp,
                minHeight = 38.dp
            )
            .padding(
                PaddingValues(
                    horizontal = 16.dp,
                    vertical = 0.dp
                )
            ),
        verticalAlignment = Alignment.CenterVertically,
        content = content
    )
}