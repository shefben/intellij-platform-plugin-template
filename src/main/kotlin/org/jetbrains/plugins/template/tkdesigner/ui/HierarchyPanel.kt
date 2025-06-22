package org.jetbrains.plugins.template.tkdesigner.ui

import org.jetbrains.plugins.template.tkdesigner.model.WidgetModel
import java.awt.BorderLayout
import javax.swing.JPanel
import javax.swing.JScrollPane
import javax.swing.JTree
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeModel
import javax.swing.tree.TreePath
import javax.swing.event.TreeSelectionEvent
import javax.swing.event.TreeSelectionListener
import javax.swing.JOptionPane
import javax.swing.tree.DefaultTreeCellRenderer

/** Panel showing a tree representation of the widget hierarchy. */
class HierarchyPanel(private val design: DesignAreaPanel) : JPanel(BorderLayout()) {
    private val rootNode = DefaultMutableTreeNode("Dialog")
    private val treeModel = DefaultTreeModel(rootNode)
    private val tree = JTree(treeModel)
    
    init {
        add(JScrollPane(tree), BorderLayout.CENTER)
        tree.isEditable = true
        tree.dragEnabled = true
        tree.dropMode = javax.swing.DropMode.ON_OR_INSERT
        tree.transferHandler = object : javax.swing.TransferHandler() {
            override fun getSourceActions(c: javax.swing.JComponent?) = MOVE

            override fun createTransferable(c: javax.swing.JComponent?): java.awt.datatransfer.Transferable {
                val node = tree.selectionPath?.lastPathComponent as? DefaultMutableTreeNode ?: return object : java.awt.datatransfer.StringSelection("") {}
                return object : java.awt.datatransfer.StringSelection(node.toString()) {
                    val dragged = node
                    override fun lostOwnership(clipboard: java.awt.datatransfer.Clipboard?, contents: java.awt.datatransfer.Transferable?) {}
                }
            }

            override fun importData(support: javax.swing.TransferHandler.TransferSupport): Boolean {
                val drop = support.dropLocation as? JTree.DropLocation ?: return false
                val target = drop.path.lastPathComponent as? DefaultMutableTreeNode ?: return false
                val dragged = tree.selectionPath?.lastPathComponent as? DefaultMutableTreeNode ?: return false
                if (dragged === target || dragged.isNodeAncestor(target)) return false

                val dModel = dragged.userObject as? WidgetModel ?: return false
                val tModel = target.userObject as? WidgetModel

                (dragged.parent as? DefaultMutableTreeNode)?.remove(dragged)
                treeModel.nodesWereRemoved(dragged.parent as DefaultMutableTreeNode, intArrayOf(0), arrayOf(dragged))
                target.add(dragged)
                treeModel.reload(target)

                dModel.parent?.children?.remove(dModel) ?: design.model.widgets.remove(dModel)
                if (tModel == null) { design.model.widgets.add(dModel); dModel.parent = null } else { dModel.parent = tModel; tModel.children.add(dModel) }
                design.refreshWidget(dModel)
                design.revalidate(); design.repaint()
                return true
            }
        }
        tree.cellRenderer = object : DefaultTreeCellRenderer() {
            override fun getTreeCellRendererComponent(tree: JTree?, value: Any?, selected: Boolean, expanded: Boolean, leaf: Boolean, row: Int, hasFocus: Boolean): java.awt.Component {
                val c = super.getTreeCellRendererComponent(tree, value, selected, expanded, leaf, row, hasFocus)
                val node = value as DefaultMutableTreeNode
                val w = node.userObject as? WidgetModel
                if (w != null) text = w.properties["name"] ?: w.type
                return c
            }
        }
        tree.addTreeSelectionListener(TreeSelectionListener { e: TreeSelectionEvent ->
            val path = e.path
            val node = path.lastPathComponent as? DefaultMutableTreeNode
            val model = node?.userObject as? WidgetModel
            model?.let { m -> design.selectModel(m) }
        })

        val menu = javax.swing.JPopupMenu()
        val rename = javax.swing.JMenuItem("Rename")
        val delete = javax.swing.JMenuItem("Delete")
        menu.add(rename); menu.add(delete)
        rename.addActionListener {
            val node = tree.selectionPath?.lastPathComponent as? DefaultMutableTreeNode ?: return@addActionListener
            val w = node.userObject as? WidgetModel ?: return@addActionListener
            val newName = JOptionPane.showInputDialog(this, "New name", w.properties["name"] ?: w.type)
            if (newName != null) { w.properties["name"] = newName; treeModel.nodeChanged(node) }
        }
        delete.addActionListener {
            val node = tree.selectionPath?.lastPathComponent as? DefaultMutableTreeNode ?: return@addActionListener
            val w = node.userObject as? WidgetModel ?: return@addActionListener
            design.selectModel(w)
            design.deleteSelected()
            refresh()
        }
        tree.addMouseListener(object : java.awt.event.MouseAdapter() {
            override fun mousePressed(e: java.awt.event.MouseEvent) {
                if (e.isPopupTrigger || e.button == java.awt.event.MouseEvent.BUTTON3) {
                    tree.selectionPath = tree.getPathForLocation(e.x, e.y)
                    menu.show(tree, e.x, e.y)
                }
            }
            override fun mouseReleased(e: java.awt.event.MouseEvent) {
                if (e.isPopupTrigger || e.button == java.awt.event.MouseEvent.BUTTON3) {
                    tree.selectionPath = tree.getPathForLocation(e.x, e.y)
                    menu.show(tree, e.x, e.y)
                }
            }
        })
    }

    fun refresh() {
        rootNode.removeAllChildren()
        design.model.widgets.forEach { addNode(rootNode, it) }
        treeModel.reload()
        tree.expandRow(0)
    }

    private fun addNode(parent: DefaultMutableTreeNode, w: WidgetModel) {
        val node = DefaultMutableTreeNode(w)
        parent.add(node)
        w.children.forEach { addNode(node, it) }
    }
}
