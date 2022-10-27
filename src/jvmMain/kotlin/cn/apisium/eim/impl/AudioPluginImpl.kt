package cn.apisium.eim.impl

import cn.apisium.eim.api.CurrentPosition
import cn.apisium.eim.api.processor.AudioPlugin
import cn.apisium.eim.api.processor.FailedToLoadAudioPluginException
import cn.apisium.eim.api.processor.PluginDescription
import cn.apisium.eim.bufferSize
import cn.apisium.eim.sampleRate
import cn.apisium.eim.utils.EIMInputStream
import cn.apisium.eim.utils.EIMOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive

class AudioPluginImpl(
    override val description: PluginDescription
) : AudioPlugin {
    private var _inputChannelsCount = 0
    private var _outputChannelsCount = 0
    override val inputChannelsCount: Int
        get() = _inputChannelsCount
    override val outputChannelsCount: Int
        get() = _outputChannelsCount
    override var name = description.name
    private var _isLaunched = false
    override val isLaunched
        get() = _isLaunched
    private var process: Process? = null
    private var inputStream: EIMInputStream? = null
    private var outputStream: EIMOutputStream? = null

    override fun processBlock(buffers: Array<FloatArray>, position: CurrentPosition, midiBuffer: ArrayList<Byte>?) {
        if (!_isLaunched) return
        val output = outputStream!!
        output.write(1)
        output.writeBoolean(position.isPlaying)
        output.writeDouble(position.bpm)
        output.writeLong(position.timeInSamples)
        val inputChannels = _inputChannelsCount.coerceAtMost(buffers.size)
        val outputChannels = _outputChannelsCount.coerceAtMost(buffers.size)
        output.write(inputChannels)
        output.write(outputChannels)
        for (i in 0 until inputChannels) buffers[i].forEach(output::writeFloat)
        output.flush()

        val input = inputStream!!
        input.read()
        for (i in 0 until outputChannels) {
            for (j in 0 until buffers[i].size) {
                buffers[i][j] = input.readFloat()
            }
        }
    }

    override fun prepareToPlay() {
        val output = outputStream ?: return
        output.write(0)
        output.writeInt(sampleRate.toInt())
        output.writeInt(bufferSize)
        output.flush()
    }

    override suspend fun launch(): Boolean {
        if (_isLaunched) return true
        return withContext(Dispatchers.IO) {
            val pb = ProcessBuilder("D:\\Cpp\\EIMPluginScanner\\build\\EIMHost_artefacts\\Debug\\EIMHost.exe", " -l",
                JsonPrimitive(Json.encodeToString(PluginDescription.serializer(), description)).toString())

            pb.redirectError(ProcessBuilder.Redirect.INHERIT)
            val p = pb.start()

            val flag1 = p.inputStream.read() == 1
            val flag2 = p.inputStream.read() == 2
            val isBigEndian = flag1 && flag2
            val input = EIMInputStream(isBigEndian, p.inputStream)

            val id = input.read()
            if (id == 127) {
                throw FailedToLoadAudioPluginException(input.readString())
            }
            if (id != 0) {
                p.destroy()
                throw FailedToLoadAudioPluginException("Failed to load plugin")
            }

            p.onExit().thenAccept {
                _isLaunched = false
                process = null
            }

            try {
                val output = EIMOutputStream(isBigEndian, p.outputStream)
                _inputChannelsCount = input.readInt()
                _outputChannelsCount = input.readInt()

                process = p
                inputStream = input
                outputStream = output
                _isLaunched = true
            } catch (e: Throwable) {
                p.destroy()
                throw e
            }
            true
        }
    }

    override fun close() {
        if (process != null) {
            process!!.destroyForcibly()
            process = null
            inputStream = null
            outputStream = null
            _isLaunched = false
        }
    }
}