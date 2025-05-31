package com.jules.tkinterdesigner.toolWindow

import com.intellij.ui.JBColor
import com.intellij.ui.components.JBPanel
import com.jules.tkinterdesigner.model.DesignedDialog
import com.jules.tkinterdesigner.model.DesignedWidget
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.RenderingHints
import java.awt.datatransfer.DataFlavor
import java.awt.dnd.*
import java.awt.Rectangle // Added for Rectangle usage
import java.awt.BasicStroke // For selection rectangle
import java.awt.event.KeyAdapter // For deletion
import java.awt.event.KeyEvent // For deletion
import java.awt.event.MouseAdapter // For mouse interaction

class VisualCanvasPanel : JBPanel<VisualCanvasPanel>(), DropTargetListener {
    internal var currentDesign: DesignedDialog = DesignedDialog() // Changed to internal
    private var selectedWidgetId: String? = null
    private var dragOffsetX: Int = 0
    private var dragOffsetY: Int = 0

    private enum class ResizeMode { NONE, BOTTOM_RIGHT, BOTTOM_LEFT, TOP_RIGHT, TOP_LEFT, BOTTOM, TOP, LEFT, RIGHT }
    private var currentResizeMode: ResizeMode = ResizeMode.NONE
    private val resizeHandleSize = 8
    private var dragOverPoint: java.awt.Point? = null
    private var dragOverWidgetType: String? = null

    var onWidgetSelected: ((DesignedWidget?) -> Unit)? = null

    init {
        background = JBColor.WHITE // Default background
        // Register the panel as a drop target
        DropTarget(this, DnDConstants.ACTION_COPY_OR_MOVE, this, true, null)

        val mouseAdapterInstance = CanvasMouseAdapter() // Renamed to avoid conflict
        addMouseListener(mouseAdapterInstance)
        addMouseMotionListener(mouseAdapterInstance)
        isFocusable = true // For KeyListener

        addKeyListener(object : KeyAdapter() {
            override fun keyPressed(e: KeyEvent) {
                if (e.keyCode == KeyEvent.VK_DELETE && selectedWidgetId != null) {
                    val previouslySelectedId = selectedWidgetId
                    currentDesign.widgets.removeIf { it.id == selectedWidgetId }
                    selectedWidgetId = null
                    currentResizeMode = ResizeMode.NONE
                    if (previouslySelectedId != null) { // Check if selection actually changed
                        onWidgetSelected?.invoke(null)
                    }
                    repaint()
                }
            }
        })
    }

    // Helper to get the bottom-right resize handle for a widget
    private fun getBottomRightResizeHandleRect(widget: DesignedWidget): Rectangle {
        return Rectangle(
            widget.x + widget.width - resizeHandleSize / 2,
            widget.y + widget.height - resizeHandleSize / 2,
            resizeHandleSize,
            resizeHandleSize
        )
    }

    override fun paintComponent(g: Graphics) {
        super.paintComponent(g)
        val g2d = g as Graphics2D

        // Enable anti-aliasing for smoother graphics
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)

        // Draw Dialog Representation
        g2d.color = JBColor.LIGHT_GRAY // Dialog background
        g2d.fillRect(0, 0, currentDesign.width, currentDesign.height)
        g2d.color = JBColor.DARK_GRAY // Dialog border
        g2d.drawRect(0, 0, currentDesign.width -1 , currentDesign.height - 1) // -1 to keep border inside dimensions

        // Draw Title Bar
        val titleBarHeight = 30
        g2d.color = JBColor.GRAY // Title bar background
        g2d.fillRect(0, 0, currentDesign.width, titleBarHeight)
        g2d.color = JBColor.BLACK // Title bar border
        g2d.drawRect(0, 0, currentDesign.width -1, titleBarHeight -1)
        g2d.color = JBColor.WHITE // Title text color
        g2d.drawString(currentDesign.title, 5, titleBarHeight / 2 + g2d.fontMetrics.ascent / 2 - 2)

