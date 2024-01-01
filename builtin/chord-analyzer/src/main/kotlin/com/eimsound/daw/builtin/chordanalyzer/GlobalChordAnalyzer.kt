package com.eimsound.daw.builtin.chordanalyzer

import kotlin.io.path.Path

var chordAnalyzerInitialized = false
    private set
val chordAnalyzer: ChordAnalyzer by lazy {
    val fallback = if (System.getProperty("os.name").contains("Windows")) "EIMUtils/EIMUtils.exe"
    else "EIMUtils/EIMUtils"
    ChordAnalyzerImpl(Path(System.getProperty("eim.eimutils.file", fallback))).apply {
        chordAnalyzerInitialized = true
    }
}
