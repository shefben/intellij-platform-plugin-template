package org.jetbrains.plugins.template.tkdesigner.ui

import com.intellij.icons.AllIcons
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
        val icon = iconFor(type)
        return JButton(icon).apply {
            toolTipText = type
            addActionListener { design.beginAddWidget(type) }
        }
    }

    private fun iconFor(type: String) = when (type) {
        "Button" -> AllIcons.General.Add
        "Label" -> AllIcons.General.Information
        "Entry" -> AllIcons.Actions.Edit
        "Text" -> AllIcons.FileTypes.Text
        "Frame" -> AllIcons.Nodes.Package
        "Canvas" -> AllIcons.Nodes.Artifact
        "Menu" -> AllIcons.Actions.MenuOpen
        "Menubutton" -> AllIcons.Actions.MenuOpen
        "PanedWindow" -> AllIcons.Actions.SplitVertically
        "Scrollbar" -> AllIcons.Actions.MoveDown
        "Checkbutton" -> AllIcons.Actions.Checked
        "Radiobutton" -> AllIcons.Actions.Checked_selected
        "Listbox" -> AllIcons.Actions.ShowAsTree
        "Scale" -> AllIcons.General.ArrowDown
        "Spinbox" -> AllIcons.General.ArrowRight
        else -> AllIcons.General.Add
    }
}
