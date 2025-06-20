package org.jetbrains.plugins.template.tkdesigner.ui

import com.intellij.ui.components.JBTextField
import com.intellij.ui.components.JBCheckBox
import javax.swing.JColorChooser
import com.intellij.openapi.fileChooser.FileChooser
import com.intellij.openapi.fileChooser.FileChooserDescriptor
import com.intellij.openapi.vfs.VfsUtil
import org.jetbrains.plugins.template.tkdesigner.ui.DesignAreaPanel
import org.jetbrains.plugins.template.tkdesigner.model.WidgetModel
import org.jetbrains.plugins.template.tkdesigner.model.DialogModel
import org.jetbrains.plugins.template.tkdesigner.model.DesignProject
import org.jetbrains.plugins.template.tkdesigner.DesignerSettings
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.beans.Introspector
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JComponent

/**
 * Panel showing properties of selected widget.
 */
import com.intellij.ui.DocumentAdapter
import javax.swing.event.DocumentEvent

class PropertyPanel(private val design: DesignAreaPanel, private val project: DesignProject) : JPanel(GridBagLayout()) {

    private var fields = mutableMapOf<String, JComponent>()
    private var currentWidget: WidgetModel? = null
    private var currentDialog: DialogModel? = null
    private var updating = false

    fun bind(widget: WidgetModel?) {
        removeAll()
        fields.clear()
        updating = true
        currentDialog = null
        currentWidget = widget

        if (widget == null) {
            revalidate()
            repaint()
            updating = false
            return
        }

        val c = GridBagConstraints()
        c.insets = java.awt.Insets(2, 2, 2, 2)
        c.anchor = GridBagConstraints.NORTHWEST

        var row = 0
        fun addField(name: String, value: String, type: Class<*>? = null) {
            val label = JLabel("$name:")
            val comp: JComponent = when {
                name == "layout" -> javax.swing.JComboBox(arrayOf("place", "pack", "grid")).apply {
                    selectedItem = value
                    addActionListener { applyChanges() }
                }
                type == java.awt.Color::class.java -> javax.swing.JButton().apply {
                    background = try { java.awt.Color.decode(value) } catch (_: Exception) { java.awt.Color.white }
                    addActionListener {
                        val col = JColorChooser.showDialog(this, "Choose Color", background)
                        col?.let { background = it; putClientProperty("value", String.format("#%06x", it.rgb and 0xffffff)) }
                        applyChanges()
                    }
                    putClientProperty("value", value)
                }
                type == Boolean::class.java || type == java.lang.Boolean::class.java -> JBCheckBox().apply {
                    isSelected = value.toBoolean()
                    addChangeListener { applyChanges() }
                }
                name.contains("image", true) || name.contains("icon", true) || name.contains("file", true) -> JBTextField(value).also { field ->
                    val browse = javax.swing.JButton("...")
                    browse.addActionListener {
                        val desc = FileChooserDescriptor(true, false, false, false, false, false)
                        val file = FileChooser.chooseFile(desc, null, null)
                        file?.let {
                            val io = VfsUtil.virtualToIoFile(it)
                            val path = if (project.basePath.isNotEmpty()) {
                                val base = java.nio.file.Paths.get(project.basePath)
                                base.relativize(io.toPath()).toString()
                            } else io.absolutePath
                            field.text = path
                            project.resources.add(path)
                            applyChanges()
                        }
                    }
                    val panel = javax.swing.JPanel(java.awt.BorderLayout())
                    panel.add(field, java.awt.BorderLayout.CENTER)
                    panel.add(browse, java.awt.BorderLayout.EAST)
                    fields[name] = field
                    c.gridx = 0; c.gridy = row; add(label, c)
                    c.gridx = 1; add(panel, c)
                    row++
                    return
                }
                name.contains("font", true) || type == java.awt.Font::class.java -> javax.swing.JButton(value).apply {
                    putClientProperty("value", value)
                    addActionListener {
                        val fonts = java.awt.GraphicsEnvironment.getLocalGraphicsEnvironment().availableFontFamilyNames
                        val combo = javax.swing.JComboBox(fonts)
                        combo.selectedItem = value
                        val size = javax.swing.JSpinner(javax.swing.SpinnerNumberModel(12, 6, 72, 1))
                        val p = javax.swing.JPanel().apply { add(combo); add(size) }
                        if (javax.swing.JOptionPane.showConfirmDialog(this, p, "Choose Font", javax.swing.JOptionPane.OK_CANCEL_OPTION) == javax.swing.JOptionPane.OK_OPTION) {
                            val v = "${'$'}{combo.selectedItem}-${'$'}{size.value}"
                            text = v
                            putClientProperty("value", v)
                            applyChanges()
                        }
                    }
                }
                name.startsWith("on_") -> JBTextField(value).also { field ->
                    val snippets = mapOf("Print" to "print('Clicked')", "Close" to "root.destroy")
                    val choose = javax.swing.JButton("...")
                    choose.addActionListener {
                        val opts = snippets.keys.toTypedArray()
                        val sel = javax.swing.JOptionPane.showInputDialog(this, "Snippet", "Choose", javax.swing.JOptionPane.PLAIN_MESSAGE, null, opts, opts.first())
                        if (sel != null) { field.text = snippets[sel] ?: ""; applyChanges() }
                    }
                    val panel = javax.swing.JPanel(java.awt.BorderLayout())
                    panel.add(field, java.awt.BorderLayout.CENTER)
                    panel.add(choose, java.awt.BorderLayout.EAST)
                    fields[name] = field
                    c.gridx = 0; c.gridy = row; add(label, c)
                    c.gridx = 1; add(panel, c)
                    row++
                    return
                }
                name == "text" -> JBTextField(value).also { field ->
                    val tButton = javax.swing.JButton("T")
                    tButton.addActionListener {
                        val key = javax.swing.JOptionPane.showInputDialog(this, "Translation key", widget.properties["textKey"] ?: "")
                        if (key != null && key.isNotBlank()) {
                            widget.properties["textKey"] = key
                            val map = project.translations.getOrPut("en") { mutableMapOf() }
                            map[key] = field.text
                        }
                    }
                    val panel = javax.swing.JPanel(java.awt.BorderLayout())
                    panel.add(field, java.awt.BorderLayout.CENTER)
                    panel.add(tButton, java.awt.BorderLayout.EAST)
                    fields[name] = field
                    c.gridx = 0; c.gridy = row; add(label, c)
                    c.gridx = 1; add(panel, c)
                    row++
                    return
                }
                else -> JBTextField(value)
            }
            fields[name] = comp
            c.gridx = 0
            c.gridy = row
            add(label, c)
            c.gridx = 1
            add(comp, c)
            row++
        }

        addField("x", widget.x.toString())
        addField("y", widget.y.toString())
        addField("width", widget.width.toString())
        addField("height", widget.height.toString())
        if (widget.children.isNotEmpty() || widget.type == "Frame" || widget.type == "Canvas") {
            addField("layout", widget.layout)
        }

        val designWidget = design.getDesignWidget(widget)
        designWidget?.let { dw ->
            val beanInfo = Introspector.getBeanInfo(dw.component.javaClass)
            for (pd in beanInfo.propertyDescriptors) {
                if (pd.writeMethod != null && pd.readMethod != null && pd.name != "class") {
                    val value = try { pd.readMethod.invoke(dw.component)?.toString() ?: "" } catch (e: Exception) { "" }
                    if (!fields.containsKey(pd.name)) {
                        addField(pd.name, value, pd.propertyType)
                    }
                }
            }
        }

        for ((k, v) in widget.properties) {
            if (k != "textKey" && !fields.containsKey(k)) addField(k, v)
        }
        for ((k, v) in widget.events) {
            val name = "on_$k"
            if (!fields.containsKey(name)) addField(name, v)
        }

        val listener = object : DocumentAdapter() {
            override fun textChanged(e: DocumentEvent) { if (!updating) applyChanges() }
        }
        for (field in fields.values) {
            when (field) {
                is JBTextField -> field.document.addDocumentListener(listener)
                is JBCheckBox -> field.addActionListener { if (!updating) applyChanges() }
                is javax.swing.JButton -> {} // handled internally
            }
        }

        updating = false
        revalidate()
        repaint()
    }

