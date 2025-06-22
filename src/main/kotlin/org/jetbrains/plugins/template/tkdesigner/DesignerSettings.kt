package org.jetbrains.plugins.template.tkdesigner

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.service

@State(name = "TkDesignerSettings", storages = [Storage("tkdesigner.xml")])
class DesignerSettings : PersistentStateComponent<DesignerSettings.State> {
    data class State(
        var gridSize: Int = 10,
        var paletteX: Int = 200,
        var paletteY: Int = 200,
        var paletteColumns: Int = 4,
        var interpreter: String = "python",
        var extraWidgets: MutableList<String> = mutableListOf(),
        var shortcuts: MutableMap<String, String> = mutableMapOf()
    )

    private var myState = State()

    override fun getState(): State = myState

    override fun loadState(state: State) {
        myState = state
    }

    companion object {
        fun instance(): DesignerSettings = service()
    }
}
