@file:Suppress("DuplicatedCode")

package com.eimsound.daw.components

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import com.eimsound.audioprocessor.PlayPosition
import com.eimsound.audioprocessor.convertPPQToSeconds
import com.eimsound.dsp.data.AudioThumbnail
import com.eimsound.dsp.data.EnvelopePointList
import org.jetbrains.skia.*
import kotlin.math.absoluteValue
import kotlin.math.ceil

private const val STEP_IN_PX = 1F
private const val HALF_STEP_IN_PX = STEP_IN_PX / 2
private const val WAVEFORM_DAMPING = 0.94F
private const val REDUNDANT = 50

private fun drawZero(x: Float, y: Float, buffer: FloatArray, offset: Int) {
    var i = offset
    buffer[i++] = x
    buffer[i++] = y + HALF_STEP_IN_PX
    buffer[i++] = x + HALF_STEP_IN_PX
    buffer[i++] = y - HALF_STEP_IN_PX
    buffer[i++] = x + STEP_IN_PX
    buffer[i++] = y + HALF_STEP_IN_PX

    buffer[i++] = x
    buffer[i++] = y + HALF_STEP_IN_PX
    buffer[i++] = x + STEP_IN_PX
    buffer[i++] = y + HALF_STEP_IN_PX
    buffer[i++] = x + HALF_STEP_IN_PX
    buffer[i] = y - HALF_STEP_IN_PX
}

private fun drawMinAndMax(x: Float, y: Float, min: Float, max: Float, buffer: FloatArray, offset: Int) {
    if (min + max < STEP_IN_PX) {
        drawZero(x, y, buffer, offset)
        return
    }
    var i = offset
    buffer[i++] = x
    buffer[i++] = y - max
    buffer[i++] = x
    buffer[i++] = y + min
    buffer[i++] = x + STEP_IN_PX
    buffer[i++] = y + min

    buffer[i++] = x
    buffer[i++] = y - max
    buffer[i++] = x + STEP_IN_PX
    buffer[i++] = y - max
    buffer[i++] = x + STEP_IN_PX
    buffer[i] = y + min
}

private fun drawDefault(x: Float, y: Float, v: Float, buffer: FloatArray, offset: Int) {
    if (v.absoluteValue < STEP_IN_PX) {
        drawZero(x, y, buffer, offset)
        return
    }
    var i = offset
    buffer[i++] = x
    buffer[i++] = y - v
    buffer[i++] = x
    buffer[i++] = y + HALF_STEP_IN_PX
    buffer[i++] = x + STEP_IN_PX
    buffer[i++] = y + HALF_STEP_IN_PX

    buffer[i++] = x
    buffer[i++] = y - v
    buffer[i++] = x + STEP_IN_PX
    buffer[i++] = y + HALF_STEP_IN_PX
    buffer[i++] = x + STEP_IN_PX
    buffer[i] = y - v
}

private fun Canvas.drawMinAndMax(
    thumbnail: AudioThumbnail, startSeconds: Float, endSeconds: Float,
    channelHeight: Float, halfChannelHeight: Float, drawHalfChannelHeight: Float,
    paint: Paint, width: Float
) {
    val buffer = FloatArray(ceil(width / STEP_IN_PX + REDUNDANT).toInt() * 12)
    repeat(thumbnail.channels) { ch ->
        var min = 0F
        var max = 0F
        var i = 0
        thumbnail.query(ch, width, startSeconds, endSeconds, STEP_IN_PX) { x, min0, max0 ->
            val y = 2 + channelHeight * ch + halfChannelHeight
            val curMax = max0.absoluteValue.coerceAtMost(1F) * drawHalfChannelHeight
            val curMin = min0.absoluteValue.coerceAtMost(1F) * drawHalfChannelHeight
            if (curMin > min) min = curMin
            else min *= WAVEFORM_DAMPING
            if (curMax > max) max = curMax
            else max *= WAVEFORM_DAMPING
            drawMinAndMax(x, y, min, max, buffer, i)
            i += 12
        }
        drawVertices(VertexMode.TRIANGLES, buffer, null, null, null, BlendMode.SRC, paint)
    }
}
private fun Canvas.drawDefault(
    thumbnail: AudioThumbnail, startSeconds: Float, endSeconds: Float,
    channelHeight: Float, halfChannelHeight: Float, drawHalfChannelHeight: Float,
    paint: Paint, width: Float
) {
    val buffer = FloatArray(ceil(width / STEP_IN_PX + REDUNDANT).toInt() * 12)
    repeat(thumbnail.channels) { ch ->
        var i = 0
        thumbnail.query(ch, width, startSeconds, endSeconds, STEP_IN_PX) { x, min, max ->
            val v = (if (max.absoluteValue > min.absoluteValue) max else min).coerceIn(-1F, 1F) * drawHalfChannelHeight
            val y = 2 + channelHeight * ch + halfChannelHeight
            drawDefault(x, y, v, buffer, i)
            i += 12
        }
        drawVertices(VertexMode.TRIANGLES, buffer, null, null, null, BlendMode.SRC, paint)
    }
}

