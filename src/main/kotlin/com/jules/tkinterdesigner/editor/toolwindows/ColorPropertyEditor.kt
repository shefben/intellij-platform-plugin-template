package com.jules.tkinterdesigner.editor.toolwindows

import com.intellij.ui.ColorChooser
import com.intellij.ui.JBColor
import java.awt.Color
import java.awt.Component
import java.awt.event.ActionEvent
import java.awt.event.ActionListener
import javax.swing.AbstractCellEditor
import javax.swing.JButton
import javax.swing.JTable
import javax.swing.SwingUtilities
import javax.swing.table.TableCellEditor

class ColorPropertyEditor(private val parentComponentForDialog: Component) : AbstractCellEditor(), TableCellEditor, ActionListener {

    private var currentColor: Color? = null
    private val button: JButton = JButton()

    init {
        button.actionCommand = "edit_color"
        button.addActionListener(this)
        // button.isContentAreaFilled = false // Optional: make it look less like a full button
        button.isBorderPainted = false // Optional: make it look less like a full button
        button.isFocusPainted = false
    }

    override fun getTableCellEditorComponent(
        table: JTable, value: Any?,
        isSelected: Boolean, row: Int, column: Int
    ): Component {
        currentColor = when (value) {
            is Color -> value
            is String -> try {
                Color.decode(value)
            } catch (e: NumberFormatException) {
                try { // Try common color names
                    val field = Color::class.java.getField(value.toUpperCase())
                    field.get(null) as? Color ?: JBColor.GRAY
                } catch (ex: Exception) { JBColor.GRAY }
            }
            else -> JBColor.GRAY
        }
        button.text = String.format("#%02x%02x%02x", currentColor!!.red, currentColor!!.green, currentColor!!.blue)
        button.background = currentColor // Show color on button itself
        button.foreground = if (isColorDark(currentColor!!)) Color.WHITE else Color.BLACK // Contrast text
        return button
    }

    override fun getCellEditorValue(): Any? {
        return currentColor // This will be a Color object
    }

    override fun actionPerformed(e: ActionEvent) {
        if ("edit_color" == e.actionCommand) {
            val newColor = ColorChooser.chooseColor(
                SwingUtilities.getWindowAncestor(parentComponentForDialog), // Ensure proper parent for dialog
                "Choose Color",
                currentColor,
                true // enable opacity
            )
            if (newColor != null) {
                currentColor = newColor
            }
            fireEditingStopped() // Important to signal the table editing has finished
        }
    }

    private fun isColorDark(color: Color): Boolean {
        // Basic luminance check
        val luminance = (0.299 * color.red + 0.587 * color.green + 0.114 * color.blue) / 255
        return luminance < 0.5
    }
}