        // Draw Widget Representations
        for (widget in currentDesign.widgets) { // Existing widgets
            g2d.color = JBColor.GRAY // Widget background
            g2d.fillRect(widget.x, widget.y, widget.width, widget.height)
            g2d.color = JBColor.BLACK // Widget border
            g2d.drawRect(widget.x, widget.y, widget.width - 1, widget.height - 1)

            if (widget.id == selectedWidgetId) {
                val originalStroke = g2d.stroke
                g2d.stroke = BasicStroke(2f)
                g2d.color = JBColor.BLUE
                g2d.drawRect(widget.x - 1, widget.y - 1, widget.width + 1, widget.height + 1) // Slightly larger for visibility
                g2d.stroke = originalStroke

                // Draw bottom-right resize handle
                val handleRect = getBottomRightResizeHandleRect(widget)
                g2d.color = JBColor.BLUE
                g2d.fillRect(handleRect.x, handleRect.y, handleRect.width, handleRect.height)
            }

            g2d.color = JBColor.BLACK // Widget text color
            // Prioritize name, then text, then type for display label
            val nameProperty = widget.properties["name"] as? String
            val textProperty = widget.properties["text"] as? String

            var primaryDisplay = nameProperty
                ?: textProperty
                ?: widget.type

            // Append type if name or text was primary, and append ID for clarity
            if (nameProperty != null && nameProperty != widget.type) {
                 primaryDisplay += " (${widget.type})"
            }
            primaryDisplay += " [${widget.id.takeLast(4)}]"


            // Basic text centering attempt
            val textWidth = g2d.fontMetrics.stringWidth(primaryDisplay)
            val textHeight = g2d.fontMetrics.ascent
            val textX = widget.x + (widget.width - textWidth) / 2
            val textY = widget.y + (widget.height + textHeight) / 2
            g2d.drawString(primaryDisplay, textX, textY)
        }

