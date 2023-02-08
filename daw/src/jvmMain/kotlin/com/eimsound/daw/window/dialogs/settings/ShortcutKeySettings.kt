package com.eimsound.daw.window.dialogs.settings

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.*
import androidx.compose.ui.unit.dp
import com.eimsound.daw.EchoInMirror
import com.eimsound.daw.api.Command
import com.eimsound.daw.api.sortedKeys
import com.eimsound.daw.components.ReadonlyTextField
import com.eimsound.daw.components.Tab
import com.eimsound.daw.components.utils.clickableWithIcon
import com.eimsound.daw.impl.CommandManagerImpl
import com.eimsound.daw.utils.mutableStateSetOf
import com.eimsound.daw.utils.toMutableStateSet
import java.awt.event.KeyEvent

internal object ShortcutKeySettings : Tab {
    @Composable
    override fun label() {
        Text("快捷键")
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
            val commandManager = EchoInMirror.commandManager as CommandManagerImpl
            val commands = mutableMapOf<Command, String>()

            commandManager.commands.forEach { (key, command) ->
                commands[command] = key
            }
            commandManager.customCommands.forEach { (key, commandName) ->
                commands[commandManager.commandsMap[commandName]!!] = key
            }

            commands.forEach { (command, key) ->
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(command.displayName, Modifier.weight(1f))
                    var curKey by remember { mutableStateOf(key) }
                    val keyCompose = remember { key.split(" ").map { Key(it.toLong()) }.toMutableStateSet() }
                    val keyDown = remember { mutableStateSetOf<Key>() }
                    val preKeyCompose = remember { mutableStateListOf<Key>() }

                    fun cancel() {
                        keyCompose.clear()
                        selectKey = ""
                        preKeyCompose.forEach {
                            keyCompose.add(it)
                        }
                    }

                    fun done() {
                        selectKey = ""
                        val keyStr = keyCompose.sortedKeys().joinToString(separator = " ") {
                            it.keyCode.toString()
                        }
                        if (keyStr.isEmpty() || keyStr in commandManager.commands || keyStr in commandManager.customCommands) {
                            cancel()
                            return
                        }
                        if (curKey !in commandManager.commands && curKey !in commandManager.customCommands) {
                            cancel()
                            return
                        }
                        val commandTar: String
                        if (curKey in commandManager.customCommands) {
                            commandTar = commandManager.customCommands[curKey]!!
                            commandManager.customCommands -= curKey
                        } else {
                            commandTar = commandManager.commands[curKey]!!.name
                        }
                        commandManager.customCommands[keyStr] = commandTar
                        curKey = keyStr
                        commandManager.saveCustomShortcutKeys()
                    }

                    ReadonlyTextField(Modifier
                        .weight(1f)
                        .onFocusChanged {
                            if (!it.isFocused && selectKey == curKey) done()
                        }
                        .clickableWithIcon {
                            if (selectKey == curKey) {
                                done()
                            } else {
                                selectKey = curKey
                                preKeyCompose.clear()
                                keyDown.clear()
                                keyCompose.forEach {
                                    preKeyCompose.add(it)
                                }
                                keyCompose.clear()
                            }
                        }.onKeyEvent {
                            if (selectKey != curKey) return@onKeyEvent false
                            if (it.type == KeyEventType.KeyDown) {
                                when (it.key) {
                                    Key.Escape -> cancel()
                                    Key.Enter -> return@onKeyEvent false
                                    else -> {
                                        keyDown.add(it.key)
                                        keyCompose.add(it.key)
                                    }
                                }
                            } else if (it.type == KeyEventType.KeyUp) {
                                keyDown.remove(it.key)
                                if (keyDown.isEmpty())
                                    done()
                            }
                            true
                        }) {
                        Text(keyCompose.sortedKeys().joinToString(separator = "+") {
                            KeyEvent.getKeyText(it.nativeKeyCode)
                        }, Modifier.fillMaxWidth())
                    }
                    Spacer(Modifier.weight(0.5f))
                }
                Spacer(Modifier.height(10.dp))
            }

        }
    }
}