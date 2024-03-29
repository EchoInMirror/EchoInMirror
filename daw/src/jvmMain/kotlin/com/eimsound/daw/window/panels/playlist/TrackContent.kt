@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")

package com.eimsound.daw.window.panels.playlist

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitPointerSlopOrCancellation
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastAll
import androidx.compose.ui.util.fastForEach
import com.eimsound.audioprocessor.oneBarPPQ
import com.eimsound.daw.actions.doClipsAmountAction
import com.eimsound.daw.actions.doClipsDisabledAction
import com.eimsound.daw.actions.doClipsEditActionAction
import com.eimsound.daw.actions.doClipsSplitAction
import com.eimsound.daw.api.*
import com.eimsound.daw.api.clips.*
import com.eimsound.daw.api.processor.Track
import com.eimsound.daw.components.LocalFloatingLayerProvider
import com.eimsound.daw.components.utils.EditAction
import com.eimsound.daw.components.utils.HorizontalResize
import com.eimsound.daw.components.utils.onRightClick
import com.eimsound.daw.components.utils.toOnSurfaceColor
import com.eimsound.daw.utils.fitInUnit
import com.eimsound.daw.utils.fitInUnitFloor
import com.eimsound.daw.utils.isCrossPlatformCtrlPressed
import kotlin.math.absoluteValue
import kotlin.math.roundToInt

private const val RESIZE_HAND_WIDTH = 4
private val RESIZE_HAND_MODIFIER = Modifier.width(RESIZE_HAND_WIDTH.dp).fillMaxHeight()
    .pointerHoverIcon(PointerIcon.HorizontalResize)

internal var resizeDirectionRight = false

