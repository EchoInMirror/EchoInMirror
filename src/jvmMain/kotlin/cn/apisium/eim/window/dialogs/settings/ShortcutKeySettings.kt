package cn.apisium.eim.window.dialogs.settings

import androidx.compose.foundation.layout.*
import cn.apisium.eim.components.Tab
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.*
import androidx.compose.ui.unit.dp
import cn.apisium.eim.EchoInMirror
import cn.apisium.eim.api.Command
import cn.apisium.eim.components.CustomTextField
import cn.apisium.eim.impl.CommandManagerImpl
import com.fasterxml.jackson.databind.ObjectMapper
import java.awt.event.KeyEvent


internal object ShortcutKeySettings : Tab {
    @Composable
    override fun label() {
        Text("快捷键设置")
    }

    @Composable
    override fun icon() {
        Icon(Icons.Filled.Keyboard, "Shortcut")
    }

    @Composable
    @OptIn(ExperimentalComposeUiApi::class)
    override fun content() {
        var selectKey by remember { mutableStateOf("") }
        Column {
            val commandManager: CommandManagerImpl = EchoInMirror.commandManager as CommandManagerImpl
            val commands = mutableMapOf<Command, String>()

            commandManager.commands.forEach { (key, command) ->
                commands[command] = key
            }
            commandManager.customCommand.forEach { (key, commandName) ->
                commands[commandManager.commandMap[commandName]!!] = key
            }

            commands.forEach { (command, key) ->
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(command.displayName, Modifier.weight(1f))
                    var curKey by remember { mutableStateOf(key) }
                    val keyCompose = remember { key.split(" ").toMutableStateList() }
                    val preKeyCompose = remember { mutableStateListOf<String>() }

                    fun done() {
                        selectKey = ""
                        val keyStr = keyCompose.joinToString(separator = " ")
                        if (keyStr in commandManager.commands || keyStr in commandManager.customCommand) return
                        if (curKey !in commandManager.commands && curKey !in commandManager.customCommand) return
                        val commandTar: String
                        if (curKey in commandManager.customCommand) {
                            commandTar = commandManager.customCommand[curKey]!!
                            commandManager.customCommand.minus(curKey)
                        } else {
                            commandTar = commandManager.commands[curKey]!!.name
                        }
                        commandManager.customCommand[keyStr] = commandTar
                        curKey = keyStr
                        ObjectMapper().writeValue(
                            commandManager.customShortcutKeyPath.toFile(),
                            commandManager.customCommand
                        )
                    }

                    fun cancel() {
                        keyCompose.clear()
                        selectKey = ""
                        preKeyCompose.forEach {
                            keyCompose.add(it)
                        }
                    }

                    CustomTextField(keyCompose.joinToString(separator = "+") {
                        KeyEvent.getKeyText(Key(it.toLong()).nativeKeyCode)
                    },
                        {},
                        Modifier.weight(2f),
                        singleLine = true,
                        trailingIcon = {
                            IconButton({
                                if (selectKey == curKey) {
                                    done()
                                } else {
                                    selectKey = curKey
                                    preKeyCompose.clear()
                                    keyCompose.forEach {
                                        preKeyCompose.add(it)
                                    }
                                    keyCompose.clear()
                                }
                            }, modifier = Modifier
                                .onFocusChanged { it ->
                                    if (!it.isFocused && selectKey == curKey) {
                                        cancel()
                                    }
                                }
                                .onKeyEvent { it ->
                                    if (it.type != KeyEventType.KeyDown || selectKey != curKey) return@onKeyEvent false

                                    if (it.key == Key.Escape) {
                                        cancel()
                                    } else {
                                        keyCompose.add(it.key.keyCode.toString())
                                    }
                                    true
                                }) {
                                Icon(Icons.Filled.Edit, "edit")
                            }
                        })
                    Spacer(Modifier.weight(0.5f))
                }
                Spacer(Modifier.height(10.dp))
            }

        }
    }
}