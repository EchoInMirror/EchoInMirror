package com.eimsound.daw.components.app

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.FrameWindowScope
import com.eimsound.audioprocessor.data.QUANTIFICATION_UNITS
import com.eimsound.audioprocessor.data.getEditUnit
import com.eimsound.daw.Configuration
import com.eimsound.daw.api.EchoInMirror
import com.eimsound.daw.api.EditorTool
import com.eimsound.daw.components.*
import com.eimsound.daw.components.IconButton
import com.eimsound.daw.components.icons.Magnet
import com.eimsound.daw.components.icons.MetronomeTick
import com.eimsound.daw.components.utils.absoluteError
import com.eimsound.daw.dawutils.EDITOR_TOOL_ICONS

private val TIME_VALUES = listOf("时间", "拍")

@Composable
private fun CurrentTime() {
    DropdownSelector(
        {
            Configuration.isTimeDisplayInBeats = it == TIME_VALUES[1]
            Configuration.save()
        },
        TIME_VALUES,
        TIME_VALUES[if (Configuration.isTimeDisplayInBeats) 1 else 0]
    ) {
        val a: String
        val b: String
        val c: String
        if (Configuration.isTimeDisplayInBeats) {
            val ppqPosition = EchoInMirror.currentPosition.ppqPosition
            val timeSigNumerator = EchoInMirror.currentPosition.timeSigNumerator
            a = (1 + ppqPosition / timeSigNumerator).toInt().toString().padStart(2, '0')
            b = (1 + ppqPosition.toInt() % timeSigNumerator).toString().padStart(2, '0')
            c = (1 + (ppqPosition - ppqPosition.toInt()) * (16 / EchoInMirror.currentPosition.timeSigDenominator))
                .toInt().toString()
        } else {
            val seconds = EchoInMirror.currentPosition.timeInSeconds
            a = (seconds.toInt() / 60).toString().padStart(2, '0')
            b = (seconds.toInt() % 60).toString().padStart(2, '0')
            c = ((seconds * 1000).toInt() % 1000).toString().padStart(3, '0')
        }
        CustomOutlinedTextField(
            "${a}:${b}:${c}", { },
            Modifier.width(110.dp),
            singleLine = true,
            textStyle = MaterialTheme.typography.labelLarge.copy(LocalContentColor.current),
            suffix = { ExpandIcon(it) },
            colors = textFieldGrayColors(),
            paddingValues = TextFieldDefaults.contentPaddingWithLabel(8.dp, 4.dp, 3.dp, 4.dp)
        )
    }
}

@Composable
private fun Quantification() {
    DropdownSelector(
        { EchoInMirror.quantification = it },
        QUANTIFICATION_UNITS, EchoInMirror.quantification, Modifier.pointerHoverIcon(PointerIcon.Hand),
        isSelected = { it.getEditUnit(EchoInMirror.currentPosition) == EchoInMirror.editUnit },
        itemContent = {
            Text(it.name, fontWeight = if (it.isSpecial) FontWeight.Bold else null)
        }
    ) {
        CustomOutlinedTextField(
            EchoInMirror.quantification.name, { },
            Modifier.width(100.dp),
            readOnly = true,
            singleLine = true,
            textStyle = MaterialTheme.typography.labelLarge.copy(LocalContentColor.current),
            prefix = {
                Icon(Magnet, "Quantification", modifier = Modifier.size(15.dp).offset((-2).dp, 1.dp))
            },
            suffix = { ExpandIcon(it) },
            colors = textFieldGrayColors(),
            paddingValues = TextFieldDefaults.contentPaddingWithLabel(8.dp, 4.dp, 3.dp, 4.dp)
        )
    }
}

//@Composable
//fun RootNote() {
//    FloatingLayer({ _, close ->
//        Surface(Modifier.width(IntrinsicSize.Min), MaterialTheme.shapes.extraSmall,
//            shadowElevation = 6.dp, tonalElevation = 1.dp) {
//            Column {
//                KEY_NAMES.forEach { MenuItem(close) { Text(it) } }
//            }
//        }
//    }) {
//        AppBarItem("C", "根音")
//    }
//}

//@Composable
//private fun TimeSignature() {
//    AppBarItem(subTitle = "拍号") {
//        Row(verticalAlignment = Alignment.Bottom) {
//            BasicTextField(EchoInMirror.currentPosition.timeSigNumerator.toString(), {
//                EchoInMirror.currentPosition.timeSigNumerator = it.toIntOrNull()?.coerceIn(1, 32) ?: return@BasicTextField
//            }, Modifier.width(IntrinsicSize.Min), textStyle = getAppBarFont(), singleLine = true)
//            AppBarTitle("/")
//            FloatingLayer({ _, close ->
//                Surface(shape = MaterialTheme.shapes.extraSmall,
//                    shadowElevation = 6.dp, tonalElevation = 1.dp) {
//                    Column {
//                        arrayOf(2, 4, 8, 16).forEach {
//                            MenuItem({
//                                close()
//                                EchoInMirror.currentPosition.timeSigDenominator = it
//                            }, EchoInMirror.currentPosition.timeSigDenominator == it) {
//                                Text(it.toString())
//                            }
//                        }
//                    }
//                }
//            }) {
//                AppBarTitle(EchoInMirror.currentPosition.timeSigDenominator.toString())
//            }
//        }
//    }
//}

