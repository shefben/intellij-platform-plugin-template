package org.jetbrains.plugins.template.tkdesigner

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
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
import java.awt.BorderLayout
import kotlin.io.path.createTempFile
import javax.swing.JButton
import javax.swing.JPanel
import javax.swing.JSplitPane

class TkinterDesignerToolWindowFactory : ToolWindowFactory {
    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val projectModel = DesignProject().apply { basePath = project.basePath ?: "" }
        var model = projectModel.activeDialog
        val designArea = DesignAreaPanel(model)
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
        val loadButton = JButton("Load")
        val saveButton = JButton("Save")
        val genButton = JButton("Generate")
        val previewButton = JButton("Preview")
        val undoButton = JButton("Undo")
        val redoButton = JButton("Redo")
        val alignLeft = JButton("Align Left")
        val alignRight = JButton("Align Right")
        val alignTop = JButton("Align Top")
        val alignBottom = JButton("Align Bottom")
        val centerH = JButton("Center H")
        val centerV = JButton("Center V")
        val distH = JButton("Distribute H")
        val distV = JButton("Distribute V")
        val dialogChooser = javax.swing.JComboBox<String>().apply { addItem("Dialog 1") }
        val addDialog = JButton("+")
        val pythonField = JBTextField(projectModel.pythonInterpreter, 10)
        val group = JButton("Group")
        val importButton = JButton("Import Python")
        val top = JPanel().apply {
            add(javax.swing.JLabel("Python:"))
            add(pythonField)
            add(dialogChooser)
            add(addDialog)
            add(loadButton)
            add(saveButton)
            add(genButton)
            add(previewButton)
            add(importButton)
            add(undoButton)
            add(redoButton)
            add(alignLeft)
            add(alignRight)
            add(alignTop)
            add(alignBottom)
            add(centerH)
            add(centerV)
            add(distH)
            add(distV)
            add(group)
        }
        panel.add(top, BorderLayout.NORTH)

        loadButton.addActionListener {
            val path = Messages.showInputDialog(project, "Enter path to load", "Load", null)
            path?.let {
                val f = java.io.File(it)
                if (f.exists()) {
                    val text = f.readText()
                    val proj = try { DesignProject.fromJson(text) } catch (_: Exception) { null }
                    if (proj != null) {
                        projectModel.dialogs.clear(); projectModel.dialogs.addAll(proj.dialogs)
                        projectModel.resources.clear(); projectModel.resources.addAll(proj.resources)
                        model = projectModel.activeDialog
                        dialogChooser.removeAllItems()
                        projectModel.dialogs.indices.forEach { dialogChooser.addItem("Dialog ${it+1}") }
                        designArea.loadModel(model)
                    } else {
                        val loaded = DialogModel.fromJson(text)
                        projectModel.dialogs.clear(); projectModel.dialogs.add(loaded)
                        dialogChooser.removeAllItems(); dialogChooser.addItem("Dialog 1")
                        model = loaded
                        designArea.loadModel(loaded)
                    }
                }
            }
        }

        saveButton.addActionListener {
            properties.applyChanges()
            val path = Messages.showInputDialog(project, "Enter path to save", "Save", null)
            path?.let {
                val f = java.io.File(if (it.endsWith(".tkdesign")) it else "$it.tkdesign")
                f.writeText(projectModel.toJson())
            }
        }

        genButton.addActionListener {
            properties.applyChanges()
            val pythonCode = TkinterGenerator.generate(designArea.model, projectModel.translations)
            val clipboard = com.intellij.openapi.ide.CopyPasteManager.getInstance()
            clipboard.setContents(java.awt.datatransfer.StringSelection(pythonCode))
            Messages.showInfoMessage(project, "Python code copied to clipboard", "Generated")
        }

        previewButton.addActionListener {
            properties.applyChanges()
            projectModel.pythonInterpreter = pythonField.text
            val code = TkinterGenerator.generate(designArea.model, projectModel.translations)
            try {
                val tmp = kotlin.io.path.createTempFile("preview", ".py").toFile()
                tmp.writeText(code)
                Runtime.getRuntime().exec(arrayOf(projectModel.pythonInterpreter, tmp.absolutePath))
            } catch (e: Exception) {
                Messages.showErrorDialog(project, e.message, "Preview failed")
            }
        }

        undoButton.addActionListener { designArea.undo() }
        redoButton.addActionListener { designArea.redo() }
        alignLeft.addActionListener { designArea.alignLeft() }
        alignRight.addActionListener { designArea.alignRight() }
        alignTop.addActionListener { designArea.alignTop() }
        alignBottom.addActionListener { designArea.alignBottom() }
        centerH.addActionListener { designArea.alignCenterHorizontal() }
        centerV.addActionListener { designArea.alignCenterVertical() }
        distH.addActionListener { designArea.distributeHorizontal() }
        distV.addActionListener { designArea.distributeVertical() }
        group.addActionListener { designArea.groupSelected() }
        importButton.addActionListener {
            val path = Messages.showInputDialog(project, "Python file to import", "Import", null)
            path?.let {
                val f = java.io.File(it)
                if (f.exists()) {
                    val loaded = TkinterImporter.importScript(f.readText())
                    projectModel.dialogs.clear(); projectModel.dialogs.add(loaded)
                    dialogChooser.removeAllItems(); dialogChooser.addItem("Dialog 1")
                    model = loaded
                    designArea.loadModel(loaded)
                }
            }
        }

        dialogChooser.addActionListener {
            val idx = dialogChooser.selectedIndex
            if (idx >= 0 && idx < projectModel.dialogs.size) {
                model = projectModel.dialogs[idx]
                projectModel.current = idx
                designArea.loadModel(model)
            }
        }

        addDialog.addActionListener {
            val newDialog = DialogModel()
            projectModel.dialogs.add(newDialog)
            dialogChooser.addItem("Dialog ${projectModel.dialogs.size}")
            dialogChooser.selectedIndex = projectModel.dialogs.size - 1
        }

        val content = ContentFactory.getInstance().createContent(panel, null, false)
        toolWindow.contentManager.addContent(content)

        palette.setLocation(200, 200)
        palette.isVisible = true
    }

    override fun shouldBeAvailable(project: Project) = true
}
