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
            val name = "${w.type.lowercase()}_${index}"
            val textExpr = w.properties["textKey"]?.let { "tr(\"$it\")" } ?: "\"${w.properties["text"] ?: ""}\""
            when (w.type) {
                "Button" -> builder.appendLine("$name = tk.Button($parent, text=$textExpr)")
                "Label" -> builder.appendLine("$name = tk.Label($parent, text=$textExpr)")
                "Entry" -> builder.appendLine("$name = tk.Entry($parent)")
                "Text" -> builder.appendLine("$name = tk.Text($parent)")
                "Frame" -> builder.appendLine("$name = tk.Frame($parent)")
                "Canvas" -> builder.appendLine("$name = tk.Canvas($parent)")
                "Menu" -> builder.appendLine("$name = tk.Menu($parent)")
                "Menubutton" -> builder.appendLine("$name = tk.Menubutton($parent, text=$textExpr)")
                "PanedWindow" -> builder.appendLine("$name = tk.PanedWindow($parent)")
                "Scrollbar" -> builder.appendLine("$name = tk.Scrollbar($parent)")
                "Checkbutton" -> builder.appendLine("$name = tk.Checkbutton($parent, text=$textExpr)")
                "Radiobutton" -> builder.appendLine("$name = tk.Radiobutton($parent, text=$textExpr)")
                "Listbox" -> builder.appendLine("$name = tk.Listbox($parent)")
                "Scale" -> builder.appendLine("$name = tk.Scale($parent, from_=0, to=100)")
                "Spinbox" -> builder.appendLine("$name = tk.Spinbox($parent, from_=0, to=10)")
            }
            w.properties["image"]?.let {
                builder.appendLine("${name}_img = tk.PhotoImage(file=r'${it}')")
                builder.appendLine("$name.configure(image=${name}_img)")
            }
            when ((w.parent?.layout ?: rootManager)) {
                "pack" -> builder.appendLine("$name.pack()")
                "grid" -> builder.appendLine("$name.grid(row=${w.properties["row"] ?: 0}, column=${w.properties["column"] ?: 0})")
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
