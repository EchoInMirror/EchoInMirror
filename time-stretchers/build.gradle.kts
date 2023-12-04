plugins {
    kotlin("multiplatform")
}

repositories {
    maven("https://mvn.0110.be/releases")
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
                implementation("com.tianscar.soundtouch:soundtouch-jni-core:1.0.6")
                implementation("be.tarsos.dsp:core:${extra["eim.dependencies.tarsos"]}")
            }
        }
        named("jvmMain") {
            dependencies {
                implementation("com.tianscar.soundtouch:soundtouch-jni-javase:1.0.6")
            }
        }
    }
}
