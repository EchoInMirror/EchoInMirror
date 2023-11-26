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
                api(project(":audio-processors"))
                api(project(":utils"))
                api(project(":commons"))
                api("org.pf4j:pf4j:3.8.0")
                implementation(compose.runtime)
                implementation(compose.ui)
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:${extra["eim.dependencies.kotlinx.coroutines"]}")
            }
        }
        named("jvmMain") {

        }
    }
}
