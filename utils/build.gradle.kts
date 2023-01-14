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
                implementation("com.aventrix.jnanoid:jnanoid:2.0.0")
                compileOnly(compose.runtime)
                compileOnly(compose.ui)
            }
        }
        named("jvmMain") {

        }
    }
}
