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
        } else {
            // If the type is Color and value is String, try to parse it.
            // This helps bridge storage as String and editing as Color.
            if (propDef.type == Color::class.java && currentValue is String) {
                try {
                    // Basic color name parsing or hex
                    if (currentValue.startsWith("#")) Color.decode(currentValue) else Color.getColor(currentValue, Color.LIGHT_GRAY)
                } catch (e: Exception) {
                    Color.LIGHT_GRAY // Fallback
                }
            } else {
                currentValue
            }
        }
    }

    override fun isCellEditable(rowIndex: Int, columnIndex: Int): Boolean {
        return columnIndex == 1 // Only value column is editable
    }

    override fun setValueAt(aValue: Any?, rowIndex: Int, columnIndex: Int) {
        if (columnIndex == 1 && currentWidget != null) {
            val (propDef, _) = widgetProperties[rowIndex]
            var valueToStore = aValue

            // If the property is a Color, store its string representation (e.g., hex)
            if (propDef.type == Color::class.java && aValue is Color) {
                valueToStore = String.format("#%02x%02x%02x", aValue.red, aValue.green, aValue.blue)
            }

            currentWidget!!.properties[propDef.name] = valueToStore ?: propDef.defaultValue

            // Update the list in the model directly too (or re-fetch)
            widgetProperties = widgetProperties.mapIndexed { index, pair ->
                if (index == rowIndex) Pair(propDef, valueToStore ?: propDef.defaultValue) else pair
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
