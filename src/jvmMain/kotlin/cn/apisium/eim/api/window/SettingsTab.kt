package cn.apisium.eim.api.window

import androidx.compose.runtime.Composable

interface SettingsTab {
    @Composable
    fun label()
    @Composable
    fun icon()
    @Composable
    fun content()
    @Composable
    fun buttons() { }
}
