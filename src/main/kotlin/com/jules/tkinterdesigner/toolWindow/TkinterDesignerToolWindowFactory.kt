package com.jules.tkinterdesigner.toolWindow

import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBPanel
import com.intellij.ui.components.JBScrollPane // Keep this for property editor
import com.intellij.ui.components.JBTextArea // For code dialog
import com.intellij.ui.content.ContentFactory
import com.jules.tkinterdesigner.TkinterDesignerBundle
import com.jules.tkinterdesigner.codegen.TkinterCodeGenerator // For code generation
import com.jules.tkinterdesigner.model.DesignedWidget
import com.jules.tkinterdesigner.model.WidgetPaletteInfo
import com.jules.tkinterdesigner.model.WidgetPropertyRegistry
import com.jules.tkinterdesigner.services.TkinterProjectService
import java.awt.BorderLayout // Keep this for main panel layout
import java.awt.GridBagConstraints // Keep this for property editor
import java.awt.GridBagLayout // Keep this for property editor
import java.awt.datatransfer.StringSelection
import java.awt.dnd.DnDConstants
import java.awt.dnd.DragGestureEvent
import java.awt.dnd.DragGestureListener
import java.awt.dnd.DragSource
import javax.swing.BoxLayout // Keep this for widget palette
import javax.swing.JButton
import javax.swing.JComboBox // Keep this for property editor
import javax.swing.JDialog // For code dialog
import javax.swing.JLabel // Keep this for property editor
import javax.swing.JTextField // Keep this for property editor
import javax.swing.SwingUtilities // For code dialog
import javax.swing.event.DocumentEvent // Keep this for property editor
import javax.swing.event.DocumentListener // Keep this for property editor


class TkinterDesignerToolWindowFactory : ToolWindowFactory {

