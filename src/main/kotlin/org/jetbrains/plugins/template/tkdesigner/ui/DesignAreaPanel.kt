package org.jetbrains.plugins.template.tkdesigner.ui

import org.jetbrains.plugins.template.tkdesigner.model.DialogModel
import org.jetbrains.plugins.template.tkdesigner.model.WidgetModel
import java.awt.*
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.JComponent
import javax.swing.JLayeredPane

/**
 * Panel used as design surface for Tkinter dialogs.
 */
class DesignAreaPanel(var model: DialogModel) : JLayeredPane() {

    var selectionListener: ((WidgetModel?) -> Unit)? = null
    var dialogListener: ((DialogModel) -> Unit)? = null

    private val designWidgets = mutableListOf<DesignWidget>()

    private val selectedWidgets = mutableSetOf<DesignWidget>()

    private val history = mutableListOf<DialogModel>()
    private var historyIndex = -1

    var gridSize = 10

    private var pendingAddType: String? = null
    private var dragWidget: DesignWidget? = null
    private var dragStartX = 0
    private var dragStartY = 0
    private var dragInitial: Map<DesignWidget, Rectangle> = emptyMap()
    private var overlay: String? = null
    private var guideX: Int? = null
    private var guideY: Int? = null

    var selected: DesignWidget? = null
        private set

    override fun paintComponent(g: Graphics) {
        super.paintComponent(g)
        g.color = Color.LIGHT_GRAY
        var x = 0
        while (x < width) {
            g.drawLine(x, 0, x, height)
            x += gridSize
        }
        var y = 0
        while (y < height) {
            g.drawLine(0, y, width, y)
            y += gridSize
        }
        g.color = Color.DARK_GRAY
        var rx = 0
        while (rx < width) {
            g.drawString(rx.toString(), rx + 2, 10)
            rx += 50
        }
        var ry = 0
        while (ry < height) {
            g.drawString(ry.toString(), 2, ry + 20)
            ry += 50
        }
        overlay?.let {
            g.color = Color.BLACK
            g.drawString(it, 10, height - 10)
        }
        guideX?.let {
            g.color = Color.RED
            g.drawLine(it, 0, it, height)
        }
        guideY?.let {
            g.color = Color.RED
            g.drawLine(0, it, width, it)
        }
    }

