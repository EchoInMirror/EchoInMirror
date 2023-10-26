package com.eimsound.daw.dawutils

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.res.ResourceLoader
import androidx.compose.ui.res.loadImageBitmap

@OptIn(ExperimentalComposeUiApi::class)
val Logo = BitmapPainter(loadImageBitmap(ResourceLoader.Default.load("logo@2x.png")))
@OptIn(ExperimentalComposeUiApi::class)
val EIMChan by lazy { BitmapPainter(loadImageBitmap(ResourceLoader.Default.load("eim-chan.png"))) }
