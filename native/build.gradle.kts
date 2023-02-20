import org.jetbrains.compose.ExperimentalComposeLibrary

plugins {
    kotlin("multiplatform")
    id("org.jetbrains.compose")
}

repositories {
    mavenCentral()
    maven("https://jitpack.io")
}

kotlin {
    jvmToolchain(19)
    jvm {
        compilations.all {
            kotlinOptions.jvmTarget = "19"
        }
    }
    sourceSets {
        named("commonMain") {
            dependencies {
            }
        }
        named("jvmMain") {
            dependencies {
                api(project(":dsp"))

                implementation(project(":utils"))
                implementation(compose.runtime)
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:${extra["eim.dependencies.kotlinx.coroutines"]}")
                implementation("com.fasterxml.jackson.module:jackson-module-kotlin:${extra["eim.dependencies.jackson"]}")
                implementation("org.apache.commons:commons-lang3:${extra["eim.dependencies.commons.lang"]}")
                implementation("org.slf4j:slf4j-simple:${extra["eim.dependencies.slf4j"]}")
                implementation("com.github.ShirasawaSama:JavaSharedMemory:0.0.2")

                @Suppress("OPT_IN_IS_NOT_ENABLED")
                @OptIn(ExperimentalComposeLibrary::class)
                compileOnly(compose.material3)
            }
        }
    }
}

//tasks.withType<JavaCompile> {
//    options.compilerArgs = options.compilerArgs + listOf("--enable-preview")
//}
