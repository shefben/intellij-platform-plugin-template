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
        val postCreationCommands = mutableMapOf<String, MutableList<String>>() // widgetId -> list of commands

        val necessaryImports = mutableSetOf("import tkinter as tk", "from tkinter import ttk")
        var needsOsImportForPaths = false

        val imageVarMap = mutableMapOf<String, String>()
        val widgetVarNames = mutableMapOf<String, String>()
        val tkVariableDeclarations = mutableSetOf<String>()
        val commandHandlerFunctions = mutableSetOf<String>()
        val commandAssignments = mutableMapOf<String, String>()

        // Pass 1: Collect names, define PhotoImages, Tkinter variables, and command stubs
        for (widget in dialog.widgets) {
            val pyVarNameBase = sanitizePythonFunctionName(widget.properties["name"]?.toString()?.ifBlank { null } ?: widget.id)
            widgetVarNames[widget.id] = pyVarNameBase

            widget.properties.forEach { (propKey, propValue) ->
                val propDef = WidgetPropertyRegistry.propertiesForType[widget.type]?.find { it.name == propKey }
                // ... (PhotoImage, Tkinter variable, Command handling as before) ...
            }
        }

        if (needsOsImportForPaths) { /* ... add os imports ... */ }
        necessaryImports.forEach { headerSb.appendLine(it) }
        headerSb.appendLine()
        // ... (append PhotoImage, TkVar, Command function definitions to bodySb as before) ...

        bodySb.appendLine("root = tk.Tk()")
        bodySb.appendLine("root.title(\"${dialog.title.replace("\"", "\\\"")}\")")
        bodySb.appendLine("root.geometry(\"${dialog.width}x${dialog.height}\")")
        bodySb.appendLine()

        fun generateWidgetCodeRecursive(widget: DesignedWidget, parentPyVarName: String) {
            val currentWidgetPyVarName = widgetVarNames[widget.id] ?: return
            val constructorArgs = mutableListOf<String>()

            widget.properties.forEach { (propKey, propValue) ->
                // Skip internal properties and also "tabs", "activeTabIndex" for Notebook as they are handled specially
                if (propKey == "name" || propKey == "x" || propKey == "y" || propKey == "width" || propKey == "height" ||
                    propKey.endsWith("_vartype") || propKey.endsWith("_value") ||
                    (widget.type == "ttk.Notebook" && (propKey == "tabs" || propKey == "activeTabIndex"))) {
                    return@forEach
                }
                // ... (formattedValue logic as before for image, command, tkvariable, color, string, bool, int) ...
                val propDef = WidgetPropertyRegistry.propertiesForType[widget.type]?.find { it.name == propKey }
                val formattedValue = if ((propKey == "image" || propKey == "file") && propValue is String && propValue.isNotBlank()) {
                    imageVarMap["${widget.id}_$propKey"]
                } else if (propDef?.isCommandCallback == true && propValue is String && propValue.isNotBlank()) {
                    commandAssignments[widget.id]
                } else if (propDef?.tkVariableTypeOptions != null && propValue is String && propValue.isNotBlank()) {
                    sanitizePythonFunctionName(propValue)
                } else {
                    when (propValue) {
                        is String -> "\"${propValue.replace("\"", "\\\"")}\""
                        is Boolean -> if (propValue) "True" else "False"
                        is Int -> propValue.toString()
                        is Color -> "\"#%02x%02x%02x\"".format(propValue.red, propValue.green, propValue.blue)
                        else -> "\"${propValue.toString().replace("\"", "\\\"")}\""
                    }
                }
                if (formattedValue != null) { constructorArgs.add("$propKey=$formattedValue") }
            }
            val argsString = constructorArgs.joinToString(", ")
            val tkClass = widget.type
            bodySb.appendLine("$currentWidgetPyVarName = $tkClass($parentPyVarName${if (argsString.isNotEmpty()) ", " else ""}$argsString)")

            // For Notebook, prepare .add() commands to be run after all children are defined
            if (widget.type == "ttk.Notebook") {
                val notebookTabs = widget.properties["tabs"] as? List<Map<String, String>> ?: emptyList()
                notebookTabs.forEach { tabData ->
                    val frameId = tabData["frameId"]
                    val framePyVarName = widgetVarNames[frameId] // Get Python var name of the frame
                    val tabText = tabData["text"]?.replace("\"", "\\\"") ?: "Tab"
                    if (framePyVarName != null) {
                        postCreationCommands.getOrPut(widget.id) { mutableListOf() }
                            .add("$currentWidgetPyVarName.add($framePyVarName, text=\"$tabText\")")
                    }
                }
            }

            // Standard placement, unless child of Notebook (where .add handles it)
            // For now, assume all direct children of Notebook are Frames for tabs and are added, not placed.
            // Other containers might use .place for children.
            if (dialog.widgets.find { it.id == widget.parentId }?.type != "ttk.Notebook") {
                 bodySb.appendLine("$currentWidgetPyVarName.place(x=${widget.x}, y=${widget.y}, width=${widget.width}, height=${widget.height})")
            }
            bodySb.appendLine()

            dialog.widgets.filter { it.parentId == widget.id }.forEach { child ->
                generateWidgetCodeRecursive(child, currentWidgetPyVarName)
            }

            // Append any post-creation commands for this widget (e.g., notebook.add)
            postCreationCommands[widget.id]?.forEach { command ->
                bodySb.appendLine(command)
            }
            if (postCreationCommands.containsKey(widget.id)) bodySb.appendLine()

        }

        dialog.widgets.filter { it.parentId == null }.forEach { topLevelWidget ->
            generateWidgetCodeRecursive(topLevelWidget, "root")
        }

        bodySb.appendLine("root.mainloop()")
        return headerSb.toString() + bodySb.toString()
    }
     // sanitizePythonFunctionName as before
}
