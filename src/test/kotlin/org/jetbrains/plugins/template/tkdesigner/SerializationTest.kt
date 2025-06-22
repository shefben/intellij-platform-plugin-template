package org.jetbrains.plugins.template.tkdesigner

import org.jetbrains.plugins.template.tkdesigner.model.DesignProject
import org.jetbrains.plugins.template.tkdesigner.model.DialogModel
import org.jetbrains.plugins.template.tkdesigner.model.WidgetModel
import org.junit.Assert.*
import org.junit.Test

class SerializationTest {
    @Test
    fun projectRoundTrip() {
        val project = DesignProject(
            dialogs = mutableListOf(DialogModel(width = 300, height = 200)),
            pythonInterpreter = "python3"
        )
        project.dialogs[0].widgets.add(WidgetModel("Button", 1,2,30,20))
        val json = project.toJson()
        val restored = DesignProject.fromJson(json)
        assertEquals(project.dialogs.size, restored.dialogs.size)
        val btn = restored.dialogs[0].widgets[0]
        assertEquals("Button", btn.type)
        assertEquals(1, btn.x)
        assertEquals(200, restored.dialogs[0].height)
        // check interpreter preserved
        assertEquals("python3", restored.pythonInterpreter)
    }
}
