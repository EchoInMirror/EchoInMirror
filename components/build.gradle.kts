import org.jetbrains.compose.ComposeBuildConfig
import org.jetbrains.compose.ExperimentalComposeLibrary

plugins {
    kotlin("multiplatform")
    id("org.jetbrains.compose")
}

kotlin {
    jvmToolchain(17)
    jvm {
        compilations.all {
            kotlinOptions.jvmTarget = "17"
        }
        withJava()
    }
    @Suppress("OPT_IN_IS_NOT_ENABLED")
    sourceSets {
        named("commonMain") {
            dependencies {
                compileOnly(project(":dsp"))
                compileOnly(compose.runtime)
                compileOnly(compose.ui)
                compileOnly(compose.foundation)
                compileOnly(compose.materialIconsExtended)
                @OptIn(ExperimentalComposeLibrary::class) implementation(compose.material3)
                implementation("com.github.ajalt.colormath:colormath:3.2.1") {
                    exclude("org.jetbrains.kotlin")
                }
                implementation("org.jetbrains.compose.ui:ui-util:${ComposeBuildConfig.composeVersion}")
            }
        }
        named("jvmMain") {

        }
    }
}
