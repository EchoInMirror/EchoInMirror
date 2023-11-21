package com.eimsound.daw.commons.actions

import com.eimsound.daw.commons.Disabled

abstract class ListDisabledAction(
    private val list: List<Disabled>, private val isDisabled: Boolean? = null
) : ReversibleAction() {
    private val oldStates = list.map { it.isDisabled }

    override suspend fun perform(isForward: Boolean): Boolean {
        if (isForward) for (i in list.indices) list[i].isDisabled = isDisabled ?: !list[i].isDisabled
        else for (i in list.indices) list[i].isDisabled = oldStates[i]
        afterPerform()
        return true
    }

    protected open fun afterPerform() { }
}
