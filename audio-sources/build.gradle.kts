plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization") version "1.8.0"
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
                api(project(":dsp"))
                implementation("be.tarsos.dsp:core:2.5")
                implementation("com.googlecode.soundlibs:mp3spi:1.9.5.4") {
                    exclude("com.googlecode.soundlibs", "tritonus_share")
                    exclude("junit")
                }
                implementation("com.googlecode.soundlibs:tritonus-all:0.3.7.2") {
                    exclude("com.googlecode.soundlibs", "tritonus_share")
                }
                implementation("com.github.trilarion:vorbis-support:1.1.0")
                implementation("org.jflac:jflac-codec:1.5.2")
            }
        }
        named("jvmMain") {

        }
    }
}