private suspend fun AwaitPointerEventScope.handleDragEvent(
    playlist: Playlist, clip: TrackClip<*>, index: Int, track: Track
) {
    var event: PointerEvent
    playlist.apply {
        do {
            event = awaitPointerEvent(PointerEventPass.Initial)
            if (event.type == PointerEventType.Press) {
                if (event.buttons.isPrimaryPressed) {
                    EchoInMirror.selectedTrack = track
                    when (EchoInMirror.editorTool) {
                        EditorTool.ERASER -> {
                            if (event.keyboardModifiers.isCrossPlatformCtrlPressed) continue
                            selectedClips.clear()
                            listOf(clip).doClipsAmountAction(true)
                            continue
                        }
                        EditorTool.MUTE -> {
                            if (event.keyboardModifiers.isCrossPlatformCtrlPressed) continue
                            selectedClips.clear()
                            listOf(clip).doClipsDisabledAction()
                            continue
                        }
                        EditorTool.CUT -> {
                            if (event.keyboardModifiers.isCrossPlatformCtrlPressed) continue
                            selectedClips.clear()
                            val noteWidthPx = noteWidth.value.toPx()
                            val start = (verticalScrollState.value / noteWidthPx - clip.time).coerceAtLeast(0F)
                            val clickTime = (event.changes.first().position.x / noteWidthPx + start)
                                .fitInUnit(EchoInMirror.editUnit)
                            listOf(clip).doClipsSplitAction(clickTime)
                            continue
                        }
                        else -> {
                            isCurrentCursorSelected = true
                            EchoInMirror.selectedClip = clip
                            if (!selectedClips.contains(clip)) {
                                selectedClips.clear()
                                selectedClips.add(clip)
                            }
                        }
                    }
                    if (event.keyboardModifiers.isCrossPlatformCtrlPressed) continue
                    break
                } else if (event.buttons.isSecondaryPressed) {
                    action = EditAction.NONE
                    break
                } else if (event.buttons.isBackPressed) {
                    deletionList.add(clip)
                    continue
                }
            }
        } while (!event.changes.fastAll(PointerInputChange::changedToDownIgnoreConsumed))
        val down = event.changes[0]
        awaitPointerSlopOrCancellation(down.id, down.type, triggerOnMainAxisSlop = false) { change, _ ->
            var yAllowRange: IntRange? = null
            var minClipDuration = Int.MAX_VALUE
            var selectedClipsLeft = Int.MAX_VALUE
            var trackToIndex: HashMap<Track, Int>? = null
            var allowExpandableLeft = Int.MIN_VALUE
            var allowExpandableRight = Int.MAX_VALUE
            if (event.buttons.isPrimaryPressed) {
                val resizeHandWidth = (RESIZE_HAND_WIDTH * density).roundToInt()
                val rdr = change.position.x >= size.width - resizeHandWidth
                resizeDirectionRight = rdr
                action = if (change.position.x <= resizeHandWidth || rdr) {
                    selectedClips.forEach {
                        val left = it.time
                        if (left < selectedClipsLeft) selectedClipsLeft = left
                        val duration = it.duration
                        if (!it.clip.isExpandable) {
                            if (allowExpandableLeft < it.start) allowExpandableLeft = it.start
                            if (it.clip.maxDuration > 0 && it.clip.maxDuration - duration < allowExpandableRight)
                                allowExpandableRight = it.clip.maxDuration - duration
                        }
                        if (duration < minClipDuration) minClipDuration = duration
                    }
                    EditAction.RESIZE
                } else {
                    trackToIndex = hashMapOf()
                    getAllTrackHeights(density)
                    trackHeights.forEachIndexed { index, (track) -> trackToIndex[track] = index }
                    var selectedClipsTop = Int.MAX_VALUE
                    var selectedClipsBottom = 0
                    selectedClips.forEach {
                        val top = trackToIndex[it.track] ?: return
                        val left = it.time
                        if (top < selectedClipsTop) selectedClipsTop = top
                        if (top > selectedClipsBottom) selectedClipsBottom = top
                        if (left < selectedClipsLeft) selectedClipsLeft = left
                    }
                    yAllowRange = -selectedClipsTop..(trackHeights.size - selectedClipsBottom)
                    EditAction.MOVE
                }
            }
            when (action) {
                EditAction.MOVE -> {
                    change.consume()
                    drag(down.id) {
                        val currentY = dragStartY + it.position.y - change.position.y
                        val cur = binarySearchTrackByHeight(currentY)
                        deltaY = (cur - index).coerceIn(yAllowRange!!)
                        deltaX = ((it.position.x - change.position.x) / noteWidth.value.toPx())
                            .fitInUnit(EchoInMirror.editUnit).coerceAtLeast(-selectedClipsLeft)
                        it.consume()
                    }
                    if (deltaX != 0 || deltaY != 0) {
                        action = EditAction.NONE
                        val y = deltaY
                        val x = deltaX
                        deltaY = 0
                        deltaX = 0
                        selectedClips.toList().doClipsEditActionAction(x, 0, 0,
                            if (y == 0) null
                            else try {
                                selectedClips.map { trackHeights[trackToIndex!![it.track]!! + y].track }
                            } catch (e: Throwable) {
                                if (e !is IndexOutOfBoundsException) e.printStackTrace()
                                null
                            }
                        )
                        EchoInMirror.selectedTrack = trackHeights[(index + y).coerceAtMost(trackHeights.size - 1)].track
                    }
                    trackHeights.clear()
                    action = EditAction.NONE
                }
                EditAction.RESIZE -> {
                    change.consume()
                    drag(down.id) {
                        val noteUnit = EchoInMirror.editUnit
                        var x = ((it.position.x - change.position.x) / noteWidth.value.toPx()).fitInUnit(noteUnit)
                        if (resizeDirectionRight) {
                            if (minClipDuration + x < noteUnit) x = noteUnit - minClipDuration
                            if (allowExpandableRight != Int.MAX_VALUE && allowExpandableRight < x) x = allowExpandableRight
                        } else {
                            if (x < -selectedClipsLeft) x = -selectedClipsLeft
                            if (allowExpandableLeft != Int.MIN_VALUE && -allowExpandableLeft > x) x = -allowExpandableLeft
                        }
                        if (x != deltaX) deltaX = x
                        it.consume()
                    }
                    if (deltaX != 0) {
                        action = EditAction.NONE
                        val x = deltaX
                        deltaX = 0
                        if (resizeDirectionRight) selectedClips.toList().doClipsEditActionAction(deltaDuration = x)
                        else selectedClips.toList().doClipsEditActionAction(x, -x, x)
                    }
                    action = EditAction.NONE
                }
                else -> { }
            }
        }
    }
}

