package com.jules.tkinterdesigner.editor.toolwindows

import com.intellij.ui.JBColor
import java.awt.Color
import java.awt.Component
import javax.swing.JLabel
import javax.swing.JTable
import javax.swing.JPanel
import javax.swing.table.DefaultTableCellRenderer
import java.awt.BorderLayout
import java.awt.Dimension

class ColorPropertyRenderer : DefaultTableCellRenderer() {
    private val panel = JPanel(BorderLayout(5, 0)) // Small gap between color square and text
    private val colorSquare = JPanel()
    private val label = JLabel()

    init {
        colorSquare.preferredSize = Dimension(18, 18) // Fixed size for the color square
        colorSquare.isOpaque = true
        panel.add(colorSquare, BorderLayout.WEST)
        panel.add(label, BorderLayout.CENTER)
        panel.isOpaque = true // Panel itself should be opaque
    }

    override fun getTableCellRendererComponent(
        table: JTable, value: Any?,
        isSelected: Boolean, hasFocus: Boolean,
        row: Int, column: Int
    ): Component {
        // Let super handle selection backgrounds etc. on the main panel
        super.getTableCellRendererComponent(table, "", isSelected, hasFocus, row, column)
        panel.background = background // Use background from super call
        label.foreground = foreground
        label.font = font

        if (value is Color) {
            colorSquare.background = value
            label.text = String.format("#%02x%02x%02x", value.red, value.green, value.blue)
            colorSquare.border = if (isSelected) UIManager.getBorder("Table.focusCellHighlightBorder") else null
        } else if (value is String) { // If color is stored as string
            try {
                val color = Color.decode(value) // Handles #RRGGBB
                colorSquare.background = color
                label.text = value
            } catch (e: NumberFormatException) {
                try { // Try common color names
                    val field = Color::class.java.getField(value.toUpperCase())
                    val color = field.get(null) as? Color ?: JBColor.GRAY
                    colorSquare.background = color
                    label.text = value
                } catch (ex: Exception) {
                    colorSquare.background = JBColor.GRAY // Fallback
                    label.text = value
                }
            }
            colorSquare.border = if (isSelected) UIManager.getBorder("Table.focusCellHighlightBorder") else null
        } else {
            colorSquare.background = table.background
            label.text = value?.toString() ?: ""
            colorSquare.border = null
        }
        return panel
    }
}
