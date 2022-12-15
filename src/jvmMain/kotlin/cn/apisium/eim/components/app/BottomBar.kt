package cn.apisium.eim.components.app

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
internal fun bottomBarContent() {
    Surface(tonalElevation = 1.dp, shadowElevation = 2.dp) {
        bottomBarSelectedItem?.content()
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
