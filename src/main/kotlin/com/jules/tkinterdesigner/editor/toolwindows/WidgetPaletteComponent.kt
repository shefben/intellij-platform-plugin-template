package com.jules.tkinterdesigner.editor.toolwindows

import com.intellij.openapi.project.Project
import com.intellij.ui.components.JBPanel
import com.jules.tkinterdesigner.model.WidgetPaletteInfo
import java.awt.datatransfer.StringSelection
import java.awt.dnd.DnDConstants
import java.awt.dnd.DragGestureEvent
import java.awt.dnd.DragGestureListener
import java.awt.dnd.DragSource
import javax.swing.BoxLayout
import javax.swing.JButton
import com.intellij.icons.AllIcons
import javax.swing.Icon
import java.awt.Dimension
import java.awt.Component

class WidgetPaletteComponent(project: Project) : JBPanel<WidgetPaletteComponent>() {

    private fun getIconForWidgetType(widgetType: String): Icon {
        return when (widgetType) {
            // ttk Widgets
            "ttk.Button", "Button" -> AllIcons.Actions.Execute
            "ttk.Label", "Label" -> AllIcons.Nodes.Static
            "ttk.Entry", "Entry" -> AllIcons.Components.TextField
            "ttk.Frame", "Frame" -> AllIcons.General.LayoutEditor
            "ttk.Checkbutton" -> AllIcons.Actions.Checked
            "ttk.Combobox" -> AllIcons.Components.ComboBox // Assuming this might be added
            "ttk.Notebook" -> AllIcons.Actions.TabbedPane
            "ttk.Progressbar" -> AllIcons.Process.ProgressResume
            "ttk.Radiobutton" -> AllIcons.General.RadioButtonOn // Assuming this might be added
            "ttk.Scale" -> AllIcons.Graph.Volumne // Assuming this might be added & icon name is correct
            "ttk.Scrollbar", "Scrollbar" -> AllIcons.Actions.SplitVertically
            "ttk.Separator" -> AllIcons.General.SplitGlueH
            "ttk.Sizegrip" -> AllIcons.General.ArrowDownRight // Assuming this might be added
            "ttk.Spinbox" -> AllIcons.Actions.ShowViewer // Assuming this might be added
            "ttk.Treeview" -> AllIcons.Nodes.Tree // Assuming this might be added

            // tk Widgets (classic)
            "Canvas" -> AllIcons.General.Editable
            "Text" -> AllIcons.FileTypes.Text
            "Listbox" -> AllIcons.Actions.ListFiles // Assuming this might be added

            else -> AllIcons.General.GearPlain
        }
    }

    init {
        layout = BoxLayout(this, BoxLayout.Y_AXIS)
        // border = BorderFactory.createEmptyBorder(5, 5, 5, 5) // Add some padding

        val dragSource = DragSource.getDefaultDragSource() // Keep one DragSource instance

        WidgetPaletteInfo.WIDGETS.forEach { widgetType ->
            val icon = getIconForWidgetType(widgetType)
            val button = JButton(icon) // Use icon for the button

            button.toolTipText = "Add ${widgetType}" // Tooltip for identification
            button.alignmentX = Component.LEFT_ALIGNMENT // Align buttons to the left

            // Set a consistent size for icon buttons
            val buttonSize = Dimension(36, 36)
            button.preferredSize = buttonSize
            button.minimumSize = buttonSize
            // Allow horizontal stretch if panel is wider, but fix height.
            // For Y_AXIS BoxLayout, max width is respected if alignmentX is Component.LEFT_ALIGNMENT (or RIGHT/CENTER)
            // To make them not stretch horizontally, their own max width should be set.
            button.maximumSize = Dimension(Short.MAX_VALUE.toInt(), buttonSize.height)


            // Drag and Drop setup
            val dgl = DragGestureListener { dge ->
                val transferable = StringSelection(widgetType)
                // Use dge.startDrag() for more control if needed, or stick to DragSource method
                dragSource.startDrag( // Using the shared dragSource
                    dge,
                    DragSource.DefaultCopyDrop, // Default cursor
                    transferable,
                    null // No DragSourceListener needed for simple cases
                )
            }
            // Create a new gesture recognizer for each button, associated with the shared DragSource
            dragSource.createDefaultDragGestureRecognizer(button, DnDConstants.ACTION_COPY, dgl)

            add(button)
            // add(Box.createRigidArea(Dimension(0, 2))) // Add small vertical spacing
        }
    }
}
