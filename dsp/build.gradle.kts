plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization") version "1.8.0"
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
                api(project(":utils"))
                implementation(compose.runtime)
                implementation("com.github.albfernandez:juniversalchardet:2.4.0")
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:${extra["eim.dependencies.kotlinx.coroutines"]}")
                implementation("org.mapdb:mapdb:3.0.9") {
                    exclude(group = "com.google.guava")
                }
            }
        }
        named("commonTest") {
            dependencies {
                implementation(kotlin("test-junit"))
            }
        }
        named("jvmMain") {
            dependencies {
                implementation("com.google.guava:guava:31.1-jre")
            }
        }
    }
}
