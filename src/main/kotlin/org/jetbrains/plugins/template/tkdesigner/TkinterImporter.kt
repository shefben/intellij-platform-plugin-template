package org.jetbrains.plugins.template.tkdesigner

import org.jetbrains.plugins.template.tkdesigner.model.DialogModel
import org.jetbrains.plugins.template.tkdesigner.model.WidgetModel

/** Simple importer that parses basic Tkinter scripts with .place geometry. */
object TkinterImporter {
    var lastWarnings: List<String> = emptyList()

    fun importScript(text: String): DialogModel {
        val warnings = mutableListOf<String>()
        val model = DialogModel()
        val widgetMap = mutableMapOf<String, WidgetModel>()
        val createRegex = Regex("""(\w+)\s*=\s*tk\.(\w+)\(""")
        val placeRegex = Regex("""(\w+)\.place\(.*?x=(\d+).*?y=(\d+).*?(width=(\d+))?.*?(height=(\d+))?.*?\)""")
        val geometryRegex = Regex("""root.geometry\("(\d+)x(\d+)"\)""")
        geometryRegex.find(text)?.let { m ->
            model.width = m.groupValues[1].toInt()
            model.height = m.groupValues[2].toInt()
        }
        text.lines().forEachIndexed { index, line ->
            val createMatch = createRegex.find(line)
            if (createMatch != null) {
                val varName = createMatch.groupValues[1]
                val type = createMatch.groupValues[2]
                widgetMap[varName] = WidgetModel(type, 0, 0, 80, 30)
                val placeMatch = placeRegex.find(line)
                if (placeMatch != null) {
                    val x = placeMatch.groupValues[2].toInt()
                    val y = placeMatch.groupValues[3].toInt()
                    val width = placeMatch.groupValues.getOrNull(5)?.toIntOrNull() ?: 80
                    val height = placeMatch.groupValues.getOrNull(7)?.toIntOrNull() ?: 30
                    widgetMap[varName]?.apply {
                        this.x = x
                        this.y = y
                        this.width = width
                        this.height = height
                        model.widgets.add(this)
                    }
                } else {
                    warnings.add("Line ${index+1}: could not parse placement for $varName")
                }
            }
        }
        lastWarnings = warnings
        return model
    }
}
