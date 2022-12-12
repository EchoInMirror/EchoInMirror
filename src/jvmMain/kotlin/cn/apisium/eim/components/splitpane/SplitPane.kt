package cn.apisium.eim.components.splitpane

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp

internal data class MinimalSizes(
    val firstPlaceableMinimalSize: Dp,
    val secondPlaceableMinimalSize: Dp
)

/**
 * Pane that place it parts **vertically** from top to bottom and allows to change items **heights**.
 * The [content] block defines DSL which allow you to configure top ([SplitPaneScope.first]),
 * bottom ([SplitPaneScope.second]).
 *
 * @param modifier the modifier to apply to this layout
 * @param splitPaneState the state object to be used to control or observe the split pane state
 * @param content a block which describes the content. Inside this block you can use methods like
 * [SplitPaneScope.first], [SplitPaneScope.second], to describe parts of split pane.
 */
@Composable
fun VerticalSplitPane(
    modifier: Modifier = Modifier,
    splitPaneState: SplitPaneState = rememberSplitPaneState(),
    content: SplitPaneScope.() -> Unit
) {
    with(SplitPaneScopeImpl(isHorizontal = false, splitPaneState).apply(content)) {
        SplitPane(
            modifier = modifier,
            isHorizontal = false,
            splitPaneState = splitPaneState,
            minimalSizesConfiguration = minimalSizes,
            first = firstPlaceableContent,
            second = secondPlaceableContent,
            splitter = splitter
        )
    }
}

/**
 * Pane that place it parts **horizontally** from left to right and allows to change items **width**.
 * The [content] block defines DSL which allow you to configure left ([SplitPaneScope.first]),
 * right ([SplitPaneScope.second]) parts of split pane.
 *
 * @param modifier the modifier to apply to this layout
 * @param splitPaneState the state object to be used to control or observe the split pane state
 * @param content a block which describes the content. Inside this block you can use methods like
 * [SplitPaneScope.first], [SplitPaneScope.second], to describe parts of split pane.
 */
@Composable
fun HorizontalSplitPane(
    modifier: Modifier = Modifier,
    splitPaneState: SplitPaneState = rememberSplitPaneState(),
    content: SplitPaneScope.() -> Unit
) {
    with(SplitPaneScopeImpl(isHorizontal = true, splitPaneState).apply(content)) {
        SplitPane(
            modifier = modifier,
            isHorizontal = true,
            splitPaneState = splitPaneState,
            minimalSizesConfiguration = minimalSizes,
            first = firstPlaceableContent!!,
            second = secondPlaceableContent!!,
            splitter = splitter
        )
    }
}