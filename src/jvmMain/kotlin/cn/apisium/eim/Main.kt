package cn.apisium.eim

import androidx.compose.foundation.LocalScrollbarStyle
import androidx.compose.foundation.ScrollbarStyle
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.application
import cn.apisium.eim.window.MainWindow
//import kotlinx.coroutines.delay
//import kotlinx.coroutines.launch
//import kotlinx.coroutines.runBlocking
//import cn.apisium.eim.impl.processor.NativeAudioPluginImpl
//import cn.apisium.eim.impl.processor.nativeAudioPluginManager
import javax.swing.UIManager

fun main() {
    UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName())
    createDirectories()
    Runtime.getRuntime().addShutdownHook(Thread(EchoInMirror.bus::close))
    Runtime.getRuntime().addShutdownHook(Thread(EchoInMirror.player::close))

//    runBlocking {
//        println("start render")
//        val renderer = RendererImpl(EchoInMirror.bus)
//        renderer.start(
//            0,
//            1024,
//            EchoInMirror.currentPosition.sampleRate,
//            EchoInMirror.currentPosition.ppq,
//            EchoInMirror.currentPosition.bpm,
//            File("aa.wav"),
//            AudioFileFormat.Type.WAVE
//        ) {
//            println(it)
//        }
//    }


//    if (IS_DEBUG) Thread {
//        runBlocking {
//            launch {
//                delay(2000)
//                var proQ: NativeAudioPluginImpl? = null
//                var spire: NativeAudioPluginImpl? = null
//                EchoInMirror.audioProcessorManager.nativeAudioPluginManager.descriptions.forEach {
//                    if (it.name == "FabFilter Pro-Q 3") proQ = NativeAudioPluginImpl(it)
//                    if (it.name == "Spire-1.5") spire = NativeAudioPluginImpl(it)
//                }
//                proQ!!.launch()
//                spire!!.launch()
//                subTrack2.preProcessorsChain.add(spire!!)
//                track.postProcessorsChain.add(proQ!!)
//
//            }
//        }
//    }.start()
    EchoInMirror.windowManager.openMainWindow()
    application {
        MaterialTheme(if (EchoInMirror.windowManager.isDarkTheme) darkColorScheme() else lightColorScheme()) {
            val color = MaterialTheme.colorScheme.onSurface
            CompositionLocalProvider(
                LocalScrollbarStyle provides ScrollbarStyle(16.dp, 8.dp, RoundedCornerShape(4.dp),
                    300, color.copy(0.26f), color.copy(0.60f))
            ) {
                MainWindow()
            }
        }
    }
}
