package com.jules.tkinterdesigner.toolWindow

// ... (imports as before) ...
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBPanel
import com.intellij.ui.components.JBTextField
import com.jules.tkinterdesigner.model.DesignedDialog
import com.jules.tkinterdesigner.model.DesignedWidget
import com.jules.tkinterdesigner.messaging.WIDGET_MODIFIED_TOPIC
import com.jules.tkinterdesigner.messaging.WIDGET_SELECTION_TOPIC
import com.jules.tkinterdesigner.messaging.WidgetPropertyListener
import java.awt.*
import java.awt.datatransfer.DataFlavor
import java.awt.dnd.*
import java.awt.event.*
import javax.swing.JMenuItem
import javax.swing.JPopupMenu
import javax.swing.SwingUtilities
import kotlin.math.roundToInt


class VisualCanvasPanel( /* ... constructor ... */ ) : JBPanel<VisualCanvasPanel>(), DropTargetListener {
    // ... (properties as before) ...
    internal var currentDesign: DesignedDialog = initialDialog // etc.
    internal val selectedWidgetIds: MutableSet<String> = mutableSetOf()
    private var primarySelectedWidgetId: String? = null
    fun getPrimarySelectedWidgetId(): String? = primarySelectedWidgetId
    private var dragOffsetXWorld: Int = 0
    private var dragOffsetYWorld: Int = 0
    private enum class ResizeMode { NONE, TOP_LEFT, TOP, TOP_RIGHT, LEFT, RIGHT, BOTTOM_LEFT, BOTTOM, BOTTOM_RIGHT }
    private var currentResizeMode: ResizeMode = ResizeMode.NONE
    private val resizeHandleScreenSize = 8
    private var initialMouseScreenX: Int = 0
    private var initialMouseScreenY: Int = 0
    private var initialPrimaryWidgetWorldBounds: Rectangle? = null
    private var initialMultiWidgetStates: MutableMap<String, Rectangle> = mutableMapOf()
    var gridSize: Int = 10
        set(value) { field = value.coerceAtLeast(1); repaint() }
    var showGrid: Boolean = true
        set(value) { field = value; repaint() }
    private val snapThresholdScreen: Int = 6
    private var dragOverPointScreen: Point? = null
    private var dragOverWidgetType: String? = null
    private lateinit var canvasPopupMenu: JPopupMenu
    private lateinit var deleteWidgetAction: JMenuItem
    private lateinit var editPropertiesAction: JMenuItem
    private lateinit var alignLeftAction: JMenuItem
    private lateinit var alignRightAction: JMenuItem
    private lateinit var alignTopAction: JMenuItem
    private lateinit var alignBottomAction: JMenuItem
    private lateinit var alignHCenterAction: JMenuItem
    private lateinit var alignVCenterAction: JMenuItem
    private lateinit var addNotebookTabAction: JMenuItem
    private lateinit var distributeHorizontallyAction: JMenuItem
    private lateinit var distributeVerticallyAction: JMenuItem
    private var marqueeStartScreenPoint: Point? = null
    private var marqueeCurrentScreenPoint: Point? = null
    private val marqueeRectStroke = BasicStroke(1f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0f, floatArrayOf(3f), 0f)
    private var editingWidgetId: String? = null
    private var activeTextEditor: JBTextField? = null
    private var scaleFactor: Double = 1.0
    private val minScale: Double = 0.2
    private val maxScale: Double = 3.0
    private val sashScreenRects: MutableMap<Pair<String, Int>, Rectangle> = mutableMapOf()
    private var draggingSashInfo: Pair<String, Int>? = null
    private var initialSashDragMouseScreenPos: Int = 0
    private var currentSashDragLineScreenPos: Int? = null

