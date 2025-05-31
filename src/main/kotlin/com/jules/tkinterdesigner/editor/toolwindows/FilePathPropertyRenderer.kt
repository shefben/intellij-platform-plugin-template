package com.jules.tkinterdesigner.editor.toolwindows

import java.awt.Component
import javax.swing.JTable
import javax.swing.table.DefaultTableCellRenderer

class FilePathPropertyRenderer : DefaultTableCellRenderer() {

    override fun getTableCellRendererComponent(
        table: JTable, value: Any?,
        isSelected: Boolean, hasFocus: Boolean,
        row: Int, column: Int
    ): Component {
        super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column)
        // Simply display the path string. Tooltips or icons could be added later.
        text = value as? String ?: ""
        return this
    }
}
