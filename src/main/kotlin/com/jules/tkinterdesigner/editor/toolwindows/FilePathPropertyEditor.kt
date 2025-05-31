package com.jules.tkinterdesigner.editor.toolwindows

import com.intellij.openapi.fileChooser.FileChooser
import com.intellij.openapi.fileChooser.FileChooserDescriptor
import com.intellij.openapi.project.Project
import com.intellij.ui.components.JBTextField
import java.awt.BorderLayout
import java.awt.Component
import java.awt.event.ActionEvent
import java.awt.event.ActionListener
import javax.swing.*
import javax.swing.table.TableCellEditor

class FilePathPropertyEditor(
    private val project: Project?, // Project is needed for FileChooser
    private val parentComponentForDialog: Component
) : AbstractCellEditor(), TableCellEditor, ActionListener {

    private val panel = JPanel(BorderLayout())
    private val textField = JBTextField()
    private val button = JButton("...")

    private var currentPath: String? = null

    init {
        panel.add(textField, BorderLayout.CENTER)
        panel.add(button, BorderLayout.EAST)
        button.addActionListener(this)

        // Stop editing when focus is lost from the text field, committing the typed value
        textField.addFocusListener(object : java.awt.event.FocusAdapter() {
            override fun focusLost(e: java.awt.event.FocusEvent?) {
                fireEditingStopped()
            }
        })
    }

    override fun getTableCellEditorComponent(
        table: JTable, value: Any?,
        isSelected: Boolean, row: Int, column: Int
    ): Component {
        currentPath = value as? String ?: ""
        textField.text = currentPath
        return panel
    }

    override fun getCellEditorValue(): Any? {
        // Return the text from the text field, which might have been manually edited
        // or set by the file chooser.
        return textField.text
    }

    override fun actionPerformed(e: ActionEvent) {
        if (e.source == button) {
            val descriptor = FileChooserDescriptor(
                true, // choose files
                false, // choose folders
                false, // choose jars
                false, // choose jar contents
                false, // choose multiple
                false // choose archives
            ).withTitle("Select Image File")
             .withFileFilter { vf ->
                 // Allow common image extensions; case-insensitive
                 val ext = vf.extension?.toLowerCase()
                 ext in listOf("png", "gif", "jpg", "jpeg", "ico", "bmp", "ppm", "pgm")
             }


            // If project is null, file chooser might not work well or might show a default context.
            // For a proper project-contextual file chooser, project should ideally not be null.
            val chosenFile = FileChooser.chooseFile(descriptor, project, parentComponentForDialog, null)

            if (chosenFile != null) {
                textField.text = chosenFile.path
                currentPath = chosenFile.path
                fireEditingStopped() // Signal that editing is done and value can be retrieved
            } else {
                // Optional: if you want to cancel editing if no file is chosen
                // fireEditingCanceled()
            }
        }
    }
}
