import org.jetbrains.compose.ExperimentalComposeLibrary

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
            }
        }
        named("jvmMain") {
            dependencies {
                api(project(":dsp"))

                implementation(project(":utils"))
                implementation(compose.runtime)
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.4")
                implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.14.+")
                implementation("org.apache.commons:commons-lang3:3.12.0")
                implementation("org.slf4j:slf4j-simple:2.0.3")

                @Suppress("OPT_IN_IS_NOT_ENABLED")
                @OptIn(ExperimentalComposeLibrary::class)
                compileOnly(compose.material3)
            }
        }
    }
}
