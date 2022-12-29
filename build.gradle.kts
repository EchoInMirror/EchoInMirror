import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.compose.ExperimentalComposeLibrary
import java.net.URL
import java.net.HttpURLConnection
import java.io.FileOutputStream
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jetbrains.compose.ComposeBuildConfig

plugins {
    kotlin("multiplatform")
    id("org.jetbrains.compose")
    id("com.github.johnrengelman.shadow") version "7.1.2"
}

group = "com.eimsound"
version = "0.0.0"

repositories {
    google()
    mavenCentral()
//    maven("https://jitpack.io")
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    maven("https://s01.oss.sonatype.org/content/repositories/releases/")
}

kotlin {
    jvmToolchain(17)
    jvm {
        compilations.all {
            kotlinOptions.jvmTarget = "17"
        }
        withJava()
    }
    @Suppress("UNUSED_VARIABLE", "OPT_IN_IS_NOT_ENABLED")
    sourceSets {
        val jvmMain by getting {
            dependencies {
                implementation(compose.desktop.currentOs) {
                    exclude("org.jetbrains.compose.material")
                }
                implementation("org.jetbrains.compose.ui:ui-util:${ComposeBuildConfig.composeVersion}")
                implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.14.+")
                implementation(compose.materialIconsExtended)
                @OptIn(ExperimentalComposeLibrary::class) implementation(compose.material3)
//                @OptIn(ExperimentalComposeLibrary::class) implementation(compose.desktop.components.splitPane)

                implementation("commons-io:commons-io:2.11.0")
                implementation("org.apache.commons:commons-lang3:3.12.0")
                implementation("org.pf4j:pf4j:3.8.0")
                implementation("org.slf4j:slf4j-simple:2.0.3")
                implementation("com.github.ajalt.colormath:colormath:3.2.1") {
                    exclude("org.jetbrains.kotlin")
                }
            }
        }

        val jvmTest by getting {
            dependencies {
                implementation(kotlin("test-junit"))
            }
        }
    }
}

compose.desktop {
    application {
        mainClass = "MainKt"
        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "EchoInMirror"
            packageVersion = "1.0.0"
        }
    }
}

task<Copy>("downloadEIMHost") {
    val file = File("EIMHost.exe")
    if (file.exists()) {
        println("File exists, skipping download.")
        return@task
    }
    val connection = URL("https://github.com/EchoInMirror/EIMHost/releases/latest/download/EIMHost.exe").openConnection() as HttpURLConnection
    connection.connect()
    val input = connection.inputStream
    val output = FileOutputStream(file)
    input.copyTo(output)
    input.close()
    output.close()
}

// Run before build
tasks.withType<GradleBuild> {
    dependsOn(":downloadEIMHost")
}

tasks.withType<KotlinCompile>().configureEach {
    kotlinOptions.languageVersion = "1.8"
}

tasks.withType<Jar> {
    manifest {
        attributes(
            "Name" to "EchoInMirror",
            "Main-Class" to "cn.apisium.eim.MainKt",
            "Version" to project.version,
        )
    }
}

tasks.withType<ShadowJar> {
    archiveClassifier.set("")
    archiveVersion.set("")
}
