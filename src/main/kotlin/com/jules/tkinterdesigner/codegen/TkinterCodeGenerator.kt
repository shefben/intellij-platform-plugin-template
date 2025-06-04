package com.jules.tkinterdesigner.codegen

import com.jules.tkinterdesigner.model.DesignedDialog
import com.jules.tkinterdesigner.model.DesignedWidget
import com.jules.tkinterdesigner.model.WidgetPropertyRegistry
import java.awt.Color
import java.io.File

object TkinterCodeGenerator {

    private fun sanitizePythonFunctionName(name: String): String { /* ... (as before) ... */ }

    fun generateCode(dialog: DesignedDialog): String {
        val headerSb = StringBuilder()
        val bodySb = StringBuilder()
        val postCreationCommands = mutableMapOf<String, MutableList<String>>()

        val necessaryImports = mutableSetOf("import tkinter as tk", "from tkinter import ttk")
        var needsOsImportForPaths = false

        val imageVarMap = mutableMapOf<String, String>()
        val widgetVarNames = mutableMapOf<String, String>()
        val tkVariableDeclarations = mutableSetOf<String>()
        val commandHandlerFunctions = mutableSetOf<String>()
        val commandAssignments = mutableMapOf<String, String>()

        // Pass 1: Collect names, define PhotoImages, Tkinter variables, and command stubs
        for (widget in dialog.widgets) {
            // ... (pyVarNameBase, widgetVarNames population as before) ...
            val pyVarNameBase = sanitizePythonFunctionName(widget.properties["name"]?.toString()?.ifBlank { null } ?: widget.id); widgetVarNames[widget.id] = pyVarNameBase
            widget.properties.forEach { (propKey, propValue) ->
                val propDef = WidgetPropertyRegistry.propertiesForType[widget.type]?.find { it.name == propKey }
                // ... (PhotoImage, Tkinter variable, Command handling as before) ...
            }
        }

        // ... (Import handling, PhotoImage, TkVar, Command function definitions to bodySb as before) ...

        bodySb.appendLine("root = tk.Tk()")
        // ... (root title, geometry as before) ...

        fun generateWidgetCodeRecursive(widget: DesignedWidget, parentPyVarName: String) {
            val currentWidgetPyVarName = widgetVarNames[widget.id] ?: return
            val constructorArgs = mutableListOf<String>()
            val parentWidget = dialog.widgets.find { it.id == widget.parentId }


            widget.properties.forEach { (propKey, propValue) ->
                // Skip internal properties and also "tabs", "activeTabIndex", "panes", "pane_options" for specific containers
                if (propKey == "name" || propKey == "x" || propKey == "y" || propKey == "width" || propKey == "height" ||
                    propKey.endsWith("_vartype") || propKey.endsWith("_value") ||
                    (widget.type == "ttk.Notebook" && (propKey == "tabs" || propKey == "activeTabIndex")) ||
                    ((widget.type == "tk.PanedWindow" || widget.type == "ttk.PanedWindow") && (propKey == "panes" || propKey == "pane_options"))
                    ) {
                    return@forEach
                }
                // ... (formattedValue logic as before for image, command, tkvariable, color, string, bool, int) ...
                 val propDef = WidgetPropertyRegistry.propertiesForType[widget.type]?.find { it.name == propKey }
                 val formattedValue = if ((propKey == "image" || propKey == "file") && propValue is String && propValue.isNotBlank()) { /* ... */ }
                                     else if (propDef?.isCommandCallback == true && propValue is String && propValue.isNotBlank()) { /* ... */ }
                                     else if (propDef?.tkVariableTypeOptions != null && propValue is String && propValue.isNotBlank()) { /* ... */ }
                                     else { /* ... (when (propValue) ...) ... */ }
                 if (formattedValue != null) { constructorArgs.add("$propKey=$formattedValue") }
            }
            val argsString = constructorArgs.joinToString(", ")
            val tkClass = widget.type
            bodySb.appendLine("$currentWidgetPyVarName = $tkClass($parentPyVarName${if (argsString.isNotEmpty()) ", " else ""}$argsString)")

            if (widget.type == "ttk.Notebook") { /* ... notebook.add logic using postCreationCommands ... */ }

            // PanedWindow .add() calls are different, they happen after the child is created.
            // The child itself doesn't need .place() if it's a pane.
            if (parentWidget?.type !in listOf("tk.PanedWindow", "ttk.PanedWindow", "ttk.Notebook")) {
                 bodySb.appendLine("$currentWidgetPyVarName.place(x=${widget.x}, y=${widget.y}, width=${widget.width}, height=${widget.height})")
            }
            bodySb.appendLine()

            dialog.widgets.filter { it.parentId == widget.id }.forEach { child ->
                generateWidgetCodeRecursive(child, currentWidgetPyVarName)
            }

            postCreationCommands[widget.id]?.forEach { command -> bodySb.appendLine(command) }
            if (postCreationCommands.containsKey(widget.id)) bodySb.appendLine()

            // If the PARENT was a PanedWindow, this widget (child) needs to be .add()ed to it.
            if (parentWidget != null && (parentWidget.type == "tk.PanedWindow" || parentWidget.type == "ttk.PanedWindow")) {
                val parentPWPyVarName = widgetVarNames[parentWidget.id]
                val paneOptionsMap = parentWidget.properties["pane_options"] as? Map<String, Map<String, String>>
                val thisPaneOptions = paneOptionsMap?.get(widget.id) ?: emptyMap()
                val optionsStr = thisPaneOptions.map { (k, v) ->
                    // Attempt to convert to Int if it's a known int option like 'weight'
                    if (k == "weight") "$k=${v.toIntOrNull() ?: 1}" else "$k=\"${v.replace("\"", "\\\"")}\""
                }.joinToString(", ")

                bodySb.appendLine("$parentPWPyVarName.add($currentWidgetPyVarName${if (optionsStr.isNotBlank()) ", " else ""}$optionsStr)")
                // Add a newline after .add for PanedWindow child
                bodySb.appendLine()
            }
        }

        dialog.widgets.filter { it.parentId == null }.forEach { topLevelWidget ->
            generateWidgetCodeRecursive(topLevelWidget, "root")
        }

        bodySb.appendLine("root.mainloop()")
        return headerSb.toString() + bodySb.toString()
    }
    // sanitizePythonFunctionName as before
    // Ensure all stubs (imports, photoimage, tkvar, command func defs) are correctly placed into headerSb or bodySb
}
