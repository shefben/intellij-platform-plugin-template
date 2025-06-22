package org.jetbrains.plugins.template.tkdesigner

import com.google.gson.Gson
import com.intellij.diff.DiffContext
import com.intellij.diff.FrameDiffTool
import com.intellij.diff.requests.ContentDiffRequest
import com.intellij.diff.requests.DiffRequest
import com.intellij.util.ui.UIUtil
import java.awt.BorderLayout
import javax.swing.*
import javax.swing.table.AbstractTableModel
import javax.swing.table.DefaultTableCellRenderer

/** Simple diff viewer for .tkdesign files highlighting changed properties. */
class TkdesignDiffTool : FrameDiffTool {
    override fun getName(): String = "Tkdesign Diff"

    override fun canShow(context: DiffContext, request: DiffRequest): Boolean {
        if (request !is ContentDiffRequest) return false
        val files = request.contents.mapNotNull { (it as? com.intellij.diff.contents.FileContent)?.file }
        return files.size == 2 && files.all { it.extension == "tkdesign" }
    }

    override fun createComponent(context: DiffContext, request: DiffRequest): FrameDiffTool.DiffViewer {
        request as ContentDiffRequest
        val leftText = (request.contents[0] as? com.intellij.diff.contents.DocumentContent)?.document?.text ?: ""
        val rightText = (request.contents[1] as? com.intellij.diff.contents.DocumentContent)?.document?.text ?: ""
        val entries = collectDiff(leftText, rightText)
        val model = object : AbstractTableModel() {
            override fun getRowCount() = entries.size
            override fun getColumnCount() = 3
            override fun getColumnName(col: Int) = arrayOf("Path", "Before", "After")[col]
            override fun getValueAt(rowIndex: Int, columnIndex: Int): Any {
                val e = entries[rowIndex]
                return when (columnIndex) {
                    0 -> e.path
                    1 -> e.left ?: ""
                    else -> e.right ?: ""
                }
            }
        }
        val table = JTable(model).apply {
            setDefaultRenderer(Object::class.java, object : DefaultTableCellRenderer() {
                override fun getTableCellRendererComponent(
                    table: JTable,
                    value: Any?,
                    isSelected: Boolean,
                    hasFocus: Boolean,
                    row: Int,
                    column: Int
                ): java.awt.Component {
                    val comp = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column)
                    if (column > 0) {
                        val left = entries[row].left?.toString() ?: ""
                        val right = entries[row].right?.toString() ?: ""
                        if (left != right) {
                            comp.background = if (column == 1) java.awt.Color(0xFFEEEE) else java.awt.Color(0xEEFFEE)
                        }
                    }
                    return comp
                }
            })
        }
        val split = JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
            JScrollPane(JTextArea(leftText).apply { isEditable = false; background = UIUtil.getPanelBackground() }),
            JScrollPane(JTextArea(rightText).apply { isEditable = false; background = UIUtil.getPanelBackground() }))
        split.resizeWeight = 0.5
        val panel = JPanel(BorderLayout()).apply {
            add(split, BorderLayout.CENTER)
            add(JScrollPane(table), BorderLayout.SOUTH)
        }
        return object : FrameDiffTool.DiffViewer {
            override fun getComponent(): JComponent = panel
            override fun getPreferredFocusedComponent(): JComponent? = null
            override fun init() = FrameDiffTool.ToolbarComponents()
            override fun dispose() {}
        }
    }

    data class DiffEntry(val path: String, val left: Any?, val right: Any?)

    internal fun collectDiff(left: String, right: String): List<DiffEntry> {
        val gson = Gson()
        val leftMap = gson.fromJson(left, Map::class.java) as Map<String, Any?>? ?: emptyMap<String, Any?>()
        val rightMap = gson.fromJson(right, Map::class.java) as Map<String, Any?>? ?: emptyMap<String, Any?>()
        val result = mutableListOf<DiffEntry>()
        fun walk(a: Map<String, Any?>, b: Map<String, Any?>, prefix: String) {
            for (key in a.keys union b.keys) {
                val av = a[key]
                val bv = b[key]
                if (av is Map<*, *> && bv is Map<*, *>) {
                    walk(av as Map<String, Any?>, bv as Map<String, Any?>, "$prefix$key.")
                } else if (av != bv) {
                    result += DiffEntry(prefix + key, av, bv)
                }
            }
        }
        walk(leftMap, rightMap, "")
        return result
    }
}