    init { /* ... (as before) ... */ }
    fun setScale(newScale: Double) { /* ... */ }
    // ... (zoom methods, coordinate transformations, setupPopupMenu as before, but ActionListeners for distribute will be added now) ...
    private fun setupPopupMenu() {
        canvasPopupMenu = JPopupMenu()
        deleteWidgetAction = JMenuItem("Delete Widget(s)"); deleteWidgetAction.addActionListener { /* ... */ }; canvasPopupMenu.add(deleteWidgetAction)
        editPropertiesAction = JMenuItem("Edit Properties"); editPropertiesAction.addActionListener { /* ... */ }; canvasPopupMenu.add(editPropertiesAction)
        canvasPopupMenu.addSeparator()
        alignLeftAction = JMenuItem("Align Left Edges"); canvasPopupMenu.add(alignLeftAction)
        alignTopAction = JMenuItem("Align Top Edges"); canvasPopupMenu.add(alignTopAction)
        alignRightAction = JMenuItem("Align Right Edges"); canvasPopupMenu.add(alignRightAction)
        alignBottomAction = JMenuItem("Align Bottom Edges"); canvasPopupMenu.add(alignBottomAction)
        alignHCenterAction = JMenuItem("Align Horizontal Centers"); canvasPopupMenu.add(alignHCenterAction)
        alignVCenterAction = JMenuItem("Align Vertical Centers"); canvasPopupMenu.add(alignVCenterAction)
        alignLeftAction.addActionListener { alignSelectedWidgets(Alignment.LEFT) }
        alignTopAction.addActionListener { alignSelectedWidgets(Alignment.TOP) }
        alignRightAction.addActionListener { alignSelectedWidgets(Alignment.RIGHT) }
        alignBottomAction.addActionListener { alignSelectedWidgets(Alignment.BOTTOM) }
        alignHCenterAction.addActionListener { alignSelectedWidgets(Alignment.HORIZONTAL_CENTER) }
        alignVCenterAction.addActionListener { alignSelectedWidgets(Alignment.VERTICAL_CENTER) }

        canvasPopupMenu.addSeparator()
        addNotebookTabAction = JMenuItem("Add Tab to Notebook")
        addNotebookTabAction.addActionListener { addTabToSelectedNotebook() }
        canvasPopupMenu.add(addNotebookTabAction)

        canvasPopupMenu.addSeparator()
        distributeHorizontallyAction = JMenuItem("Distribute Horizontally")
        distributeVerticallyAction = JMenuItem("Distribute Vertically")
        distributeHorizontallyAction.addActionListener { distributeSelectedWidgets(true) }
        distributeVerticallyAction.addActionListener { distributeSelectedWidgets(false) }
        canvasPopupMenu.add(distributeHorizontallyAction)
        canvasPopupMenu.add(distributeVerticallyAction)
    }


    private enum class Alignment { LEFT, TOP, RIGHT, BOTTOM, HORIZONTAL_CENTER, VERTICAL_CENTER }
    private fun alignSelectedWidgets(alignment: Alignment) { /* ... (full implementation as before) ... */ }

    private fun distributeSelectedWidgets(isHorizontal: Boolean) {
        if (selectedWidgetIds.size < 2) return

        val selectedDesignedWidgets = selectedWidgetIds.mapNotNull { id ->
            currentDesign.widgets.find { it.id == id }?.let { widget ->
                getWidgetAbsoluteWorldBounds(widget.id)?.let { bounds -> Pair(widget, bounds) }
            }
        }

        if (selectedDesignedWidgets.size < 2) return

        val sortedWidgets = if (isHorizontal) {
            selectedDesignedWidgets.sortedBy { it.second.x }
        } else {
            selectedDesignedWidgets.sortedBy { it.second.y }
        }

        val firstWidgetPair = sortedWidgets.first()
        val lastWidgetPair = sortedWidgets.last()

        if (isHorizontal) {
            val minX = firstWidgetPair.second.x
            val maxXPlusWidth = lastWidgetPair.second.x + lastWidgetPair.second.width
            val totalSpan = maxXPlusWidth - minX
            val sumOfWidgetWidths = sortedWidgets.sumOf { it.second.width }

            if (sortedWidgets.size > 1 && totalSpan > sumOfWidgetWidths) { // Ensure there's space to distribute
                val totalSpacing = totalSpan - sumOfWidgetWidths
                val spacing = totalSpacing.toDouble() / (sortedWidgets.size - 1)

                var currentWorldX = minX.toDouble()
                sortedWidgets.forEach { (widget, initialAbsBounds) ->
                    val parentAbsBounds = getWidgetAbsoluteWorldBounds(widget.parentId) ?: Rectangle(0,0,0,0)
                    val newRelativeX = (currentWorldX - parentAbsBounds.x).roundToInt()
                    if (widget.x != newRelativeX) {
                        widget.x = newRelativeX
                        project.messageBus.syncPublisher(WIDGET_MODIFIED_TOPIC).widgetModified(widget, currentDesign)
                    }
                    currentWorldX += initialAbsBounds.width + spacing
                }
            }
        } else { // Vertical distribution
            val minY = firstWidgetPair.second.y
            val maxYPlusHeight = lastWidgetPair.second.y + lastWidgetPair.second.height
            val totalSpan = maxYPlusHeight - minY
            val sumOfWidgetHeights = sortedWidgets.sumOf { it.second.height }

            if (sortedWidgets.size > 1 && totalSpan > sumOfWidgetHeights) { // Ensure there's space
                val totalSpacing = totalSpan - sumOfWidgetHeights
                val spacing = totalSpacing.toDouble() / (sortedWidgets.size - 1)

                var currentWorldY = minY.toDouble()
                sortedWidgets.forEach { (widget, initialAbsBounds) ->
                    val parentAbsBounds = getWidgetAbsoluteWorldBounds(widget.parentId) ?: Rectangle(0,0,0,0)
                    val newRelativeY = (currentWorldY - parentAbsBounds.y).roundToInt()
                    if (widget.y != newRelativeY) {
                        widget.y = newRelativeY
                        project.messageBus.syncPublisher(WIDGET_MODIFIED_TOPIC).widgetModified(widget, currentDesign)
                    }
                    currentWorldY += initialAbsBounds.height + spacing
                }
            }
        }
        repaint()
    }

