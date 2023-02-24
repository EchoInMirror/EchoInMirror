plugins {
    kotlin("multiplatform")
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
                implementation("com.aventrix.jnanoid:jnanoid:2.0.0")
                api("io.github.oshai:kotlin-logging-jvm:4.0.0-beta-22")
                api("org.jetbrains.kotlinx:kotlinx-serialization-json:${extra["eim.dependencies.kotlinx.serialization"]}")
                compileOnly(compose.runtime)
                compileOnly(compose.ui)
            }
        }
        named("jvmMain") {

        }
    }
}
