package org.jetbrains.plugins.template.tkdesigner.ui

import com.intellij.icons.AllIcons
import java.awt.GridLayout
import javax.swing.JButton
import javax.swing.JDialog
import javax.swing.JPanel
import javax.swing.JTabbedPane
import java.awt.BorderLayout


/**
 * Floating palette with Tkinter widgets.
 */
class ComponentPalette(private val design: DesignAreaPanel) : JDialog() {
    init {
        title = "Widgets"
        isAlwaysOnTop = true
        isResizable = false
        layout = BorderLayout()

        val cols = org.jetbrains.plugins.template.tkdesigner.DesignerSettings.instance().state.paletteColumns
        fun panel(types: List<String>): JPanel = JPanel(GridLayout(0, cols, 2, 2)).apply {
            for (t in types) add(createButton(t))
        }

        val tabs = JTabbedPane()
        tabs.addTab("Inputs", panel(listOf("Button","Label","Entry","Text","Checkbutton","Radiobutton","Listbox","Scale","Spinbox","ttk.Combobox")))
        tabs.addTab("Containers", panel(listOf("Frame","Canvas","PanedWindow","Toplevel","ttk.Treeview")))
        tabs.addTab("Menus", panel(listOf("Menu","Menubutton","Scrollbar")))

        val customList = org.jetbrains.plugins.template.tkdesigner.DesignerSettings.instance().state.extraWidgets
        val customPanel = panel(customList)
        val customize = JButton("+").apply {
            toolTipText = "Add widget type"
            addActionListener {
                val name = javax.swing.JOptionPane.showInputDialog(this, "Widget class", "")
                if (!name.isNullOrBlank()) {
                    customList.add(name)
                    customPanel.add(createButton(name))
                    customPanel.revalidate()
                    customPanel.repaint()
                }
            }
        }
        customPanel.add(customize)
        tabs.addTab("Custom", customPanel)

        add(tabs, BorderLayout.CENTER)
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
        "ttk.Combobox" -> AllIcons.Actions.ListFiles
        "ttk.Treeview" -> AllIcons.General.ProjectStructure
        "Toplevel" -> AllIcons.Actions.New
        else -> AllIcons.General.Add
    }
}
