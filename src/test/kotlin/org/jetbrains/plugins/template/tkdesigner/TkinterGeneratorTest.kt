package org.jetbrains.plugins.template.tkdesigner

import org.jetbrains.plugins.template.tkdesigner.model.DialogModel
import org.jetbrains.plugins.template.tkdesigner.model.WidgetModel
import org.junit.Test
import org.junit.Assert.assertTrue

class TkinterGeneratorTest {
    @Test
    fun simpleButton() {
        val model = DialogModel(width = 200, height = 100)
        model.widgets.add(WidgetModel("Button", 10, 20, 80, 30))
        val code = TkinterGenerator.generate(model)
        assertTrue(code.contains("tk.Button"))
        assertTrue(code.contains("place(x=10"))
    }

    @Test
    fun nestedFrameGrid() {
        val model = DialogModel(width = 300, height = 200)
        val frame = WidgetModel("Frame", 0, 0, 100, 50, layout = "grid")
        val button = WidgetModel("Button", 0, 0, 50, 20)
        frame.children.add(button); button.parent = frame
        model.widgets.add(frame)
        val code = TkinterGenerator.generate(model)
        assertTrue(code.contains("tk.Frame"))
        assertTrue(code.contains("grid("))
    }
}
