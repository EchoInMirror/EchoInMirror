package cn.apisium.eim.window

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
private fun TrackItem(track: Track) {
    Row(Modifier.fillMaxWidth().padding(8.dp)) {
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
        Surface(Modifier.width(200.dp).fillMaxHeight().zIndex(5f), shadowElevation = 2.dp, tonalElevation = 1.dp) {
            Column {
                EchoInMirror.bus.subTracks.forEach {
                    key(it.uuid) {
                        TrackItem(it)
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
                val trackHeight = 70.dp
                Box(Modifier.weight(1f).onGloballyPositioned {
                    with(localDensity) {
                        contentWidth = it.size.width.toDp()
                        cursorOffsetY = it.size.height.toDp()
                    }
                }) {
                    val appBarHeight = with (localDensity) { APP_BAR_FULL_HEIGHT.toPx() }
                    EditorGrid(noteWidth, horizontalScroll, (appBarHeight - playlistVerticalScrollState.value).coerceAtLeast(0f))
                    Column(Modifier.horizontalScroll(horizontalScroll).verticalScroll(playlistVerticalScrollState)
                        .width(10000.dp).padding(top = APP_BAR_FULL_HEIGHT)) {
                        Divider()
                        EchoInMirror.bus.subTracks.forEach {
                            key(it.uuid) { TrackContent(trackHeight, it, noteWidth) }
                        }
                    }
                    PlayHead(noteWidth, horizontalScroll, contentWidth, cursorOffsetY = cursorOffsetY)
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