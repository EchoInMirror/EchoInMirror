package com.eimsound.daw.components

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.staticCompositionLocalOf

val LocalSnackbarHost = staticCompositionLocalOf { SnackbarHostState() }
