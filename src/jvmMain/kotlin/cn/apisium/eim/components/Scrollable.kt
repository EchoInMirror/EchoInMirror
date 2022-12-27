package cn.apisium.eim.components

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

@Composable
fun Scrollable(vertical: Boolean = true, horizontal: Boolean = true, content: @Composable () -> Unit) {
    Box(Modifier.fillMaxSize()) {
        val stateVertical = rememberScrollState(0)
        val stateHorizontal = rememberScrollState(0)
        var modifier = Modifier.fillMaxSize()
        if (vertical) modifier = modifier.verticalScroll(stateVertical)
        Box(if (horizontal) modifier.horizontalScroll(stateHorizontal) else modifier) { content() }
        if (vertical) VerticalScrollbar(
            rememberScrollbarAdapter(stateVertical),
            Modifier.align(Alignment.CenterEnd).fillMaxHeight()
        )
        if (horizontal) HorizontalScrollbar(
            rememberScrollbarAdapter(stateHorizontal),
            Modifier.align(Alignment.BottomStart).fillMaxWidth()
        )
    }
}
