plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization") version System.getProperty("kotlin.version")
    id("org.jetbrains.compose")
}

kotlin {
    val jvmVersion = extra["jvm.version"] as String
    jvmToolchain(jvmVersion.toInt())
    jvm {
        compilations.all {
            kotlinOptions.jvmTarget = jvmVersion
        }
    }
    sourceSets {
        named("commonMain") {
            dependencies {
                implementation("be.tarsos.dsp:core:${extra["eim.dependencies.tarsos"]}")
                implementation("com.github.albfernandez:juniversalchardet:2.4.0")
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:${extra["eim.dependencies.kotlinx.coroutines"]}")
                implementation("org.mapdb:mapdb:3.0.10") {
                    exclude(group = "com.google.guava")
                }
                compileOnly(project(":audio-sources"))
                compileOnly(project(":commons"))
                compileOnly(compose.runtime)
            }
        }
        named("commonTest") {
            dependencies {
                implementation(kotlin("test-junit"))
            }
        }
    }
}
