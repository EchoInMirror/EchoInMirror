package com.eimsound.dsp

import be.tarsos.dsp.AudioEvent
import be.tarsos.dsp.filters.LowPassFS
import be.tarsos.dsp.io.TarsosDSPAudioFormat
import com.eimsound.audiosources.AudioSource
import kotlin.math.roundToInt

fun detectBPM(source: AudioSource): List<Pair<Int, Float>> {
    var cnt = 0
    val list = groupByTempo(source.sampleRate, identifyIntervals(findPeaks(LowPassedAudioSource(source))))
        .entries.sortedByDescending { it.value }.subList(0, 3)
    list.forEach { cnt += it.value }
    return list.map { Pair(it.key, it.value.toFloat() / cnt) }
}

private fun findPeaks(data: LowPassedAudioSource): List<Long> {
    var peaks = listOf<Long>()
    var threshold = 0.9
    val minThreshold = 0.3
    val minPeaks = 15
    while (peaks.size < minPeaks && threshold >= minThreshold) {
        data.position = 0
        peaks = findPeaksAtThreshold(data, threshold)
        threshold -= 0.05
    }
    if (peaks.size < minPeaks) return emptyList()
    return peaks
}

private fun findPeaksAtThreshold(data: LowPassedAudioSource, threshold: Double): List<Long> {
    val peaks = mutableListOf<Long>()
    while (data.position < data.length) {
        var i = 0
        data.process()
        while (i < data.bufferSize) {
            if (data.data[i] > threshold) {
                peaks.add(data.position + i)
                i += (data.sampleRate / 4).roundToInt()
            }
            i++
        }
        data.position += i
    }
    return peaks
}

private fun identifyIntervals(peaks: List<Long>): Map<Long, Int> {
    val intervals = mutableMapOf<Long, Int>()
    peaks.forEachIndexed { index, peak ->
        for (i in 0..9) {
            if (index + i >= peaks.size) break
            val interval = peaks[index + i] - peak
            intervals[interval] = intervals.getOrDefault(interval, 0) + 1
        }
    }
    return intervals
}

private fun groupByTempo(sampleRate: Float, intervals: Map<Long, Int>): Map<Int, Int> {
    val tempoCounts = mutableMapOf<Int, Int>()
    intervals.forEach { (interval, count) ->
        if (interval != 0L) {
            var theoreticalTempo = (60F / (interval / sampleRate))
            while (theoreticalTempo < 90) theoreticalTempo *= 2
            while (theoreticalTempo > 180) theoreticalTempo /= 2
            val temp = theoreticalTempo.roundToInt()
            tempoCounts[temp] = tempoCounts.getOrDefault(temp, 0) + count
        }
    }
    return tempoCounts
}

private class LowPassedAudioSource(private val target: AudioSource) {
    val bufferSize = 4096
    private val filter = LowPassFS(350F, target.sampleRate)
    private val buffers = Array(target.channels) { FloatArray(bufferSize) }
    private val event = AudioEvent(
        TarsosDSPAudioFormat(target.sampleRate, 2, target.channels, true, false)
    ).apply { floatBuffer = buffers[0] }

    val length = target.length
    var position = 0L
    val data = buffers[0]
    val sampleRate = target.sampleRate

    fun process() {
        target.getSamples(position, bufferSize, buffers)
        if (target.channels > 1) {
            repeat (bufferSize) { i ->
                buffers[0][i] = (buffers[0][i] + buffers[1][i]) / 2
            }
        }
        filter.process(event)
    }
}
