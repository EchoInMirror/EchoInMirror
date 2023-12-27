@file:Suppress("DuplicatedCode")

package com.eimsound.daw.components

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.eimsound.audioprocessor.PlayPosition
import com.eimsound.audioprocessor.convertPPQToSeconds
import com.eimsound.daw.components.utils.drawRectNative
import com.eimsound.daw.components.utils.NativePainter
import com.eimsound.dsp.data.AudioThumbnail
import com.eimsound.dsp.data.EnvelopePointList
import org.jetbrains.skia.BlendMode
import org.jetbrains.skia.Canvas
import org.jetbrains.skia.ColorFilter
import org.jetbrains.skia.Paint
import kotlin.math.absoluteValue
import kotlin.time.measureTime

private const val STEP_IN_PX = 0.5F
private const val HALF_STEP_IN_PX = STEP_IN_PX / 2
private const val WAVEFORM_DAMPING = 0.93F

private fun Canvas.drawMinAndMax(
    thumbnail: AudioThumbnail, startSeconds: Double, endSeconds: Double,
    channelHeight: Float, halfChannelHeight: Float, drawHalfChannelHeight: Float,
    paint: Paint, width: Float
) {
    var min = 0F
    var max = 0F
    thumbnail.query(width, startSeconds, endSeconds, STEP_IN_PX) { x, ch, min0, max0 ->
        val y = 2 + channelHeight * ch + halfChannelHeight
        val curMax = max0.absoluteValue.coerceAtMost(1F) * drawHalfChannelHeight
        val curMin = min0.absoluteValue.coerceAtMost(1F) * drawHalfChannelHeight
        if (curMin > min) min = curMin
        else min *= WAVEFORM_DAMPING
        if (curMax > max) max = curMax
        else max *= WAVEFORM_DAMPING
        if (min + max < 0.3F) {
            drawRectNative(x, y - HALF_STEP_IN_PX,x + STEP_IN_PX, y + HALF_STEP_IN_PX, paint)
            return@query
        }
        drawRectNative(x, y - max, x + STEP_IN_PX, y + min, paint)
    }
}
private fun Canvas.drawDefault(
    thumbnail: AudioThumbnail, startSeconds: Double, endSeconds: Double,
    channelHeight: Float, halfChannelHeight: Float, drawHalfChannelHeight: Float,
    paint: Paint, width: Float
) {
    thumbnail.query(width, startSeconds, endSeconds, STEP_IN_PX) { x, ch, min, max ->
        val v = (if (max.absoluteValue > min.absoluteValue) max else min).coerceIn(-1F, 1F) * drawHalfChannelHeight
        val y = 2 + channelHeight * ch + halfChannelHeight
        if (v.absoluteValue < 0.3F) {
            drawRectNative(x, y - HALF_STEP_IN_PX, x + STEP_IN_PX, y + HALF_STEP_IN_PX, paint)
            return@query
        }
        if (v > 0) drawRectNative(x, y - v, x + STEP_IN_PX, y, paint)
        else drawRectNative(x, y, x + STEP_IN_PX, y - v, paint)
    }
}

@Composable
fun Waveform(
    thumbnail: AudioThumbnail,
    startSeconds: Double = 0.0,
    endSeconds: Double = thumbnail.lengthInSamples.toDouble() / thumbnail.sampleRate,
    color: Color = MaterialTheme.colorScheme.primary,
    isDrawMinAndMax: Boolean = true,
    modifier: Modifier = Modifier
) {
    thumbnail.read()
    val paint = remember { Paint() }
    remember(color) { paint.colorFilter = ColorFilter.makeBlend(color.toArgb(), BlendMode.SRC_IN) }
    NativePainter(modifier.fillMaxSize()) { size ->
        val channelHeight = (size.height / thumbnail.channels) - 2
        val halfChannelHeight = channelHeight / 2
        val drawHalfChannelHeight = halfChannelHeight - 1
        if (isDrawMinAndMax) {
            drawMinAndMax(
                thumbnail, startSeconds, endSeconds, channelHeight, halfChannelHeight,
                drawHalfChannelHeight, paint, size.width
            )
        } else {
            drawDefault(
                thumbnail, startSeconds, endSeconds, channelHeight, halfChannelHeight,
                drawHalfChannelHeight, paint, size.width
            )
        }
    }
}