@Composable
private fun Playlist.ClipItem(it: TrackClip<*>, track: Track, index: Int) {
    Box {
        with (LocalDensity.current) {
            val isSelected = selectedClips.contains(it)
            val scrollXPPQ = horizontalScrollState.value / noteWidth.value.toPx()
            val contentPPQ = contentWidth.value / noteWidth.value.value
            val action0 = action
            if (it.time <= scrollXPPQ + contentPPQ && it.time + it.duration >= scrollXPPQ) {
                val clipStartPPQ = it.time
                val clipEndPPQ = clipStartPPQ + it.duration
                var clipStartPPQOnMove = clipStartPPQ
                var clipEndPPQOnMove = clipEndPPQ
                var y = 0.dp
                val currentMoveObj = if (isSelected && action0 == EditAction.MOVE && trackHeights.isNotEmpty())
                    trackHeights[(deltaY + index).coerceAtMost(trackHeights.size - 1)] else null
                var currentMoveTrackHeight = currentMoveObj?.track?.height?.dp ?: 0.dp
                if (currentMoveTrackHeight == 0.dp) currentMoveTrackHeight = trackHeight
                val curTrackHeight = if (track.height == 0) trackHeight else track.height.dp
                if (isSelected) {
                    if (currentMoveObj != null) {
                        clipStartPPQOnMove += deltaX
                        clipEndPPQOnMove += deltaX
                        if (deltaY != 0) {
                            y = (currentMoveObj.height - trackHeights[index].height).toDp() -
                                    currentMoveTrackHeight + curTrackHeight +
                                    ((currentMoveTrackHeight - curTrackHeight).coerceAtLeast(0.dp) / 2)
                        }
                    } else if (action0 == EditAction.RESIZE) {
                        if (resizeDirectionRight) clipEndPPQOnMove += deltaX
                        else clipStartPPQOnMove += deltaX
                    }
                }
                val widthInPPQ = clipEndPPQOnMove.toFloat().coerceAtMost(scrollXPPQ + contentPPQ) -
                        clipStartPPQOnMove.toFloat().coerceAtLeast(scrollXPPQ)
                val curTrack = currentMoveObj?.track ?: track
                val curOrMovingTrackHeight = if (curTrack.height == 0) trackHeight else curTrack.height.dp
                val floatingLayerProvider = LocalFloatingLayerProvider.current
                Box(Modifier
                    .absoluteOffset(noteWidth.value * (clipStartPPQ - scrollXPPQ).coerceAtLeast(0F))
                    .onRightClick { pos, _ ->
                        floatingLayerProvider.openPlaylistMenu(pos, listOf(it), this@ClipItem)
                    }
                    .pointerInput(it, track, index) {
                        awaitEachGesture { handleDragEvent(this@ClipItem, it, index, track) }
                    }
                    .width(noteWidth.value * widthInPPQ)
                    .requiredHeight(curOrMovingTrackHeight)
                    .run {
                        if (clipStartPPQ != clipStartPPQOnMove) {
                            absoluteOffset(noteWidth.value * (
                                    if (clipStartPPQOnMove < scrollXPPQ) {
                                        if (deltaX < 0) -(clipStartPPQ - scrollXPPQ).coerceAtLeast(0F) else 0F
                                    } else clipStartPPQOnMove - clipStartPPQ.toFloat().coerceAtLeast(scrollXPPQ)), y)
                        } else if (y.value != 0F) absoluteOffset(0.dp, y)
                        else this
                    }
                ) {
                    if (!deletionList.contains(it)) {
                        val color = it.color ?: curTrack.color
                        var isDisabled = curTrack.isDisabled
                        if (!isDisabled) {
                            val tmp = it.isDisabled
                            isDisabled = if (action0 == EditAction.DISABLE && disableList.contains(it)) !tmp
                            else tmp
                        }
                        Column(
                            Modifier
                                .fillMaxSize()
                                .background(color.copy(if (isDisabled) 0.4F else 0.7F), MaterialTheme.shapes.extraSmall)
                                .run {
                                    if (isSelected) border(
                                        2.dp, MaterialTheme.colorScheme.primary,
                                        MaterialTheme.shapes.extraSmall
                                    )
                                    else this
                                }
                                .clip(MaterialTheme.shapes.extraSmall)
                                .run {
                                    if (EchoInMirror.editorTool.ordinal <= 1) pointerHoverIcon(action0.toPointerIcon(PointerIcon.Hand))
                                    else this
                                }
                        ) {
                            val contentColor = color.toOnSurfaceColor()
                            if (curOrMovingTrackHeight.value >= 40) {
                                Box(Modifier.fillMaxWidth().background(if (isDisabled) color.copy(0.5F) else color)) {
                                    Row(Modifier.padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                                        it.clip.icon?.let { icon ->
                                            Icon(icon, it.clip.name, Modifier.size(15.dp).padding(end = 2.dp), contentColor)
                                        }
                                        Text(
                                            it.clip.name.ifEmpty { track.name },
                                            color = contentColor, style = MaterialTheme.typography.labelMedium,
                                            maxLines = 1, overflow = TextOverflow.Ellipsis,
                                            textDecoration = if (isDisabled) TextDecoration.LineThrough else null
                                        )
                                    }
                                }
                            }
                            @Suppress("UNCHECKED_CAST")
                            (it.clip.factory as ClipFactory<Clip>).PlaylistContent(
                                it as TrackClip<Clip>, track,
                                contentColor.copy(animateFloatAsState(if (isSelected) 1F else 0.8F).value),
                                noteWidth,
                                (scrollXPPQ - clipStartPPQOnMove).coerceAtLeast(0F) +
                                        (it.start + if (isSelected && action0 == EditAction.RESIZE &&
                                            !resizeDirectionRight) deltaX else 0),
                                widthInPPQ
                            )
                        }
                        Spacer(RESIZE_HAND_MODIFIER)
                        Spacer(RESIZE_HAND_MODIFIER.align(Alignment.TopEnd))
                    }
                }
            }
        }
    }
}

