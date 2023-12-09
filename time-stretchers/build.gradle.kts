plugins {
    kotlin("multiplatform")
}

repositories {
    maven("https://jitpack.io")
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
                implementation("be.tarsos.dsp:core:${extra["eim.dependencies.tarsos"]}")
            }
        }
        named("jvmMain") {
            dependencies {
                implementation("com.github.EchoInMirror:EIMTimeStretchers:0.0.2")
            }
        }
    }
}
