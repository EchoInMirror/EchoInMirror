package com.eimsound.daw.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Error
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.TaskAlt
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEachReversed
import com.eimsound.daw.components.utils.*
import kotlinx.coroutines.delay

enum class SnackbarType(val icon: ImageVector) {
    Info(Icons.Outlined.Info),
    Error(Icons.Outlined.Error),
    Warning(Icons.Outlined.Warning),
    Success(Icons.Outlined.TaskAlt);

    @Composable
    fun toColor() = when (this) {
        Info -> MaterialTheme.colorScheme.primary
        Error -> MaterialTheme.colorScheme.absoluteError
        Warning -> MaterialTheme.colorScheme.warning
        Success -> MaterialTheme.colorScheme.success
    }
}

private const val DEFAULT_DURATION = 10000

private class Snackbar(
    val type: SnackbarType = SnackbarType.Info,
    val duration: Int = DEFAULT_DURATION,
    val content: @Composable RowScope.() -> Unit
) {
    var isVisible by mutableStateOf(false)
}

val GlobalSnackbarProvider = SnackbarProvider()
val LocalSnackbarProvider = staticCompositionLocalOf { GlobalSnackbarProvider }

@Composable
private fun SnackbarAnim(snackbar: Snackbar) {
    val height = remember { arrayOf(0) }
    val anim by animateFloatAsState(
        if (height[0] == 0 || snackbar.isVisible) 1F else 0F,
        spring(stiffness = Spring.StiffnessMediumLow)
    )
    AnimatedVisibility(snackbar.isVisible,
        Modifier.onPlaced { if (it.size.height > height[0]) height[0] = it.size.height }
            .run { if (height[0] > 0) height(LocalDensity.current.run { (height[0] * anim).toDp() }) else this },
        enter = slideInHorizontally { it },
        exit = slideOutHorizontally { it }
    ) {
        SnackbarItem(snackbar)
    }
}

@Composable
private fun SnackbarItem(snackbar: Snackbar) {
    val color = snackbar.type.toColor()
    Surface(Modifier.padding(8.dp), MaterialTheme.shapes.small, color, color.toOnSurfaceColor(), 16.dp, 8.dp) {
        Row(Modifier.padding(8.dp, 8.dp, 16.dp, 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(snackbar.type.icon, snackbar.type.name, Modifier.padding(end = 8.dp))
            snackbar.content(this)
        }
    }
}

class SnackbarProvider {
    private var snackbars by mutableStateOf<List<Snackbar>>(listOf())

    fun enqueueSnackbar(type: SnackbarType = SnackbarType.Info, duration: Int = DEFAULT_DURATION,
                        content: @Composable RowScope.() -> Unit) {
        snackbars += Snackbar(type, duration, content)
    }

    fun enqueueSnackbar(content: String, type: SnackbarType = SnackbarType.Info, duration: Int = DEFAULT_DURATION) {
        snackbars += Snackbar(type, duration) { Text(content) }
    }

    fun enqueueSnackbar(error: Throwable, duration: Int = DEFAULT_DURATION) {
        snackbars += Snackbar(SnackbarType.Error, duration) { Text("发生错误: ${error.message}") }
    }

    @Composable
    fun SnackbarsContainer(modifier: Modifier = Modifier) {
        Column(modifier.widthIn(max = 500.dp), horizontalAlignment = Alignment.End) {
            snackbars.fastForEachReversed { snackbar ->
                key(snackbar) {
                    LaunchedEffect(snackbar) {
                        snackbar.isVisible = true
                        delay(snackbar.duration.toLong())
                        snackbar.isVisible = false
                        delay(400)
                        snackbars -= snackbar
                    }
                    SnackbarAnim(snackbar)
                }
            }
        }
    }
}
