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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastAll
import androidx.compose.ui.util.fastForEach
import com.eimsound.audioprocessor.oneBarPPQ
import com.eimsound.daw.actions.doClipsAmountAction
import com.eimsound.daw.actions.doClipsEditActionAction
import com.eimsound.daw.api.ClipManager
import com.eimsound.daw.api.EchoInMirror
import com.eimsound.daw.api.TrackClip
import com.eimsound.daw.api.defaultMidiClipFactory
import com.eimsound.daw.api.processor.Track
import com.eimsound.daw.components.utils.EditAction
import com.eimsound.daw.components.utils.HorizontalResize
import com.eimsound.daw.components.utils.toOnSurfaceColor
import com.eimsound.daw.utils.fitInUnit
import com.eimsound.daw.utils.isCrossPlatformCtrlPressed
import kotlin.math.absoluteValue

private val RESIZE_HAND_MODIFIER = Modifier.width(4.dp).fillMaxHeight()
    .pointerHoverIcon(PointerIcon.HorizontalResize)

internal var resizeDirectionRight = false

private suspend fun AwaitPointerEventScope.handleDragEvent(playlist: Playlist, clip: TrackClip<*>, index: Int, track: Track) {
    var event: PointerEvent
    playlist.apply {
        do {
            event = awaitPointerEvent(PointerEventPass.Initial)
            if (event.type == PointerEventType.Press) {
                if (event.buttons.isPrimaryPressed) {
                    EchoInMirror.selectedTrack = track
                    EchoInMirror.selectedClip = clip
                    if (!selectedClips.contains(clip)) {
                        selectedClips.clear()
                        selectedClips.add(clip)
                    }
                    if (event.keyboardModifiers.isCrossPlatformCtrlPressed) continue
                    break
                } else if (event.buttons.isSecondaryPressed) {
                    selectedClips.clear()
                    action = EditAction.NONE
                    break
                } else if (event.buttons.isTertiaryPressed) {
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
                val fourDp = 4 * density
                val rdr = change.position.x > size.width - fourDp
                resizeDirectionRight = rdr
                action = if (change.position.x < fourDp || rdr) {
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
                                e.printStackTrace()
                                null
                            }
                        )
                        EchoInMirror.selectedTrack = trackHeights[(index + y).coerceAtMost(trackHeights.size - 1)].track
                    }
                    trackHeights.clear()
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
                }
                else -> { }
            }
        }
    }
}

@Composable
private fun Playlist.ClipItem(density: Density, it: TrackClip<*>, track: Track, index: Int) {
    Box {
        with (density) {
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
                Box(Modifier
                    .absoluteOffset(noteWidth.value * (clipStartPPQ - scrollXPPQ).coerceAtLeast(0F))
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
                        val trackColor = curTrack.color
                        Column(
                            Modifier
                                .fillMaxSize()
                                .background(trackColor.copy(if (curTrack.isBypassed) 0.4F else 0.7F), MaterialTheme.shapes.extraSmall)
                                .run {
                                    if (isSelected) border(
                                        2.dp, MaterialTheme.colorScheme.primary,
                                        MaterialTheme.shapes.extraSmall
                                    )
                                    else this
                                }
                                .clip(MaterialTheme.shapes.extraSmall)
                                .pointerHoverIcon(action0.toPointerIcon(PointerIcon.Hand))
                        ) {
                            val contentColor = trackColor.toOnSurfaceColor()
                            if (curOrMovingTrackHeight.value >= 40) {
                                Text(
                                    it.clip.name.ifEmpty { track.name },
                                    Modifier.fillMaxWidth()
                                        .background(if (curTrack.isBypassed) trackColor.copy(0.5F) else trackColor)
                                        .padding(horizontal = 4.dp),
                                    contentColor, style = MaterialTheme.typography.labelMedium,
                                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                                    textDecoration = if (curTrack.isBypassed) TextDecoration.LineThrough else null
                                )
                            }
                            @Suppress("TYPE_MISMATCH")
                            it.clip.factory.PlaylistContent(
                                it, track,
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
private fun Playlist.TrackItems(track: Track, density: Density, index: Int) {
    track.clips.read()
    track.clips.fastForEach {
        key(it) {
            ClipItem(density, it, track, index)
        }
    }
}

@Composable
private fun SubTrackContents(index: Int, track: Track, density: Density, playlist: Playlist): Int {
    var i = index + 1
    if (!track.collapsed) track.subTracks.fastForEach { key(it) { i += TrackContent(playlist, it, i, density) } }
    return i - index
}

@Composable
internal fun TrackContent(playlist: Playlist, track: Track, index: Int, density: Density): Int {
    playlist.apply {
        Box(Modifier.fillMaxWidth().height(if (track.height == 0) trackHeight else track.height.dp)) {
            Box(Modifier.fillMaxSize().pointerInput(track) {
                awaitPointerEventScope {
                    var time = 0L
                    while (true) {
                        val event = awaitFirstDown(false)
                        if (action != EditAction.NONE) continue
                        time = if (event.previousUptimeMillis - time < viewConfiguration.longPressTimeoutMillis) {
                            val len = EchoInMirror.currentPosition.oneBarPPQ
                            val clip = ClipManager.instance.createTrackClip(
                                ClipManager.instance.defaultMidiClipFactory.createClip(),
                                (event.position.x / noteWidth.value.toPx()).fitInUnit(len),
                                len,
                                0,
                                track
                            )
                            listOf(clip).doClipsAmountAction(false)
                            0L
                        } else event.previousUptimeMillis
                    }
                }
            })
            TrackItems(track, density, index)
        }
    }
    Divider()
    return SubTrackContents(index, track, density, playlist)
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
