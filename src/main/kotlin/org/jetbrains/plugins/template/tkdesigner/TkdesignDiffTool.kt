package org.jetbrains.plugins.template.tkdesigner

import com.google.gson.Gson
import com.intellij.diff.DiffContext
import com.intellij.diff.FrameDiffTool
import com.intellij.diff.requests.ContentDiffRequest
import com.intellij.diff.requests.DiffRequest
import com.intellij.util.ui.UIUtil
import java.awt.BorderLayout
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.JTextArea

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
        val diff = buildDiff(leftText, rightText)
        val area = JTextArea(diff).apply { isEditable = false; background = UIUtil.getPanelBackground() }
        val panel = JPanel().apply { layout = java.awt.BorderLayout(); add(area, java.awt.BorderLayout.CENTER) }
        return object : FrameDiffTool.DiffViewer {
            override fun getComponent(): JComponent = panel
            override fun getPreferredFocusedComponent(): JComponent? = null
            override fun init() = FrameDiffTool.ToolbarComponents()
            override fun dispose() {}
        }
    }

    private fun buildDiff(left: String, right: String): String {
        val gson = Gson()
        val leftMap = gson.fromJson(left, Map::class.java) as Map<String, Any?>
        val rightMap = gson.fromJson(right, Map::class.java) as Map<String, Any?>
        return buildString { compareMaps(leftMap, rightMap, "") }
    }

    private fun StringBuilder.compareMaps(a: Map<String, Any?>, b: Map<String, Any?>, prefix: String) {
        for (key in a.keys union b.keys) {
            val av = a[key]
            val bv = b[key]
            if (av is Map<*, *> && bv is Map<*, *>) {
                compareMaps(av as Map<String, Any?>, bv as Map<String, Any?>, "$prefix$key.")
            } else if (av != bv) {
                appendLine("$prefix$key: $av -> $bv")
            }
        }
    }
}
