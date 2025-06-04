package com.jules.tkinterdesigner.editor.toolwindows

import com.intellij.openapi.project.Project
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBPanel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextArea
import com.intellij.ui.table.JBTable
import com.jules.tkinterdesigner.codegen.TkinterCodeGenerator
import com.jules.tkinterdesigner.messaging.WIDGET_MODIFIED_TOPIC
import com.jules.tkinterdesigner.messaging.WIDGET_SELECTION_TOPIC
import com.jules.tkinterdesigner.messaging.WidgetSelectionListener
import com.jules.tkinterdesigner.model.DesignedDialog
import com.jules.tkinterdesigner.model.DesignedWidget
import com.jules.tkinterdesigner.model.PropertyDefinition
import com.jules.tkinterdesigner.model.WidgetPropertyRegistry
import java.awt.BorderLayout
import java.awt.Color
import java.awt.Component
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import javax.swing.*
import javax.swing.table.DefaultTableCellRenderer
import javax.swing.table.DefaultCellEditor
import javax.swing.table.TableCellRenderer
import javax.swing.table.TableCellEditor as SwingTableCellEditor

class PropertyEditorComponent(private val project: Project) : JBPanel<PropertyEditorComponent>(BorderLayout()) {

    private var currentSelectedWidget: DesignedWidget? = null
    private var currentDesignDialog: DesignedDialog? = null // Used for context, e.g. finding parent widgets

    private val tableModel: WidgetPropertyTableModel
    private val propertiesTable: JBTable
    private val titleLabel: JBLabel
    private val generateCodeButton: JButton
    private val notebookTabsPanel: JBPanel<JBPanel<*>>
    private val tabInfoLabel: JBLabel
    private val paneOptionsPanel: JBPanel<JBPanel<*>> // Panel for PanedWindow child options
    private val paneOptionsLabel: JBLabel

