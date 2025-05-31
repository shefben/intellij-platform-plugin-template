package com.jules.tkinterdesigner.editor.toolwindows

import com.intellij.openapi.fileChooser.FileChooser
import com.intellij.openapi.fileChooser.FileChooserDescriptor
import com.intellij.openapi.project.Project // Already present, ensure it's used
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.ui.components.JBTextField
import java.awt.BorderLayout
import java.awt.Component
import java.awt.event.ActionEvent
import java.awt.event.ActionListener
import javax.swing.*
import javax.swing.table.TableCellEditor

class FilePathPropertyEditor(
    private val project: Project, // Changed to non-nullable, will be passed from PropertyEditorComponent
    private val parentComponentForDialog: Component
) : AbstractCellEditor(), TableCellEditor, ActionListener {

    private val panel = JPanel(BorderLayout())
    private val pathTextField = JBTextField() // Renamed for clarity
    private val button = JButton("...")

    // private var currentPath: String? = null // Not strictly needed as state here, text field holds it

    init {
        panel.add(pathTextField, BorderLayout.CENTER)
        panel.add(button, BorderLayout.EAST)
        button.addActionListener(this)

        pathTextField.addFocusListener(object : java.awt.event.FocusAdapter() {
            override fun focusLost(e: java.awt.event.FocusEvent?) {
                fireEditingStopped()
            }
        })
    }

    override fun getTableCellEditorComponent(
        table: JTable, value: Any?,
        isSelected: Boolean, row: Int, column: Int
    ): Component {
        // currentPath = value as? String ?: "" // Store if needed for comparison, but textField is source of truth
        pathTextField.text = value as? String ?: ""
        return panel
    }

    override fun getCellEditorValue(): Any? {
        return pathTextField.text
    }

    override fun actionPerformed(e: ActionEvent) {
        if (e.source == button) {
            val descriptor = FileChooserDescriptor(
                true, false, false, false, false, false
            ).withTitle("Select Image File")
             .withFileFilter { vf ->
                 val ext = vf.extension?.toLowerCase()
                 ext in listOf("png", "gif", "jpg", "jpeg", "ico", "bmp", "ppm", "pgm")
             }

            val baseDirToOpen = project.baseDir // Or determine a more specific starting directory

            val chosenFile = FileChooser.chooseFile(descriptor, project, parentComponentForDialog, baseDirToOpen)

            if (chosenFile != null) {
                val relativePath = project.baseDir?.let { VfsUtilCore.getRelativePath(chosenFile, it, '/') }

                if (relativePath != null) {
                    pathTextField.text = relativePath
                } else {
                    pathTextField.text = chosenFile.path // Fallback to absolute path
                }
                // currentPath = pathTextField.text // Update if currentPath member is used
                fireEditingStopped()
            }
        }
    }
}