        // Draw Drag-Over Feedback
        dragOverPoint?.let { point ->
            dragOverWidgetType?.let { type ->
                val defaultDropWidth = 100 // Same as in drop()
                val defaultDropHeight = 30  // Same as in drop()
                g2d.color = JBColor.GREEN.withAlpha(100) // Semi-transparent green
                g2d.fillRect(point.x, point.y, defaultDropWidth, defaultDropHeight)
                g2d.color = JBColor.GREEN
                g2d.drawRect(point.x, point.y, defaultDropWidth, defaultDropHeight)

                // Draw type string inside feedback rectangle
                val textWidth = g2d.fontMetrics.stringWidth(type)
                val textHeight = g2d.fontMetrics.ascent
                val textX = point.x + (defaultDropWidth - textWidth) / 2
                val textY = point.y + (defaultDropHeight + textHeight) / 2
                g2d.color = JBColor.BLACK // Ensure text is visible
                g2d.drawString(type, textX, textY)
            }
        }
    }

    fun addTestWidget() {
        val testWidget = DesignedWidget(
            id = "btnTest",
            type = "Button",
            x = 50,
            y = 50,
            width = 100,
            height = 30,
            properties = mutableMapOf("text" to "Click Me")
        )
        currentDesign.widgets.add(testWidget)
        repaint()
    }

    // DropTargetListener methods
    override fun dragEnter(dtde: DropTargetDragEvent) {
        if (dtde.isDataFlavorSupported(DataFlavor.stringFlavor)) {
            dtde.acceptDrag(DnDConstants.ACTION_COPY)
            try {
                dragOverWidgetType = dtde.transferable.getTransferData(DataFlavor.stringFlavor) as? String
            } catch (e: Exception) {
                // Handle exception if needed
                dragOverWidgetType = null
            }
        } else {
            dtde.rejectDrag()
        }
    }

    override fun dragOver(dtde: DropTargetDragEvent) {
        if (dtde.isDataFlavorSupported(DataFlavor.stringFlavor)) {
            dtde.acceptDrag(DnDConstants.ACTION_COPY)
            dragOverPoint = dtde.location
        } else {
            dtde.rejectDrag()
            dragOverPoint = null
            dragOverWidgetType = null
        }
        repaint()
    }

    override fun dropActionChanged(dtde: DropTargetDragEvent) {
        // Can be empty for now
    }

    override fun dragExit(dte: DropTargetEvent) {
        dragOverPoint = null
        dragOverWidgetType = null
        repaint()
    }

    override fun drop(dtde: DropTargetDropEvent) {
        dragOverPoint = null // Clear visual feedback first
        dragOverWidgetType = null
        // Repaint to clear the feedback immediately before adding the new widget
        // This might cause a slight flicker but ensures the ghost is gone.
        // A more complex solution might involve double buffering or selective repaint.
        repaint()


        if (dtde.isDataFlavorSupported(DataFlavor.stringFlavor)) {
            dtde.acceptDrop(DnDConstants.ACTION_COPY)
            val transferable = dtde.transferable
            try {
                val widgetType = transferable.getTransferData(DataFlavor.stringFlavor) as String
                val dropPoint = dtde.location

                val newWidgetId = "widget_${System.currentTimeMillis()}"
                // Default name: type in lowercase + last 4 digits of ID (or simple counter if preferred)
                val shortIdSuffix = newWidgetId.takeLast(4)
                val defaultName = "${widgetType.toLowerCase().replace(".","_")}_${shortIdSuffix}"

                val newWidget = DesignedWidget(
                    id = newWidgetId,
                    type = widgetType,
                    x = dropPoint.x,
                    y = dropPoint.y,
                    width = 100, // Default width
                    height = 30, // Default height
                    properties = mutableMapOf(
                        "text" to widgetType, // Default "text" property
                        "name" to defaultName  // Default "name" property
                    )
                )
                currentDesign.widgets.add(newWidget)
                repaint()
                dtde.dropComplete(true)
            } catch (e: Exception) {
                e.printStackTrace()
                dtde.dropComplete(false)
            }
        } else {
            dtde.rejectDrop()
        }
    }

    private inner class CanvasMouseAdapter : MouseAdapter() { // Changed from java.awt.event.MouseAdapter
        override fun mousePressed(e: java.awt.event.MouseEvent) {
            requestFocusInWindow()

            selectedWidgetId?.let { widgetId ->
                currentDesign.widgets.find { it.id == widgetId }?.let { widget ->
                    val bottomRightHandle = getBottomRightResizeHandleRect(widget)
                    if (bottomRightHandle.contains(e.point)) {
                        currentResizeMode = ResizeMode.BOTTOM_RIGHT
                        // Store initial drag point relative to widget corner for precise resizing later if needed
                        dragOffsetX = e.x - (widget.x + widget.width)
                        dragOffsetY = e.y - (widget.y + widget.height)
                        repaint()
                        return
                    }
                }
            }

            var foundWidget = false
            // Iterate widgets in reverse for LIFO selection (topmost widgets first)
            for (widget in currentDesign.widgets.reversed()) {
                val widgetRect = Rectangle(widget.x, widget.y, widget.width, widget.height)
                if (widgetRect.contains(e.point)) {
                    selectedWidgetId = widget.id
                    dragOffsetX = e.x - widget.x
                    dragOffsetY = e.y - widget.y
                    currentResizeMode = ResizeMode.NONE // Reset resize mode
                    foundWidget = true
                    break
                }
            }

            val oldSelectedId = selectedWidgetId
            if (!foundWidget) {
                selectedWidgetId = null
                currentResizeMode = ResizeMode.NONE
            }
            // Notify if selection changed
            if (oldSelectedId != selectedWidgetId) {
                 onWidgetSelected?.invoke(currentDesign.widgets.find { it.id == selectedWidgetId })
            }
            repaint()
        }

        override fun mouseDragged(e: java.awt.event.MouseEvent) {
            selectedWidgetId?.let { widgetId ->
                currentDesign.widgets.find { it.id == widgetId }?.let { widget ->
                    if (currentResizeMode == ResizeMode.BOTTOM_RIGHT) {
                        // Simple bottom-right resize
                        widget.width = Math.max(20, e.x - widget.x) // dragOffsetX is relative to corner here
                        widget.height = Math.max(20, e.y - widget.y) // dragOffsetY is relative to corner here
                    } else if (currentResizeMode == ResizeMode.NONE) { // Moving
                        widget.x = e.x - dragOffsetX
                        widget.y = e.y - dragOffsetY
                    }
                    repaint()
                }
            }
        }

        override fun mouseReleased(e: java.awt.event.MouseEvent) {
            if (currentResizeMode != ResizeMode.NONE) {
                 currentResizeMode = ResizeMode.NONE
                 // Potentially finalize resize, e.g. snap to grid, update property editor
            }
        }
    }
}
