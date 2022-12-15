package cn.apisium.eim.window

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import cn.apisium.eim.EchoInMirror
import cn.apisium.eim.api.Track
import cn.apisium.eim.components.EditorGrid
import cn.apisium.eim.components.PlayHead
import cn.apisium.eim.components.Timeline
import cn.apisium.eim.components.app.APP_BAR_FULL_HEIGHT

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TrackItem(track: Track, height: Dp) {
    Row(Modifier.height(height)) {
        Canvas(Modifier.fillMaxHeight().width(8.dp).background(track.color.copy(alpha = 0.5F))) {
            val y = size.height * (1F - track.levelMeter.maxLevel.toPercentage())
            drawRect(track.color, Offset(0F, y), Size(size.width, size.height - y))
        }
        ListItem(
            headlineText = { Text("Three line list item") },
            overlineText = { Text("OVERLINE") },
            supportingText = { Text("Secondary text") },
            leadingContent = {
                Icon(
                    Icons.Outlined.Settings,
                    contentDescription = null
                )
            },
            trailingContent = { Text("meta") }
        )
    }
    Divider()
    track.subTracks.forEach { key(it.uuid) { TrackItem(it, height) } }
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
        Surface(Modifier.width(200.dp).fillMaxHeight().zIndex(5f), shadowElevation = 2.dp, tonalElevation = 1.dp) {
            Column(Modifier.padding(top = APP_BAR_FULL_HEIGHT).verticalScroll(playlistVerticalScrollState)) {
                Divider()
                EchoInMirror.bus.subTracks.forEach {
                    key(it.uuid) {
                        TrackItem(it, trackHeight)
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
                        PlayHead(noteWidth, horizontalScroll, contentWidth, cursorOffsetY = cursorOffsetY)
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