package org.jetbrains.plugins.template.tkdesigner

import org.jetbrains.plugins.template.tkdesigner.model.DialogModel
import org.jetbrains.plugins.template.tkdesigner.model.WidgetModel

/** Utility functions for creating starter dialog templates. */
object DialogTemplates {
    fun blank(): DialogModel = DialogModel()

    fun simpleForm(): DialogModel {
        val dlg = DialogModel(width = 300, height = 200)
        val label = WidgetModel("Label", 10, 10, 80, 20).apply { properties["text"] = "Name:" }
        val entry = WidgetModel("Entry", 100, 10, 150, 20)
        dlg.widgets.add(label)
        dlg.widgets.add(entry)
        return dlg
    }

    fun menuDialog(): DialogModel {
        val dlg = DialogModel(width = 400, height = 300)
        val menu = WidgetModel("Menu", 0, 0, 400, 20)
        dlg.widgets.add(menu)
        return dlg
    }
}
