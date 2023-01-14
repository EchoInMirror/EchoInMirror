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

private val DEFAULT_PADDING_DP = 6.dp
@OptIn(ExperimentalMaterial3Api::class)
private val DEFAULT_PADDING = TextFieldDefaults.textFieldWithLabelPadding(DEFAULT_PADDING_DP,
    DEFAULT_PADDING_DP, DEFAULT_PADDING_DP, 0.dp)

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
    supportingText: @Composable (() -> Unit)? = null,
    isError: Boolean = false,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    singleLine: Boolean = false,
    maxLines: Int = Int.MAX_VALUE,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    shape: Shape = TextFieldDefaults.outlinedShape,
    colors: TextFieldColors = TextFieldDefaults.outlinedTextFieldColors(),
    paddingValues: PaddingValues = DEFAULT_PADDING
) {
    BasicTextField(value, onValueChange, modifier, enabled, readOnly, textStyle, keyboardOptions, keyboardActions,
        singleLine, maxLines, visualTransformation, {}, interactionSource) {
        TextFieldDefaults.OutlinedTextFieldDecorationBox(value, it, enabled, singleLine, visualTransformation,
            interactionSource, isError, label, placeholder, leadingIcon, trailingIcon, supportingText, colors, paddingValues) {
            TextFieldDefaults.OutlinedBorderContainerBox(
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
    supportingText: @Composable (() -> Unit)? = null,
    isError: Boolean = false,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    singleLine: Boolean = false,
    maxLines: Int = Int.MAX_VALUE,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    shape: Shape = TextFieldDefaults.filledShape,
    colors: TextFieldColors = TextFieldDefaults.textFieldColors(),
    paddingValues: PaddingValues = DEFAULT_PADDING
) {
    BasicTextField(value, onValueChange, modifier, enabled, readOnly, textStyle, keyboardOptions, keyboardActions,
        singleLine, maxLines, visualTransformation, {}, interactionSource) {
        TextFieldDefaults.TextFieldDecorationBox(value, it, enabled, singleLine, visualTransformation,
            interactionSource, isError, label, placeholder, leadingIcon, trailingIcon, supportingText, shape, colors, paddingValues)
        }
    }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReadonlyTextField(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Box(modifier) {
        TextFieldDefaults.TextFieldDecorationBox("", content, true,
            singleLine = true,
            visualTransformation = VisualTransformation.None,
            interactionSource = remember { MutableInteractionSource() },
            trailingIcon = { Icon(Icons.Filled.ExpandMore, null, Modifier.padding(horizontal = 8.dp)) }
        )
    }
}
