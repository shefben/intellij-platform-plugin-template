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
    private var currentDesignDialog: DesignedDialog? = null

    private val tableModel: WidgetPropertyTableModel
    private val propertiesTable: JBTable
    private val titleLabel: JBLabel
    private val generateCodeButton: JButton
    private val notebookTabsPanel: JBPanel<JBPanel<*>> // Panel for notebook tab details
    private val tabInfoLabel: JBLabel // To show text like "Tabs:"

    init {
        titleLabel = JBLabel("No widget selected.")
        add(titleLabel, BorderLayout.NORTH)

        tableModel = WidgetPropertyTableModel(emptyList(), project, null, null)
        propertiesTable = JBTable(tableModel)
        // ... (table setup as before) ...
        propertiesTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION)
        propertiesTable.rowHeight = 24
        val valueColumn = propertiesTable.columnModel.getColumn(1)
        valueColumn.cellRenderer = TableCellRenderer { table,value,isSelected,hasFocus,row,column -> /* ... as before ... */ }
        valueColumn.cellEditor = object : AbstractCellEditor(), SwingTableCellEditor { /* ... as before ... */ }


        // Panel to hold both properties table and notebook tabs info
        val centerContentPanel = JBPanel<JBPanel<*>>(BorderLayout())
        centerContentPanel.add(JBScrollPane(propertiesTable), BorderLayout.CENTER)

        notebookTabsPanel = JBPanel<JBPanel<*>>(GridBagLayout())
        tabInfoLabel = JBLabel("Tabs:").apply { isVisible = false }
        val gbcTabLabel = GridBagConstraints().apply { gridx=0; gridy=0; anchor=GridBagConstraints.WEST; }
        notebookTabsPanel.add(tabInfoLabel, gbcTabLabel)
        centerContentPanel.add(notebookTabsPanel, BorderLayout.SOUTH)

        add(centerContentPanel, BorderLayout.CENTER)


        generateCodeButton = JButton("Generate Code for Current Design")
        // ... (button setup as before) ...
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
        // Clear previous notebook tab UI
        notebookTabsPanel.removeAll()
        val gbcTabLabel = GridBagConstraints().apply { gridx=0; gridy=0; anchor=GridBagConstraints.WEST; insets = Insets(2,2,2,2)}
        tabInfoLabel.isVisible = false
        notebookTabsPanel.add(tabInfoLabel, gbcTabLabel)


        if (selectedCount > 1) {
            titleLabel.text = "$selectedCount widgets selected"
            tableModel.updateData(null, null, emptyList())
        } else if (primaryWidget != null) {
            titleLabel.text = "Properties: ${primaryWidget.type} (${primaryWidget.properties["name"] ?: primaryWidget.id})"
            val propertyDefinitions = WidgetPropertyRegistry.propertiesForType[primaryWidget.type] ?: emptyList()
            val propertyPairs = propertyDefinitions.map { propDef ->
                Pair(propDef, primaryWidget.properties.getOrDefault(propDef.name, propDef.defaultValue))
            }
            tableModel.updateData(primaryWidget, currentDesignDialog, propertyPairs)

            if (primaryWidget.type == "ttk.Notebook") {
                tabInfoLabel.isVisible = true
                val tabs = primaryWidget.properties["tabs"] as? List<Map<String, String>> ?: emptyList()
                var yPos = 1 // Start below "Tabs:" label
                tabs.forEachIndexed { index, tabData ->
                    val tabName = tabData["text"] ?: "Tab ${index + 1}"
                    val frameId = tabData["frameId"] ?: "N/A"

                    val nameField = JTextField(tabName).apply { isEditable = true /* Allow editing later */ }
                    nameField.document.addDocumentListener(object: DocumentListener {
                        fun update() {
                            (primaryWidget.properties["tabs"] as? MutableList<MutableMap<String,String>>)?.getOrNull(index)?.set("text", nameField.text)
                            project.messageBus.syncPublisher(WIDGET_MODIFIED_TOPIC).propertyChanged(primaryWidget, currentDesignDialog)
                        }
                        override fun insertUpdate(e: DocumentEvent?) = update()
                        override fun removeUpdate(e: DocumentEvent?) = update()
                        override fun changedUpdate(e: DocumentEvent?) = update()
                    })

                    val gbc = GridBagConstraints().apply { gridx=0; gridy=yPos; anchor=GridBagConstraints.WEST; fill=GridBagConstraints.HORIZONTAL; weightx=0.4; insets = Insets(0,2,0,2) }
                    notebookTabsPanel.add(JBLabel("Tab ${index+1}:"), gbc)
                    gbc.gridx=1; weightx=0.6;
                    notebookTabsPanel.add(nameField, gbc)
                    // val frameLabel = JBLabel("Frame: $frameId") // For display only
                    // gbc.gridx=1; gridy=yPos+1; gridwidth=2
                    // notebookTabsPanel.add(frameLabel, gbc)
                    yPos++
                }
            }
        } else {
            titleLabel.text = "No widget selected."
            tableModel.updateData(null, null, emptyList())
        }
        notebookTabsPanel.revalidate()
        notebookTabsPanel.repaint()
        propertiesTable.revalidate() // Ensure table also repaints if its size changes due to panel above/below
        propertiesTable.repaint()
    }
    // Ensure the valueColumn setup from the init block is correctly placed (it was complex)
    // For brevity, assuming it's correctly merged into the init block.
}
