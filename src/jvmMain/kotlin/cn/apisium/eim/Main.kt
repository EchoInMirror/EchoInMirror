package cn.apisium.eim

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import cn.apisium.eim.components.eimAppBar
import cn.apisium.eim.components.sideBar
import cn.apisium.eim.components.statusBar
import kotlinx.serialization.json.JsonPrimitive
import java.io.InputStream
import java.io.OutputStream
import java.nio.file.Files
import java.nio.file.Paths
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioSystem
import kotlin.math.sin

var hz = 440.0

const val bufferSize = 1024
const val channels = 2
const val bits = 2
const val sampleRate = 44800F
const val bpm = 140.0
var currentPosition = 0L
var currentTime = 0F
val buffers = arrayOf(FloatArray(bufferSize), FloatArray(bufferSize))
val outputBuffer = ByteArray(channels * bufferSize * bits)

var currentAngle = 0.0
var isBigEndian = false
const val volume = 0.01F

private val writeBuffer = ByteArray(8)
fun InputStream.readInt() = if (isBigEndian) (read() shl 24) or (read() shl 16) or (read() shl 8) or read()
else read() or (read() shl 8) or (read() shl 16) or (read() shl 24)
fun InputStream.readFloat() = Float.fromBits(readInt())
fun OutputStream.writeInt(value: Int) {
    if (isBigEndian) {
        writeBuffer[0] = (value ushr 24).toByte()
        writeBuffer[1] = (value ushr 16).toByte()
        writeBuffer[2] = (value ushr 8).toByte()
        writeBuffer[3] = value.toByte()
    } else {
        writeBuffer[3] = (value ushr 24).toByte()
        writeBuffer[2] = (value ushr 16).toByte()
        writeBuffer[1] = (value ushr 8).toByte()
        writeBuffer[0] = value.toByte()
    }
    write(writeBuffer, 0, 4)
}
fun OutputStream.writeLong(value: Long) {
    if (isBigEndian) {
        writeBuffer[0] = (value ushr 56).toByte()
        writeBuffer[1] = (value ushr 48).toByte()
        writeBuffer[2] = (value ushr 40).toByte()
        writeBuffer[3] = (value ushr 32).toByte()
        writeBuffer[4] = (value ushr 24).toByte()
        writeBuffer[5] = (value ushr 16).toByte()
        writeBuffer[6] = (value ushr 8).toByte()
        writeBuffer[7] = value.toByte()
    } else {
        writeBuffer[7] = (value ushr 56).toByte()
        writeBuffer[6] = (value ushr 48).toByte()
        writeBuffer[5] = (value ushr 40).toByte()
        writeBuffer[4] = (value ushr 32).toByte()
        writeBuffer[3] = (value ushr 24).toByte()
        writeBuffer[2] = (value ushr 16).toByte()
        writeBuffer[1] = (value ushr 8).toByte()
        writeBuffer[0] = value.toByte()
    }
    write(writeBuffer, 0, 8)
}
fun OutputStream.writeFloat(value: Float) = writeInt(java.lang.Float.floatToIntBits(value))
fun OutputStream.writeDouble(value: Double) = writeLong(java.lang.Double.doubleToLongBits(value))

@OptIn(ExperimentalMaterial3Api::class)
fun main() = application {
    val icon = painterResource("logo.png")
    Window(onCloseRequest = ::exitApplication, icon = icon, title = "Echo In Mirror") {
        MaterialTheme {
            Row {
                sideBar()
                Scaffold(
                    topBar = { eimAppBar() },
                    content = {
                        Box(Modifier.fillMaxSize()) {

                        }
                    },
                    bottomBar = { statusBar() }
                )
            }
        }
    }

    val processBuilder = ProcessBuilder("D:\\Cpp\\EIMPluginScanner\\build\\EIMPluginHost_artefacts\\Debug\\EIMPluginHost.exe", " -l",
        JsonPrimitive(Files.readString(Paths.get("./plugin.txt"))).toString())

    processBuilder.redirectError(ProcessBuilder.Redirect.INHERIT)
    val process = processBuilder.start()
    if (process.inputStream.read() != 0) {
        process.destroy()
        throw RuntimeException("Failed to load plugin")
    }
    Runtime.getRuntime().addShutdownHook(Thread(process::destroy))
    val input = process.inputStream
    val out = process.outputStream

    val flag1 = input.read() == 1
    val flag2 = input.read() == 2
    isBigEndian = flag1 && flag2
    val pluginInputChannels = input.readInt()
    val pluginOutputChannels = input.readInt()

    out.write(0)
    out.writeInt(sampleRate.toInt())
    out.writeInt(bufferSize)
    out.flush()

    fun processBlock() {
        for (i in 0 until bufferSize) {
            val samplePos = sin(currentAngle).toFloat() * volume
            currentAngle += 2 * Math.PI * hz / sampleRate

            buffers[0][i] = samplePos
            buffers[1][i] = (sin(currentAngle).toFloat() * 5).coerceAtLeast(1F) * volume
        }

        out.write(1)
        out.writeDouble(bpm)
        out.writeLong(currentPosition)
        for (i in 0 until channels) {
            for (j in 0 until bufferSize) {
                out.writeFloat(buffers[i][j])
            }
        }
        for (i in 0 until pluginInputChannels - channels) {
            for (j in 0 until bufferSize) {
                out.writeFloat(0F)
            }
        }
        out.flush()

        input.read()
        for (i in 0 until channels) {
            for (j in 0 until bufferSize) {
                buffers[i][j] = input.readFloat()
            }
        }
        for (i in 0 until pluginOutputChannels - channels) {
            for (j in 0 until bufferSize) {
                input.readFloat()
            }
        }

        for (j in 0 until channels) {
            for (i in 0 until bufferSize) {
                var value = (buffers[j][i] * 32767).toInt()
                for (k in 0 until bits) {
                    outputBuffer[i * channels * bits + j * bits + k] = value.toByte()
                    value = value shr 8
                }
            }
        }
        currentPosition += bufferSize
        currentTime = currentPosition / sampleRate
    }

    Thread {
        val af = AudioFormat(AudioFormat.Encoding.PCM_SIGNED, sampleRate, bits * 8, channels, bits * channels, sampleRate, false)

        val sdl = AudioSystem.getSourceDataLine(af)
        sdl.open(af, channels * bufferSize * 2)
        sdl.start()

        while (true) {
            processBlock()
            sdl.write(outputBuffer, 0, channels * bufferSize * bits)
        }
    }.start()
}
