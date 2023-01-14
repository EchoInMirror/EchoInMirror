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
        withJava()
    }
    sourceSets {
        named("commonMain") {
            dependencies {
                api(project(":dsp"))
                api(project(":utils"))
                implementation(compose.runtime)
                implementation(compose.ui)
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.4")
                implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.14.+")
            }
        }
        named("jvmMain") {

        }
    }
}
