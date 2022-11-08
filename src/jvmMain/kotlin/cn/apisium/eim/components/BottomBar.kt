package cn.apisium.eim.components

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cn.apisium.eim.Border
import cn.apisium.eim.border

@Composable
fun bottomBarContent() {
    Surface(
        tonalElevation = 2.dp,
        shadowElevation = 2.dp,
        shape = RoundedCornerShape(0.dp, 16.dp, 0.dp, 0.dp)
    ) {
        Box(Modifier.fillMaxSize().border(start = Border(0.6.dp, MaterialTheme.colorScheme.onSurfaceVariant.copy(0.2F)))) {
            val stateVertical = rememberScrollState(0)
            Box(Modifier.fillMaxSize().horizontalScroll(stateVertical)) {
                Box(Modifier.padding(14.dp)) { }
            }
            HorizontalScrollbar(
                modifier = Modifier.align(Alignment.CenterEnd).fillMaxWidth(),
                adapter = rememberScrollbarAdapter(stateVertical)
            )
        }
    }
}
