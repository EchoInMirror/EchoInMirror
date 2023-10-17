package com.eimsound.daw.components

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp

private val DEFAULT_PADDING = TextFieldDefaults.contentPaddingWithLabel(
    start = 8.dp,
    top = 6.dp,
    end = 8.dp,
    bottom = 4.dp,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomOutlinedTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    textStyle: TextStyle = LocalTextStyle.current.copy(LocalContentColor.current),
    label: @Composable (() -> Unit)? = null,
    placeholder: @Composable (() -> Unit)? = null,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    prefix: @Composable (() -> Unit)? = null,
    suffix: @Composable (() -> Unit)? = null,
    supportingText: @Composable (() -> Unit)? = null,
    isError: Boolean = false,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    singleLine: Boolean = false,
    maxLines: Int = Int.MAX_VALUE,
    minLines: Int = 1,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    shape: Shape = OutlinedTextFieldDefaults.shape,
    colors: TextFieldColors = TextFieldDefaults.colors(),
    paddingValues: PaddingValues = DEFAULT_PADDING
) {
    BasicTextField(value, onValueChange, modifier, enabled, readOnly, textStyle, keyboardOptions, keyboardActions,
        singleLine, maxLines, minLines, visualTransformation, {}, interactionSource) {
        TextFieldDefaults.DecorationBox(value, it, enabled, singleLine, visualTransformation, interactionSource,
            isError, label, placeholder, leadingIcon, trailingIcon, prefix, suffix, supportingText, shape, colors, paddingValues) {
            TextFieldDefaults.ContainerBox(
                enabled,
                isError,
                interactionSource,
                colors,
                shape
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    textStyle: TextStyle = LocalTextStyle.current.copy(LocalContentColor.current),
    label: @Composable (() -> Unit)? = null,
    placeholder: @Composable (() -> Unit)? = null,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    prefix: @Composable (() -> Unit)? = null,
    suffix: @Composable (() -> Unit)? = null,
    supportingText: @Composable (() -> Unit)? = null,
    isError: Boolean = false,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    singleLine: Boolean = false,
    maxLines: Int = Int.MAX_VALUE,
    minLines: Int = 1,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    shape: Shape = TextFieldDefaults.shape,
    colors: TextFieldColors = TextFieldDefaults.colors(),
    paddingValues: PaddingValues = DEFAULT_PADDING
) {
    BasicTextField(value, onValueChange, modifier, enabled, readOnly, textStyle, keyboardOptions, keyboardActions,
        singleLine, maxLines, minLines, visualTransformation, {}, interactionSource) {
        TextFieldDefaults.DecorationBox(value, it, enabled, singleLine, visualTransformation, interactionSource,
            isError, label, placeholder, leadingIcon, trailingIcon, prefix, suffix, supportingText, shape, colors, paddingValues)
        }
    }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReadonlyTextField(modifier: Modifier = Modifier, enabled: Boolean = false, content: @Composable () -> Unit) {
    Box(modifier) {
        TextFieldDefaults.DecorationBox("", content, enabled,
            singleLine = true,
            visualTransformation = VisualTransformation.None,
            interactionSource = remember { MutableInteractionSource() },
            trailingIcon = { Icon(Icons.Filled.ExpandMore, null, Modifier.padding(horizontal = 8.dp)) }
        )
    }
}
