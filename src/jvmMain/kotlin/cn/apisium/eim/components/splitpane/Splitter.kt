package cn.apisium.eim.components.splitpane

import androidx.compose.runtime.Composable

enum class SplitterHandleAlignment {
    BEFORE,
    ABOVE,
    AFTER
}

internal data class Splitter(
    val measuredPart: @Composable () -> Unit,
    val handlePart: @Composable () -> Unit = measuredPart,
    val alignment: SplitterHandleAlignment = SplitterHandleAlignment.ABOVE
)
