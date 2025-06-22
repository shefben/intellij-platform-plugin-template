package org.jetbrains.plugins.template.tkdesigner

import org.jetbrains.plugins.template.tkdesigner.model.DialogModel
import org.jetbrains.plugins.template.tkdesigner.model.WidgetModel
import org.junit.Test
import org.junit.Assert.assertTrue

class GeneratorEventsTest {
    @Test
    fun eventBinding() {
        val model = DialogModel()
        val btn = WidgetModel("Button", 0, 0, 50, 20)
        btn.events["<Button-1>"] = "on_click"
        model.widgets.add(btn)
        val code = TkinterGenerator.generate(model)
        assertTrue(code.contains("bind(\"<Button-1>\""))
    }
}
