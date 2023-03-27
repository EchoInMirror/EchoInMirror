package com.eimsound.daw.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Error
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.TaskAlt
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
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

    @OptIn(ExperimentalFoundationApi::class)
    @Composable
    fun SnackbarsContainer(modifier: Modifier = Modifier) {
        BoxWithConstraints(modifier) {
            LazyColumn(Modifier.widthIn(max = 500.dp).fillMaxHeight().rotate(180f)) {
                items(snackbars, key = { it }) { snackbar ->
                    LaunchedEffect(snackbar) {
                        snackbar.isVisible = true
                        delay(snackbar.duration.toLong())
                        snackbar.isVisible = false
                        delay(500)
                        snackbars -= snackbar
                    }
                    val color = snackbar.type.toColor()
                    AnimatedVisibility(snackbar.isVisible, Modifier.rotate(180f).animateItemPlacement(),
                        enter = slideInHorizontally { it },
                        exit = slideOutHorizontally { it }
                    ) {
                        Surface(Modifier.padding(8.dp), MaterialTheme.shapes.small, color, color.toOnSurfaceColor(), 16.dp, 8.dp) {
                            Row(Modifier.padding(8.dp, 8.dp, 16.dp, 8.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(snackbar.type.icon, snackbar.type.name, Modifier.padding(end = 8.dp))
                                snackbar.content(this)
                            }
                        }
                    }
                }
            }
        }
    }
}
