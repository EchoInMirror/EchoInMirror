@file:OptIn(ExperimentalComposeUiApi::class)

package cn.apisium.eim.commands

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.input.key.Key
import cn.apisium.eim.api.AbstractCommand

object DeleteCommand: AbstractCommand("EIM:Delete", arrayOf(Key.Delete))
object CopyCommand: AbstractCommand("EIM:Copy", arrayOf(Key.CtrlLeft, Key.C))
object CutCommand: AbstractCommand("EIM:Cut", arrayOf(Key.CtrlLeft, Key.X))
object PasteCommand: AbstractCommand("EIM:Paste", arrayOf(Key.CtrlLeft, Key.V))
object SelectAllCommand: AbstractCommand("EIM:Select All", arrayOf(Key.CtrlLeft, Key.A))
