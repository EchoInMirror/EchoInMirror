package com.eimsound.daw.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import com.eimsound.daw.api.Command
import com.eimsound.daw.api.EchoInMirror
import com.eimsound.daw.commands.*
import com.eimsound.daw.dawutils.CLIPBOARD_MANAGER
import com.eimsound.daw.commons.MultiSelectableEditor
import com.eimsound.daw.commons.SerializableEditor

@Composable
fun CommandMenuItem(command: Command, showIcon: Boolean, enabled: Boolean = true, iconModifier: Modifier = DEFAULT_ICON_MODIFIER, onClick: () -> Unit) {
    CommandMenuItem(command,
        if (showIcon) EchoInMirror.commandManager.getKeysOfCommand(command)
        else emptyArray(), enabled, iconModifier, onClick)
}

/**
 * Calls by [com.eimsound.daw.impl.CommandManagerImpl]
 */
fun initEditorMenuItems() {
    @Suppress("INVISIBLE_MEMBER")
    editorMenuComposable = @Composable { editor, showIcon, close ->
        val hasSelected = editor.hasSelected
        if (editor is MultiSelectableEditor) {
            CommandMenuItem(SelectAllCommand, showIcon) {
                close()
                editor.selectAll()
            }
        }
        if (editor.canDelete) {
            CommandMenuItem(DeleteCommand, showIcon, hasSelected) {
                close()
                editor.delete()
            }
        }
        CommandMenuItem(CopyCommand, showIcon, hasSelected) {
            close()
            editor.copy()
        }
        if (editor.canDelete) {
            CommandMenuItem(CutCommand, showIcon, hasSelected) {
                close()
                editor.cut()
            }
        }
        CommandMenuItem(PasteCommand, showIcon, editor.canPaste) {
            close()
            editor.paste()
        }
        if (editor is SerializableEditor) {
            CommandMenuItem(CopyToClipboard, showIcon, hasSelected) {
                close()
                CLIPBOARD_MANAGER?.setText(AnnotatedString(editor.copyAsString()))
            }
            CommandMenuItem(PasteFromClipboard, showIcon) {
                close()
                editor.pasteFromString(CLIPBOARD_MANAGER?.getText()?.text ?: return@CommandMenuItem)
            }
        }
    }
}
