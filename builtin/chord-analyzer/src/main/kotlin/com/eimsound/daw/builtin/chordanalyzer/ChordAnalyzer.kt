package com.eimsound.daw.builtin.chordanalyzer

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.BufferedWriter
import java.nio.file.Path
import kotlin.io.path.absolutePathString

data class Chords(
    val chords: List<String>,
    val durations: List<Float>
)

interface ChordAnalyzer : AutoCloseable {
    suspend fun analyze(chord: List<String>, starts: List<Int>, durations: List<Int>): Chords
}

class ChordAnalyzerImpl(file: Path) : ChordAnalyzer {
    private val mutex = Mutex()
    private val input: BufferedReader
    private val output: BufferedWriter
    private val process = ProcessBuilder(file.absolutePathString(), "chords").run {
        redirectError()
        start().apply {
            input = inputStream.bufferedReader()
            output = outputStream.bufferedWriter()
        }
    }

    override suspend fun analyze(chord: List<String>, starts: List<Int>, durations: List<Int>) = withContext(Dispatchers.IO) {
        mutex.withLock {
//            val gg = StringWriter()
//            val output = BufferedWriter(gg)
            output.write("chords_detect\n")
            output.write(chord.joinToString(","))
            output.write("\n")
            starts.forEachIndexed { i, it ->
                if (i != 0) output.write(",")
                output.write(it.toString())
            }
            output.write("\n")
            durations.forEachIndexed { i, it ->
                if (i != 0) output.write(",")
                output.write(it.toString())
            }
            output.write("\n")
            output.flush()
//            println(gg.buffer.toString())
//            gg.close()

            Chords(
                input.readLine().split(","),
                input.readLine().split(",").map { it.toFloatOrNull() ?: Float.MAX_VALUE }
            )
        }
    }

    override fun close() { process.destroy() }
}
