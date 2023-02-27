package com.eimsound.daw.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import com.eimsound.daw.components.utils.Zero

@Composable
fun AbsoluteElevation(elevation: Dp = Dp.Zero, content: @Composable () -> Unit) {
    CompositionLocalProvider(
        LocalContentColor provides MaterialTheme.colorScheme.surface,
        LocalAbsoluteTonalElevation provides elevation,
        content = content
    )
}

@Composable
fun AbsoluteElevationCard(
    modifier: Modifier = Modifier,
    shape: Shape = MaterialTheme.shapes.extraSmall,
    colors: CardColors = CardDefaults.elevatedCardColors(MaterialTheme.colorScheme.surface),
    elevation: CardElevation = CardDefaults.cardElevation(),
    border: BorderStroke? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    AbsoluteElevation {
        Card(modifier, shape, colors, elevation, border, content)
    }
}
