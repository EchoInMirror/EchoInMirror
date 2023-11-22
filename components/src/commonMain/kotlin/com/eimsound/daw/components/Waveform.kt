@file:Suppress("DuplicatedCode")

package com.eimsound.daw.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import com.eimsound.audioprocessor.CurrentPosition
import com.eimsound.audioprocessor.convertPPQToSeconds
import com.eimsound.audioprocessor.data.AudioThumbnail
import com.eimsound.audioprocessor.data.EnvelopePointList
import kotlin.math.absoluteValue

private const val STEP_IN_PX = 0.5F
private const val WAVEFORM_DAMPING = 0.93F

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
            var min = 0F
            var max = 0F
            thumbnail.query(size.width.toDouble(), startSeconds, endSeconds, STEP_IN_PX) { x, ch, min0, max0 ->
                val y = 2 + channelHeight * ch + halfChannelHeight
                val curMax = max0.absoluteValue * drawHalfChannelHeight
                val curMin = min0.absoluteValue * drawHalfChannelHeight
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
        } else {
            thumbnail.query(size.width.toDouble(), startSeconds, endSeconds, STEP_IN_PX) { x, ch, min, max ->
                val v = (if (max.absoluteValue > min.absoluteValue) max else min) * drawHalfChannelHeight
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
    }
}

@Composable
fun Waveform(
    thumbnail: AudioThumbnail, position: CurrentPosition,
    startPPQ: Float, widthPPQ: Float,
    volumeEnvelope: EnvelopePointList? = null,
    color: Color = MaterialTheme.colorScheme.primary,
    isDrawMinAndMax: Boolean = true,
    modifier: Modifier = Modifier
) {
    val startSeconds = position.convertPPQToSeconds(startPPQ)
    val endSeconds = position.convertPPQToSeconds(startPPQ + widthPPQ)
    Canvas(modifier.fillMaxSize().graphicsLayer { }) {
        val channelHeight = (size.height / thumbnail.channels) - 2
        val halfChannelHeight = channelHeight / 2
        val drawHalfChannelHeight = halfChannelHeight - 1
        val stepPPQ = widthPPQ / size.width
        volumeEnvelope?.read()
        if (isDrawMinAndMax) {
            var min = 0F
            var max = 0F
            thumbnail.query(size.width.toDouble(), startSeconds, endSeconds, STEP_IN_PX) { x, ch, min0, max0 ->
                val y = 2 + channelHeight * ch + halfChannelHeight
                val volume = volumeEnvelope?.getValue((startPPQ + x * stepPPQ).toInt(), 1F) ?: 1F
                val curMin = min0.absoluteValue * volume * drawHalfChannelHeight
                val curMax = max0.absoluteValue * volume * drawHalfChannelHeight
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
        } else {
            thumbnail.query(size.width.toDouble(), startSeconds, endSeconds, STEP_IN_PX) { x, ch, min, max ->
                val v = (if (max.absoluteValue > min.absoluteValue) max else min) *
                        (volumeEnvelope?.getValue((startPPQ + x * stepPPQ).toInt(), 1F) ?: 1F) * drawHalfChannelHeight
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
    }
}
