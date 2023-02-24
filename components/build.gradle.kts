import org.jetbrains.compose.ComposeBuildConfig
import org.jetbrains.compose.ExperimentalComposeLibrary

plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization") version "1.8.0"
    id("org.jetbrains.compose")
}

kotlin {
    val jvmVersion = extra["jvm.version"] as String
    jvmToolchain(jvmVersion.toInt())
    jvm {
        compilations.all {
            kotlinOptions.jvmTarget = jvmVersion
        }
        withJava()
    }
    sourceSets {
        named("commonMain") {
            dependencies {
                compileOnly(project(":dsp"))
                compileOnly(project(":api"))
                compileOnly(compose.runtime)
                compileOnly(compose.ui)
                compileOnly(compose.foundation)
                compileOnly(compose.materialIconsExtended)
                @OptIn(ExperimentalComposeLibrary::class) implementation(compose.material3)
                implementation("com.github.ajalt.colormath:colormath:3.2.1") {
                    exclude("org.jetbrains.kotlin")
                }
                implementation("org.jetbrains.compose.ui:ui-util:${ComposeBuildConfig.composeVersion}")
                api("cafe.adriel.bonsai:bonsai-core:1.2.0")
                api("cafe.adriel.bonsai:bonsai-file-system:1.2.0")
            }
        }
        named("jvmMain") {

        }
    }
}
