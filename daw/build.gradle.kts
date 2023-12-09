import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.jetbrains.compose.ComposeBuildConfig
import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URI

plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization") version "1.9.20"
    id("org.jetbrains.compose")
    id("com.github.johnrengelman.shadow") version "8.1.1"
    id("java")
}

repositories {
    mavenCentral()
    maven("https://jitpack.io")
}

kotlin {
    val jvmVersion = extra["jvm.version"] as String
    jvmToolchain(jvmVersion.toInt())
    jvm {
        compilations.all {
            kotlinOptions.jvmTarget = jvmVersion
        }
        withJava()
    }
    sourceSets {
        named("jvmMain") {
            dependencies {
                api(project(":commons"))
                api(project(":components"))
                api(project(":utils"))
                api(project(":dsp"))
                api(project(":api"))
                api(project(":native"))
                api(project(":audio-sources"))
                api(project(":time-stretchers"))

                api(compose.desktop.currentOs) {
                    exclude("org.jetbrains.compose.material")
                }
                api(compose.materialIconsExtended)
                api(compose.material3)

                api("commons-io:commons-io:2.11.0")
                api("org.ocpsoft.prettytime:prettytime:5.0.6.Final")
                api("org.mapdb:mapdb:3.0.9") {
                    exclude(group = "com.google.guava")
                }
                implementation("org.apache.logging.log4j:log4j-slf4j2-impl:2.20.0")
                implementation("org.jetbrains.compose.ui:ui-util:${ComposeBuildConfig.composeVersion}")
                implementation("com.github.Apisium:appcenter-sdk-jvm:1.0.6")
                implementation("com.github.Dansoftowner:jSystemThemeDetector:61d9025a31")
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

fun downloadFileFromGithub(repo: String, destName: String, sourceName: String = destName): Boolean {
    val file = File(destName)
    if (!File("src").exists() || file.exists()) return false
    val connection = URI.create("https://github.com/EchoInMirror/$repo/releases/latest/download/$sourceName")
        .toURL().openConnection() as HttpURLConnection
    connection.connect()
    val input = connection.inputStream
    val output = FileOutputStream(file)
    input.copyTo(output)
    input.close()
    output.close()
    return true
}

val isArm by lazy { System.getProperty("os.arch") == "aarch64" }

project(":daw") {
    task<Copy>("downloadEIMHost") {
        val os = org.gradle.nativeplatform.platform.internal.DefaultNativePlatform.getCurrentOperatingSystem()
        if (os.isWindows) {
            downloadFileFromGithub("EIMHost", "EIMHost.exe", "EIMHost-x64.exe")
//            downloadFileFromGithub("EIMHost", "EIMHost-x86.exe", "EIMHost-x86.exe")
        } else if (os.isMacOsX) {
            if (downloadFileFromGithub("EIMHost", "EIMHost-MacOS.zip",
                    if (isArm) "EIMHost-MacOS.zip" else "EIMHost-MacOS-x86.zip")) {
                from(zipTree(File("EIMHost-MacOS.zip"))).into(".")
            }
        } else {
            downloadFileFromGithub("EIMHost", "EIMHost", "EIMHost-Linux")
        }
    }
}

tasks.withType<Jar> {
    manifest {
        attributes(
            "Name" to "EchoInMirror",
            "Main-Class" to "com.eimsound.daw.MainKt",
            "Implementation-Version" to project.version,
            "Release-Time" to System.currentTimeMillis(),
            "App-Center-Secret" to (System.getenv("APP_CENTER_SECRET") ?: "")
        )
    }

    from("../LICENSE")
}

tasks.withType<ShadowJar> {
    mergeServiceFiles()
    archiveClassifier.set("")
    archiveVersion.set("")
}

tasks.withType<JavaExec> {
    jvmArgs(
        "--enable-preview",
        "--enable-native-access=ALL-UNNAMED",
        "--add-exports=java.desktop/com.apple.eawt.event=ALL-UNNAMED"
    )
}

// Run before build
tasks.withType<GradleBuild> {
    dependsOn(":downloadEIMHost")
    dependsOn(":downloadTimeStretcher")
}
