package com.eimsound.daw.window

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ListAlt
import androidx.compose.material.icons.outlined.AddCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.window.ApplicationScope
import androidx.compose.ui.window.Window
import com.eimsound.daw.EchoInMirror
import com.eimsound.daw.components.*
import com.eimsound.daw.components.utils.clickableWithIcon
import com.eimsound.daw.recentProjects
import com.eimsound.daw.utils.*
import java.awt.Dimension
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.attribute.BasicFileAttributes
import kotlin.io.path.isRegularFile
import kotlin.io.path.name

private data class ProjectInfo(val path: Path, val createdTime: String, val lastModified: String)

private class Projects: Tab {
    @Composable
    override fun label() {
        Text("最近项目")
    }

    @Composable
    override fun icon() {
        Icon(Icons.Filled.ListAlt, "Projects")
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun content() {
        Column {
            Row {
                val window = CurrentWindow.current
                val id = remember { Any() }
                val localFloatingDialogProvider = LocalFloatingDialogProvider.current
                ExtendedFloatingActionButton({
                    openFolderBrowser(window)?.let {
                        if (!it.isDirectory) return@let
                        if (Files.list(it.toPath()).findFirst().isPresent && !File(it, "eim.json").exists()) {
                            localFloatingDialogProvider.openFloatingDialog({
                                localFloatingDialogProvider.closeFloatingDialog(id)
                            }, hasOverlay = true, key = id) {
                                Dialog({ localFloatingDialogProvider.closeFloatingDialog(id) }) {
                                    Text("无法打开该文件夹, 因为它不是一个有效的项目文件夹 (不为空或不存在 eim.json 文件)")
                                }
                            }
                        }
                    }
                }) {
                    Icon(Icons.Outlined.AddCircle, "NewProject")
                    Gap(8)
                    Text(text = "打开目录")
                }
            }

            Gap(16)

            val projects = remember { mutableStateListOf<ProjectInfo>() }
            LaunchedEffect(recentProjects) {
                recentProjects.fastForEach {
                    val path = Paths.get(it)
                    val projectInfoFile = path.resolve("eim.json")
                    if (!projectInfoFile.isRegularFile()) return@fastForEach
                    try {
                        val info = Files.readAttributes(projectInfoFile, BasicFileAttributes::class.java)
                        projects.add(ProjectInfo(path, TIME_FORMATTER.format(info.creationTime().toInstant()),
                            TIME_PRETTIER.format(info.lastModifiedTime().toInstant())
                        ))
                    } catch (e: Exception) {
                        projects.add(ProjectInfo(path, "未知", "未知"))
                    }
                }
            }

            projects.fastForEach {
                ListItem({ Text(it.path.name) },
                    Modifier.clickableWithIcon { EchoInMirror.windowManager.openProject(it.path) },
                    supportingText = { Text(it.createdTime) },
                    trailingContent = { Text(it.lastModified) },
                    leadingContent = {
                        Avatar(randomColor()) { Text(it.path.name.firstOrNull()?.toString() ?: "") }
                    }
                )
                Divider()
            }
        }
    }
}

val welcomeWindowTabs = mutableStateListOf<Tab>(Projects())

@Composable
fun ApplicationScope.ProjectWindow() {
    Window(::exitApplication, icon = Logo, title = "Echo In Mirror") {
        window.minimumSize = Dimension(860, 700)
        val floatingDialogProvider = remember { FloatingDialogProvider() }
        CompositionLocalProvider(LocalFloatingDialogProvider.provides(floatingDialogProvider)) {
            Tabs(welcomeWindowTabs)
        }
        floatingDialogProvider.FloatingDialogs()
    }
}
