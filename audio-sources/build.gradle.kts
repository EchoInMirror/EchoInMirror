plugins {
    kotlin("multiplatform")
}

repositories {
    maven("https://mvn.0110.be/releases")
}

kotlin {
    jvmToolchain(17)
    jvm {
        compilations.all {
            kotlinOptions.jvmTarget = "17"
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
                implementation("com.fasterxml.jackson.module:jackson-module-kotlin:${extra["eim.dependencies.jackson"]}")
            }
        }
        named("jvmMain") {

        }
    }
}