private fun Canvas.drawMinAndMax(
    thumbnail: AudioThumbnail, startPPQ: Float, startSeconds: Double,
    endSeconds: Double, channelHeight: Float, halfChannelHeight: Float, drawHalfChannelHeight: Float,
    stepPPQ: Float, volumeEnvelope: EnvelopePointList?, width: Float, paint: Paint
) {
    var min = 0F
    var max = 0F
    thumbnail.query(width, startSeconds, endSeconds, STEP_IN_PX) { x, ch, min0, max0 ->
        val y = 2 + channelHeight * ch + halfChannelHeight
        val volume = volumeEnvelope?.getValue((startPPQ + x * stepPPQ).toInt(), 1F) ?: 1F
        val curMin = (min0.absoluteValue * volume).coerceAtMost(1F) * drawHalfChannelHeight
        val curMax = (max0.absoluteValue * volume).coerceAtMost(1F) * drawHalfChannelHeight
        if (curMin > min) min = curMin
        else min *= WAVEFORM_DAMPING
        if (curMax > max) max = curMax
        else max *= WAVEFORM_DAMPING
        if (min + max < 0.3F) {
            drawRectNative(x, y - HALF_STEP_IN_PX, x + STEP_IN_PX, y + HALF_STEP_IN_PX, paint)
            return@query
        }
        drawRectNative(x, y - max, x + STEP_IN_PX, y + min, paint)
    }
}
private fun Canvas.drawDefault(
    thumbnail: AudioThumbnail, startPPQ: Float, startSeconds: Double,
    endSeconds: Double, channelHeight: Float, halfChannelHeight: Float, drawHalfChannelHeight: Float,
    stepPPQ: Float, volumeEnvelope: EnvelopePointList?, width: Float, paint: Paint
) {
    thumbnail.query(width, startSeconds, endSeconds, STEP_IN_PX) { x, ch, min, max ->
        val v = ((if (max.absoluteValue > min.absoluteValue) max else min) *
                (volumeEnvelope?.getValue((startPPQ + x * stepPPQ).toInt(), 1F) ?: 1F))
            .coerceIn(-1F, 1F) * drawHalfChannelHeight
        val y = 2 + channelHeight * ch + halfChannelHeight
        if (v.absoluteValue < 0.3F) {
            drawRectNative(x, y - HALF_STEP_IN_PX, x + STEP_IN_PX, y + HALF_STEP_IN_PX, paint)
            return@query
        }
        if (v > 0) drawRectNative(x, y - v, x + STEP_IN_PX, y, paint)
        else drawRectNative(x, y, x + STEP_IN_PX, y - v, paint)
    }
}

@Composable
fun Waveform(
    thumbnail: AudioThumbnail, position: PlayPosition,
    startPPQ: Float, widthPPQ: Float, timeScale: Float = 1F,
    volumeEnvelope: EnvelopePointList? = null,
    color: Color = MaterialTheme.colorScheme.primary,
    isDrawMinAndMax: Boolean = true,
    modifier: Modifier = Modifier
) {
    volumeEnvelope?.read()
    thumbnail.read()
    val paint = remember { Paint() }
    remember(color) { paint.colorFilter = ColorFilter.makeBlend(color.toArgb(), BlendMode.SRC_IN) }
    NativePainter(modifier.fillMaxSize()) { size ->
        val channelHeight = (size.height / thumbnail.channels) - 2F
        val halfChannelHeight = channelHeight / 2
        val drawHalfChannelHeight = halfChannelHeight - 1
        val stepPPQ = widthPPQ / size.width
        val factor = (thumbnail.sampleRate / position.sampleRate) * timeScale
        val startSeconds = position.convertPPQToSeconds(startPPQ) / factor
        val endSeconds = position.convertPPQToSeconds(startPPQ + widthPPQ) / factor
        println(measureTime {
            if (isDrawMinAndMax) {
                drawMinAndMax(
                    thumbnail, startPPQ, startSeconds, endSeconds, channelHeight, halfChannelHeight,
                    drawHalfChannelHeight, stepPPQ, volumeEnvelope, size.width, paint
                )
            } else {
                drawDefault(
                    thumbnail, startPPQ, startSeconds, endSeconds, channelHeight, halfChannelHeight,
                    drawHalfChannelHeight, stepPPQ, volumeEnvelope, size.width, paint
                )
            }
        })
    }
}
