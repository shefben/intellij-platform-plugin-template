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

class WidgetPaletteComponent(project: Project) : JBPanel<WidgetPaletteComponent>() { // project param might be used later

    init {
        layout = BoxLayout(this, BoxLayout.Y_AXIS)
        // preferredSize = java.awt.Dimension(150, 0) // Size will be managed by tool window

        val dragSource = DragSource.getDefaultDragSource()
        for (widgetType in WidgetPaletteInfo.WIDGETS) {
            val button = JButton(widgetType)
            button.toolTipText = "Drag to add a ${widgetType} to the canvas"
            dragSource.createDefaultDragGestureRecognizer(
                button,
                DnDConstants.ACTION_COPY,
                object : DragGestureListener {
                    override fun dragGestureRecognized(dge: DragGestureEvent) {
                        val transferable = StringSelection(widgetType)
                        // The DragSourceListener (last arg) can be null for simple cases
                        dragSource.startDrag(
                            dge,
                            DragSource.DefaultCopyDrop, // cursor
                            transferable,
                            null // dsListener
                        )
                    }
                }
            )
            add(button)
        }
    }
}