    init {
        titleLabel = JBLabel("No widget selected.")
        add(titleLabel, BorderLayout.NORTH)

        tableModel = WidgetPropertyTableModel(emptyList(), project, null, null)
        propertiesTable = JBTable(tableModel)
        propertiesTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION)
        propertiesTable.rowHeight = 24
        val valueColumn = propertiesTable.columnModel.getColumn(1)
        // ... (Cell Renderer and Editor setup as before) ...
        valueColumn.cellRenderer = TableCellRenderer { table,value,isSelected,hasFocus,row,column ->
            val modelRow = table.convertRowIndexToModel(row);
            if (modelRow < 0 || modelRow >= tableModel.rowCount) { DefaultTableCellRenderer().getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column) }
            else { val propDef = tableModel.getPropertyDefinition(modelRow); when {
                propDef.name == "font" -> FontPropertyRenderer().getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column)
                propDef.name == "image" || propDef.name == "file" -> FilePathPropertyRenderer().getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column)
                propDef.type == Color::class.java -> ColorPropertyRenderer().getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column)
                propDef.type == Boolean::class.java -> BooleanPropertyRenderer().getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column)
                else -> DefaultTableCellRenderer().getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column)
            }}
        }
        valueColumn.cellEditor = object : AbstractCellEditor(), SwingTableCellEditor { /* ... as before ... */ }


        val centerContentPanel = JBPanel<JBPanel<*>>(BorderLayout())
        centerContentPanel.add(JBScrollPane(propertiesTable), BorderLayout.CENTER)

        notebookTabsPanel = JBPanel<JBPanel<*>>(GridBagLayout())
        tabInfoLabel = JBLabel("Tabs:").apply { isVisible = false }
        notebookTabsPanel.add(tabInfoLabel, GridBagConstraints().apply{gridx=0;gridy=0;anchor=GridBagConstraints.WEST;})

        paneOptionsPanel = JBPanel<JBPanel<*>>(GridBagLayout())
        paneOptionsLabel = JBLabel("Pane Options:").apply { isVisible = false }
        paneOptionsPanel.add(paneOptionsLabel, GridBagConstraints().apply{gridx=0;gridy=0;anchor=GridBagConstraints.WEST;})

        val southOfTablePanel = JBPanel<JBPanel<*>>(BoxLayout(this, BoxLayout.Y_AXIS))
        southOfTablePanel.add(notebookTabsPanel)
        southOfTablePanel.add(paneOptionsPanel)
        centerContentPanel.add(southOfTablePanel, BorderLayout.SOUTH)

        add(centerContentPanel, BorderLayout.CENTER)

        generateCodeButton = JButton("Generate Code for Current Design")
        val southPanel = JBPanel<JBPanel<*>>()
        southPanel.add(generateCodeButton)
        add(southPanel, BorderLayout.SOUTH)
        generateCodeButton.isEnabled = false

        project.messageBus.connect(this).subscribe(WIDGET_SELECTION_TOPIC, object : WidgetSelectionListener {
            override fun widgetSelected(primaryWidget: DesignedWidget?, allSelectedIds: Set<String>, dialog: DesignedDialog?) {
                currentSelectedWidget = primaryWidget
                currentDesignDialog = dialog
                updateDisplayForSelection(primaryWidget, allSelectedIds.size)
                generateCodeButton.isEnabled = dialog != null
            }
        })
    }

    private fun updateDisplayForSelection(primaryWidget: DesignedWidget?, selectedCount: Int) {
        notebookTabsPanel.removeAll(); tabInfoLabel.isVisible = false; notebookTabsPanel.add(tabInfoLabel, GridBagConstraints().apply{gridx=0;gridy=0;anchor=GridBagConstraints.WEST; insets = Insets(2,2,2,2)})
        paneOptionsPanel.removeAll(); paneOptionsLabel.isVisible = false; paneOptionsPanel.add(paneOptionsLabel, GridBagConstraints().apply{gridx=0;gridy=0;anchor=GridBagConstraints.WEST; insets = Insets(2,2,2,2)})


        if (selectedCount > 1) {
            titleLabel.text = "$selectedCount widgets selected"
            tableModel.updateData(null, null, emptyList())
        } else if (primaryWidget != null) {
            titleLabel.text = "Properties: ${primaryWidget.type} (${primaryWidget.properties["name"] ?: primaryWidget.id})"
            val propertyDefinitions = WidgetPropertyRegistry.propertiesForType[primaryWidget.type] ?: emptyList()
            val propertyPairs = propertyDefinitions.map { propDef ->
                Pair(propDef, primaryWidget.properties.getOrDefault(propDef.name, propDef.defaultValue.toString())) // Ensure value is string from map
            }
            tableModel.updateData(primaryWidget, currentDesignDialog, propertyPairs)

            if (primaryWidget.type == "ttk.Notebook") { /* ... notebook tab UI population ... */ }

            // Check if selected widget is a child of a PanedWindow
            primaryWidget.parentId?.let { parentId ->
                currentDesignDialog?.widgets?.find { it.id == parentId }?.let { parentWidget ->
                    if (parentWidget.type == "tk.PanedWindow" || parentWidget.type == "ttk.PanedWindow") {
                        paneOptionsLabel.isVisible = true
                        val paneOptionsDefs = WidgetPropertyRegistry.propertiesForType["_PaneOptions"] ?: emptyList()
                        val currentPaneAllOptions = (parentWidget.properties["pane_options"] as? Map<String, Map<String, String>>)?.get(primaryWidget.id) ?: emptyMap()

                        var yPos = 1
                        paneOptionsDefs.forEach { paneOptDef ->
                            val gbcName = GridBagConstraints().apply{gridx=0;gridy=yPos; anchor=GridBagConstraints.WEST; insets=Insets(0,2,0,2); weightx=0.3}
                            paneOptionsPanel.add(JBLabel(paneOptDef.name + ":"), gbcName)

                            val gbcValue = GridBagConstraints().apply{gridx=1;gridy=yPos; anchor=GridBagConstraints.WEST; fill=GridBagConstraints.HORIZONTAL; weightx=0.7; insets=Insets(0,2,0,2)}
                            val valueStr = currentPaneAllOptions[paneOptDef.name] ?: paneOptDef.defaultValue.toString()

                            // For now, only use JTextField for pane options like 'weight'
                            val editorField = JTextField(valueStr)
                            editorField.document.addDocumentListener(object: DocumentListener {
                                fun update() {
                                    val paneOptionsMap = parentWidget.properties.getOrPut("pane_options") { mutableMapOf<String,MutableMap<String,String>>() } as MutableMap<String,MutableMap<String,String>>
                                    val specificPaneOpts = paneOptionsMap.getOrPut(primaryWidget.id) { mutableMapOf() }
                                    specificPaneOpts[paneOptDef.name] = editorField.text
                                    project.messageBus.syncPublisher(WIDGET_MODIFIED_TOPIC).propertyChanged(parentWidget, currentDesignDialog) // Parent (PanedWindow) was modified
                                }
                                override fun insertUpdate(e: DocumentEvent?) = update()
                                override fun removeUpdate(e: DocumentEvent?) = update()
                                override fun changedUpdate(e: DocumentEvent?) = update()
                            })
                            paneOptionsPanel.add(editorField, gbcValue)
                            yPos++
                        }
                    }
                }
            }

        } else {
            titleLabel.text = "No widget selected."
            tableModel.updateData(null, null, emptyList())
        }
        notebookTabsPanel.revalidate(); notebookTabsPanel.repaint()
        paneOptionsPanel.revalidate(); paneOptionsPanel.repaint()
        propertiesTable.revalidate(); propertiesTable.repaint()
    }
    // Full table valueColumn.cellEditor definition
}
