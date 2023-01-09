package net.catten.property.editor.app.editor

import java.awt.BorderLayout
import javax.swing.BorderFactory
import javax.swing.BoxLayout
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JScrollPane
import javax.swing.JTable
import javax.swing.JTextField
import javax.swing.tree.DefaultMutableTreeNode

class JsonValueEditorView {

    val frame = JPanel().apply {
        layout = BorderLayout(3, 3)
        // https://stackoverflow.com/questions/5854005/setting-horizontal-and-vertical-margins
//        border = BorderFactory.createEmptyBorder(0, 3, 3, 3)
    }

    val table = JTable(
        arrayOf(
            arrayOf("integer", "key1", "value1"),
            arrayOf("decimal", "key2", "value2"),
            arrayOf("boolean", "key3", "value3"),
            arrayOf("string", "key4", "value4"),
            arrayOf("null", "key5", "value5"),
        ),
        arrayOf("Type", "Key", "Value")
    ).apply {
        fillsViewportHeight = true
        frame.add(JScrollPane(this), BorderLayout.CENTER)
    }

    fun setCurrentEditingNode(defaultMutableTreeNode: DefaultMutableTreeNode) {
        val node = defaultMutableTreeNode.userObject
    }
}