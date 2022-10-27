import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.compose.ExperimentalComposeLibrary

plugins {
    kotlin("multiplatform")
    id("org.jetbrains.compose")
    kotlin("plugin.serialization") version "1.7.20"
}

group = "com.eimsound"
version = "1.0-SNAPSHOT"

repositories {
    google()
    mavenCentral()
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    maven("https://s01.oss.sonatype.org/content/repositories/releases/")
}

kotlin {
    jvm {
        compilations.all {
            kotlinOptions.jvmTarget = "17"
        }
        withJava()
    }
    sourceSets {
        @Suppress("UNUSED_VARIABLE", "OPT_IN_IS_NOT_ENABLED") val jvmMain by getting {
            dependencies {
                implementation(compose.desktop.currentOs) {
                    exclude("org.jetbrains.compose.material")
                }
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.4.1")
                implementation(compose.materialIconsExtended)
                @OptIn(ExperimentalComposeLibrary::class) implementation(compose.material3)
                @OptIn(ExperimentalComposeLibrary::class) implementation(compose.desktop.components.splitPane)
            }
        }
    }
}

compose.desktop {
    application {
        mainClass = "MainKt"
        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "EchoInMirror"
            packageVersion = "1.0.0"
        }
    }
}
