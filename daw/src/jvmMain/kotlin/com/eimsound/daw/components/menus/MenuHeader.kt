package com.eimsound.daw.components.menus

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.eimsound.daw.components.Gap

@Composable
fun MenuHeader(title: String, enable: Boolean = true, icon: ImageVector? = null, content: @Composable RowScope.() -> Unit) {
    Row(Modifier.padding(start = 12.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        if (icon != null) {
            Icon(icon, null, Modifier.size(18.dp))
            Gap(4)
        }
        Text(title, Modifier.weight(1F), style = MaterialTheme.typography.titleSmall,
            maxLines = 1, overflow = TextOverflow.Ellipsis,
            textDecoration = if (enable) null else TextDecoration.LineThrough,
            color = LocalContentColor.current.copy(alpha = if (enable) 1F else 0.7F),
            fontWeight = FontWeight.ExtraBold
        )
        content()
    }
}
