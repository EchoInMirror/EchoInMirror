plugins {
    kotlin("multiplatform")
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
            }
        }
    }
}
