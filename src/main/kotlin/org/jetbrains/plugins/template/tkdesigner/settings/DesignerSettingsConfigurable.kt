package org.jetbrains.plugins.template.tkdesigner.settings

import com.intellij.openapi.options.SearchableConfigurable
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.JTextField
import javax.swing.JSpinner
import javax.swing.SpinnerNumberModel
import org.jetbrains.plugins.template.tkdesigner.DesignerSettings

class DesignerSettingsConfigurable : SearchableConfigurable {
    private val interpreterField = JTextField()
    private val gridField = JSpinner(SpinnerNumberModel(10, 1, 50, 1))
    private val columnsField = JSpinner(SpinnerNumberModel(4, 1, 10, 1))

    private val shortcutFields = mutableMapOf<String, JTextField>()
    private val actions = listOf("undo","redo","copy","paste","dup","del")

    override fun getId(): String = "tkdesigner.settings"

    override fun getDisplayName(): String = "Tkinter Designer"

    override fun createComponent(): JComponent {
        val panel = JPanel()
        panel.layout = java.awt.GridBagLayout()
        val c = java.awt.GridBagConstraints()
        c.insets = java.awt.Insets(2,2,2,2)
        c.gridx = 0; c.gridy = 0; panel.add(javax.swing.JLabel("Python interpreter:"), c)
        c.gridx = 1; panel.add(interpreterField, c)
        c.gridx = 0; c.gridy = 1; panel.add(javax.swing.JLabel("Grid size:"), c)
        c.gridx = 1; panel.add(gridField, c)
        c.gridx = 0; c.gridy = 2; panel.add(javax.swing.JLabel("Palette columns:"), c)
        c.gridx = 1; panel.add(columnsField, c)

        val shortcutsPanel = JPanel(java.awt.GridLayout(0,2,2,2))
        actions.forEach { act ->
            shortcutsPanel.add(javax.swing.JLabel(act.capitalize()))
            val field = JTextField(10)
            shortcutFields[act] = field
            shortcutsPanel.add(field)
        }
        c.gridx = 0; c.gridy = 3; c.gridwidth = 2
        panel.add(javax.swing.JLabel("Shortcuts"), c)
        c.gridy = 4
        panel.add(shortcutsPanel, c)
        reset()
        return panel
    }

    override fun isModified(): Boolean {
        val state = DesignerSettings.instance().state
        if (interpreterField.text != state.interpreter) return true
        if ((gridField.value as Int) != state.gridSize) return true
        if ((columnsField.value as Int) != state.paletteColumns) return true
        for ((k, field) in shortcutFields) {
            if ((state.shortcuts[k] ?: "") != field.text) return true
        }
        return false
    }

    override fun apply() {
        val state = DesignerSettings.instance().state
        state.interpreter = interpreterField.text
        state.gridSize = gridField.value as Int
        state.paletteColumns = columnsField.value as Int
        for ((k, field) in shortcutFields) {
            if (field.text.isNotBlank()) state.shortcuts[k] = field.text
        }
    }

    override fun reset() {
        val state = DesignerSettings.instance().state
        interpreterField.text = state.interpreter
        gridField.value = state.gridSize
        columnsField.value = state.paletteColumns
        actions.forEach { act -> shortcutFields[act]?.text = state.shortcuts[act] ?: "" }
    }
}
