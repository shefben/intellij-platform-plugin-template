package com.jules.tkinterdesigner.toolWindow

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

class VisualCanvasPanel(
    private val project: Project,
    initialDialog: DesignedDialog
) : JBPanel<VisualCanvasPanel>(), DropTargetListener {

    internal var currentDesign: DesignedDialog = initialDialog
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

    private var marqueeStartScreenPoint: Point? = null
    private var marqueeCurrentScreenPoint: Point? = null
    private val marqueeRectStroke = BasicStroke(1f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0f, floatArrayOf(3f), 0f)

    private var editingWidgetId: String? = null
    private var activeTextEditor: JBTextField? = null

    private var scaleFactor: Double = 1.0
    private val minScale: Double = 0.2
    private val maxScale: Double = 3.0

    init {
        background = JBColor.PanelBackground
        DropTarget(this, DnDConstants.ACTION_COPY_OR_MOVE, this, true, null)
        setupPopupMenu()
        val mouseAdapterInstance = CanvasMouseAdapter()
        addMouseListener(mouseAdapterInstance)
        addMouseMotionListener(mouseAdapterInstance)
        addMouseWheelListener { e ->
            if (e.isControlDown) {
                e.consume()
                // val worldPointBeforeZoom = screenToWorld(e.point) // For zoom-around-point
                if (e.wheelRotation < 0) { zoomIn() } else { zoomOut() }
                // Adjust view for zoom-around-point (not implemented yet)
                // val worldPointAfterZoom = screenToWorld(e.point)
                // viewOffsetX += (worldPointAfterZoom.x - worldPointBeforeZoom.x) * scaleFactor ...
            } else {
                 parent?.dispatchEvent(SwingUtilities.convertMouseEvent(this, e, parent))
            }
        }
        isFocusable = true
        addKeyListener(object : KeyAdapter() {
            override fun keyPressed(e: KeyEvent) {
                if (e.keyCode == KeyEvent.VK_DELETE && selectedWidgetIds.isNotEmpty()) {
                    deleteWidgetAction.doClick()
                }
            }
        })
        project.messageBus.connect(this).subscribe(WIDGET_MODIFIED_TOPIC, object : WidgetPropertyListener {
            override fun propertyChanged(widget: DesignedWidget, dialog: DesignedDialog?) {
                if (dialog == currentDesign && currentDesign.widgets.any { it.id == widget.id }) {
                    repaint()
                }
            }
        })
    }

    fun setScale(newScale: Double) {
        scaleFactor = newScale.coerceIn(minScale, maxScale)
        repaint()
    }
    fun zoomIn() { setScale(scaleFactor * 1.1) }
    fun zoomOut() { setScale(scaleFactor / 1.1) }
    fun zoomActual() { setScale(1.0) }

    private fun screenToWorld(p: Point): Point = Point((p.x / scaleFactor).roundToInt(), (p.y / scaleFactor).roundToInt())
    private fun worldToScreen(p: Point): Point = Point((p.x * scaleFactor).roundToInt(), (p.y * scaleFactor).roundToInt())
    private fun worldToScreenLength(l: Int): Int = (l * scaleFactor).roundToInt()
    private fun screenToWorldLength(l: Int): Int = (l / scaleFactor).roundToInt()
    private fun screenToWorldRect(sr: Rectangle): Rectangle = Rectangle(screenToWorld(sr.location), Dimension(screenToWorldLength(sr.width), screenToWorldLength(sr.height)))
    private fun worldToScreenRect(wr: Rectangle): Rectangle = Rectangle(worldToScreen(wr.location), Dimension(worldToScreenLength(wr.width), worldToScreenLength(wr.height)))


    private fun setupPopupMenu() { /* ... (Copy full method from previous correct version) ... */ }

    private fun getWidgetAbsoluteWorldBounds(widgetId: String?): Rectangle? {
        if (widgetId == null) return Rectangle(0,0,currentDesign.width, currentDesign.height) // World bounds of the dialog itself
        val widget = currentDesign.widgets.find { it.id == widgetId } ?: return null
        var currentAbsX = widget.x; var currentAbsY = widget.y
        var parent = currentDesign.widgets.find { it.id == widget.parentId }
        while (parent != null) { currentAbsX += parent.x; currentAbsY += parent.y; parent = currentDesign.widgets.find { it.id == parent.parentId } }
        return Rectangle(currentAbsX, currentAbsY, widget.width, widget.height)
    }

    private fun getResizeHandleScreenRects(widgetWorldAbsRect: Rectangle): Map<ResizeMode, Rectangle> {
        val r = mutableMapOf<ResizeMode, Rectangle>(); val h = resizeHandleScreenSize / 2
        val s = worldToScreenRect(widgetWorldAbsRect) // Convert widget bounds to screen for handle positioning
        r[ResizeMode.TOP_LEFT]=Rectangle(s.x-h, s.y-h, resizeHandleScreenSize, resizeHandleScreenSize); r[ResizeMode.TOP]=Rectangle(s.x+s.width/2-h, s.y-h, resizeHandleScreenSize, resizeHandleScreenSize); r[ResizeMode.TOP_RIGHT]=Rectangle(s.x+s.width-h, s.y-h, resizeHandleScreenSize, resizeHandleScreenSize); r[ResizeMode.LEFT]=Rectangle(s.x-h, s.y+s.height/2-h, resizeHandleScreenSize, resizeHandleScreenSize); r[ResizeMode.RIGHT]=Rectangle(s.x+s.width-h, s.y+s.height/2-h, resizeHandleScreenSize, resizeHandleScreenSize); r[ResizeMode.BOTTOM_LEFT]=Rectangle(s.x-h, s.y+s.height-h, resizeHandleScreenSize, resizeHandleScreenSize); r[ResizeMode.BOTTOM]=Rectangle(s.x+s.width/2-h, s.y+s.height-h, resizeHandleScreenSize, resizeHandleScreenSize); r[ResizeMode.BOTTOM_RIGHT]=Rectangle(s.x+s.width-h, s.y+s.height-h, resizeHandleScreenSize, resizeHandleScreenSize)
        return r
    }

    private fun drawWidgetAndChildren(g2d: Graphics2D, widget: DesignedWidget, parentWorldAbsX: Int, parentWorldAbsY: Int) {
        val worldX = parentWorldAbsX + widget.x
        val worldY = parentWorldAbsY + widget.y
        val worldRect = Rectangle(worldX, worldY, widget.width, widget.height)
        val screenRect = worldToScreenRect(worldRect)

        g2d.color = JBColor.GRAY; g2d.fillRect(screenRect.x, screenRect.y, screenRect.width, screenRect.height)
        g2d.color = JBColor.BLACK; g2d.drawRect(screenRect.x, screenRect.y, screenRect.width - 1, screenRect.height - 1)

        val tempG = g2d.create() as Graphics2D
        try {
            tempG.translate(screenRect.x, screenRect.y)
            // Widget-specific drawing scaled to screenRect.width/height
            when (widget.type) { /* ... (widget specific drawing logic, using screenRect.width/height for internal scaling) ... */ }
        } finally { tempG.dispose() }

        if (selectedWidgetIds.contains(widget.id)) {
            val oS=g2d.stroke; val sW=(if(widget.id==primarySelectedWidgetId)2f else 1f); g2d.stroke=BasicStroke(sW); g2d.color=if(widget.id==primarySelectedWidgetId)JBColor.BLUE else JBColor.CYAN.darker(); g2d.drawRect(screenRect.x-1,screenRect.y-1,screenRect.width+1,screenRect.height+1); g2d.stroke=oS
            if (selectedWidgetIds.size == 1 && widget.id == primarySelectedWidgetId) {
                val hRs=getResizeHandleScreenRects(worldRect); g2d.color=JBColor.BLUE; hRs.values.forEach{g2d.fill(it)}
            }
        }

        if (!(widget.id == editingWidgetId && activeTextEditor != null)) {
            val oF=g2d.font; val sFS=(oF.size2D).toFloat(); g2d.font=oF.deriveFont(sFS.coerceIn((6f/scaleFactor).toFloat(), (72f/scaleFactor).toFloat())); // Scale font but clamp
            g2d.color = JBColor.BLACK; val nP=widget.properties["name"] as? String; val tP=widget.properties["text"] as? String; var pD=nP?:tP?:widget.type; if(nP!=null&&nP!=widget.type){pD+=" (${widget.type})"}; pD+=" [${widget.id.takeLast(4)}]"; val tM=g2d.fontMetrics.getStringBounds(pD,g2d); val tX=screenRect.x+(screenRect.width-tM.width.toInt())/2; val tY=screenRect.y+(screenRect.height-tM.height.toInt())/2+g2d.fontMetrics.ascent; g2d.drawString(pD,tX,tY)
            g2d.font = oF
        }
        currentDesign.widgets.filter{it.parentId==widget.id}.forEach{val oC=g2d.clip; g2d.setClip(screenRect.x,screenRect.y,screenRect.width,screenRect.height); drawWidgetAndChildren(g2d,it,worldX,worldY); g2d.clip=oC}
    }

    override fun paintComponent(g: Graphics) {
        super.paintComponent(g)
        val g2d = g as Graphics2D
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON); g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)
        g2d.color = background; g2d.fillRect(0,0,getWidth(),getHeight())

        if(showGrid){
            g2d.color=JBColor.border().withAlpha(70); val cgsWorld=gridSize
            var xWorld=0; while(true){val xScreen=worldToScreenLength(xWorld); if(xScreen>this.width)break; g2d.drawLine(xScreen,0,xScreen,this.height); xWorld+=cgsWorld}
            var yWorld=0; while(true){val yScreen=worldToScreenLength(yWorld); if(yScreen>this.height)break; g2d.drawLine(0,yScreen,this.width,yScreen); yWorld+=cgsWorld}
        }

        val dialogScreenRect = worldToScreenRect(Rectangle(0,0,currentDesign.width,currentDesign.height))
        g2d.color = JBColor.LIGHT_GRAY; g2d.fillRect(dialogScreenRect.x,dialogScreenRect.y,dialogScreenRect.width,dialogScreenRect.height); g2d.color=JBColor.DARK_GRAY; g2d.drawRect(dialogScreenRect.x,dialogScreenRect.y,dialogScreenRect.width-1,dialogScreenRect.height-1)
        val tBHScreen=worldToScreenLength(30); g2d.color=JBColor.GRAY; g2d.fillRect(dialogScreenRect.x,dialogScreenRect.y,dialogScreenRect.width,tBHScreen); g2d.color=JBColor.BLACK; g2d.drawRect(dialogScreenRect.x,dialogScreenRect.y,dialogScreenRect.width-1,tBHScreen-1); val oF=g2d.font; g2d.font=oF.deriveFont((oF.size2D).toFloat()); g2d.color=JBColor.WHITE; g2d.drawString(currentDesign.title,dialogScreenRect.x+5,dialogScreenRect.y+tBHScreen/2+g2d.fontMetrics.ascent/2-2); g2d.font=oF

        currentDesign.widgets.filter{it.parentId==null}.forEach{drawWidgetAndChildren(g2d,it,0,0)}

        if (marqueeStartScreenPoint != null && marqueeCurrentScreenPoint != null) { val s=marqueeStartScreenPoint!!; val c=marqueeCurrentScreenPoint!!; val mDR=Rectangle(Math.min(s.x,c.x),Math.min(s.y,c.y),Math.abs(s.x-c.x),Math.abs(s.y-c.y)); g2d.color=JBColor.BLUE.withAlpha(50); g2d.fill(mDR); val oS=g2d.stroke; g2d.stroke=marqueeRectStroke; g2d.color=JBColor.BLUE; g2d.draw(mDR); g2d.stroke=oS }
        dragOverPointScreen?.let{p->dragOverWidgetType?.let{t-> val dW=worldToScreenLength(100);val dH=worldToScreenLength(30);g2d.color=JBColor.GREEN.withAlpha(100);g2d.fillRect(p.x,p.y,dW,dH);g2d.color=JBColor.GREEN;g2d.drawRect(p.x,p.y,dW,dH);val tM=g2d.fontMetrics.getStringBounds(t,g2d);val tX=p.x+(dW-tM.width.toInt())/2;val tY=p.y+(dH-tM.height.toInt())/2+g2d.fontMetrics.ascent;g2d.color=JBColor.BLACK;g2d.drawString(t,tX,tY)}}
    }

    private fun commitActiveTextEdit() { /* ... (full implementation as before) ... */ }
    private fun getWidgetAndAbsoluteWorldBoundsAtPoint(pScreen:Point,wTS:List<DesignedWidget>,cPWAX:Int,cPWAY:Int,f:((DesignedWidget)->Boolean)?=null):Pair<DesignedWidget,Rectangle>?{ /* ... (full implementation as before, using screenToWorld(pScreen) at start) ... */ }
    private fun snapToGrid(worldCoord: Int, currentWorldGridSize: Int): Int {
        if (!showGrid || currentWorldGridSize <= 0) return worldCoord
        val worldSnapThresh = screenToWorldLength(snapThresholdScreen) // Convert screen threshold to world
        val remainder = worldCoord % currentWorldGridSize
        return when {
            Math.abs(remainder) <= worldSnapThresh -> worldCoord - remainder
            Math.abs(currentWorldGridSize - remainder) <= worldSnapThresh -> worldCoord + (currentWorldGridSize - remainder)
            else -> worldCoord
        }
    }

    private inner class CanvasMouseAdapter : MouseAdapter() {
        private fun showPopupMenuIfTrigger(e: MouseEvent) { /* ... (full implementation as before) ... */ }
        override fun mousePressed(e: MouseEvent) { /* ... (full implementation using screenToWorld(e.point) at start) ... */ }
        override fun mouseDragged(e: MouseEvent) { /* ... (full implementation using screenToWorld(e.point) for dx/dy, then calculations in world coords) ... */ }
        override fun mouseReleased(e: MouseEvent) { /* ... (full implementation using screenToWorld(e.point) for marquee) ... */ }
    }
    // Ensure all stubbed out methods (init, setupPopup, getWidgetAbsoluteWorldBoundsAtPoint, etc.) are filled with their complete prior implementations,
    // then apply scaling logic within them.
}
