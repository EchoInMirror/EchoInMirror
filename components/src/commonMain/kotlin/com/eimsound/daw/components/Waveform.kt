package com.eimsound.daw.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import com.eimsound.audioprocessor.data.AudioThumbnail
import kotlin.math.absoluteValue

private const val STEP_IN_PX = 0.5F

@Composable
fun Waveform(
    thumbnail: AudioThumbnail,
    startSeconds: Double = 0.0,
    endSeconds: Double = thumbnail.lengthInSamples.toDouble() / thumbnail.sampleRate,
    color: Color = MaterialTheme.colorScheme.primary,
    isDrawMinAndMax: Boolean = true,
    modifier: Modifier = Modifier
) {
    Canvas(modifier.fillMaxSize().graphicsLayer { }) {
        val channelHeight = (size.height / thumbnail.channels) - 2
        val halfChannelHeight = channelHeight / 2
        val drawHalfChannelHeight = halfChannelHeight - 1
        if (isDrawMinAndMax) {
            thumbnail.query(size.width.toDouble(), startSeconds, endSeconds, STEP_IN_PX) { x, ch, min, max ->
                val y = 2 + channelHeight * ch + halfChannelHeight
                if (min == 0F && max == 0F) {
                    drawLine(color, Offset(x, y), Offset(x + STEP_IN_PX, y), STEP_IN_PX)
                    return@query
                }
                drawLine(
                    color,
                    Offset(x, y - max.absoluteValue * drawHalfChannelHeight),
                    Offset(x, y + min.absoluteValue * drawHalfChannelHeight),
                    0.5F
                )
            }
        } else {
            thumbnail.query(size.width.toDouble(), startSeconds, endSeconds, STEP_IN_PX) { x, ch, min, max ->
                val v = if (max.absoluteValue > min.absoluteValue) max else min
                val y = 2 + channelHeight * ch + halfChannelHeight
                if (v == 0F) {
                    drawLine(color, Offset(x, y), Offset(x + STEP_IN_PX, y), STEP_IN_PX)
                    return@query
                }
                drawLine(
                    color,
                    Offset(x, y),
                    Offset(x, y - v * drawHalfChannelHeight),
                    STEP_IN_PX
                )
            }
        }
    }
}