@Composable
private fun BPM() {
    CustomOutlinedTextField(
        "%.2f".format(EchoInMirror.currentPosition.bpm),
        {
            EchoInMirror.currentPosition.bpm = it.toDoubleOrNull()?.coerceIn(1.0, 600.0) ?: return@CustomOutlinedTextField
        },
        Modifier.width(100.dp),
        singleLine = true,
        textStyle = MaterialTheme.typography.labelLarge.copy(LocalContentColor.current),
        prefix = {
            Icon(MetronomeTick, "BPM", modifier = Modifier.size(18.dp).offset())
        },
        suffix = {
            NumberInputArrows {
                EchoInMirror.currentPosition.bpm = (EchoInMirror.currentPosition.bpm + it).coerceIn(1.0, 600.0)
            }
        },
        colors = textFieldGrayColors(),
        keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number),
        paddingValues = TextFieldDefaults.contentPaddingWithLabel(6.dp, 6.dp, 3.dp, 4.dp)
    )
}

val APP_BAR_ACTIONS_ICON_MODIFIER = Modifier.size(18.dp)
private val LeftContent: @Composable RowScope.() -> Unit = {
    SegmentedButtons(borderColor = MaterialTheme.colorScheme.outlineVariant) {
        EditorTool.entries.apply {
            forEachIndexed { index, tool ->
                key(tool) {
                    val selected = EchoInMirror.editorTool == tool
                    SegmentedButton({ EchoInMirror.editorTool = tool }, selected, showIcon = false) {
                        if (index == 0) Spacer(Modifier.width(3.dp))
                        Icon(EDITOR_TOOL_ICONS[index], tool.name, APP_BAR_ACTIONS_ICON_MODIFIER,
                            if (selected) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.outline)
                        if (index == size - 1) Spacer(Modifier.width(3.dp))
                    }
                    if (index < size - 1) SegmentedDivider()
                }
            }
        }
    }
}

private val CenterContent: @Composable RowScope.() -> Unit = {
    IconButton({ EchoInMirror.currentPosition.isPlaying = !EchoInMirror.currentPosition.isPlaying }) {
        Icon(
            imageVector = if (EchoInMirror.currentPosition.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
            contentDescription = if (EchoInMirror.currentPosition.isPlaying) "Pause" else "Play"
        )
    }
    IconButton({
        EchoInMirror.currentPosition.isPlaying = false
        EchoInMirror.currentPosition.ppqPosition = 0.0
    }) {
        Icon(
            imageVector = Icons.Filled.Stop,
            contentDescription = "Stop"
        )
    }
    IconButton({ }, colors = IconButtonDefaults.iconButtonColors(contentColor = MaterialTheme.colorScheme.absoluteError)) {
        Icon(
            imageVector = Icons.Filled.FiberManualRecord,
            contentDescription = "Record"
        )
    }

    FilledTonalIconButton({
        EchoInMirror.currentPosition.isLooping = !EchoInMirror.currentPosition.isLooping
    }, Modifier.padding(start = 6.dp).size(32.dp),
        colors = if (EchoInMirror.currentPosition.isLooping)
            IconButtonDefaults.iconButtonColors(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.onPrimary)
        else IconButtonDefaults.iconButtonColors(MaterialTheme.colorScheme.secondary, MaterialTheme.colorScheme.onSecondary.copy(0.84F))
    ) {
        Icon(
            imageVector = Icons.Filled.Loop,
            contentDescription = "Loop",
            Modifier.size(22.dp)
        )
    }
}

private val RightContent: @Composable RowScope.() -> Unit = {
    Gap(16)
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        Quantification()
        CurrentTime()
        BPM()
    }
}

@Composable
internal fun FrameWindowScope.EimAppBar() {
    Surface(modifier = Modifier.fillMaxWidth(), shadowElevation = 2.dp, tonalElevation = 2.dp) {
        Column(Modifier.fillMaxWidth()) {
            TitleBar()
            Layout(
                {
                    Row(verticalAlignment = Alignment.CenterVertically, content = LeftContent)
                    Row(verticalAlignment = Alignment.CenterVertically, content = CenterContent)
                    Row(verticalAlignment = Alignment.CenterVertically, content = RightContent)
                },
                Modifier.height(52.dp).padding(horizontal = 10.dp)
            ) { (left, center, right), constraints ->
                var width = constraints.maxWidth
                val centerPlaceable = center.measure(constraints.copy(0))
                width -= centerPlaceable.width
                val rightPlaceable = if (width > 0) right.measure(constraints.copy(0, width)) else null
                width -= rightPlaceable?.width ?: 0
                val leftPlaceable = if (width > 0) left.measure(constraints.copy(0, width)) else null
                var mid = constraints.maxWidth / 2
                val rightX = constraints.maxWidth - (rightPlaceable?.width ?: 0)
                if (mid + centerPlaceable.width / 2 > rightX) mid = rightX - centerPlaceable.width / 2

                layout(constraints.maxWidth, constraints.maxHeight) {
                    rightPlaceable?.place(rightX, 0)
                    leftPlaceable?.place(0, 0)
                    centerPlaceable.place(mid - centerPlaceable.width / 2, 0)
                }
            }
        }
    }
}
