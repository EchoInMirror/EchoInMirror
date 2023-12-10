pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    }

    plugins {
        kotlin("multiplatform").version(extra["kotlin.version"] as String)
        id("org.jetbrains.compose").version(extra["compose.version"] as String)
    }
}

buildscript {
    System.setProperty("kotlin.version", extra["kotlin.version"] as String)
}

rootProject.name = "EchoInMirror"

include(
    ":commons",
    ":components",
    ":utils",
    ":audio-sources",
    ":dsp",
    ":audio-processors",
    ":native",
    ":api",
    ":daw"
)
