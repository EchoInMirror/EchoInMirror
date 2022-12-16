package cn.apisium.eim.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp

@Composable
fun SegmentedButtons(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Row(
        horizontalArrangement = Arrangement.Center,
        modifier = modifier
            .clip(RoundedCornerShape(percent = 100))
            .height(20.dp)
            .border(
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface),
                shape = RoundedCornerShape(percent = 100)
            )
    ) {
        content()
    }
}

@Composable
fun SegmentedDivider(modifier: Modifier = Modifier) = Divider(modifier.fillMaxHeight().width(1.dp), color = MaterialTheme.colorScheme.onSurface)

@Composable
fun SegmentedButton(
    onClick: () -> Unit,
    active: Boolean = false,
    showIcon: Boolean = true,
    modifier: Modifier = Modifier,
    enable: Boolean = true,
    content: @Composable () -> Unit
) {
    val fn = @Composable {
        ProvideTextStyle(value = MaterialTheme.typography.labelLarge) {
            Row(Modifier.fillMaxHeight().padding(horizontal = 6.dp), Arrangement.Center, Alignment.CenterVertically) {
                if (active && showIcon) Icon(Icons.Filled.Check, null)
                content()
            }
        }
    }
    Box(modifier
        .let { if (active) it.background(MaterialTheme.colorScheme.secondaryContainer) else it }
        .clickable(enable, onClick = onClick)
    ) {
        if (active) CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.secondary, content = fn)
        else fn()
    }
}
