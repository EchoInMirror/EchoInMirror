package com.eimsound.daw.components

import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEach

interface Tab {
    @Composable
    fun label()
    @Composable
    fun icon()
    @Composable
    fun content()
    @Composable
    fun buttons() { }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Tabs(tabs: List<Tab>, content: (@Composable (Tab?) -> Unit)? = null) {
    Surface(Modifier.fillMaxSize(), tonalElevation = 4.dp) {
        Row {
            var selected by remember { mutableStateOf(tabs.getOrNull(0)?.let { it::class.java.name } ?: "") }
            val selectedTab = tabs.find { it::class.java.name == selected }
            Column(Modifier.width(240.dp).padding(12.dp)) {
                tabs.fastForEach {
                    key(it::class.java.name) {
                        NavigationDrawerItem(
                            modifier = Modifier.height(46.dp),
                            icon = { it.icon() },
                            label = { it.label() },
                            selected = selected == it::class.java.name,
                            onClick = { selected = it::class.java.name }
                        )
                    }
                }
            }
            Column(Modifier.fillMaxSize()) {
                val stateVertical = rememberScrollState(0)
                Box(Modifier.weight(1F)) {
                    Box(Modifier.fillMaxSize().verticalScroll(stateVertical)) {
                        Box(Modifier.padding(14.dp)) { selectedTab?.content() }
                    }
                    VerticalScrollbar(
                        rememberScrollbarAdapter(stateVertical),
                        Modifier.align(Alignment.CenterEnd).fillMaxHeight()
                    )
                }
                content?.invoke(selectedTab)
            }
        }
    }
}