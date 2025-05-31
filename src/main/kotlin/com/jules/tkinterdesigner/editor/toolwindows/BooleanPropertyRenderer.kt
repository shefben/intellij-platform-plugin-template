package com.jules.tkinterdesigner.editor.toolwindows

import java.awt.Component
import javax.swing.JCheckBox
import javax.swing.JTable
import javax.swing.SwingConstants
import javax.swing.UIManager
import javax.swing.table.TableCellRenderer

class BooleanPropertyRenderer : JCheckBox(), TableCellRenderer {

    init {
        isOpaque = true
        horizontalAlignment = SwingConstants.CENTER // Center the checkbox in the cell
    }

    override fun getTableCellRendererComponent(
        table: JTable, value: Any?,
        isSelected: Boolean, hasFocus: Boolean,
        row: Int, column: Int
    ): Component {
        if (isSelected) {
            foreground = table.selectionForeground
            super.setBackground(table.selectionBackground) // Use super to avoid ClassCastException if this is JPanel
        } else {
            foreground = table.foreground
            background = table.background
        }

        this.isSelected = value as? Boolean ?: false

        // For focus border if needed
        // border = if (hasFocus) UIManager.getBorder("Table.focusCellHighlightBorder") else UIManager.getBorder("CheckBox.border")

        return this
    }
}
