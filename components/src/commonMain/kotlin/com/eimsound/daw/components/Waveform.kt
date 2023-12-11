@file:Suppress("DuplicatedCode")

package com.eimsound.daw.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import com.eimsound.audioprocessor.PlayPosition
import com.eimsound.audioprocessor.convertPPQToSeconds
import com.eimsound.dsp.data.AudioThumbnail
import com.eimsound.dsp.data.EnvelopePointList
import kotlin.math.absoluteValue

private const val STEP_IN_PX = 0.5F
private const val WAVEFORM_DAMPING = 0.93F

private fun DrawScope.drawMinAndMax(
    thumbnail: AudioThumbnail, startSeconds: Double, endSeconds: Double,
    channelHeight: Float, halfChannelHeight: Float, drawHalfChannelHeight: Float,
    color: Color
) {
    var min = 0F
    var max = 0F
    thumbnail.query(size.width.toDouble(), startSeconds, endSeconds, STEP_IN_PX) { x, ch, min0, max0 ->
        val y = 2 + channelHeight * ch + halfChannelHeight
        val curMax = max0.absoluteValue.coerceAtMost(1F) * drawHalfChannelHeight
        val curMin = min0.absoluteValue.coerceAtMost(1F) * drawHalfChannelHeight
        if (curMin > min) min = curMin
        else min *= WAVEFORM_DAMPING
        if (curMax > max) max = curMax
        else max *= WAVEFORM_DAMPING
        if (min + max < 0.3F) {
            drawLine(color, Offset(x, y), Offset(x + STEP_IN_PX, y), STEP_IN_PX)
            return@query
        }
        drawLine(
            color,
            Offset(x, y - max),
            Offset(x, y + min),
            0.5F
        )
    }
}
private fun DrawScope.drawDefault(
    thumbnail: AudioThumbnail, startSeconds: Double, endSeconds: Double,
    channelHeight: Float, halfChannelHeight: Float, drawHalfChannelHeight: Float,
    color: Color
) {
    thumbnail.query(size.width.toDouble(), startSeconds, endSeconds, STEP_IN_PX) { x, ch, min, max ->
        val v = (if (max.absoluteValue > min.absoluteValue) max else min).coerceIn(-1F, 1F) * drawHalfChannelHeight
        val y = 2 + channelHeight * ch + halfChannelHeight
        if (v.absoluteValue < 0.3F) {
            drawLine(color, Offset(x, y), Offset(x + STEP_IN_PX, y), STEP_IN_PX)
            return@query
        }
        drawLine(
            color,
            Offset(x, y),
            Offset(x, y - v),
            STEP_IN_PX
        )
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
    Canvas(modifier.fillMaxSize().graphicsLayer { }) {
        val channelHeight = (size.height / thumbnail.channels) - 2
        val halfChannelHeight = channelHeight / 2
        val drawHalfChannelHeight = halfChannelHeight - 1
        if (isDrawMinAndMax) {
            drawMinAndMax(
                thumbnail, startSeconds, endSeconds, channelHeight, halfChannelHeight,
                drawHalfChannelHeight, color
            )
        } else {
            drawDefault(
                thumbnail, startSeconds, endSeconds, channelHeight, halfChannelHeight,
                drawHalfChannelHeight, color
            )
        }
    }
}

private fun DrawScope.drawMinAndMax(
    thumbnail: AudioThumbnail, startPPQ: Float, startSeconds: Double,
    endSeconds: Double, channelHeight: Float, halfChannelHeight: Float, drawHalfChannelHeight: Float,
    stepPPQ: Float, color: Color, volumeEnvelope: EnvelopePointList?
) {
    var min = 0F
    var max = 0F
    thumbnail.query(size.width.toDouble(), startSeconds, endSeconds, STEP_IN_PX) { x, ch, min0, max0 ->
        val y = 2 + channelHeight * ch + halfChannelHeight
        val volume = volumeEnvelope?.getValue((startPPQ + x * stepPPQ).toInt(), 1F) ?: 1F
        val curMin = (min0.absoluteValue * volume).coerceAtMost(1F) * drawHalfChannelHeight
        val curMax = (max0.absoluteValue * volume).coerceAtMost(1F) * drawHalfChannelHeight
        if (curMin > min) min = curMin
        else min *= WAVEFORM_DAMPING
        if (curMax > max) max = curMax
        else max *= WAVEFORM_DAMPING
        if (min + max < 0.3F) {
            drawLine(color, Offset(x, y), Offset(x + STEP_IN_PX, y), STEP_IN_PX)
            return@query
        }
        drawLine(
            color,
            Offset(x, y - max),
            Offset(x, y + min),
            0.5F
        )
    }
}
private fun DrawScope.drawDefault(
    thumbnail: AudioThumbnail, startPPQ: Float, startSeconds: Double,
    endSeconds: Double, channelHeight: Float, halfChannelHeight: Float, drawHalfChannelHeight: Float,
    stepPPQ: Float, color: Color, volumeEnvelope: EnvelopePointList?
) {
    thumbnail.query(size.width.toDouble(), startSeconds, endSeconds, STEP_IN_PX) { x, ch, min, max ->
        val v = ((if (max.absoluteValue > min.absoluteValue) max else min) *
                (volumeEnvelope?.getValue((startPPQ + x * stepPPQ).toInt(), 1F) ?: 1F))
            .coerceIn(-1F, 1F) * drawHalfChannelHeight
        val y = 2 + channelHeight * ch + halfChannelHeight
        if (v.absoluteValue < 0.3F) {
            drawLine(color, Offset(x, y), Offset(x + STEP_IN_PX, y), STEP_IN_PX)
            return@query
        }
        drawLine(
            color,
            Offset(x, y),
            Offset(x, y - v),
            STEP_IN_PX
        )
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
    thumbnail.read()
    val factor = (thumbnail.sampleRate / position.sampleRate) * timeScale
    val startSeconds = position.convertPPQToSeconds(startPPQ) / factor
    val endSeconds = position.convertPPQToSeconds(startPPQ + widthPPQ) / factor
    Canvas(modifier.fillMaxSize().graphicsLayer { }) {
        val channelHeight = (size.height / thumbnail.channels) - 2
        val halfChannelHeight = channelHeight / 2
        val drawHalfChannelHeight = halfChannelHeight - 1
        val stepPPQ = widthPPQ / size.width
        volumeEnvelope?.read()
        if (isDrawMinAndMax) {
            drawMinAndMax(
                thumbnail, startPPQ, startSeconds, endSeconds, channelHeight, halfChannelHeight,
                drawHalfChannelHeight, stepPPQ, color, volumeEnvelope
            )
        } else {
            drawDefault(
                thumbnail, startPPQ, startSeconds, endSeconds, channelHeight, halfChannelHeight,
                drawHalfChannelHeight, stepPPQ, color, volumeEnvelope
            )
        }
    }
}
