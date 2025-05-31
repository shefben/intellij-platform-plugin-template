package com.jules.tkinterdesigner.editor.toolwindows

import java.awt.Component
import java.awt.event.ActionEvent
import java.awt.event.ActionListener
import javax.swing.AbstractCellEditor
import javax.swing.JButton
import javax.swing.JTable
import javax.swing.SwingUtilities
import javax.swing.table.TableCellEditor

class FontPropertyEditor(private val parentComponentForDialog: Component) : AbstractCellEditor(), TableCellEditor, ActionListener {

    private var currentFontString: String? = null
    private val button: JButton = JButton()

    init {
        button.actionCommand = "edit_font"
        button.addActionListener(this)
        button.horizontalAlignment = SwingConstants.LEFT
        button.isBorderPainted = false
        // button.isContentAreaFilled = false // Optional
    }

    override fun getTableCellEditorComponent(
        table: JTable, value: Any?,
        isSelected: Boolean, row: Int, column: Int
    ): Component {
        currentFontString = value as? String ?: "TkDefaultFont"
        button.text = currentFontString
        // Optionally try to parse and set button font for preview, but can be complex
        // try { button.font = Font.decode(currentFontString) } catch (e: Exception) { /* use default */ }
        return button
    }

    override fun getCellEditorValue(): Any? {
        return currentFontString
    }

    override fun actionPerformed(e: ActionEvent) {
        if ("edit_font" == e.actionCommand) {
            val dialog = FontChooserDialog(SwingUtilities.getWindowAncestor(parentComponentForDialog), currentFontString)
            if (dialog.showAndGet()) { // showAndGet returns true if OK was pressed
                currentFontString = dialog.getSelectedFontString()
            }
            fireEditingStopped() // Important to signal the table editing has finished
        }
    }
}
