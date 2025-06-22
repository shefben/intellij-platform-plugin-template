package org.jetbrains.plugins.template.tkdesigner

import org.jetbrains.plugins.template.tkdesigner.model.DialogModel
import org.jetbrains.plugins.template.tkdesigner.model.WidgetModel

/**
 * Generates Python Tkinter code from a [DialogModel].
 */
object TkinterGenerator {
    fun generate(model: DialogModel, translations: Map<String, Map<String, String>> = emptyMap()): String {
        val builder = StringBuilder()
        builder.appendLine("import tkinter as tk")
        if (translations.isNotEmpty()) {
            val json = com.google.gson.Gson().toJson(translations)
            builder.appendLine("translations = $json")
            builder.appendLine("lang = 'en'")
            builder.appendLine("def tr(key): return translations.get(lang, {}).get(key, key)")
        }
        builder.appendLine()
        builder.appendLine("root = tk.Tk()")
        builder.appendLine("root.geometry(\"${model.width}x${model.height}\")")
        val rootManager = model.layout
        fun renderWidget(w: WidgetModel, parent: String, index: Int) {
            val name = w.properties["name"] ?: "${w.type.lowercase()}_${index}"
            val textExpr = w.properties["textKey"]?.let { "tr(\"$it\")" } ?: "\"${w.properties["text"] ?: ""}\""

            val options = mutableListOf<String>()
            w.properties.forEach { (k, v) ->
                if (k in listOf("row", "column", "text", "textKey", "image", "name")) return@forEach
                val value = v.toIntOrNull() ?: v.toDoubleOrNull() ?: v
                val expr = if (value is Number) value.toString() else "\"$v\""
                options += "$k=$expr"
            }
            if (w.properties.containsKey("text") || w.properties.containsKey("textKey")) {
                options += "text=$textExpr"
            }

            val typeExpr = if (w.type.startsWith("ttk.")) "tkinter.ttk.${w.type.substringAfter('.') }" else "tk.${w.type}"
            builder.appendLine("$name = $typeExpr($parent${if (options.isNotEmpty()) ", " + options.joinToString(", ") else ""})")

            w.properties["image"]?.let {
                builder.appendLine("${name}_img = tk.PhotoImage(file=r'${it}')")
                builder.appendLine("$name.configure(image=${name}_img)")
            }

            val layout = w.layout.ifEmpty { w.parent?.layout ?: rootManager }
            when (layout) {
                "pack" -> {
                    val packOpts = listOf("side", "fill", "expand", "padx", "pady").mapNotNull { key ->
                        w.properties[key]?.let { "$key=${it}" }
                    }
                    builder.appendLine("$name.pack(${packOpts.joinToString(", ")})")
                }
                "grid" -> {
                    val row = w.properties["row"] ?: "0"
                    val col = w.properties["column"] ?: "0"
                    val sticky = w.properties["sticky"]?.let { ", sticky=\"$it\"" } ?: ""
                    builder.appendLine("$name.grid(row=$row, column=$col$sticky)")
                }
                else -> builder.appendLine("$name.place(x=${w.x}, y=${w.y}, width=${w.width}, height=${w.height})")
            }

            for ((event, cb) in w.events) {
                builder.appendLine("$name.bind(\"$event\", lambda e: $cb())")
            }
            w.children.forEachIndexed { i, child -> renderWidget(child, name, i) }
        }

        model.widgets.forEachIndexed { i, w -> renderWidget(w, "root", i) }
        builder.appendLine("root.mainloop()")
        return builder.toString()
    }
}