    fun bindDialog(dialog: DialogModel) {
        removeAll()
        fields.clear()
        updating = true
        currentWidget = null
        currentDialog = dialog

        val c = GridBagConstraints()
        c.insets = java.awt.Insets(2, 2, 2, 2)
        c.anchor = GridBagConstraints.NORTHWEST

        var row = 0
        fun addField(name: String, value: String) {
            val label = JLabel("$name:")
            val field = JBTextField(value)
            fields[name] = field
            c.gridx = 0
            c.gridy = row
            add(label, c)
            c.gridx = 1
            add(field, c)
            row++
        }

        addField("width", dialog.width.toString())
        addField("height", dialog.height.toString())
        addField("layout", dialog.layout)
        addField("gridSize", design.gridSize.toString())

        val listener = object : DocumentAdapter() {
            override fun textChanged(e: DocumentEvent) { if (!updating) applyChanges() }
        }
        for (field in fields.values) {
            if (field is JBTextField) field.document.addDocumentListener(listener)
            if (field is JBCheckBox) field.addActionListener { if (!updating) applyChanges() }
        }

        updating = false
        revalidate()
        repaint()
    }

    fun applyChanges() {
        currentWidget?.let { applyWidgetChanges(it) }
        currentDialog?.let { applyDialogChanges(it) }
    }

