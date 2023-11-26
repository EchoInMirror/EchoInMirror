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
    }
    sourceSets {
        named("commonMain") {
            dependencies {
                implementation("org.mapdb:mapdb:3.0.10") {
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
