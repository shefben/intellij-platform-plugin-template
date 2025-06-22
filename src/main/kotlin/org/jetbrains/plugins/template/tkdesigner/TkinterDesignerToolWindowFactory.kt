package org.jetbrains.plugins.template.tkdesigner

import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextField
import com.intellij.ui.content.ContentFactory
import org.jetbrains.plugins.template.tkdesigner.model.DialogModel
import org.jetbrains.plugins.template.tkdesigner.model.DesignProject
import org.jetbrains.plugins.template.tkdesigner.ui.ComponentPalette
import org.jetbrains.plugins.template.tkdesigner.ui.DesignAreaPanel
import org.jetbrains.plugins.template.tkdesigner.ui.PropertyPanel
import org.jetbrains.plugins.template.tkdesigner.ui.HierarchyPanel
import org.jetbrains.plugins.template.tkdesigner.DesignerSettings
import org.jetbrains.plugins.template.tkdesigner.DialogTemplates
import java.awt.BorderLayout
import javax.swing.JButton
import javax.swing.JPanel
import javax.swing.JSplitPane
import org.jetbrains.plugins.template.tkdesigner.actions.DesignerToolbar

class TkinterDesignerToolWindowFactory : ToolWindowFactory {
    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val settings = DesignerSettings.instance()
        val projectModel = DesignProject().apply {
            basePath = project.basePath ?: ""
            pythonInterpreter = settings.state.interpreter
        }
        var model = projectModel.activeDialog
        val designArea = DesignAreaPanel(model, projectModel).apply { gridSize = settings.state.gridSize }
        val palette = ComponentPalette(designArea)
        val properties = PropertyPanel(designArea, projectModel).apply { isVisible = false }
        val hierarchy = HierarchyPanel(designArea)
        designArea.selectionListener = {
            properties.bind(it)
            properties.isVisible = it != null
            hierarchy.refresh()
        }
        designArea.dialogListener = {
            properties.bindDialog(it)
            properties.isVisible = true
            hierarchy.refresh()
        }

        val panel = JPanel(BorderLayout())
        val centerSplit = JSplitPane(JSplitPane.HORIZONTAL_SPLIT, JBScrollPane(designArea), properties)
        centerSplit.resizeWeight = 0.7
        val split = JSplitPane(JSplitPane.HORIZONTAL_SPLIT, JBScrollPane(hierarchy), centerSplit)
        split.resizeWeight = 0.2
        panel.add(split, BorderLayout.CENTER)
        val dialogChooser = javax.swing.JComboBox<String>().apply { addItem("Dialog 1") }
        val pythonField = JBTextField(projectModel.pythonInterpreter, 10)
        pythonField.document.addDocumentListener(object : com.intellij.ui.DocumentAdapter() {
            override fun textChanged(e: javax.swing.event.DocumentEvent) { settings.state.interpreter = pythonField.text }
        })
        val addDialog = JButton("+")
        val toolbar = DesignerToolbar(designArea, projectModel, properties, project, dialogChooser, pythonField).toolbar
        val top = JPanel().apply {
            add(javax.swing.JLabel("Python:"))
            add(pythonField)
            add(dialogChooser)
            add(addDialog)
            add(toolbar.component)
        }
        panel.add(top, BorderLayout.NORTH)


        dialogChooser.addActionListener {
            val idx = dialogChooser.selectedIndex
            if (idx >= 0 && idx < projectModel.dialogs.size) {
                model = projectModel.dialogs[idx]
                projectModel.current = idx
                designArea.loadModel(model)
            }
        }

        addDialog.addActionListener {
            val options = arrayOf("Blank", "Simple Form", "Menu Dialog")
            val sel = javax.swing.JOptionPane.showInputDialog(panel, "Template", "New Dialog", javax.swing.JOptionPane.PLAIN_MESSAGE, null, options, options[0])
            val newDialog = when (sel) {
                "Simple Form" -> DialogTemplates.simpleForm()
                "Menu Dialog" -> DialogTemplates.menuDialog()
                else -> DialogTemplates.blank()
            }
            projectModel.dialogs.add(newDialog)
            dialogChooser.addItem("Dialog ${projectModel.dialogs.size}")
            dialogChooser.selectedIndex = projectModel.dialogs.size - 1
        }

        val content = ContentFactory.getInstance().createContent(panel, null, false)
        toolWindow.contentManager.addContent(content)

        palette.setLocation(settings.state.paletteX, settings.state.paletteY)
        palette.addComponentListener(object : java.awt.event.ComponentAdapter() {
            override fun componentMoved(e: java.awt.event.ComponentEvent) {
                settings.state.paletteX = palette.x
                settings.state.paletteY = palette.y
            }
        })
        palette.isVisible = true
    }

    override fun shouldBeAvailable(project: Project) = true
}
