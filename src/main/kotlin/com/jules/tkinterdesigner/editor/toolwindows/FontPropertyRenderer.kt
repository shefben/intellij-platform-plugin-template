package com.jules.tkinterdesigner.editor.toolwindows

import java.awt.Component
import java.awt.Font
import javax.swing.JTable
import javax.swing.table.DefaultTableCellRenderer

class FontPropertyRenderer : DefaultTableCellRenderer() {

    override fun getTableCellRendererComponent(
        table: JTable, value: Any?,
        isSelected: Boolean, hasFocus: Boolean,
        row: Int, column: Int
    ): Component {
        super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column)

        if (value is String) {
            text = value // Display the font string directly
            try {
                // Attempt to parse the font string to apply it to the renderer component for visual feedback
                // This is a simple parsing logic, might need to be more robust
                val parts = value.split(" ")
                var family = value
                var size = table.font.size // Default to table's font size
                var style = Font.PLAIN

                if (parts.isNotEmpty()) {
                    // Try to find size and style
                    val potentialSizeIndex = parts.indexOfFirst { it.toIntOrNull() != null }
                    if (potentialSizeIndex != -1) {
                        size = parts[potentialSizeIndex].toInt()
                        family = parts.take(potentialSizeIndex).joinToString(" ")
                        if (family.isEmpty() && potentialSizeIndex > 0) family = parts[0] // case "12 bold"
                        else if (family.isEmpty()) family = table.font.family


                        var tempStyle = 0
                        if (parts.any { it.equals("bold", ignoreCase = true) }) {
                            tempStyle = tempStyle or Font.BOLD
                        }
                        if (parts.any { it.equals("italic", ignoreCase = true) || it.equals("oblique", ignoreCase = true) }) {
                            tempStyle = tempStyle or Font.ITALIC
                        }
                         if (tempStyle != 0) style = tempStyle else Font.PLAIN

                    } else { // No obvious size found, assume whole string is family
                        family = value
                    }
                }
                 font = Font(family, style, size)
            } catch (e: Exception) {
                // If parsing fails, fallback to table's default font but keep the text
                font = table.font
            }
        } else {
            text = value?.toString() ?: ""
        }
        return this
    }
}
