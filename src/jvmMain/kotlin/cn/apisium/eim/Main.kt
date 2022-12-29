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
