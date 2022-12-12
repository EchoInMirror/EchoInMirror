package cn.apisium.eim.components

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cn.apisium.eim.utils.Border
import cn.apisium.eim.utils.border
import cn.apisium.eim.components.app.bottomBarSelectedItem

@Composable
fun bottomBarContent() {
    Surface(
        tonalElevation = 2.dp,
        shadowElevation = 2.dp
    ) {
        Box(Modifier.fillMaxSize().border(start = Border(0.6.dp, MaterialTheme.colorScheme.onSurfaceVariant.copy(0.2F)))) {
            bottomBarSelectedItem?.content()
        }
    }
}

@Composable
fun BottomContentScrollable(content: @Composable () -> Unit) {
    Box(Modifier.fillMaxSize()) {
        val stateVertical = rememberScrollState(0)
        Box(Modifier.fillMaxSize().verticalScroll(stateVertical)) { content() }
        VerticalScrollbar(
            modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight(),
            adapter = rememberScrollbarAdapter(stateVertical)
        )
    }
}
