package com.jules.tkinterdesigner.codegen

import com.jules.tkinterdesigner.model.DesignedDialog
import com.jules.tkinterdesigner.model.DesignedWidget
import java.awt.Color

object TkinterCodeGenerator {

    fun generateCode(dialog: DesignedDialog): String {
        val sb = StringBuilder()

        // Imports
        sb.appendLine("import tkinter as tk")
        sb.appendLine("from tkinter import ttk")
        sb.appendLine()

        // Main Window Setup
        sb.appendLine("root = tk.Tk()")
        sb.appendLine("root.title(\"${dialog.title.replace("\"", "\\\"")}\")")
        sb.appendLine("root.geometry(\"${dialog.width}x${dialog.height}\")")
        sb.appendLine()

        val imageVarMap = mutableMapOf<String, String>()
        val widgetVarNames = mutableMapOf<String, String>()

        // First pass for PhotoImage definitions and to collect all widget var names
        // This pass should iterate all widgets, regardless of hierarchy, to define images globally.
        for (widget in dialog.widgets) {
            val pyVarNameBase = (widget.properties["name"]?.toString()?.replace(" ", "_")?.replace("[^A-Za-z0-9_]".toRegex(), "") ?: widget.id.replace("-","_"))
            widgetVarNames[widget.id] = pyVarNameBase // Store the base name, might need suffixes for non-unique names later

            widget.properties.forEach { (key, value) ->
                if ((key == "image" || key == "file") && value is String && value.isNotBlank()) {
                    val imageVar = "${pyVarNameBase}_${key}_img"
                    imageVarMap["${widget.id}_$key"] = imageVar
                    val pythonPath = value.replace("\\", "/")
                    sb.appendLine("$imageVar = tk.PhotoImage(file=\"$pythonPath\")")
                }
            }
        }
        if (imageVarMap.isNotEmpty()) {
            sb.appendLine()
        }

        // Recursive function to generate widget code
        fun generateWidgetCodeRecursive(widget: DesignedWidget, parentPyVarName: String) {
            val currentWidgetPyVarName = widgetVarNames[widget.id] ?: return

            val constructorArgs = mutableListOf<String>()
            widget.properties.forEach { (key, value) ->
                // Skip properties handled by .place() or internal ones like 'name'
                if (key == "name" || key == "x" || key == "y" || key == "width" || key == "height") {
                    return@forEach
                }

                val formattedValue = if ((key == "image" || key == "file") && value is String && value.isNotBlank()) {
                    imageVarMap["${widget.id}_$key"] ?: "\"INVALID_IMAGE_REF\""
                } else {
                    when (value) {
                        is String -> "\"${value.replace("\"", "\\\"")}\""
                        is Boolean -> if (value) "True" else "False"
                        is Int -> value.toString()
                        is Color -> "\"#%02x%02x%02x\"".format(value.red, value.green, value.blue)
                        else -> "\"${value.toString().replace("\"", "\\\"")}\""
                    }
                }
                constructorArgs.add("$key=$formattedValue")
            }
            val argsString = constructorArgs.joinToString(", ")
            val tkClass = widget.type

            sb.appendLine("$currentWidgetPyVarName = $tkClass($parentPyVarName${if (argsString.isNotEmpty()) ", " else ""}$argsString)")
            sb.appendLine("$currentWidgetPyVarName.place(x=${widget.x}, y=${widget.y}, width=${widget.width}, height=${widget.height})")
            sb.appendLine()

            dialog.widgets.filter { it.parentId == widget.id }.forEach { child ->
                generateWidgetCodeRecursive(child, currentWidgetPyVarName)
            }
        }

        dialog.widgets.filter { it.parentId == null }.forEach { topLevelWidget ->
            generateWidgetCodeRecursive(topLevelWidget, "root")
        }

        sb.appendLine("root.mainloop()")
        return sb.toString()
    }
}
