package com.eimsound.daw.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardCommandKey
import androidx.compose.material.icons.filled.KeyboardOptionKey
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.nativeKeyCode
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEachIndexed
import com.eimsound.daw.api.Command
import com.eimsound.daw.components.icons.KeyboardShift
import java.awt.event.KeyEvent

val DEFAULT_ICON_MODIFIER = Modifier.size(18.dp).padding(top = 2.dp)
val DEFAULT_COMMAND_NAME_MODIFIER = Modifier.padding(start = 4.dp)

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun KeysHint(keys: Array<Key>, iconModifier: Modifier = DEFAULT_ICON_MODIFIER) {
    var hasCtrl = false
    var hasShift = false
    var hasAlt = false
    val list = keys.distinct().filter {
        when (it) {
            Key.CtrlLeft -> hasCtrl = true
            Key.ShiftLeft -> hasShift = true
            Key.AltLeft -> hasAlt = true
            else -> return@filter true
        }
        return@filter false
    }.sortedBy { it.keyCode }
    if (hasCtrl) Icon(Icons.Filled.KeyboardCommandKey, "Control", iconModifier)
    if (hasShift) Icon(KeyboardShift, "Shift", iconModifier)
    if (hasAlt) Icon(Icons.Filled.KeyboardOptionKey, "Alt", iconModifier)
    list.fastForEachIndexed { index, key -> Text((if (index == 0) "" else "+") + KeyEvent.getKeyText(key.nativeKeyCode)) }
}

@Composable
fun CommandMenuItem(command: Command, keys: Array<Key> = command.keyBindings, enabled: Boolean = true,
                    iconModifier: Modifier = DEFAULT_ICON_MODIFIER, onClick: () -> Unit) {
    MenuItem(false, onClick, enabled = enabled, modifier = Modifier.fillMaxWidth()) {
        command.icon?.let { Icon(it, it.name, iconModifier) }
        Text(command.displayName, DEFAULT_COMMAND_NAME_MODIFIER)
        Filled(Modifier.widthIn(12.dp))
        KeysHint(keys, iconModifier)
    }
}
