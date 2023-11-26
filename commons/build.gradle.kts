plugins {
    kotlin("multiplatform")
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
                api("org.jetbrains.kotlinx:kotlinx-serialization-json:${extra["eim.dependencies.kotlinx.serialization"]}")
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:${extra["eim.dependencies.kotlinx.coroutines"]}")
                compileOnly(compose.ui)
            }
        }
        named("jvmMain") {

        }
    }
}
