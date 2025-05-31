package com.jules.tkinterdesigner.editor.toolwindows

import com.intellij.openapi.project.Project
import com.intellij.ui.components.JBTextField
import com.intellij.util.ui.FormBuilder
import com.jules.tkinterdesigner.messaging.WIDGET_MODIFIED_TOPIC
import com.jules.tkinterdesigner.model.DesignedDialog
import com.jules.tkinterdesigner.model.DesignedWidget
import com.jules.tkinterdesigner.model.PropertyDefinition
import java.awt.Component
import java.awt.event.FocusAdapter
import java.awt.event.FocusEvent
import javax.swing.*
import javax.swing.table.TableCellEditor

class TkVariablePropertyEditor(
    private val project: Project, // For message bus
    private val parentComponentForDialog: Component, // For any dialogs if needed
    private var currentWidget: DesignedWidget?, // The widget whose properties are being edited
    private var currentDialog: DesignedDialog?, // The dialog context for message bus
    private var propertyDef: PropertyDefinition // The definition of the Tk variable property itself
) : AbstractCellEditor(), TableCellEditor {

    private val editorPanel: JPanel
    private val varNameField = JBTextField()
    private val varTypeComboBox = JComboBox<String>()
    private val initialValueField = JBTextField()

    private var initialVarName: String = ""
    private var initialVarType: String = ""
    private var initialValueContent: String = ""


    init {
        propertyDef.tkVariableTypeOptions?.forEach { varTypeComboBox.addItem(it) }

        editorPanel = FormBuilder.createFormBuilder()
            .addLabeledComponent("Name:", varNameField)
            .addLabeledComponent("Type:", varTypeComboBox)
            .addLabeledComponent("Initial Value:", initialValueField)
            .panel
        editorPanel.border = BorderFactory.createEmptyBorder(2,2,2,2) // Add some padding


        val stopEditingListener = {
            fireEditingStopped()
        }
        varNameField.addActionListener { stopEditingListener() }
        varTypeComboBox.addActionListener { stopEditingListener() }
        initialValueField.addActionListener { stopEditingListener() }

        // Stop editing when focus is lost from any field to commit changes
        val focusLostListener = object : FocusAdapter() {
            override fun focusLost(e: FocusEvent?) {
                // Check if the new focus owner is part of this editorPanel.
                // If not, then stop editing. This prevents stopping editing when tabbing between fields.
                var newFocusOwner = e?.oppositeComponent
                while (newFocusOwner != null) {
                    if (newFocusOwner == editorPanel) return // Focus still within editor
                    newFocusOwner = newFocusOwner.parent
                }
                fireEditingStopped()
            }
        }
        varNameField.addFocusListener(focusLostListener)
        varTypeComboBox.addFocusListener(focusLostListener) // Might not be ideal for combo box, but let's try
        initialValueField.addFocusListener(focusLostListener)

    }

    override fun getTableCellEditorComponent(
        table: JTable, value: Any?,
        isSelected: Boolean, row: Int, column: Int
    ): Component {
        // `value` is the variable name string
        initialVarName = value as? String ?: propertyDef.defaultValue as? String ?: ""
        varNameField.text = initialVarName

        initialVarType = currentWidget?.properties?.get("${propertyDef.name}_vartype") as? String
            ?: propertyDef.tkVariableTypeOptions?.firstOrNull() ?: "StringVar"
        varTypeComboBox.selectedItem = initialVarType

        val defaultValuePropName = propertyDef.tkVariableDefaultValueProperty
        val defaultValFromWidget = if (defaultValuePropName != null) currentWidget?.properties?.get(defaultValuePropName) as? String else null

        initialValueContent = currentWidget?.properties?.get("${propertyDef.name}_value") as? String
            ?: defaultValFromWidget ?: "" // Use linked property or empty
        initialValueField.text = initialValueContent

        return editorPanel
    }

    override fun getCellEditorValue(): Any? {
        // This is called when editing stops. Here we update the auxiliary properties
        // and return the primary value (variable name).
        val newVarName = varNameField.text.trim()
        val newVarType = varTypeComboBox.selectedItem as? String ?: "StringVar"
        val newInitialValue = initialValueField.text

        var changed = false
        if (newVarName != initialVarName) {
            changed = true
        }
        if (newVarType != currentWidget?.properties?.get("${propertyDef.name}_vartype")) {
            currentWidget?.properties?.set("${propertyDef.name}_vartype", newVarType)
            changed = true
        }
        if (newInitialValue != currentWidget?.properties?.get("${propertyDef.name}_value")) {
            currentWidget?.properties?.set("${propertyDef.name}_value", newInitialValue)
            changed = true
        }

        if (changed && currentWidget != null) {
            // Store the primary variable name
            currentWidget!!.properties[propertyDef.name] = if (newVarName.isBlank()) propertyDef.defaultValue else newVarName
            project.messageBus.syncPublisher(WIDGET_MODIFIED_TOPIC).propertyChanged(currentWidget!!, currentDialog)
        }

        return if (newVarName.isBlank()) propertyDef.defaultValue else newVarName
    }

    override fun stopCellEditing(): Boolean {
        // This can be called by JTable when it wants to stop editing (e.g. focus loss).
        // We need to ensure our values are committed.
        val result = super.stopCellEditing() // This will eventually call getCellEditorValue()
        return result
    }
}
