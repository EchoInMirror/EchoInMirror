package cn.apisium.eim.components

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.window.WindowDraggableArea
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import androidx.compose.ui.window.WindowScope

@Composable
@Preview
fun WindowScope.eimAppBar() = WindowDraggableArea {
    CenterAlignedTopAppBar(
        title = {
            Row(verticalAlignment = Alignment.Bottom) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(end = 8.dp)) {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = "00:00:",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = (-0.7F).sp,
                            lineHeight = 0.sp
                        )
                        Text(
                            text = "000",
                            fontSize = 16.sp,
                            letterSpacing = (-2).sp,
                            lineHeight = 0.sp
                        )
                    }
                    Text(
                        text = "秒",
                        fontSize = 12.sp,
                        lineHeight = 0.sp
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(end = 12.dp)) {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = "01:01:",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = (-0.7F).sp,
                            lineHeight = 0.sp
                        )
                        Text(
                            text = "1",
                            fontSize = 16.sp,
                            letterSpacing = (-2).sp,
                            lineHeight = 0.sp
                        )
                    }
                    Text(
                        text = "小节",
                        fontSize = 12.sp,
                        lineHeight = 0.sp
                    )
                }

                IconButton(onClick = { /* doSomething() */ }) {
                    Icon(
                        imageVector = Icons.Filled.PlayArrow,
                        modifier = Modifier.size(34.dp),
                        contentDescription = "Play"
                    )
                }

                IconButton(onClick = { /* doSomething() */ }) {
                    Icon(
                        imageVector = Icons.Filled.Stop,
                        modifier = Modifier.size(34.dp),
                        contentDescription = "Stop"
                    )
                }

                IconButton(onClick = { /* doSomething() */ }) {
                    Icon(
                        imageVector = Icons.Filled.FiberManualRecord,
                        modifier = Modifier.size(34.dp),
                        contentDescription = "Record"
                    )
                }
            }
        },
        navigationIcon = {
            IconButton(onClick = { /* doSomething() */ }) {
                Icon(
                    imageVector = EIMLogo,
                    modifier = Modifier.size(34.dp),
                    contentDescription = "Localized description"
                )
            }
        },
        actions = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(end = 12.dp)) {
                Text(
                    text = "C",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.7F).sp,
                    lineHeight = 0.sp
                )
                Text(
                    text = "根音",
                    fontSize = 12.sp,
                    lineHeight = 0.sp
                )
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(end = 12.dp)) {
                Text(
                    text = "自然大调",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.7F).sp,
                    lineHeight = 0.sp
                )
                Text(
                    text = "调式",
                    fontSize = 12.sp,
                    lineHeight = 0.sp
                )
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(end = 12.dp)) {
                Text(
                    text = "4/4",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.7F).sp,
                    lineHeight = 0.sp
                )
                Text(
                    text = "拍号",
                    fontSize = 12.sp,
                    lineHeight = 0.sp
                )
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(end = 12.dp)) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = "120.",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.7F).sp,
                        lineHeight = 0.sp
                    )
                    Text(
                        text = "00",
                        fontSize = 16.sp,
                        letterSpacing = (-2).sp,
                        lineHeight = 0.sp
                    )
                }
                Text(
                    text = "BPM",
                    fontSize = 12.sp,
                    lineHeight = 0.sp
                )
            }
        }
    )
}
