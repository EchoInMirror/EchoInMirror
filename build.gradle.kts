import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.compose.ExperimentalComposeLibrary
import java.net.URL
import java.net.HttpURLConnection
import java.io.FileOutputStream
import java.util.Date
import java.text.DateFormat
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
    @Suppress("OPT_IN_IS_NOT_ENABLED")
    sourceSets {
        named("jvmMain") {
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
                implementation("org.ocpsoft.prettytime:prettytime:5.0.6.Final")
                implementation("com.aventrix.jnanoid:jnanoid:2.0.0")
                implementation("com.github.dragoon000320:tarsosdsp:1.0")

                // Audio decoders
                implementation("com.googlecode.soundlibs:mp3spi:1.9.5.4") {
                    exclude("com.googlecode.soundlibs", "tritonus_share")
                }
                implementation("com.googlecode.soundlibs:tritonus-all:0.3.7.2") {
                    exclude("com.googlecode.soundlibs", "tritonus_share")
                }
                implementation("com.github.trilarion:vorbis-support:1.1.0")
                implementation("org.jflac:jflac-codec:1.5.0")
            }
        }

        named("jvmTest") {
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
            "Implementation-Version" to project.version,
            "Release-Time" to DateFormat.getDateTimeInstance().format(Date())
        )
    }
}

tasks.withType<ShadowJar> {
    archiveClassifier.set("")
    archiveVersion.set("")
}
