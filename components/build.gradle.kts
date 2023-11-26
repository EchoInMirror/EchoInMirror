import org.jetbrains.compose.ComposeBuildConfig

plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization") version "1.9.20"
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
                compileOnly(project(":audio-processors"))
                compileOnly(project(":api"))
                compileOnly(project(":commons"))
                compileOnly(compose.runtime)
                compileOnly(compose.preview)
                compileOnly(compose.ui)
                compileOnly(compose.foundation)
                compileOnly(compose.materialIconsExtended)
                implementation(compose.material3)
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