@Composable
fun Waveform(
    thumbnail: AudioThumbnail,
    startSeconds: Float = 0F,
    endSeconds: Float = thumbnail.lengthInSamples / thumbnail.sampleRate,
    color: Color = MaterialTheme.colorScheme.primary,
    isDrawMinAndMax: Boolean = true,
    modifier: Modifier = Modifier
) {
    thumbnail.read()
    val paint = remember { Paint() }
    remember(color) { paint.colorFilter = ColorFilter.makeBlend(color.toArgb(), BlendMode.SRC_IN) }
    Spacer(modifier.fillMaxSize().drawBehind {
        val channelHeight = (size.height / thumbnail.channels) - 2
        val halfChannelHeight = channelHeight / 2
        val drawHalfChannelHeight = halfChannelHeight - 1
        if (isDrawMinAndMax) {
            drawContext.canvas.nativeCanvas.drawMinAndMax(
                thumbnail, startSeconds, endSeconds, channelHeight, halfChannelHeight,
                drawHalfChannelHeight, paint, size.width
            )
        } else {
            drawContext.canvas.nativeCanvas.drawDefault(
                thumbnail, startSeconds, endSeconds, channelHeight, halfChannelHeight,
                drawHalfChannelHeight, paint, size.width
            )
        }
    }.graphicsLayer {  })
}

private fun Canvas.drawMinAndMax(
    thumbnail: AudioThumbnail, startPPQ: Float, startSeconds: Float,
    endSeconds: Float, channelHeight: Float, halfChannelHeight: Float, drawHalfChannelHeight: Float,
    stepPPQ: Float, volumeEnvelope: EnvelopePointList?, width: Float, paint: Paint
) {
    val buffer = FloatArray(ceil(width / STEP_IN_PX + REDUNDANT).toInt() * 12)
    repeat(thumbnail.channels) { ch ->
        var min = 0F
        var max = 0F
        var i = 0
        thumbnail.query(ch, width, startSeconds, endSeconds, STEP_IN_PX) { x, min0, max0 ->
            val y = 2 + channelHeight * ch + halfChannelHeight
            val volume = volumeEnvelope?.getValue((startPPQ + x * stepPPQ).toInt(), 1F) ?: 1F
            val curMin = (min0.absoluteValue * volume).coerceAtMost(1F) * drawHalfChannelHeight
            val curMax = (max0.absoluteValue * volume).coerceAtMost(1F) * drawHalfChannelHeight
            if (curMin > min) min = curMin
            else min *= WAVEFORM_DAMPING
            if (curMax > max) max = curMax
            else max *= WAVEFORM_DAMPING
            drawMinAndMax(x, y, min, max, buffer, i)
            i += 12
        }
        drawVertices(VertexMode.TRIANGLES, buffer, null, null, null, BlendMode.SRC, paint)
    }
}
private fun Canvas.drawDefault(
    thumbnail: AudioThumbnail, startPPQ: Float, startSeconds: Float,
    endSeconds: Float, channelHeight: Float, halfChannelHeight: Float, drawHalfChannelHeight: Float,
    stepPPQ: Float, volumeEnvelope: EnvelopePointList?, width: Float, paint: Paint
) {
    val buffer = FloatArray(ceil(width / STEP_IN_PX + REDUNDANT).toInt() * 12)
    repeat(thumbnail.channels) { ch ->
        var i = 0
        thumbnail.query(ch, width, startSeconds, endSeconds, STEP_IN_PX) { x, min, max ->
            val v = ((if (max.absoluteValue > min.absoluteValue) max else min) *
                    (volumeEnvelope?.getValue((startPPQ + x * stepPPQ).toInt(), 1F) ?: 1F))
                .coerceIn(-1F, 1F) * drawHalfChannelHeight
            val y = 2 + channelHeight * ch + halfChannelHeight
            drawDefault(x, y, v, buffer, i)
            i += 12
        }
        drawVertices(VertexMode.TRIANGLES, buffer, null, null, null, BlendMode.SRC, paint)
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
    Spacer(modifier.fillMaxSize().drawBehind {
        val channelHeight = (size.height / thumbnail.channels) - 2F
        val halfChannelHeight = channelHeight / 2
        val drawHalfChannelHeight = halfChannelHeight - 1
        val stepPPQ = widthPPQ / size.width
        val factor = (thumbnail.sampleRate / position.sampleRate) * timeScale
        val startSeconds = (position.convertPPQToSeconds(startPPQ) / factor).toFloat()
        val endSeconds = (position.convertPPQToSeconds(startPPQ + widthPPQ) / factor).toFloat()
        if (isDrawMinAndMax) {
            drawContext.canvas.nativeCanvas.drawMinAndMax(
                thumbnail, startPPQ, startSeconds, endSeconds, channelHeight, halfChannelHeight,
                drawHalfChannelHeight, stepPPQ, volumeEnvelope, size.width, paint
            )
        } else {
            drawContext.canvas.nativeCanvas.drawDefault(
                thumbnail, startPPQ, startSeconds, endSeconds, channelHeight, halfChannelHeight,
                drawHalfChannelHeight, stepPPQ, volumeEnvelope, size.width, paint
            )
        }
    }.graphicsLayer {  })
}
