package com.eimsound.daw.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eimsound.daw.utils.toOnSurfaceColor

@Composable
fun Avatar(color: Color? = null, modifier: Modifier = Modifier, shape: Shape = CircleShape,
           content: @Composable BoxScope.() -> Unit) {
    ProvideTextStyle(TextStyle(fontSize = 18.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp, textAlign = TextAlign.Center)) {
        if (color == null) {
            Surface(modifier.size(40.dp, 40.dp), shape = shape, tonalElevation = 4.dp) {
                Box(Modifier.fillMaxSize(), Alignment.Center, content = content)
            }
        } else {
            Surface(modifier.size(40.dp, 40.dp), shape = shape, tonalElevation = 6.dp, color = color) {
                CompositionLocalProvider(LocalContentColor provides color.toOnSurfaceColor()) {
                    Box(Modifier.fillMaxSize(), Alignment.Center, content = content)
                }
            }
        }
    }
}
