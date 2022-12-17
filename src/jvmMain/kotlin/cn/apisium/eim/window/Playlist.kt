package cn.apisium.eim.window

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import cn.apisium.eim.EchoInMirror
import cn.apisium.eim.api.Track
import cn.apisium.eim.components.*
import cn.apisium.eim.components.app.APP_BAR_FULL_HEIGHT
import cn.apisium.eim.components.icons.Crown
import cn.apisium.eim.components.icons.DebugStepOver

private val TRACK_ITEM_ICON_SIZE = Modifier.size(16.dp)

@Composable
private fun TrackItem(track: Track, height: Dp, index: Int) {
    Row(Modifier.height(height)) {
        Canvas(Modifier.fillMaxHeight().width(8.dp).background(track.color.copy(alpha = 0.5F))) {
            val y = size.height * (1F - track.levelMeter.maxLevel.toPercentage())
            drawRect(track.color, Offset(0F, y), Size(size.width, size.height - y))
        }
        Row(Modifier.padding(8.dp, 4.dp)) {
            Text(index.toString(),
                Modifier.width(20.dp),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
            Column(Modifier.fillMaxHeight(), Arrangement.SpaceBetween) {
                Text(track.name,
                    style = MaterialTheme.typography.labelLarge,
                    overflow = TextOverflow.Ellipsis,
                    maxLines = 1
                )
                SegmentedButtons {
                    SegmentedButton({ track.isMute = !track.isMute }, track.isMute, false) {
                        Icon(if (track.isMute) Icons.Filled.VolumeOff else Icons.Filled.VolumeUp, null, TRACK_ITEM_ICON_SIZE)
                    }
                    SegmentedDivider()
                    SegmentedButton({ track.isSolo = !track.isSolo }, track.isSolo, false) {
                        Icon(Crown, null, TRACK_ITEM_ICON_SIZE)
                    }
                    SegmentedDivider()
                    SegmentedButton({ track.isDisabled = !track.isDisabled }, track.isDisabled, false) {
                        Icon(DebugStepOver, null, TRACK_ITEM_ICON_SIZE)
                    }
                }
                VolumeSlider(track, Modifier.fillMaxWidth().offset((-4).dp), false)
            }
        }
    }
    Divider()
    track.subTracks.forEachIndexed { i, it -> key(it.uuid) { TrackItem(it, height, i + 1) } }
}

@Composable
private fun TrackContent(height: Dp = 70.dp, track: Track, noteWidth: Dp, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().height(height)) {
        track.notes.forEach {
            Box(Modifier.height(height / 128).width(noteWidth * it.duration)
                .offset(x = noteWidth * it.time, y = height - height / 128 * it.note.note)
                .background(track.color))
        }
    }
    Divider()
    track.subTracks.forEach { key(it.uuid) { TrackContent(height, it, noteWidth, modifier) } }
}

val playlistVerticalScrollState = ScrollState(0)

@Composable
fun Playlist() {
    Row {
        val trackHeight = 70.dp
        Surface(Modifier.width(200.dp).fillMaxHeight().zIndex(5f), shadowElevation = 2.dp, tonalElevation = 2.dp) {
            Column(Modifier.padding(top = APP_BAR_FULL_HEIGHT).verticalScroll(playlistVerticalScrollState)) {
                Divider()
                EchoInMirror.bus.subTracks.forEachIndexed { i, it ->
                    key(it.uuid) {
                        TrackItem(it, trackHeight, i + 1)
                    }
                }
            }
        }
        Box(Modifier.fillMaxSize()) {
            val noteWidth = 0.2.dp
            val horizontalScroll = rememberScrollState()
            Column {
                var contentWidth by remember { mutableStateOf(0.dp) }
                var cursorOffsetY by remember { mutableStateOf(0.dp) }
                val localDensity = LocalDensity.current
                Box(Modifier.weight(1f).onGloballyPositioned {
                    with(localDensity) {
                        contentWidth = it.size.width.toDp()
                        cursorOffsetY = it.size.height.toDp()
                    }
                }) {
                    val appBarHeight = with (localDensity) { APP_BAR_FULL_HEIGHT.toPx() }
                    val topPaddingHeight = (appBarHeight - playlistVerticalScrollState.value).coerceAtLeast(0f)
                    val topPaddingHeightInDp = with (localDensity) { topPaddingHeight.toDp() }
                    EditorGrid(noteWidth, horizontalScroll, topPaddingHeight)
                    Column(Modifier.horizontalScroll(horizontalScroll).verticalScroll(playlistVerticalScrollState)
                        .width(10000.dp).padding(top = APP_BAR_FULL_HEIGHT)) {
                        Divider()
                        EchoInMirror.bus.subTracks.forEach {
                            key(it.uuid) { TrackContent(trackHeight, it, noteWidth) }
                        }
                    }
                    Box(Modifier.padding(top = topPaddingHeightInDp)) {
                        PlayHead(noteWidth, horizontalScroll, contentWidth, isCursorOnBottom = true)
                    }
                }
                Timeline(Modifier.zIndex(3f), noteWidth, horizontalScroll)
            }
            HorizontalScrollbar(
                modifier = Modifier.align(Alignment.TopStart).fillMaxWidth(),
                adapter = rememberScrollbarAdapter(horizontalScroll)
            )
        }
    }
}