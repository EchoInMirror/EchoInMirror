import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("org.jetbrains.compose") apply false
    kotlin("multiplatform") version System.getProperty("kotlinVersion") apply false
    `maven-publish`
}

group = "com.eimsound.daw"

allprojects {
    repositories {
        mavenCentral()
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
        maven("https://mvn.0110.be/releases")
    }
}

subprojects {
    tasks.withType<KotlinCompile>().configureEach {
        kotlinOptions.languageVersion = "1.9"
    }
}
