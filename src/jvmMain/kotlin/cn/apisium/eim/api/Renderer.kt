package cn.apisium.eim.api

import java.io.File
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioFileFormat

abstract class Renderer(val formatEncoding: AudioFormat.Encoding, val renderTarget: Renderable) {
    abstract suspend fun start(startPosition: CurrentPosition,endPosition: CurrentPosition, file: File, audioType:AudioFileFormat.Type, callback:(Float)->Unit)
}