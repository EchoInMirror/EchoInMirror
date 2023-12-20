@file:Suppress("DuplicatedCode")

package com.eimsound.daw.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.*
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.unit.IntSize
import com.eimsound.audioprocessor.PlayPosition
import com.eimsound.audioprocessor.convertPPQToSeconds
import com.eimsound.dsp.data.AudioThumbnail
import com.eimsound.dsp.data.EnvelopePointList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.absoluteValue

private const val STEP_IN_PX = 0.5F
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
            drawRect(x, y,x + STEP_IN_PX, y, paint)
            return@query
        }
        drawRect(x, y - max, x + STEP_IN_PX, y + min, paint)
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
            drawRect(x, y, x + STEP_IN_PX, y, paint)
            return@query
        }
        drawRect(x, y, x + STEP_IN_PX, y - v, paint)
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
    var size: IntSize? by remember { mutableStateOf(null) }
    Box(Modifier.fillMaxSize().onPlaced { size = it.size }) {
        val image by produceState<ImageBitmap?>(
            null, size, thumbnail, thumbnail.read(), startSeconds, endSeconds, isDrawMinAndMax
        ) {
            val curSize = size
            if (curSize == null) {
                value = null
                return@produceState
            }
            withContext(Dispatchers.Default) {
                val bitmap = ImageBitmap(curSize.width, curSize.height)
                val paint = Paint()
                Canvas(bitmap).apply {
                    val channelHeight = (curSize.height.toFloat() / thumbnail.channels) - 2
                    val halfChannelHeight = channelHeight / 2
                    val drawHalfChannelHeight = halfChannelHeight - 1
                    if (isDrawMinAndMax) {
                        drawMinAndMax(
                            thumbnail, startSeconds, endSeconds, channelHeight, halfChannelHeight,
                            drawHalfChannelHeight, paint, curSize.width.toFloat()
                        )
                    } else {
                        drawDefault(
                            thumbnail, startSeconds, endSeconds, channelHeight, halfChannelHeight,
                            drawHalfChannelHeight, paint, curSize.width.toFloat()
                        )
                    }
                }
                value = bitmap
            }
        }
        image?.let { Image(it, "Waveform", modifier, colorFilter = ColorFilter.tint(color)) }
    }
}

private fun Canvas.drawMinAndMax(
    thumbnail: AudioThumbnail, startPPQ: Float, startSeconds: Double,
    endSeconds: Double, channelHeight: Float, halfChannelHeight: Float, drawHalfChannelHeight: Float,
    stepPPQ: Float, volumeEnvelope: EnvelopePointList?, paint: Paint, width: Float
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
            drawRect(x, y, x + STEP_IN_PX, y, paint)
            return@query
        }
        drawRect(x, y - max, x + STEP_IN_PX, y + min, paint)
    }
}
private fun Canvas.drawDefault(
    thumbnail: AudioThumbnail, startPPQ: Float, startSeconds: Double,
    endSeconds: Double, channelHeight: Float, halfChannelHeight: Float, drawHalfChannelHeight: Float,
    stepPPQ: Float, volumeEnvelope: EnvelopePointList?, paint: Paint, width: Float
) {
    thumbnail.query(width, startSeconds, endSeconds, STEP_IN_PX) { x, ch, min, max ->
        val v = ((if (max.absoluteValue > min.absoluteValue) max else min) *
                (volumeEnvelope?.getValue((startPPQ + x * stepPPQ).toInt(), 1F) ?: 1F))
            .coerceIn(-1F, 1F) * drawHalfChannelHeight
        val y = 2 + channelHeight * ch + halfChannelHeight
        if (v.absoluteValue < 0.3F) {
            drawRect(x, y, x + STEP_IN_PX, y, paint)
            return@query
        }
        drawRect(x, y, x + STEP_IN_PX, y - v, paint)
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
    var size: IntSize? by remember { mutableStateOf(null) }
    Box(modifier.fillMaxSize().onPlaced { size = it.size }) {
        val factor = (thumbnail.sampleRate / position.sampleRate) * timeScale
        val startSeconds = position.convertPPQToSeconds(startPPQ) / factor
        val endSeconds = position.convertPPQToSeconds(startPPQ + widthPPQ) / factor
        val image by produceState<ImageBitmap?>(
            null, size, thumbnail, thumbnail.read(), startSeconds, endSeconds, volumeEnvelope, isDrawMinAndMax
        ) {
            val curSize = size
            if (curSize == null) {
                value = null
                return@produceState
            }
            withContext(Dispatchers.Default) {
                val bitmap = ImageBitmap(curSize.width, curSize.height)
                val paint = Paint()
                val channelHeight = (curSize.height / thumbnail.channels) - 2F
                val halfChannelHeight = channelHeight / 2
                val drawHalfChannelHeight = halfChannelHeight - 1
                val stepPPQ = widthPPQ / curSize.width
                Canvas(bitmap).apply {
                    if (isDrawMinAndMax) {
                        drawMinAndMax(
                            thumbnail, startPPQ, startSeconds, endSeconds, channelHeight, halfChannelHeight,
                            drawHalfChannelHeight, stepPPQ, volumeEnvelope, paint, curSize.width.toFloat()
                        )
                    } else {
                        drawDefault(
                            thumbnail, startPPQ, startSeconds, endSeconds, channelHeight, halfChannelHeight,
                            drawHalfChannelHeight, stepPPQ, volumeEnvelope, paint, curSize.width.toFloat()
                        )
                    }
                }
                value = bitmap
            }
        }
        image?.let { Image(it, "Waveform", Modifier.fillMaxSize(), colorFilter = ColorFilter.tint(color)) }
    }
}
