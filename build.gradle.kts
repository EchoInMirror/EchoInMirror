import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("multiplatform") version "1.8.0" apply false
    `maven-publish`
}

group = "com.eimsound.daw"

allprojects {
    repositories {
        mavenCentral()
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    }
}

subprojects {
    tasks.withType<KotlinCompile>().configureEach {
        kotlinOptions.languageVersion = "1.8"
    }
}