    init {
        layout = null
        background = Color.WHITE
        preferredSize = Dimension(model.width, model.height)
        isOpaque = true
        addMouseListener(object : MouseAdapter() {
            override fun mousePressed(e: MouseEvent) {
                if (pendingAddType != null && e.button == MouseEvent.BUTTON1) {
                    val m = WidgetModel(pendingAddType!!, e.x, e.y, 1, 1)
                    val comp = createComponent(m)
                    val dw = DesignWidget(m, comp)
                    addWidget(dw, attachListeners = false, recordHistory = false)
                    dragWidget = dw
                    dragStartX = e.x
                    dragStartY = e.y
                    overlay = null
                    guideX = null
                    guideY = null
                } else if (e.button == MouseEvent.BUTTON1) {
                    val clicked = designWidgets.findLast { it.component.bounds.contains(e.point) }
                    if (clicked != null) {
                        if (e.isShiftDown) {
                            if (selectedWidgets.contains(clicked)) selectedWidgets.remove(clicked) else selectedWidgets.add(clicked)
                        } else {
                            selectedWidgets.clear(); selectedWidgets.add(clicked)
                        }
                        selected = clicked
                        selectionListener?.invoke(clicked.model)
                        dragInitial = selectedWidgets.associateWith { it.component.bounds }
                        dragStartX = e.x
                        dragStartY = e.y
                        overlay = null
                    } else {
                        selectedWidgets.clear();
                        selected = null
                        selectionListener?.invoke(null)
                        dialogListener?.invoke(model)
                    }
                }
            }

            override fun mouseReleased(e: MouseEvent) {
                if (dragWidget != null) {
                    installListeners(dragWidget!!)
                    selectedWidgets.clear();
                    selected = dragWidget
                    selectedWidgets.add(dragWidget!!)
                    selectionListener?.invoke(dragWidget!!.model)
                    dragWidget = null
                    pendingAddType = null
                    pushHistory()
                    overlay = null
                    guideX = null
                    guideY = null
                }
            }
        })

        addMouseMotionListener(object : MouseAdapter() {
            override fun mouseDragged(e: MouseEvent) {
                dragWidget?.let { widget ->
                    val x1 = e.x
                    val y1 = e.y
                    val nx = snap(minOf(x1, dragStartX))
                    val ny = snap(minOf(y1, dragStartY))
                    val nw = snap(kotlin.math.abs(x1 - dragStartX))
                    val nh = snap(kotlin.math.abs(y1 - dragStartY))
                    var ax = nx
                    var ay = ny
                    guideX = null; guideY = null
                    for (dw in designWidgets) {
                        if (dw === widget) continue
                        if (kotlin.math.abs(nx - dw.component.x) < 5) { ax = dw.component.x; guideX = ax }
                        if (kotlin.math.abs(ny - dw.component.y) < 5) { ay = dw.component.y; guideY = ay }
                    }
                    widget.component.setBounds(ax, ay, nw, nh)
                    widget.model.x = ax
                    widget.model.y = ay
                    widget.model.width = nw
                    widget.model.height = nh
                    overlay = "${nw}x$nh"
                    repaint()
                }
            }
        })

        val im = inputMap
        val am = actionMap
        im.put(javax.swing.KeyStroke.getKeyStroke("control Z"), "undo")
        im.put(javax.swing.KeyStroke.getKeyStroke("control Y"), "redo")
        im.put(javax.swing.KeyStroke.getKeyStroke("control C"), "copy")
        im.put(javax.swing.KeyStroke.getKeyStroke("control V"), "paste")
        im.put(javax.swing.KeyStroke.getKeyStroke("control D"), "dup")
        am.put("undo", object : javax.swing.AbstractAction() { override fun actionPerformed(e: java.awt.event.ActionEvent?) { undo() } })
        am.put("redo", object : javax.swing.AbstractAction() { override fun actionPerformed(e: java.awt.event.ActionEvent?) { redo() } })
        am.put("copy", object : javax.swing.AbstractAction() { override fun actionPerformed(e: java.awt.event.ActionEvent?) { copySelection() } })
        am.put("paste", object : javax.swing.AbstractAction() { override fun actionPerformed(e: java.awt.event.ActionEvent?) { paste() } })
        am.put("dup", object : javax.swing.AbstractAction() { override fun actionPerformed(e: java.awt.event.ActionEvent?) { duplicate() } })
    }

    fun beginAddWidget(type: String) {
        pendingAddType = type
    }

    fun addWidget(widget: DesignWidget, attachListeners: Boolean = true, recordHistory: Boolean = true): DesignWidget {
        if (widget.model.parent == null) {
            model.widgets.add(widget.model)
            add(widget.component, Integer(0))
        } else {
            getDesignWidget(widget.model.parent!!)?.component?.add(widget.component)
            widget.model.parent!!.children.add(widget.model)
        }
        designWidgets.add(widget)
        widget.component.setBounds(widget.model.x, widget.model.y, widget.model.width, widget.model.height)
        revalidate()
        repaint()
        if (attachListeners) installListeners(widget)
        if (recordHistory) pushHistory()
        return widget
    }

    fun addWidget(model: WidgetModel, attachListeners: Boolean = true, recordHistory: Boolean = true) {
        val comp = createComponent(model)
        val dw = DesignWidget(model, comp)
        addWidget(dw, attachListeners, recordHistory)
        model.children.forEach { child ->
            child.parent = model
            addWidget(child, attachListeners, recordHistory)
        }
    }

    fun getDesignWidget(model: WidgetModel): DesignWidget? =
        designWidgets.find { it.model === model }

    fun selectModel(model: WidgetModel) {
        val dw = getDesignWidget(model) ?: return
        selectedWidgets.clear()
        selected = dw
        selectedWidgets.add(dw)
        selectionListener?.invoke(model)
        repaint()
    }

    fun clear() {
        removeAll()
        designWidgets.clear()
        model.widgets.clear()
        selected = null
        selectionListener?.invoke(null)
        repaint()
    }

    fun loadModel(newModel: DialogModel) {
        clear()
        model = newModel
        preferredSize = Dimension(model.width, model.height)
        for (w in model.widgets) {
            addWidget(w, recordHistory = false)
        }
    }

    private fun snap(value: Int): Int = (value / gridSize) * gridSize

    private fun pushHistory() {
        if (historyIndex < history.size - 1) {
            history.subList(historyIndex + 1, history.size).clear()
        }
        fun cloneWidget(w: WidgetModel): WidgetModel = w.copy(
            properties = w.properties.toMutableMap(),
            events = w.events.toMutableMap(),
            children = w.children.map { cloneWidget(it) }.toMutableList(),
            parent = null
        )

        val copy = model.copy(
            width = model.width,
            height = model.height,
            widgets = model.widgets.map { cloneWidget(it) }.toMutableList()
        )
        history.add(copy)
        historyIndex = history.size - 1
    }

