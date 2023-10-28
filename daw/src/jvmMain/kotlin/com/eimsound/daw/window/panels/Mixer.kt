package com.eimsound.daw.window.panels

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Piano
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.eimsound.audioprocessor.AudioProcessor
import com.eimsound.audioprocessor.AudioProcessorDescriptionAndFactory
import com.eimsound.audioprocessor.createAudioProcessorOrNull
import com.eimsound.daw.actions.doAddOrRemoveAudioProcessorAction
import com.eimsound.daw.actions.doMoveAudioProcessorAction
import com.eimsound.daw.actions.doReplaceAudioProcessorAction
import com.eimsound.daw.api.EchoInMirror
import com.eimsound.daw.api.processor.Bus
import com.eimsound.daw.api.processor.Track
import com.eimsound.daw.api.processor.TrackAudioProcessorWrapper
import com.eimsound.daw.api.window.Panel
import com.eimsound.daw.api.window.PanelDirection
import com.eimsound.daw.components.*
import com.eimsound.daw.components.dragdrop.GlobalDraggable
import com.eimsound.daw.components.dragdrop.GlobalDropTarget
import com.eimsound.daw.components.dragdrop.LocalGlobalDragAndDrop
import com.eimsound.daw.components.menus.openAudioProcessorMenu
import com.eimsound.daw.components.silder.DefaultTrack
import com.eimsound.daw.components.silder.Slider
import com.eimsound.daw.components.utils.clickableWithIcon
import com.eimsound.daw.components.utils.onRightClickOrLongPress
import com.eimsound.daw.components.utils.toOnSurfaceColor
import com.eimsound.daw.window.dialogs.openQuickLoadDialog
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

private val TRACK_WIDTH = 90.dp

data class TrackAudioProcessorMoveAction(val index: Int, val list: MutableList<TrackAudioProcessorWrapper>)

@OptIn(DelicateCoroutinesApi::class)
@Composable
private fun MixerProcessorDropTarget(
    isLoading: MutableState<Boolean>, list: MutableList<TrackAudioProcessorWrapper>, index: Int,
    onDrop: (AudioProcessor) -> Unit, modifier: Modifier = Modifier, content: @Composable BoxScope.(Boolean) -> Unit
) {
    val indexValue by rememberUpdatedState(index)
    val listValue by rememberUpdatedState(list)
    val snackbarProvider = LocalSnackbarProvider.current
    GlobalDropTarget({ data, _ ->
        if (isLoading.value) return@GlobalDropTarget
        when (data) {
            is AudioProcessorDescriptionAndFactory -> {
                GlobalScope.launch {
                    val (ap, err) = data.factory.createAudioProcessorOrNull(data.description)
                    if (err != null) {
                        snackbarProvider.enqueueSnackbar(err)
                        return@launch
                    }
                    if (ap == null) return@launch
                    isLoading.value = true
                    onDrop(ap)
                    isLoading.value = false
                }
            }
            is TrackAudioProcessorMoveAction -> {
                data.list.doMoveAudioProcessorAction(data.index, listValue, indexValue)
            }
        }
    }, modifier) {
        val isActive = it != null && LocalGlobalDragAndDrop.current.dataTransfer.let { d ->
            d is AudioProcessorDescriptionAndFactory || d is TrackAudioProcessorMoveAction
        }
        Box(
            Modifier.fillMaxSize().background(
                MaterialTheme.colorScheme.primary.copy(animateFloatAsState(if (isActive) 0.3f else 0f).value),
                CircleShape
            )
        ) {
            content(it == null)
        }
    }
}

@Composable
private fun MixerProcessorDragAndDropTarget(
    isLoading: MutableState<Boolean>, list: MutableList<TrackAudioProcessorWrapper>, index: Int,
    onDrop: (AudioProcessor) -> Unit, onDrag: (() -> Any)? = null,
    modifier: Modifier = Modifier, content: @Composable BoxScope.(Boolean) -> Unit
) {
    if (onDrag == null) MixerProcessorDropTarget(isLoading, list, index, onDrop, modifier, content)
    else GlobalDraggable(onDrag, draggingComponent = { Box(Modifier.size(TRACK_WIDTH - 4.dp, 30.dp)) { content(true) } }) {
        MixerProcessorDropTarget(isLoading, list, index, onDrop, modifier, content)
    }
}