@Composable
private fun Playlist.TrackItems(track: Track, index: Int) {
    track.clips.read()
    track.clips.fastForEach {
        key(it) {
            ClipItem(it, track, index)
        }
    }
}

@Composable
private fun SubTrackContents(index: Int, track: Track, playlist: Playlist): Int {
    var i = index + 1
    if (!track.collapsed) track.subTracks.fastForEach { key(it) { i += TrackContent(playlist, it, i) } }
    return i - index
}

private fun Density.createNewClip(
    playlist: Playlist, track: Track, x: Float,
    clip: Clip = ClipManager.instance.defaultMidiClipFactory.createClip(),
    editUnit: Int = EchoInMirror.currentPosition.oneBarPPQ,
    len: Int = EchoInMirror.currentPosition.oneBarPPQ,
    start: Int = 0
) {
    playlist.selectedClips.clear()
    val newClip = ClipManager.instance.createTrackClip(
        clip,
        ((x + playlist.horizontalScrollState.value) / playlist.noteWidth.value.toPx()).fitInUnitFloor(editUnit),
        len,
        start,
        track
    )
    listOf(newClip).doClipsAmountAction(false)
    playlist.selectedClips.clear()
    playlist.selectedClips.add(newClip)
    playlist.isCurrentCursorSelected = true
}
private fun Density.dupeClip(playlist: Playlist, track: Track, x: Float, clip: TrackClip<*>) {
    createNewClip(playlist, track, x, clip.clip.copy(), EchoInMirror.editUnit, clip.duration, clip.start)
}

@Composable
internal fun TrackContent(playlist: Playlist, track: Track, index: Int): Int {
    playlist.apply {
        Box(Modifier.fillMaxWidth().height(if (track.height == 0) trackHeight else track.height.dp)) {
            Box(Modifier.fillMaxSize().pointerInput(track) { // Double click
                awaitPointerEventScope {
                    var time = 0L
                    var lastPos: Offset? = null
                    while (true) {
                        val event = awaitFirstDown(false)
                        if (action != EditAction.NONE) continue
                        when (EchoInMirror.editorTool) {
                            EditorTool.CURSOR -> {
                                if (lastPos != null && (event.position - lastPos).getDistanceSquared() > 20 * density * density) {
                                    time = 0
                                    lastPos = null
                                    continue
                                }
                                time = if (event.previousUptimeMillis - time < viewConfiguration.longPressTimeoutMillis) {
                                    createNewClip(playlist, track, event.position.x)
                                    lastPos = null
                                    0L
                                } else {
                                    lastPos = event.position
                                    event.previousUptimeMillis
                                }
                            }
                            EditorTool.PENCIL -> {
                                val clip = playlist.selectedClips.firstOrNull()
                                if (clip == null) createNewClip(playlist, track, event.position.x)
                                else dupeClip(playlist, track, event.position.x, clip)
                            }
                            else -> { }
                        }
                    }
                }
            })
            TrackItems(track, index)
        }
    }
    Divider()
    return SubTrackContents(index, track, playlist)
}

@Composable
internal fun TrackSelection(playlist: Playlist, density: Density, horizontalScrollState: ScrollState, verticalScrollState: ScrollState) {
    if (playlist.action != EditAction.SELECT) return
    val scrollX = horizontalScrollState.value
    val scrollY = verticalScrollState.value
    val primaryColor = MaterialTheme.colorScheme.primary
    playlist.apply {
        with (density) {
            Spacer(
                Modifier.size(
                    (selectionX.coerceAtLeast(scrollX.toFloat()) - selectionStartX).absoluteValue.toDp(),
                    (selectionY.coerceAtLeast(scrollY.toFloat()) - selectionStartY).absoluteValue.toDp()
                ).offset(
                    (selectionStartX.coerceAtMost(selectionX) - scrollX).coerceAtLeast(0F).toDp(),
                    (selectionStartY.coerceAtMost(selectionY) - scrollY).coerceAtLeast(0F).toDp()
                ).background(primaryColor.copy(0.1F)).border(1.dp, primaryColor)
            )
        }
    }
}
