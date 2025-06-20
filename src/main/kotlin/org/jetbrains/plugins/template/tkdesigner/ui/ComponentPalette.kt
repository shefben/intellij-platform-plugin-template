package org.jetbrains.plugins.template.tkdesigner.ui

import java.awt.FlowLayout
import javax.swing.JButton
import javax.swing.JDialog

/**
 * Floating palette with Tkinter widgets.
 */
class ComponentPalette(private val design: DesignAreaPanel) : JDialog() {
    init {
        title = "Widgets"
        layout = FlowLayout()
        isAlwaysOnTop = true
        isResizable = false

        val types = listOf(
            "Button",
            "Label",
            "Entry",
            "Text",
            "Frame",
            "Canvas",
            "Menu",
            "Menubutton",
            "PanedWindow",
            "Scrollbar",
            "Checkbutton",
            "Radiobutton",
            "Listbox",
            "Scale",
            "Spinbox"
        )
        for (t in types) {
            add(createButton(t))
        }

        pack()
    }

    private fun createButton(type: String): JButton = JButton(type).apply {
        addActionListener { design.beginAddWidget(type) }
    }
}
