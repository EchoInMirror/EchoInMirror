plugins {
    kotlin("multiplatform")
    id("org.jetbrains.compose")
}

kotlin {
    jvmToolchain(17)
    jvm {
        compilations.all {
            kotlinOptions.jvmTarget = "17"
        }
    }
    sourceSets {
        named("commonMain") {
            dependencies {
                api(project(":utils"))
                implementation(compose.runtime)
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:${extra["eim.dependencies.kotlinx.coroutines"]}")
                implementation("com.fasterxml.jackson.module:jackson-module-kotlin:${extra["eim.dependencies.jackson"]}")
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
