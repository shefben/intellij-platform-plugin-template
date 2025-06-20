package org.jetbrains.plugins.template.tkdesigner.ui

import org.jetbrains.plugins.template.tkdesigner.model.WidgetModel
import java.awt.BorderLayout
import javax.swing.JPanel
import javax.swing.JScrollPane
import javax.swing.JTree
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeModel
import javax.swing.tree.TreePath

/** Panel showing a tree representation of the widget hierarchy. */
class HierarchyPanel(private val design: DesignAreaPanel) : JPanel(BorderLayout()) {
    private val rootNode = DefaultMutableTreeNode("Dialog")
    private val treeModel = DefaultTreeModel(rootNode)
    private val tree = JTree(treeModel)

    init {
        add(JScrollPane(tree), BorderLayout.CENTER)
        tree.addTreeSelectionListener {
            val path = it.path
            val node = path.lastPathComponent as? DefaultMutableTreeNode
            val model = node?.userObject as? WidgetModel
            model?.let { m -> design.selectModel(m) }
        }
    }

    fun refresh() {
        rootNode.removeAllChildren()
        design.model.widgets.forEach { addNode(rootNode, it) }
        treeModel.reload()
    }

    private fun addNode(parent: DefaultMutableTreeNode, w: WidgetModel) {
        val node = DefaultMutableTreeNode(w)
        parent.add(node)
        w.children.forEach { addNode(node, it) }
    }
}