private val BUTTON_PADDINGS = PaddingValues(6.dp, 0.dp)
@Composable
private fun MixerProcessorButton(isLoading: MutableState<Boolean>, list: MutableList<TrackAudioProcessorWrapper>,
                                 wrapper: TrackAudioProcessorWrapper? = null, index: Int = -1, fontStyle: FontStyle? = null,
                                 fontWeight: FontWeight? = null, onClick: () -> Unit) {
    val indexValue by rememberUpdatedState(index)
    val listValue by rememberUpdatedState(list)
    MixerProcessorDragAndDropTarget(
        isLoading, list, index,
        {
            if (wrapper == null) list.doAddOrRemoveAudioProcessorAction(it)
            else list.doReplaceAudioProcessorAction(it, index)
        },
        wrapper?.let { { TrackAudioProcessorMoveAction(indexValue, listValue) } },
        Modifier.height(28.dp).fillMaxWidth()
    ) {
        if (it) {
            var modifier = Modifier.fillMaxSize()
            if (wrapper != null) {
                val pos = remember { arrayOf(Offset.Zero) }
                val floatingLayerProvider = LocalFloatingLayerProvider.current
                val snackbarProvider = LocalSnackbarProvider.current
                modifier = modifier.onGloballyPositioned { p ->
                    pos[0] = p.positionInRoot()
                }.onRightClickOrLongPress { p ->
                    if (!isLoading.value)
                        floatingLayerProvider.openAudioProcessorMenu(pos[0] + p, wrapper, list, index,
                            snackbarProvider, isLoading)
                }
            }
            TextButton(onClick, modifier, contentPadding = BUTTON_PADDINGS) {
                if (wrapper?.description?.isInstrument == true) {
                    Icon(Icons.Default.Piano, null, Modifier.size(16.dp))
                    Gap(2)
                }
                if (wrapper == null) Text(
                    "...", fontSize = MaterialTheme.typography.labelMedium.fontSize,
                    maxLines = 1, lineHeight = 7.sp, fontStyle = fontStyle, fontWeight = fontWeight
                )
                else Marquee {
                    Text(
                        wrapper.name, Modifier.fillMaxWidth(),
                        fontSize = MaterialTheme.typography.labelSmall.fontSize, maxLines = 1, lineHeight = 7.sp,
                        textAlign = TextAlign.Center, fontStyle = fontStyle, fontWeight = fontWeight,
                        textDecoration = if (wrapper.isBypassed) TextDecoration.LineThrough else null,
                        color = if (wrapper.isBypassed) LocalContentColor.current.copy(alpha = 0.5f)
                        else LocalContentColor.current
                    )
                }
            }
        } else Text(if (wrapper == null) "添加" else "替换", Modifier.align(Alignment.Center),
            style = MaterialTheme.typography.labelMedium)
    }
}

@OptIn(DelicateCoroutinesApi::class)
@Composable
private fun MixerProcessorButtons(isLoading: MutableState<Boolean>, list: MutableList<TrackAudioProcessorWrapper>) {
    MixerProcessorDropTarget(isLoading, list, 0, { list.doAddOrRemoveAudioProcessorAction(it, index = 0) }) {
        Divider(Modifier.padding(6.dp, 2.dp), 2.dp, MaterialTheme.colorScheme.primary)
    }
    list.forEachIndexed { i, it ->
        key(it) {
            MixerProcessorButton(isLoading, list, it, i, onClick = it::onClick)
            MixerProcessorDropTarget(isLoading, list, i + 1,
                { list.doAddOrRemoveAudioProcessorAction(it, index = i + 1) }) {
                Divider(Modifier.padding(8.dp, 2.dp))
            }
        }
    }
    val floatingLayerProvider = LocalFloatingLayerProvider.current
    val snackbarProvider = LocalSnackbarProvider.current
    MixerProcessorButton(isLoading, list, fontWeight = FontWeight.Bold) {
        if (!isLoading.value) floatingLayerProvider.openQuickLoadDialog {
            if (it == null) return@openQuickLoadDialog
            isLoading.value = true
            GlobalScope.launch {
                val (ap, err) = it.factory.createAudioProcessorOrNull(it.description)
                if (err != null) {
                    snackbarProvider.enqueueSnackbar(err)
                    return@launch
                }
                if (ap == null) return@launch
                list.doAddOrRemoveAudioProcessorAction(ap)
            }.invokeOnCompletion { isLoading.value = false }
        }
    }
}

@Composable
private fun TrackName(track: Track, trackColor: Color, index: String) {
    Row(Modifier.background(trackColor).height(24.dp)
        .clickableWithIcon { EchoInMirror.selectedTrack = track }
        .padding(vertical = 2.5.dp).zIndex(2f)) {
        val color = trackColor.toOnSurfaceColor()
        Text(
            index,
            Modifier.padding(start = 5.dp, end = 3.dp),
            color = color,
            fontSize = MaterialTheme.typography.labelLarge.fontSize,
            lineHeight = 18.0.sp,
            fontWeight = FontWeight.Bold
        )
        Marquee {
            Text(
                track.name,
                color = color,
                fontSize = MaterialTheme.typography.labelLarge.fontSize,
                lineHeight = 18.0.sp,
            )
        }
    }
}

