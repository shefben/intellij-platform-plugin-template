package com.jules.tkinterdesigner.editor.toolwindows

import java.awt.Component
import java.awt.event.ActionEvent
import java.awt.event.ActionListener
import javax.swing.AbstractCellEditor
import javax.swing.JCheckBox
import javax.swing.JTable
import javax.swing.SwingConstants
import javax.swing.table.TableCellEditor

class BooleanPropertyEditor : AbstractCellEditor(), TableCellEditor, ActionListener {

    private val checkBox: JCheckBox = JCheckBox()

    init {
        checkBox.isOpaque = true // So background colors show up
        checkBox.horizontalAlignment = SwingConstants.CENTER
        checkBox.addActionListener(this)
    }

    override fun getTableCellEditorComponent(
        table: JTable, value: Any?,
        isSelected: Boolean, row: Int, column: Int
    ): Component {
        checkBox.isSelected = value as? Boolean ?: false
        // Apply selection colors if needed, though editor usually handles this
        if (isSelected) {
            checkBox.foreground = table.selectionForeground
            checkBox.background = table.selectionBackground
        } else {
            checkBox.foreground = table.foreground
            checkBox.background = table.background
        }
        return checkBox
    }

    override fun getCellEditorValue(): Any {
        return checkBox.isSelected
    }

    override fun actionPerformed(e: ActionEvent) {
        // When checkbox state changes, stop editing to commit the value
        fireEditingStopped()
    }
}
