plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization") version "1.8.0"
    id("org.jetbrains.compose")
}

kotlin {
    jvmToolchain(19)
    jvm {
        compilations.all {
            kotlinOptions.jvmTarget = "19"
        }
        withJava()
    }
    sourceSets {
        named("commonMain") {
            dependencies {
                api(project(":dsp"))
                api(project(":utils"))
                implementation(compose.runtime)
                implementation(compose.ui)
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:${extra["eim.dependencies.kotlinx.coroutines"]}")
            }
        }
        named("jvmMain") {

        }
    }
}