    fun undo() {
        if (historyIndex > 0) {
            historyIndex--
            loadModel(history[historyIndex])
        }
    }

    fun redo() {
        if (historyIndex < history.size - 1) {
            historyIndex++
            loadModel(history[historyIndex])
        }
    }

    private fun createComponent(model: WidgetModel): JComponent = when (model.type) {
        "Button" -> javax.swing.JButton(model.properties["text"] ?: "Button")
        "Label" -> javax.swing.JLabel(model.properties["text"] ?: "Label")
        "Entry" -> javax.swing.JTextField()
        "Text" -> javax.swing.JTextArea()
        "Frame" -> javax.swing.JPanel().apply { border = javax.swing.BorderFactory.createLineBorder(Color.GRAY) }
        "Canvas" -> javax.swing.JPanel().apply { background = Color.WHITE; border = javax.swing.BorderFactory.createLineBorder(Color.GRAY) }
        "Checkbutton" -> javax.swing.JCheckBox(model.properties["text"] ?: "Check")
        "Radiobutton" -> javax.swing.JRadioButton(model.properties["text"] ?: "Radio")
        "Listbox" -> javax.swing.JList<String>()
        "Scale" -> javax.swing.JSlider()
        "Spinbox" -> javax.swing.JSpinner()
        else -> javax.swing.JPanel()
    }

    private fun installListeners(widget: DesignWidget) {
        val comp = widget.component
        val listener = object : MouseAdapter() {
            var dragOffsetX = 0
            var dragOffsetY = 0
            var resizing = false

            override fun mousePressed(e: MouseEvent) {
                if (!e.isShiftDown) selectedWidgets.clear()
                selectedWidgets.add(widget)
                selected = widget
                selectionListener?.invoke(widget.model)
                dragOffsetX = e.x
                dragOffsetY = e.y
                resizing = e.x >= comp.width - 10 && e.y >= comp.height - 10
                dragInitial = selectedWidgets.associateWith { it.component.bounds }
                overlay = null
            }

            override fun mouseDragged(e: MouseEvent) {
                if (resizing) {
                    val dx = snap(e.x) - dragOffsetX
                    val dy = snap(e.y) - dragOffsetY
                    selectedWidgets.forEach { w ->
                        val start = dragInitial[w] ?: w.component.bounds
                        val newW = snap(start.width + dx)
                        val newH = snap(start.height + dy)
                        w.component.setSize(newW, newH)
                        w.model.width = newW
                        w.model.height = newH
                    }
                    overlay = "${selected!!.component.width}x${selected!!.component.height}"
                } else {
                    val dx = snap(widget.component.x + e.x - dragOffsetX) - widget.component.x
                    val dy = snap(widget.component.y + e.y - dragOffsetY) - widget.component.y
                    selectedWidgets.forEach { w ->
                        val start = dragInitial[w] ?: w.component.bounds
                        val nx = snap(start.x + dx)
                        val ny = snap(start.y + dy)
                        w.component.setLocation(nx, ny)
                        w.model.x = nx
                        w.model.y = ny
                    }
                    overlay = "(${selected!!.component.x},${selected!!.component.y})"
                }
                repaint()
            }

            override fun mouseReleased(e: MouseEvent) {
                pushHistory()
                overlay = null
                guideX = null
                guideY = null
            }
        }
        comp.addMouseListener(listener)
        comp.addMouseMotionListener(listener)
    }

    fun refreshWidget(model: WidgetModel) {
        designWidgets.find { it.model === model }?.let { widget ->
            when (val c = widget.component) {
                is javax.swing.JButton -> c.text = model.properties["text"] ?: "Button"
                is javax.swing.JLabel -> c.text = model.properties["text"] ?: "Label"
            }
            widget.component.setBounds(model.x, model.y, model.width, model.height)
            revalidate()
            repaint()
        }
    }

    fun alignLeft() {
        if (selectedWidgets.isEmpty()) return
        val x = selectedWidgets.minOf { it.component.x }
        for (w in selectedWidgets) {
            w.component.setLocation(x, w.component.y)
            w.model.x = x
        }
        pushHistory()
        repaint()
    }

