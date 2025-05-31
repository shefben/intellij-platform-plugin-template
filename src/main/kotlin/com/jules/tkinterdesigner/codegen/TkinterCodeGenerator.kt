package com.jules.tkinterdesigner.codegen

import com.jules.tkinterdesigner.model.DesignedDialog
import com.jules.tkinterdesigner.model.DesignedWidget

object TkinterCodeGenerator {

    fun generateCode(dialog: DesignedDialog): String {
        val sb = StringBuilder()

        // Imports
        sb.appendLine("import tkinter as tk")
        sb.appendLine("from tkinter import ttk")
        sb.appendLine()

        // Main Window Setup
        sb.appendLine("root = tk.Tk()")
        sb.appendLine("root.title(\"${dialog.title.replace("\"", "\\\"")}\")") // Escape quotes for Python string
        sb.appendLine("root.geometry(\"${dialog.width}x${dialog.height}\")")
        sb.appendLine()

        // Widget Loop
        for (widget in dialog.widgets) {
            val widgetVarName = widget.properties["name"]?.toString()?.replace(" ", "_")?.replace("[^A-Za-z0-9_]".toRegex(), "") ?: widget.id.replace("-","_")
            val tkClass = widget.type // e.g., "ttk.Button" or "Button"

            val constructorArgs = mutableListOf<String>()
            for ((key, value) in widget.properties) {
                if (key == "name" || key == "x" || key == "y" || key == "width" || key == "height") {
                    continue // Skip internal or layout properties handled by .place()
                }

                val formattedValue = when (value) {
                    is String -> "\"${value.replace("\"", "\\\"")}\"" // Escape quotes for Python string
                    is Int, is Boolean -> value.toString()
                    // Add more type handling as needed (e.g., for colors, fonts)
                    else -> "\"${value.toString().replace("\"", "\\\"")}\"" // Default to string representation
                }
                constructorArgs.add("$key=$formattedValue")
            }
            val argsString = constructorArgs.joinToString(", ")

            // Determine master (parent) - for now, all are direct children of root
            val master = "root"

            sb.appendLine("$widgetVarName = $tkClass($master${if (argsString.isNotEmpty()) ", " else ""}$argsString)")
            sb.appendLine("$widgetVarName.place(x=${widget.x}, y=${widget.y}, width=${widget.width}, height=${widget.height})")
            sb.appendLine()
        }

        // Main Loop
        sb.appendLine("root.mainloop()")

        return sb.toString()
    }
}
