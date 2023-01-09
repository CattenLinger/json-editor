package net.catten.property.editor.utils

import java.awt.Component
import javax.swing.JTree
import javax.swing.tree.DefaultTreeCellRenderer
import javax.swing.tree.TreeNode

fun jTree(root: TreeNode, builder: JTree.() -> Unit) = JTree(root).apply(builder)

/* adapters */

typealias TreeCellRendererFunction = DefaultTreeCellRenderer.(tree: JTree, value: Any?, sel: Boolean, expanded: Boolean, leaf: Boolean, row: Int, hasFocus: Boolean) -> Unit

open class CustomTreeCellRenderer(private val componentProvider: TreeCellRendererFunction) : DefaultTreeCellRenderer() {
    override fun getTreeCellRendererComponent(
        tree: JTree, value: Any?, sel: Boolean, expanded: Boolean, leaf: Boolean, row: Int, hasFocus: Boolean
    ): Component {
        super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus)
        componentProvider(tree, value, sel, expanded, leaf, row, hasFocus)
        return this
    }

    companion object {
        fun forValue(renderer : DefaultTreeCellRenderer.(value : Any) -> String) = CustomTreeCellRenderer { _, value, _, _, _, _, _ ->
            text = renderer(value!!)
        }
    }
}