    init {
    }

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val tkinterDesignerPanel = TkinterDesignerPanel(toolWindow)
        val content = ContentFactory.getInstance().createContent(tkinterDesignerPanel.getContent(), null, false)
        toolWindow.contentManager.addContent(content)
    }

    override fun shouldBeAvailable(project: Project) = true

    class TkinterDesignerPanel(toolWindow: ToolWindow) {
        private val service = toolWindow.project.service<TkinterProjectService>()
        private lateinit var visualCanvasPanel: VisualCanvasPanel
        private lateinit var propertyEditorPanel: JBPanel<*>

        fun getContent(): JBPanel<*> {
            val mainPanel = JBPanel<JBPanel<*>>(BorderLayout()) // Explicitly BorderLayout

            // Widget Palette
            val widgetPalettePanel = JBPanel<JBPanel<*>>()
            widgetPalettePanel.layout = BoxLayout(widgetPalettePanel, BoxLayout.Y_AXIS)
            widgetPalettePanel.preferredSize = java.awt.Dimension(150, 0)
            mainPanel.add(widgetPalettePanel, BorderLayout.WEST)

            val dragSource = DragSource.getDefaultDragSource()
            for (widgetType in WidgetPaletteInfo.WIDGETS) {
                val button = JButton(widgetType)
                button.toolTipText = "Drag to add $widgetType"
                dragSource.createDefaultDragGestureRecognizer(
                    button,
                    DnDConstants.ACTION_COPY,
                    object : DragGestureListener {
                        override fun dragGestureRecognized(dge: DragGestureEvent) {
                            val transferable = StringSelection(widgetType)
                            dragSource.startDrag(
                                dge,
                                DragSource.DefaultCopyDrop,
                                transferable,
                                null
                            )
                        }
                    }
                )
                widgetPalettePanel.add(button)
            }

            // Design Canvas
            visualCanvasPanel = VisualCanvasPanel()
            mainPanel.add(visualCanvasPanel, BorderLayout.CENTER)
            visualCanvasPanel.addTestWidget()

            // Property Editor
            propertyEditorPanel = JBPanel<JBPanel<*>>(GridBagLayout())
            propertyEditorPanel.background = com.intellij.util.ui.UIUtil.getPanelBackground() // Explicit background
            val scrollPane = JBScrollPane(propertyEditorPanel) // Wrap property panel in scroll pane
            scrollPane.preferredSize = java.awt.Dimension(280, 0)
            mainPanel.add(scrollPane, BorderLayout.EAST)

            visualCanvasPanel.onWidgetSelected = { widget -> updatePropertyEditor(widget) }
            updatePropertyEditor(null)

            return mainPanel
        }

        private fun updatePropertyEditor(selectedWidget: DesignedWidget?) {
            propertyEditorPanel.removeAll()
            val gbc = GridBagConstraints()
            gbc.fill = GridBagConstraints.HORIZONTAL
            gbc.gridx = 0
            gbc.gridy = 0
            gbc.weightx = 0.0
            gbc.anchor = GridBagConstraints.WEST

            if (selectedWidget == null) {
                propertyEditorPanel.add(JBLabel("No widget selected."), gbc)
            } else {
                gbc.gridwidth = 2
                propertyEditorPanel.add(JBLabel("Properties: ${selectedWidget.type} (${selectedWidget.properties["name"] ?: selectedWidget.id})"), gbc)
                gbc.gridwidth = 1
                gbc.gridy++

                val propertyDefinitions = WidgetPropertyRegistry.propertiesForType[selectedWidget.type] ?: emptyList()

                for (propDef in propertyDefinitions) {
                    gbc.gridx = 0
                    gbc.weightx = 0.3
                    propertyEditorPanel.add(JBLabel(propDef.name + ":"), gbc)

                    gbc.gridx = 1
                    gbc.weightx = 0.7
                    val currentValue = selectedWidget.properties.getOrDefault(propDef.name, propDef.defaultValue)

                    when {
                        propDef.options != null -> {
                            val comboBox = JComboBox(propDef.options.toTypedArray())
                            comboBox.selectedItem = currentValue as? String ?: propDef.defaultValue
                            comboBox.addItemListener { e ->
                                if (e.stateChange == java.awt.event.ItemEvent.SELECTED) {
                                    selectedWidget.properties[propDef.name] = comboBox.selectedItem as String
                                    visualCanvasPanel.repaint()
                                }
                            }
                            propertyEditorPanel.add(comboBox, gbc)
                        }
                        propDef.type == String::class.java -> {
                            val textField = JTextField(currentValue as? String ?: "")
                            textField.document.addDocumentListener(object : DocumentListener {
                                override fun insertUpdate(e: DocumentEvent?) { update() }
                                override fun removeUpdate(e: DocumentEvent?) { update() }
                                override fun changedUpdate(e: DocumentEvent?) { update() }
                                fun update() {
                                    selectedWidget.properties[propDef.name] = textField.text
                                    visualCanvasPanel.repaint()
                                    if (propDef.name == "name") {
                                       (propertyEditorPanel.getComponent(0) as? JBLabel)?.text = "Properties: ${selectedWidget.type} (${selectedWidget.properties["name"] ?: selectedWidget.id})"
                                    }
                                }
                            })
import com.intellij.ui.JBColor // For error indication background
import javax.swing.UIManager // For default background

                            propertyEditorPanel.add(textField, gbc)
                        }
                        propDef.type == Int::class.java -> {
                            val textField = JTextField((currentValue as? Int ?: propDef.defaultValue as Int).toString())
                            textField.document.addDocumentListener(object : DocumentListener {
                                private fun handleUpdate() {
                                    val text = textField.text
                                    val intValue = text.toIntOrNull()
                                    if (intValue == null && text.isNotBlank()) {
                                        textField.background = JBColor.PINK
                                        // Potentially disable apply/ok button or show error icon
                                    } else {
                                        textField.background = UIManager.getColor("TextField.background") // Revert to normal
                                        selectedWidget.properties[propDef.name] = intValue ?: propDef.defaultValue
                                        visualCanvasPanel.repaint()
                                    }
                                }
                                override fun insertUpdate(e: DocumentEvent?) { handleUpdate() }
                                override fun removeUpdate(e: DocumentEvent?) { handleUpdate() }
                                override fun changedUpdate(e: DocumentEvent?) { /* Usually not needed for plain text fields */ }
                            })
                            // Optional: Revert background on focus lost if still invalid
                            textField.addFocusListener(object : java.awt.event.FocusAdapter() {
                                override fun focusLost(e: java.awt.event.FocusEvent?) {
                                    if (textField.background == JBColor.PINK) {
                                       // Optionally revert to valid previous value or default
                                       // For now, just reset background if user clicks away
                                       // textField.text = (selectedWidget.properties[propDef.name] as? Int ?: propDef.defaultValue as Int).toString()
                                       // textField.background = UIManager.getColor("TextField.background")
                                    }
                                }
                            })
                            propertyEditorPanel.add(textField, gbc)
                        }
                        else -> {
                            propertyEditorPanel.add(JBLabel("Unsupported type: ${propDef.type.simpleName}"), gbc)
                        }
                    }
                    gbc.gridy++
                }
            }

            gbc.gridx = 0
            gbc.gridy++
            gbc.weighty = 1.0 // Filler to push to top
            gbc.gridwidth = 2
            propertyEditorPanel.add(JLabel(""), gbc)

            // Add Generate Code Button
            gbc.gridy++
            gbc.weighty = 0.0
            gbc.fill = GridBagConstraints.NONE
            gbc.anchor = GridBagConstraints.CENTER
            val generateCodeButton = JButton("Generate Code")
            generateCodeButton.addActionListener {
                val pythonCode = TkinterCodeGenerator.generateCode(visualCanvasPanel.currentDesign)

                val codeDialog = JDialog(SwingUtilities.getWindowAncestor(propertyEditorPanel), "Generated Python Code")
                val textArea = JBTextArea(pythonCode)
                textArea.isEditable = false
                codeDialog.add(JBScrollPane(textArea))
                codeDialog.setSize(600, 500)
                codeDialog.setLocationRelativeTo(null)
                codeDialog.isVisible = true
            }
            propertyEditorPanel.add(generateCodeButton, gbc)

            propertyEditorPanel.revalidate()
            propertyEditorPanel.repaint()
        }
    }
}