    fun alignTop() {
        if (selectedWidgets.isEmpty()) return
        val y = selectedWidgets.minOf { it.component.y }
        for (w in selectedWidgets) {
            w.component.setLocation(w.component.x, y)
            w.model.y = y
        }
        pushHistory()
        repaint()
    }

    fun alignRight() {
        if (selectedWidgets.isEmpty()) return
        val right = selectedWidgets.maxOf { it.component.x + it.component.width }
        for (w in selectedWidgets) {
            val x = right - w.component.width
            w.component.setLocation(x, w.component.y)
            w.model.x = x
        }
        pushHistory()
        repaint()
    }

    fun alignBottom() {
        if (selectedWidgets.isEmpty()) return
        val bottom = selectedWidgets.maxOf { it.component.y + it.component.height }
        for (w in selectedWidgets) {
            val yb = bottom - w.component.height
            w.component.setLocation(w.component.x, yb)
            w.model.y = yb
        }
        pushHistory()
        repaint()
    }

    fun alignCenterHorizontal() {
        if (selectedWidgets.isEmpty()) return
        val left = selectedWidgets.minOf { it.component.x }
        val right = selectedWidgets.maxOf { it.component.x + it.component.width }
        val center = (left + right) / 2
        for (w in selectedWidgets) {
            val x = center - w.component.width / 2
            w.component.setLocation(x, w.component.y)
            w.model.x = x
        }
        pushHistory()
        repaint()
    }

    fun alignCenterVertical() {
        if (selectedWidgets.isEmpty()) return
        val top = selectedWidgets.minOf { it.component.y }
        val bottom = selectedWidgets.maxOf { it.component.y + it.component.height }
        val center = (top + bottom) / 2
        for (w in selectedWidgets) {
            val y = center - w.component.height / 2
            w.component.setLocation(w.component.x, y)
            w.model.y = y
        }
        pushHistory()
        repaint()
    }

    fun distributeHorizontal() {
        if (selectedWidgets.size < 3) return
        val sorted = selectedWidgets.sortedBy { it.component.x }
        val first = sorted.first()
        val last = sorted.last()
        val step = (last.component.x - first.component.x) / (sorted.size - 1)
        sorted.forEachIndexed { i, dw ->
            val x = first.component.x + i * step
            dw.component.setLocation(x, dw.component.y)
            dw.model.x = x
        }
        pushHistory(); repaint()
    }

    fun distributeVertical() {
        if (selectedWidgets.size < 3) return
        val sorted = selectedWidgets.sortedBy { it.component.y }
        val first = sorted.first()
        val last = sorted.last()
        val step = (last.component.y - first.component.y) / (sorted.size - 1)
        sorted.forEachIndexed { i, dw ->
            val y = first.component.y + i * step
            dw.component.setLocation(dw.component.x, y)
            dw.model.y = y
        }
        pushHistory(); repaint()
    }

    private var clipboard: List<WidgetModel> = emptyList()

    fun copySelection() {
        clipboard = selectedWidgets.map { it.model.copy(properties = it.model.properties.toMutableMap(), events = it.model.events.toMutableMap()) }
    }

    fun paste() {
        clipboard.forEach { m ->
            val copy = m.copy(x = m.x + 10, y = m.y + 10, properties = m.properties.toMutableMap(), events = m.events.toMutableMap())
            addWidget(copy)
        }
    }

    fun duplicate() {
        copySelection(); paste()
    }

    fun groupSelected() {
        if (selectedWidgets.size < 2) return
        val iterator = selectedWidgets.iterator()
        val first = iterator.next()
        val bounds = Rectangle(first.component.bounds)
        iterator.forEachRemaining { bounds.add(it.component.bounds) }
        val groupModel = WidgetModel("Frame", bounds.x, bounds.y, bounds.width, bounds.height)
        addWidget(groupModel, recordHistory = false)
        val groupWidget = designWidgets.last()
        selectedWidgets.forEach { dw ->
            remove(dw.component)
            model.widgets.remove(dw.model)
            designWidgets.remove(dw)
            dw.model.x -= bounds.x
            dw.model.y -= bounds.y
            dw.model.parent = groupModel
            groupModel.children.add(dw.model)
            (groupWidget.component as java.awt.Container).add(dw.component)
        }
        selectedWidgets.clear()
        selectedWidgets.add(groupWidget)
        selected = groupWidget
        pushHistory()
        repaint()
    }

    data class DesignWidget(val model: WidgetModel, val component: JComponent)
}
