package com.eimsound.daw.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import com.eimsound.daw.api.Command
import com.eimsound.daw.api.EchoInMirror
import com.eimsound.daw.commands.*
import com.eimsound.daw.dawutils.CLIPBOARD_MANAGER
import com.eimsound.daw.utils.SerializableEditor

@Composable
fun CommandMenuItem(command: Command, enabled: Boolean = true, iconModifier: Modifier = DEFAULT_ICON_MODIFIER, onClick: () -> Unit) {
    CommandMenuItem(command, EchoInMirror.commandManager.getKeysOfCommand(command), enabled, iconModifier, onClick)
}

/**
 * Calls by [com.eimsound.daw.impl.CommandManagerImpl]
 */
fun initEditorMenuItems() {
    @Suppress("INVISIBLE_MEMBER")
    editorMenuComposable = @Composable { editor, close ->
        val hasSelected = editor.hasSelected
        CommandMenuItem(SelectAllCommand) {
            close()
            editor.selectAll()
        }
        CommandMenuItem(DeleteCommand, hasSelected) {
            close()
            editor.delete()
        }
        CommandMenuItem(CopyCommand, hasSelected) {
            close()
            editor.copy()
        }
        CommandMenuItem(CutCommand, hasSelected) {
            close()
            editor.cut()
        }
        CommandMenuItem(PasteCommand, editor.canPaste) {
            close()
            editor.paste()
        }
        if (editor is SerializableEditor) {
            CommandMenuItem(CopyToClipboard, hasSelected) {
                close()
                CLIPBOARD_MANAGER?.setText(AnnotatedString(editor.copyAsString()))
            }
            CommandMenuItem(PasteFromClipboard) {
                close()
                editor.pasteFromString(CLIPBOARD_MANAGER?.getText()?.text ?: return@CommandMenuItem)
            }
        }
    }
}
