import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.jetbrains.compose.ComposeBuildConfig
import org.jetbrains.compose.ExperimentalComposeLibrary
import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.text.DateFormat
import java.util.*

plugins {
    kotlin("multiplatform")
    id("org.jetbrains.compose")
    id("com.github.johnrengelman.shadow") version "7.1.2"
}

repositories {
    mavenCentral()
    maven("https://jitpack.io")
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    maven("https://mvn.0110.be/releases")
}

kotlin {
    jvmToolchain(19)
    jvm {
        compilations.all {
            kotlinOptions.jvmTarget = "19"
        }
        withJava()
    }
    sourceSets {
        named("jvmMain") {
            dependencies {
                api(project(":components"))
                api(project(":utils"))
                api(project(":api"))
                api(project(":native"))
                api(project(":audio-sources"))

                api(compose.desktop.currentOs) {
                    exclude("org.jetbrains.compose.material")
                }
                api(compose.materialIconsExtended)
                @OptIn(ExperimentalComposeLibrary::class) api(compose.material3)
                api("com.fasterxml.jackson.module:jackson-module-kotlin:${extra["eim.dependencies.jackson"]}")
//                @OptIn(ExperimentalComposeLibrary::class) implementation(compose.desktop.components.splitPane)

                api("commons-io:commons-io:2.11.0")
                api("org.apache.commons:commons-lang3:${extra["eim.dependencies.commons.lang"]}")
                api("org.pf4j:pf4j:3.8.0")
                api("org.slf4j:slf4j-simple:${extra["eim.dependencies.slf4j"]}")
                api("org.ocpsoft.prettytime:prettytime:5.0.6.Final")
                api("org.mapdb:mapdb:3.0.9") {
                    exclude(group = "com.google.guava")
                }
                implementation("org.jetbrains.compose.ui:ui-util:${ComposeBuildConfig.composeVersion}")
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

fun downloadEIMHost(ext: String) {
    val file = File("EIMHost-$ext")
    if (file.exists()) return
    val connection = URL("https://github.com/EchoInMirror/EIMHost/releases/latest/download/EIMHost-$ext")
        .openConnection() as HttpURLConnection
    connection.connect()
    val input = connection.inputStream
    val output = FileOutputStream(file)
    input.copyTo(output)
    input.close()
    output.close()
}

project(":daw") {
    task<Copy>("downloadEIMHost") {
        downloadEIMHost("x64.exe")
//        downloadEIMHost("x86.exe")
    }
}

tasks.withType<Jar> {
    manifest {
        attributes(
            "Name" to "EchoInMirror",
            "Main-Class" to "com.eimsound.daw.MainKt",
            "Implementation-Version" to project.version,
            "Release-Time" to DateFormat.getDateTimeInstance().format(Date())
        )
    }
}

tasks.withType<ShadowJar> {
    archiveClassifier.set("")
    archiveVersion.set("")
}

// Run before build
tasks.withType<GradleBuild> {
    dependsOn(":downloadEIMHost")
}
