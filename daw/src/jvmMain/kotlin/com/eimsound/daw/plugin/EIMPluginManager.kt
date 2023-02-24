package com.eimsound.daw.plugin

import org.pf4j.CompoundPluginDescriptorFinder
import org.pf4j.DefaultPluginManager

class EIMPluginManager : DefaultPluginManager() {
    override fun createPluginDescriptorFinder(): CompoundPluginDescriptorFinder =
        CompoundPluginDescriptorFinder()
}
