package com.eimsound.daw.window.dialogs.settings

import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.*
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEach
import com.eimsound.daw.api.Command
import com.eimsound.daw.api.EchoInMirror
import com.eimsound.daw.api.sortedKeys
import com.eimsound.daw.components.CustomOutlinedTextField
import com.eimsound.daw.components.SettingTab
import com.eimsound.daw.components.SettingsCard
import com.eimsound.daw.components.SettingsSection
import com.eimsound.daw.components.utils.clickableWithIcon
import com.eimsound.daw.impl.CommandManagerImpl
import com.eimsound.daw.utils.mutableStateSetOf
import com.eimsound.daw.utils.toMutableStateSet
import org.apache.commons.lang3.SystemUtils
import java.awt.event.KeyEvent

internal object ShortcutKeySettings : SettingTab {
    @Composable
    override fun label() {
        Text("快捷键")
    }

    @Composable
    override fun icon() {
        Icon(Icons.Filled.Keyboard, "Shortcut")
    }

    @Composable
    override fun content() {
        var selectKey by remember { mutableStateOf("") }
        val commandManager = EchoInMirror.commandManager as CommandManagerImpl
        val commands = mutableMapOf<Command, String>()
        commandManager.commands.forEach { (key, command) ->
            commands[command] = key
        }
        commandManager.customCommands.forEach { (key, commandName) ->
            commands[commandManager.commandsMap[commandName]!!] = key
        }
        SettingsSection("快捷键设置") {
            commands.forEach { (command, key) ->
                SettingsCard(command.displayName){
                    var curKey by remember { mutableStateOf(key) }
                    val keyCompose = remember { key.split(" ").map { Key(it.toLong()) }.toMutableStateSet() }
                    val keyDown = remember { mutableStateSetOf<Key>() }
                    val preKeyCompose = remember { mutableStateListOf<Key>() }

                    fun cancel() {
                        keyCompose.clear()
                        selectKey = ""
                        preKeyCompose.fastForEach {
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

                    CustomOutlinedTextField(
                        keyCompose.sortedKeys().joinToString(separator = "+") {
                        var cur = it
                        when (it) {
                            Key.MetaLeft, Key.MetaRight -> if (SystemUtils.IS_OS_MAC) cur = Key.CtrlLeft
                            Key.CtrlLeft, Key.CtrlRight -> if (SystemUtils.IS_OS_MAC) cur = Key.MetaLeft
                        }
                        KeyEvent.getKeyText(cur.nativeKeyCode)
                    },
                        {
                            EchoInMirror.currentPosition.bpm = it.toDoubleOrNull()?.coerceIn(1.0, 600.0) ?: return@CustomOutlinedTextField
                        },
                        Modifier.width(256.dp).height(36.dp)
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
                                    preKeyCompose.addAll(keyCompose)
                                    keyCompose.clear()
                                }
                            }.onKeyEvent {
                                if (selectKey != curKey) return@onKeyEvent false
                                var pressKey = it.key
                                when (pressKey) {
                                    Key.MetaLeft, Key.MetaRight -> if (SystemUtils.IS_OS_MAC) pressKey = Key.CtrlLeft
                                    Key.CtrlLeft, Key.CtrlRight -> if (SystemUtils.IS_OS_MAC) pressKey = Key.MetaLeft
                                    Key.ShiftRight -> pressKey = Key.ShiftLeft
                                    Key.AltRight -> pressKey = Key.AltLeft
                                    Key.Backspace -> pressKey = Key.Delete
                                }
                                if (it.type == KeyEventType.KeyDown) {
                                    when (pressKey) {
                                        Key.Escape -> cancel()
                                        Key.Enter -> return@onKeyEvent false
                                        else -> {
                                            keyDown.add(pressKey)
                                            keyCompose.add(pressKey)
                                        }
                                    }
                                } else if (it.type == KeyEventType.KeyUp) {
                                    keyDown.remove(pressKey)
                                    if (keyDown.isEmpty()) done()
                                }
                                true
                            },
                        singleLine = true,
                        textStyle = MaterialTheme.typography.labelLarge.copy(LocalContentColor.current),
                        colors = TextFieldDefaults.colors(
                            unfocusedIndicatorColor = MaterialTheme.colorScheme.outlineVariant,
                            focusedIndicatorColor = MaterialTheme.colorScheme.outlineVariant,
                        ),
                        keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number),
                        paddingValues = TextFieldDefaults.contentPaddingWithLabel(6.dp, 6.dp, 3.dp, 4.dp)
                    )
                }
            }
        }
    }
}