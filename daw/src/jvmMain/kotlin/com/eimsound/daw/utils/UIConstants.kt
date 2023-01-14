package com.eimsound.daw.utils

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.res.ResourceLoader
import androidx.compose.ui.res.loadImageBitmap

@OptIn(ExperimentalComposeUiApi::class)
val Logo = BitmapPainter(loadImageBitmap(ResourceLoader.Default.load("logo.png")))
