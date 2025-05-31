package com.jules.tkinterdesigner.codegen

import com.jules.tkinterdesigner.model.DesignedDialog
import com.jules.tkinterdesigner.model.DesignedWidget
import com.jules.tkinterdesigner.model.WidgetPropertyRegistry
import java.awt.Color
import java.io.File // For path absoluteness check

object TkinterCodeGenerator {

    private fun sanitizePythonFunctionName(name: String): String {
        var sanitized = name.trim()
        if (sanitized.isEmpty()) return "_command"
        sanitized = sanitized.replace(Regex("[^a-zA-Z0-9_]"), "_")
        if (sanitized.firstOrNull()?.isDigit() == true) {
            sanitized = "_$sanitized"
        }
        val keywords = setOf("False", "None", "True", "and", "as", "assert", "async", "await", "break", "class", "continue", "def", "del", "elif", "else", "except", "finally", "for", "from", "global", "if", "import", "in", "is", "lambda", "nonlocal", "not", "or", "pass", "raise", "return", "try", "while", "with", "yield")
        if (sanitized in keywords) {
            sanitized += "_"
        }
        return sanitized.ifEmpty { "_command" }
    }

    fun generateCode(dialog: DesignedDialog): String {
        val headerSb = StringBuilder() // For imports and initial setup that must be at the top
        val bodySb = StringBuilder()   // For PhotoImage, TkVar, command functions, and then widget code

        val necessaryImports = mutableSetOf("import tkinter as tk", "from tkinter import ttk")
        var needsOsImportForPaths = false

        val imageVarMap = mutableMapOf<String, String>()
        val widgetVarNames = mutableMapOf<String, String>()
        val tkVariableDeclarations = mutableSetOf<String>()
        val commandHandlerFunctions = mutableSetOf<String>()
        val commandAssignments = mutableMapOf<String, String>()

        for (widget in dialog.widgets) {
            val pyVarNameBase = sanitizePythonFunctionName(
                widget.properties["name"]?.toString()?.ifBlank { null } ?: widget.id
            )
            widgetVarNames[widget.id] = pyVarNameBase

            widget.properties.forEach { (propKey, propValue) ->
                val propDef = WidgetPropertyRegistry.propertiesForType[widget.type]?.find { it.name == propKey }

                if ((propKey == "image" || propKey == "file") && propValue is String && propValue.isNotBlank()) {
                    val imageVar = "${pyVarNameBase}_${propKey}_img"
                    imageVarMap["${widget.id}_$propKey"] = imageVar
                    var pythonPath = propValue.replace("\\", "/")

                    val isPathAbsolute = File(pythonPath).isAbsolute || pythonPath.startsWith("/") // Basic check
                    if (!isPathAbsolute) {
                        needsOsImportForPaths = true
                        pythonPath = "os.path.join(_script_dir, \"$pythonPath\")"
                        bodySb.appendLine("$imageVar = tk.PhotoImage(file=$pythonPath)")
                    } else {
                        bodySb.appendLine("$imageVar = tk.PhotoImage(file=\"$pythonPath\")")
                    }
                }
                else if (propDef?.tkVariableTypeOptions != null && propValue is String && propValue.isNotBlank()) {
                    val varName = sanitizePythonFunctionName(propValue)
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
                    widget.properties[propKey] = varName
                }
                else if (propDef?.isCommandCallback == true && propValue is String && propValue.isNotBlank()) {
                    val commandFuncName = sanitizePythonFunctionName(propValue)
                    commandAssignments[widget.id] = commandFuncName
                    val stub = """
                        |def ${commandFuncName}():
                        |    print("${commandFuncName} called")
                        |    pass
                        """.trimMargin()
                    commandHandlerFunctions.add(stub)
                }
            }
        }

        if (needsOsImportForPaths) {
            necessaryImports.add("import os")
            necessaryImports.add("_script_dir = os.path.dirname(os.path.abspath(__file__))")
        }
        necessaryImports.forEach { headerSb.appendLine(it) }
        headerSb.appendLine() // Newline after imports

        if (imageVarMap.isNotEmpty()) { bodySb.appendLine() }
        if (tkVariableDeclarations.isNotEmpty()) { tkVariableDeclarations.forEach { bodySb.appendLine(it) }; bodySb.appendLine() }
        if (commandHandlerFunctions.isNotEmpty()) { commandHandlerFunctions.forEach { bodySb.appendLine(it); bodySb.appendLine() } }

        bodySb.appendLine("root = tk.Tk()")
        bodySb.appendLine("root.title(\"${dialog.title.replace("\"", "\\\"")}\")")
        bodySb.appendLine("root.geometry(\"${dialog.width}x${dialog.height}\")")
        bodySb.appendLine()

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
                    imageVarMap["${widget.id}_$propKey"] // This is now the variable name, no quotes
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
                if (formattedValue != null) {
                     constructorArgs.add("$propKey=$formattedValue")
                }
            }
            val argsString = constructorArgs.joinToString(", ")
            val tkClass = widget.type
            bodySb.appendLine("$currentWidgetPyVarName = $tkClass($parentPyVarName${if (argsString.isNotEmpty()) ", " else ""}$argsString)")
            bodySb.appendLine("$currentWidgetPyVarName.place(x=${widget.x}, y=${widget.y}, width=${widget.width}, height=${widget.height})")
            bodySb.appendLine()
            dialog.widgets.filter { it.parentId == widget.id }.forEach { child ->
                generateWidgetCodeRecursive(child, currentWidgetPyVarName)
            }
        }

        dialog.widgets.filter { it.parentId == null }.forEach { topLevelWidget ->
            generateWidgetCodeRecursive(topLevelWidget, "root")
        }

        bodySb.appendLine("root.mainloop()")
        return headerSb.toString() + bodySb.toString()
    }
}