@Composable
private fun PanSlider(track: Track) {
    Slider(
        track.pan,
        { track.pan = it },
        valueRange = -1f..1f,
        modifier = Modifier.fillMaxWidth(),
        track = { modifier, progress, interactionSource, tickFractions, enabled, isVertical ->
            DefaultTrack(modifier, progress, interactionSource, tickFractions, enabled, isVertical, startPoint = 0.5f)
        }
    )
}

@Composable
private fun VolumeText(track: Track) {
    Text(
        track.levelMeter.cachedMaxLevelString,
        Modifier.width(30.dp).padding(start = 2.dp),
        fontSize = MaterialTheme.typography.labelMedium.fontSize,
        textAlign = TextAlign.Center,
        letterSpacing = (-1).sp,
        lineHeight = 12.sp,
        maxLines = 1
    )
}

@Composable
private fun TrackLevels(track: Track) {
    Row(
        Modifier.fillMaxWidth().padding(bottom = 2.dp),
        horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically
    ) {
        VolumeSlider(track)
        LevelHints(Modifier.height(136.dp).width(24.dp))
        Level(track.levelMeter, Modifier.height(136.dp))
    }
}

@Composable
private fun SubTracksButton(track: Track, index: String, containerColor: Color, depth: Int) {
    if (track.subTracks.isNotEmpty()) Row(Modifier.padding(horizontal = 7.dp)) {
        track.subTracks.forEachIndexed { i, it ->
            key(it) {
                MixerTrack(it, "$index.${i + 1}", containerColor, depth + 1)
            }
        }
    }
}

@Composable
private fun MixerTrack(track: Track, index: String, containerColor: Color = MaterialTheme.colorScheme.surface,
                       depth: Int = 0, renderChildren: Boolean = true) {
    Row(Modifier.padding(7.dp, 14.dp, 7.dp, 14.dp)
        .shadow(1.dp, MaterialTheme.shapes.medium, clip = false)
        .background(MaterialTheme.colorScheme.surfaceColorAtElevation(((depth + 2) * 2).dp))
        .clip(MaterialTheme.shapes.medium)) {
        val trackColor = if (track is Bus) MaterialTheme.colorScheme.primary else track.color
        val isSelected = EchoInMirror.selectedTrack == track
        var curModifier = Modifier.width(TRACK_WIDTH)
        if (isSelected) curModifier = curModifier.border(1.dp, trackColor, MaterialTheme.shapes.medium)
        Layout({
            Column(curModifier.shadow(1.dp, MaterialTheme.shapes.medium, clip = false)
                .background(containerColor, MaterialTheme.shapes.medium)
                .clip(MaterialTheme.shapes.medium)) {
                TrackName(track, trackColor, index)

                Column(Modifier.padding(4.dp)) {
                    PanSlider(track)

                    Row(
                        Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton({ track.isBypassed = !track.isBypassed }, 20.dp) {
                            if (track.isBypassed) Icon(Icons.Default.VolumeOff, null, tint = MaterialTheme.colorScheme.error)
                            else Icon(Icons.Default.VolumeUp, null)
                        }
                        Gap(6)
                        VolumeText(track)
                    }

                    TrackLevels(track)
                }

                Column {
                    val loading = remember { mutableStateOf(false) }
                    MixerProcessorButtons(loading, track.preProcessorsChain)
                    MixerProcessorButtons(loading, track.postProcessorsChain)
                }
            }
            if (renderChildren) SubTracksButton(track, index, containerColor, depth)
        }) { measurables, constraints ->
            if (measurables.size > 1) {
                val content = measurables[1].measure(constraints)
                var maxHeight = content.height
                val mixer = measurables[0].measure(constraints.copy(minHeight = maxHeight))
                if (mixer.height > maxHeight) maxHeight = mixer.height
                layout(content.width + mixer.width, maxHeight) {
                    mixer.place(0, 0)
                    content.place(mixer.width, 0)
                }
            } else {
                val mixer = measurables[0].measure(constraints)
                layout(mixer.width, mixer.height) {
                    mixer.place(0, 0)
                }
            }
        }
    }
}

object Mixer: Panel {
    override val name = "混音台"
    override val direction = PanelDirection.Horizontal

    @Composable
    override fun Icon() {
        Icon(Icons.Default.Tune, name)
    }

    @Composable
    override fun Content() {
        Scrollable {
            Row {
                val bus = EchoInMirror.bus!!
                val trackColor = if (EchoInMirror.windowManager.isDarkTheme)
                    MaterialTheme.colorScheme.surfaceColorAtElevation(20.dp) else MaterialTheme.colorScheme.surface
                MixerTrack(bus, "0", trackColor, renderChildren = false)
                bus.subTracks.forEachIndexed { i, it ->
                    key(it) {
                        MixerTrack(it, (i + 1).toString(), trackColor)
                    }
                }
            }
        }
    }
}
