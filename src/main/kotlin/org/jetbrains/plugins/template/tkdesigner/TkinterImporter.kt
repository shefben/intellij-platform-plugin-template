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
        val placeRegex = Regex("""(\w+)\.place\(""")
        val packRegex = Regex("""(\w+)\.pack\((.*?)\)""")
        val gridRegex = Regex("""(\w+)\.grid\((.*?)\)""")
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
                when {
                    placeRegex.containsMatchIn(line) -> {
                        val x = Regex("""x=(\d+)""").find(line)?.groupValues?.get(1)?.toInt() ?: 0
                        val y = Regex("""y=(\d+)""").find(line)?.groupValues?.get(1)?.toInt() ?: 0
                        val width = Regex("""width=(\d+)""").find(line)?.groupValues?.get(1)?.toInt() ?: 80
                        val height = Regex("""height=(\d+)""").find(line)?.groupValues?.get(1)?.toInt() ?: 30
                        widgetMap[varName]?.apply {
                            this.x = x
                            this.y = y
                            this.width = width
                            this.height = height
                            this.layout = "place"
                            model.widgets.add(this)
                        }
                    }
                    packRegex.containsMatchIn(line) -> {
                        val m = packRegex.find(line)!!
                        widgetMap[varName]?.apply {
                            this.layout = "pack"
                            m.groupValues[2].split(',').map { it.trim() }.forEach { arg ->
                                val parts = arg.split('=')
                                if (parts.size == 2) properties[parts[0]] = parts[1]
                            }
                            model.widgets.add(this)
                        }
                    }
                    gridRegex.containsMatchIn(line) -> {
                        val m = gridRegex.find(line)!!
                        widgetMap[varName]?.apply {
                            this.layout = "grid"
                            m.groupValues[2].split(',').map { it.trim() }.forEach { arg ->
                                val parts = arg.split('=')
                                if (parts.size == 2) properties[parts[0]] = parts[1]
                            }
                            model.widgets.add(this)
                        }
                    }
                    else -> warnings.add("Line ${index+1}: could not parse placement for $varName")
                }
            }
        }
        lastWarnings = warnings
        return model
    }
}
