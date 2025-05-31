package com.jules.tkinterdesigner.editor.toolwindows

import com.intellij.openapi.project.Project
import com.jules.tkinterdesigner.messaging.WIDGET_MODIFIED_TOPIC
import com.jules.tkinterdesigner.model.DesignedDialog
import com.jules.tkinterdesigner.model.DesignedWidget
import com.jules.tkinterdesigner.model.PropertyDefinition
import javax.swing.table.AbstractTableModel
import java.awt.Color // For Color type handling later

class WidgetPropertyTableModel(
    private var widgetProperties: List<Pair<PropertyDefinition, Any?>>,
    private val project: Project, // For MessageBus
    private var currentWidget: DesignedWidget?,
    private var currentDialog: DesignedDialog?
) : AbstractTableModel() {

    private val columnNames = arrayOf("Property", "Value")

    override fun getRowCount(): Int = widgetProperties.size
    override fun getColumnCount(): Int = columnNames.size
    override fun getColumnName(column: Int): String = columnNames[column]

    override fun getValueAt(rowIndex: Int, columnIndex: Int): Any? {
        val (propDef, currentValue) = widgetProperties[rowIndex]
        return if (columnIndex == 0) {
            propDef.name
        } else { // Value column
            val stringValue = currentValue as? String // All properties are stored as strings
            when (propDef.type) {
                String::class.java -> stringValue ?: propDef.defaultValue
                Int::class.java -> stringValue?.toIntOrNull() ?: propDef.defaultValue
                Boolean::class.java -> stringValue?.toBooleanStrictOrNull() ?: propDef.defaultValue
                Color::class.java -> {
                    if (stringValue != null) {
                        try {
                            if (stringValue.startsWith("#")) Color.decode(stringValue)
                            else Color.getColor(stringValue, Color.LIGHT_GRAY) // This might not work as expected for all names
                        } catch (e: Exception) { propDef.defaultValue as Color }
                    } else { propDef.defaultValue as Color }
                }
                else -> stringValue // For other types, display the string value directly
            }
        }
    }

    override fun isCellEditable(rowIndex: Int, columnIndex: Int): Boolean {
        return columnIndex == 1
    }

    override fun setValueAt(aValue: Any?, rowIndex: Int, columnIndex: Int) {
        if (columnIndex == 1 && currentWidget != null) {
            val (propDef, _) = widgetProperties[rowIndex]

            val valueToStore: String = when {
                propDef.type == Color::class.java && aValue is Color -> String.format("#%02x%02x%02x", aValue.red, aValue.green, aValue.blue)
                propDef.type == Boolean::class.java && aValue is Boolean -> aValue.toString() // "true" or "false"
                propDef.type == Int::class.java && aValue is Int -> aValue.toString()
                // For TkVariable editor, aValue is already the string (var name)
                // For Font editor, aValue is already the string
                // For FilePath editor, aValue is already the string
                aValue != null -> aValue.toString()
                else -> propDef.defaultValue.toString() // Fallback to default value as string
            }

            currentWidget!!.properties[propDef.name] = valueToStore

            // Update the local cache in table model
            val updatedPairValue = when (propDef.type) {
                 String::class.java -> valueToStore
                 Int::class.java -> valueToStore.toIntOrNull() ?: propDef.defaultValue
                 Boolean::class.java -> valueToStore.toBooleanStrictOrNull() ?: propDef.defaultValue
                 Color::class.java -> if (valueToStore.startsWith("#")) Color.decode(valueToStore) else Color.getColor(valueToStore, propDef.defaultValue as Color)
                 else -> valueToStore
            }

            widgetProperties = widgetProperties.mapIndexed { index, pair ->
                if (index == rowIndex) Pair(propDef, updatedPairValue) else pair
            }

            project.messageBus.syncPublisher(WIDGET_MODIFIED_TOPIC).propertyChanged(currentWidget!!, currentDialog)
            fireTableCellUpdated(rowIndex, columnIndex)
        }
    }

    // Called by PropertyEditorComponent to refresh the table with new data
    fun updateData(newWidget: DesignedWidget?, newDialog: DesignedDialog?, newProperties: List<Pair<PropertyDefinition, Any?>>) {
        this.currentWidget = newWidget
        this.currentDialog = newDialog
        this.widgetProperties = newProperties
        fireTableDataChanged()
    }

    // Helper for JTable to determine editor/renderer based on class
    override fun getColumnClass(columnIndex: Int): Class<*> {
        if (columnIndex == 1 && widgetProperties.isNotEmpty()) {
            // Ensure row index is valid before accessing for type.
            // This method is often called with rowIndex=0 by JTable for header rendering hints.
            // However, for specific cell rendering, JTable calls it with actual row index.
            // It's safer to check widgetProperties.firstOrNull() or specific row if available and valid.
            val firstPropDefType = widgetProperties.firstOrNull()?.first?.type
            if (firstPropDefType == Color::class.java) {
                 return Color::class.java
            }
            // For font, we are storing as String, but want custom editor.
            // getColumnClass is mainly for default renderers/editors.
            // We'll override per cell for font.
            return Any::class.java
        }
        return String::class.java
    }

    fun getPropertyDefinition(modelRowIndex: Int): PropertyDefinition {
        return widgetProperties[modelRowIndex].first
    }
}