    private fun getWidgetAbsoluteWorldBounds(widgetId: String?): Rectangle? { /* ... */ }
    private fun getResizeHandleScreenRects(widgetWorldAbsRect: Rectangle): Map<ResizeMode, Rectangle> { /* ... */ }
    private fun drawWidgetAndChildren(g2d: Graphics2D, widget: DesignedWidget, parentWorldAbsX: Int, parentWorldAbsY: Int) { /* ... */ }
    override fun paintComponent(g: Graphics) { /* ... */ }
    private fun commitActiveTextEdit() { /* ... */ }
    override fun drop(dtde: DropTargetDropEvent) { /* ... */ }
    private fun getWidgetAndAbsoluteWorldBoundsAtPoint(pScreen:Point,wTS:List<DesignedWidget>,cPWAX:Int,cPWAY:Int,f:((DesignedWidget)->Boolean)?=null):Pair<DesignedWidget,Rectangle>?{ /* ... */ }
    private fun snapToGrid(worldCoord: Int, currentWorldGridSize: Int): Int { /* ... */ }
    private fun addTabToSelectedNotebook() { /* ... */ }

    private inner class CanvasMouseAdapter : MouseAdapter() {
        private fun showPopupMenuIfTrigger(e: MouseEvent) {
            if (e.isPopupTrigger) {
                val clickedInfo = getWidgetAndAbsoluteWorldBoundsAtPoint(e.point, currentDesign.widgets.filter { it.parentId == null }, 0, 0)
                val clickedWidget = clickedInfo?.first
                if (clickedWidget != null) { /* ... (selection logic as before) ... */ }
                else { /* ... (deselect if not shift-click on background) ... */ }

                val enableSingleSelectActions = selectedWidgetIds.size == 1 && primarySelectedWidgetId != null
                val enableMultiSelectActions = selectedWidgetIds.size > 1
                val enableAnySelectActions = selectedWidgetIds.isNotEmpty()
                val primaryIsNotebook = currentDesign.widgets.find { it.id == primarySelectedWidgetId }?.type == "ttk.Notebook"

                deleteWidgetAction.isEnabled = enableAnySelectActions
                editPropertiesAction.isEnabled = enableSingleSelectActions
                alignLeftAction.isEnabled = enableMultiSelectActions; alignTopAction.isEnabled = enableMultiSelectActions
                alignRightAction.isEnabled = enableMultiSelectActions; alignBottomAction.isEnabled = enableMultiSelectActions
                alignHCenterAction.isEnabled = enableMultiSelectActions; alignVCenterAction.isEnabled = enableMultiSelectActions
                addNotebookTabAction.isEnabled = enableSingleSelectActions && primaryIsNotebook
                distributeHorizontallyAction.isEnabled = enableMultiSelectActions // Or selectedWidgetIds.size >= 2
                distributeVerticallyAction.isEnabled = enableMultiSelectActions   // Or selectedWidgetIds.size >= 2

                canvasPopupMenu.show(e.component, e.x, e.y)
            }
        }
        override fun mousePressed(e: MouseEvent) { /* ... (full implementation as before) ... */ }
        override fun mouseDragged(e: MouseEvent) { /* ... (full implementation as before) ... */ }
        override fun mouseReleased(e: MouseEvent) { /* ... (full implementation as before) ... */ }
    }
    // Stubs for unchanged long methods are assumed to be filled from previous complete versions.
}
