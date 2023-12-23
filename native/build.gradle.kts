plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization") version System.getProperty("kotlin.version")
    id("org.jetbrains.compose")
}

repositories {
    mavenCentral()
    maven("https://jitpack.io")
}

kotlin {
    val jvmVersion = extra["jvm.version"] as String
    jvmToolchain(jvmVersion.toInt())
    jvm {
        compilations.all {
            kotlinOptions.jvmTarget = jvmVersion
        }
    }
    sourceSets {
        named("commonMain") {
            dependencies {
            }
        }
        named("jvmMain") {
            dependencies {
                compileOnly(project(":dsp"))
                implementation(project(":utils"))
                implementation(compose.runtime)
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:${extra["eim.dependencies.kotlinx.coroutines"]}")
                implementation("org.apache.commons:commons-lang3:${extra["eim.dependencies.commons.lang"]}")
                implementation("org.slf4j:slf4j-api:${extra["eim.dependencies.slf4j"]}")
                implementation("com.github.ShirasawaSama:JavaSharedMemory:0.2.1")
                implementation("com.github.EchoInMirror.EIMTimeStretchers:common:0.2.0")

                val os = org.gradle.nativeplatform.platform.internal.DefaultNativePlatform.getCurrentOperatingSystem()
                implementation("com.github.EchoInMirror.EIMTimeStretchers:${
                    when {
                        os.isWindows -> "windows"
                        os.isLinux -> "linux"
                        os.isMacOsX -> "macos"
                        else -> "common" // Fall back to common
                    }
                }:0.2.0")

                compileOnly(compose.material3)
                compileOnly(compose.materialIconsExtended)
                compileOnly(project(":components"))
                compileOnly(project(":api"))
                compileOnly(project(":audio-processors"))
                compileOnly(project(":utils"))
            }
        }
    }
}

//tasks.withType<JavaCompile> {
//    options.compilerArgs = options.compilerArgs + listOf("--enable-preview")
//}