    private fun applyWidgetChanges(widget: WidgetModel) {
        val designWidget = design.getDesignWidget(widget)
        fields.forEach { (name, field) ->
            val value = when (field) {
                is JBTextField -> field.text
                is JBCheckBox -> field.isSelected.toString()
                is javax.swing.JButton -> field.getClientProperty("value")?.toString() ?: ""
                else -> ""
            }
            when (name) {
                "x" -> value.toIntOrNull()?.let { widget.x = it; designWidget?.component?.setLocation(it, widget.y) }
                "y" -> value.toIntOrNull()?.let { widget.y = it; designWidget?.component?.setLocation(widget.x, it) }
                "width" -> value.toIntOrNull()?.let { widget.width = it; designWidget?.component?.setSize(it, widget.height) }
                "height" -> value.toIntOrNull()?.let { widget.height = it; designWidget?.component?.setSize(widget.width, it) }
                "layout" -> { widget.layout = value }
                else -> {
                    if (name.startsWith("on_")) {
                        widget.events[name.substring(3)] = value
                    } else {
                        widget.properties[name] = value
                    }
                    designWidget?.let { dw ->
                        val pd = Introspector.getBeanInfo(dw.component.javaClass).propertyDescriptors.find { it.name == name }
                        if (pd?.writeMethod != null) {
                            try {
                                val converted = convertValue(value, pd.propertyType)
                                pd.writeMethod.invoke(dw.component, converted)
                            } catch (_: Exception) {
                            }
                        }
                    }
                }
            }
        }
        design.refreshWidget(widget)
    }

    private fun applyDialogChanges(dialog: DialogModel) {
        fields.forEach { (name, field) ->
            val value = when (field) {
                is JBTextField -> field.text
                is JBCheckBox -> field.isSelected.toString()
                else -> ""
            }
            when (name) {
                "width" -> value.toIntOrNull()?.let { dialog.width = it }
                "height" -> value.toIntOrNull()?.let { dialog.height = it }
                "layout" -> { dialog.layout = value }
                "gridSize" -> value.toIntOrNull()?.let { design.gridSize = it; org.jetbrains.plugins.template.tkdesigner.DesignerSettings.instance().state.gridSize = it }
            }
        }
        design.preferredSize = java.awt.Dimension(dialog.width, dialog.height)
        design.revalidate()
        design.repaint()
    }

    private fun convertValue(value: String, type: Class<*>): Any? = when (type) {
        Int::class.javaPrimitiveType, Int::class.javaObjectType -> value.toIntOrNull() ?: 0
        Float::class.javaPrimitiveType, Float::class.javaObjectType -> value.toFloatOrNull() ?: 0f
        Double::class.javaPrimitiveType, Double::class.javaObjectType -> value.toDoubleOrNull() ?: 0.0
        Boolean::class.javaPrimitiveType, Boolean::class.javaObjectType -> value.toBoolean()
        else -> value
    }
}
