package cn.apisium.eim.plugin

import org.pf4j.*

class EIMPluginManager : DefaultPluginManager() {
    override fun createPluginDescriptorFinder(): CompoundPluginDescriptorFinder =
        CompoundPluginDescriptorFinder().add(JSONPluginDescriptorFinder())
}
