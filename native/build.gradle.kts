import org.jetbrains.compose.ExperimentalComposeLibrary

plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization") version "1.8.0"
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
                implementation(project(":utils"))
                implementation(compose.runtime)
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:${extra["eim.dependencies.kotlinx.coroutines"]}")
                implementation("org.apache.commons:commons-lang3:${extra["eim.dependencies.commons.lang"]}")
                implementation("org.slf4j:slf4j-api:${extra["eim.dependencies.slf4j"]}")
                implementation("com.github.ShirasawaSama:JavaSharedMemory:0.0.2")

                @OptIn(ExperimentalComposeLibrary::class)
                compileOnly(compose.material3)
                compileOnly(compose.materialIconsExtended)
                compileOnly(project(":components"))
                compileOnly(project(":api"))
                compileOnly(project(":dsp"))
                compileOnly(project(":utils"))
            }
        }
    }
}

//tasks.withType<JavaCompile> {
//    options.compilerArgs = options.compilerArgs + listOf("--enable-preview")
//}
