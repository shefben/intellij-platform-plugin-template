package org.jetbrains.plugins.template.tkdesigner

import org.jetbrains.plugins.template.tkdesigner.model.DialogModel
import org.jetbrains.plugins.template.tkdesigner.model.WidgetModel

/** Simple importer that parses basic Tkinter scripts with .place geometry. */
object TkinterImporter {
    fun importScript(text: String): DialogModel {
        val model = DialogModel()
        val widgetMap = mutableMapOf<String, WidgetModel>()
        val createRegex = Regex("""(\w+)\s*=\s*tk\.(\w+)\(""")
        val placeRegex = Regex("""(\w+)\.place\(.*?x=(\d+).*?y=(\d+).*?(width=(\d+))?.*?(height=(\d+))?.*?\)""")
        val geometryRegex = Regex("""root.geometry\("(\d+)x(\d+)"\)""")
        geometryRegex.find(text)?.let { m ->
            model.width = m.groupValues[1].toInt()
            model.height = m.groupValues[2].toInt()
        }
        text.lines().forEach { line ->
            createRegex.find(line)?.let { m ->
                val varName = m.groupValues[1]
                val type = m.groupValues[2]
                widgetMap[varName] = WidgetModel(type, 0, 0, 80, 30)
            }
            placeRegex.find(line)?.let { m ->
                val varName = m.groupValues[1]
                val x = m.groupValues[2].toInt()
                val y = m.groupValues[3].toInt()
                val width = m.groupValues.getOrNull(5)?.toIntOrNull() ?: 80
                val height = m.groupValues.getOrNull(7)?.toIntOrNull() ?: 30
                widgetMap[varName]?.apply {
                    this.x = x
                    this.y = y
                    this.width = width
                    this.height = height
                    model.widgets.add(this)
                }
            }
        }
        return model
    }
}
