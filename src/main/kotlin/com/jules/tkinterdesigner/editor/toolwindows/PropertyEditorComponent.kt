package com.jules.tkinterdesigner.editor.toolwindows

import com.intellij.openapi.project.Project
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBPanel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextArea
import com.intellij.ui.table.JBTable
import com.jules.tkinterdesigner.codegen.TkinterCodeGenerator
import com.jules.tkinterdesigner.messaging.WIDGET_SELECTION_TOPIC
import com.jules.tkinterdesigner.messaging.WidgetSelectionListener
import com.jules.tkinterdesigner.model.DesignedDialog
import com.jules.tkinterdesigner.model.DesignedWidget
import com.jules.tkinterdesigner.model.WidgetPropertyRegistry
import java.awt.BorderLayout
import java.awt.Color
import java.awt.Component
import javax.swing.*
import javax.swing.table.DefaultTableCellRenderer
import javax.swing.table.DefaultCellEditor
import javax.swing.table.TableCellRenderer
import javax.swing.table.TableCellEditor as SwingTableCellEditor // Alias for clarity

class PropertyEditorComponent(private val project: Project) : JBPanel<PropertyEditorComponent>(BorderLayout()) {

    private var currentSelectedWidget: DesignedWidget? = null
    private var currentDesignDialog: DesignedDialog? = null

    private val tableModel: WidgetPropertyTableModel
    private val propertiesTable: JBTable
    private val titleLabel: JBLabel
    private val generateCodeButton: JButton

    init {
        titleLabel = JBLabel("No widget selected.")
        add(titleLabel, BorderLayout.NORTH)

        tableModel = WidgetPropertyTableModel(emptyList(), project, null, null)
        propertiesTable = JBTable(tableModel)
        propertiesTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION)
        propertiesTable.rowHeight = 24 // Increased row height for better editor component fit

        val valueColumn = propertiesTable.columnModel.getColumn(1) // Value column

        // Custom renderer selector
        valueColumn.cellRenderer = TableCellRenderer { table, value, isSelected, hasFocus, row, column ->
            val modelRow = table.convertRowIndexToModel(row)
            if (modelRow < 0 || modelRow >= tableModel.rowCount) {
                 return@TableCellRenderer DefaultTableCellRenderer().getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column)
            }
            val propDef = tableModel.getPropertyDefinition(modelRow)

            when {
                propDef.name == "font" -> FontPropertyRenderer().getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column)
                propDef.name == "image" || propDef.name == "file" -> FilePathPropertyRenderer().getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column)
                propDef.type == Color::class.java -> ColorPropertyRenderer().getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column)
                propDef.type == Boolean::class.java -> BooleanPropertyRenderer().getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column)
                else -> DefaultTableCellRenderer().getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column)
            }
        }

        // Custom editor selector using a custom AbstractCellEditor
        valueColumn.cellEditor = object : AbstractCellEditor(), SwingTableCellEditor {
            private var delegateEditor: SwingTableCellEditor? = null

            override fun getTableCellEditorComponent(table: JTable, value: Any?, isSelected: Boolean, row: Int, column: Int): Component {
                val modelRow = table.convertRowIndexToModel(row)
                if (modelRow < 0 || modelRow >= tableModel.rowCount) {
                    delegateEditor = DefaultCellEditor(JTextField()) // Fallback
                    return delegateEditor!!.getTableCellEditorComponent(table, value, isSelected, row, column)
                }
                val propDef = tableModel.getPropertyDefinition(modelRow)

                delegateEditor = when {
                    propDef.tkVariableTypeOptions != null -> TkVariablePropertyEditor(project, this@PropertyEditorComponent, currentSelectedWidget, currentDesignDialog, propDef)
                    propDef.name == "font" -> FontPropertyEditor(this@PropertyEditorComponent)
                    propDef.name == "image" || propDef.name == "file" -> FilePathPropertyEditor(project, this@PropertyEditorComponent)
                    propDef.type == Color::class.java -> ColorPropertyEditor(this@PropertyEditorComponent)
                    propDef.type == Boolean::class.java -> BooleanPropertyEditor()
                    propDef.options != null -> {
                        val comboBox = JComboBox(propDef.options.toTypedArray())
                        comboBox.selectedItem = value as? String ?: propDef.defaultValue
                        DefaultCellEditor(comboBox)
                    }
                    propDef.type == Int::class.java -> DefaultCellEditor(JTextField((value as? Int ?: propDef.defaultValue as Int).toString())) // Basic Int editor for now
                    else -> DefaultCellEditor(JTextField(value?.toString() ?: "")) // Default for String and others
                }
                return delegateEditor!!.getTableCellEditorComponent(table, value, isSelected, row, column)
            }

            override fun getCellEditorValue(): Any? = delegateEditor?.cellEditorValue

            override fun isCellEditable(e: java.util.EventObject?): Boolean {
                return delegateEditor?.isCellEditable(e) ?: super.isCellEditable(e)
            }

            override fun shouldSelectCell(e: java.util.EventObject?): Boolean {
                return delegateEditor?.shouldSelectCell(e) ?: super.shouldSelectCell(e)
            }

            override fun stopCellEditing(): Boolean {
                return delegateEditor?.stopCellEditing() ?: super.stopCellEditing()
            }

            override fun cancelCellEditing() {
                delegateEditor?.cancelCellEditing() ?: super.cancelCellEditing()
            }
        }

        add(JBScrollPane(propertiesTable), BorderLayout.CENTER)

        generateCodeButton = JButton("Generate Code for Current Design")
        generateCodeButton.addActionListener {
            currentDesignDialog?.let { dialog ->
                val pythonCode = TkinterCodeGenerator.generateCode(dialog)
                val codeDialog = JDialog(SwingUtilities.getWindowAncestor(this), "Generated Python Code")
                val textArea = JBTextArea(pythonCode)
                textArea.isEditable = false
                codeDialog.add(JBScrollPane(textArea))
                codeDialog.setSize(600, 500)
                codeDialog.setLocationRelativeTo(null)
                codeDialog.isVisible = true
            }
        }
        val southPanel = JBPanel<JBPanel<*>>()
        southPanel.add(generateCodeButton)
        add(southPanel, BorderLayout.SOUTH)
        generateCodeButton.isEnabled = false

        project.messageBus.connect(this).subscribe(WIDGET_SELECTION_TOPIC, object : WidgetSelectionListener {
            override fun widgetSelected(widget: DesignedWidget?, dialog: DesignedDialog?) {
                currentSelectedWidget = widget
                currentDesignDialog = dialog
                displayProperties(widget)
                generateCodeButton.isEnabled = dialog != null
            }
        })
    }

    private fun displayProperties(selectedWidget: DesignedWidget?) {
        if (selectedWidget == null) {
            titleLabel.text = "No widget selected."
            tableModel.updateData(null, null, emptyList())
        } else {
            titleLabel.text = "Properties: ${selectedWidget.type} (${selectedWidget.properties["name"] ?: selectedWidget.id})"
            val propertyDefinitions = WidgetPropertyRegistry.propertiesForType[selectedWidget.type] ?: emptyList()
            val propertyPairs = propertyDefinitions.map { propDef ->
                Pair(propDef, selectedWidget.properties.getOrDefault(propDef.name, propDef.defaultValue))
            }
            tableModel.updateData(selectedWidget, currentDesignDialog, propertyPairs)
        }
    }
}
