package org.jetbrains.plugins.template.tkdesigner.ui

import java.awt.GridLayout
import javax.swing.JButton
import javax.swing.JDialog


/**
 * Floating palette with Tkinter widgets.
 */
class ComponentPalette(private val design: DesignAreaPanel) : JDialog() {
    init {
        title = "Widgets"
        // Use four columns so the palette stays compact
        layout = GridLayout(0, 4, 2, 2)
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

    private fun createButton(type: String): JButton {
        return JButton(type).apply {
            toolTipText = type
            addActionListener { design.beginAddWidget(type) }
        }
    }
}
