package com.jules.tkinterdesigner.codegen

import com.jules.tkinterdesigner.model.DesignedDialog
import com.jules.tkinterdesigner.model.DesignedWidget
import com.jules.tkinterdesigner.model.WidgetPropertyRegistry // Moved to top
import java.awt.Color

object TkinterCodeGenerator {

    private fun sanitizePythonFunctionName(name: String): String {
        var sanitized = name.trim()
        if (sanitized.isEmpty()) return "_command" // Default if empty after trim

        // Replace spaces and invalid characters (not suitable for Python identifiers) with underscores
        sanitized = sanitized.replace(Regex("[^a-zA-Z0-9_]"), "_")

        // Ensure it doesn't start with a number
        if (sanitized.firstOrNull()?.isDigit() == true) {
            sanitized = "_$sanitized"
        }

        // Ensure it's not a Python keyword (basic list, can be expanded)
        // For a more comprehensive list, consider a library or a more extensive set.
        val keywords = setOf(
            "False", "None", "True", "and", "as", "assert", "async", "await", "break", "class", "continue",
            "def", "del", "elif", "else", "except", "finally", "for", "from", "global", "if", "import",
            "in", "is", "lambda", "nonlocal", "not", "or", "pass", "raise", "return", "try", "while",
            "with", "yield"
        )
        if (sanitized in keywords) {
            sanitized += "_"
        }
        return sanitized.ifEmpty { "_command" } // Final check if sanitization resulted in empty
    }

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
        val tkVariableDeclarations = mutableSetOf<String>()
        val commandHandlerFunctions = mutableSetOf<String>() // For function stubs
        val commandAssignments = mutableMapOf<String, String>() // widget.id to python_function_name

        // Pass 1: Collect names, define PhotoImages, Tkinter variables, and command stubs
        for (widget in dialog.widgets) {
            val pyVarNameBase = sanitizePythonFunctionName(
                widget.properties["name"]?.toString()?.ifBlank { null } ?: widget.id
            )
            widgetVarNames[widget.id] = pyVarNameBase

            widget.properties.forEach { (propKey, propValue) ->
                val propDef = WidgetPropertyRegistry.propertiesForType[widget.type]?.find { it.name == propKey }

                // PhotoImage handling
                if ((propKey == "image" || propKey == "file") && propValue is String && propValue.isNotBlank()) {
                    val imageVar = "${pyVarNameBase}_${propKey}_img"
                    imageVarMap["${widget.id}_$propKey"] = imageVar
                    val pythonPath = propValue.replace("\\", "/")
                    sb.appendLine("$imageVar = tk.PhotoImage(file=\"$pythonPath\")")
                }

                // Tkinter variable handling
                else if (propDef?.tkVariableTypeOptions != null && propValue is String && propValue.isNotBlank()) {
                    val varName = sanitizePythonFunctionName(propValue) // Sanitize user-entered variable name
                    val varType = widget.properties["${propDef.name}_vartype"] as? String ?: propDef.tkVariableTypeOptions.first()
                    val rawInitialValue = widget.properties["${propDef.name}_value"]

                    var initialValueStr = ""
                    if (rawInitialValue != null && rawInitialValue.toString().isNotEmpty()) {
                        initialValueStr = when (varType) {
                            "StringVar" -> "\"${rawInitialValue.toString().replace("\"", "\\\"")}\""
                            "IntVar" -> (rawInitialValue as? Int ?: rawInitialValue.toString().toIntOrNull() ?: 0).toString()
                            "DoubleVar" -> (rawInitialValue as? Double ?: rawInitialValue.toString().toDoubleOrNull() ?: 0.0).toString()
                            "BooleanVar" -> if (rawInitialValue as? Boolean ?: rawInitialValue.toString().toBooleanStrictOrNull() ?: false) "True" else "False"
                            else -> "\"${rawInitialValue.toString().replace("\"", "\\\"")}\""
                        }
                        tkVariableDeclarations.add("$varName = tk.$varType(value=$initialValueStr)")
                    } else {
                         tkVariableDeclarations.add("$varName = tk.$varType()")
                    }
                     // Store the sanitized name back for use in widget constructor
                    widget.properties[propKey] = varName // Ensure widget property holds the sanitized name
                }
                // Command handling
                else if (propDef?.isCommandCallback == true && propValue is String && propValue.isNotBlank()) {
                    val commandFuncName = sanitizePythonFunctionName(propValue)
                    commandAssignments[widget.id] = commandFuncName // Store which function this widget's command maps to
                    val stub = """
                        |def ${commandFuncName}():
                        |    print("${commandFuncName} called")
                        |    pass
                        """.trimMargin()
                    commandHandlerFunctions.add(stub)
                }
            }
        }

        if (imageVarMap.isNotEmpty()) { sb.appendLine() }
        if (tkVariableDeclarations.isNotEmpty()) { tkVariableDeclarations.forEach { sb.appendLine(it) }; sb.appendLine() }
        if (commandHandlerFunctions.isNotEmpty()) { commandHandlerFunctions.forEach { sb.appendLine(it); sb.appendLine() } }

        // Pass 2: Recursive function to generate widget instantiation and placement
        fun generateWidgetCodeRecursive(widget: DesignedWidget, parentPyVarName: String) {
            val currentWidgetPyVarName = widgetVarNames[widget.id] ?: return

            val constructorArgs = mutableListOf<String>()
            widget.properties.forEach { (propKey, propValue) ->
                if (propKey == "name" || propKey == "x" || propKey == "y" || propKey == "width" || propKey == "height" ||
                    propKey.endsWith("_vartype") || propKey.endsWith("_value")) {
                    return@forEach
                }

                val propDef = WidgetPropertyRegistry.propertiesForType[widget.type]?.find { it.name == propKey }
                val formattedValue = if ((propKey == "image" || propKey == "file") && propValue is String && propValue.isNotBlank()) {
                    imageVarMap["${widget.id}_$propKey"] ?: "\"INVALID_IMAGE_REF\""
                } else if (propDef?.isCommandCallback == true && propValue is String && propValue.isNotBlank()) {
                    commandAssignments[widget.id] // Use the sanitized function name directly
                } else if (propDef?.tkVariableTypeOptions != null && propValue is String && propValue.isNotBlank()) {
                    sanitizePythonFunctionName(propValue) // Use the sanitized variable name
                } else {
                    when (propValue) {
                        is String -> "\"${propValue.replace("\"", "\\\"")}\""
                        is Boolean -> if (propValue) "True" else "False"
                        is Int -> propValue.toString()
                        is Color -> "\"#%02x%02x%02x\"".format(propValue.red, propValue.green, propValue.blue)
                        else -> "\"${propValue.toString().replace("\"", "\\\"")}\""
                    }
                }
                if (formattedValue != null) { // Ensure command/variable names are not null/empty before adding
                     constructorArgs.add("$propKey=$formattedValue")
                }
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
