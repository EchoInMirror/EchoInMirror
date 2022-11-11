package cn.apisium.eim.api.window

interface WindowManager {
    var settingsDialogOpen: Boolean
    val panels: List<Panel>
    fun registerPanel(panel: Panel)
    fun unregisterPanel(panel: Panel)
}
