package net.catten.property.editor.app.editor

import javax.swing.*

class JsonPathNavigator {
    private val tfPath = JTextField()

    val btnGoPath = JButton("Go")

    val frame = JPanel().apply {
        layout = BoxLayout(this, BoxLayout.X_AXIS)
        border = BorderFactory.createEmptyBorder(0, 3, 3, 3)

        add(JLabel("Path: "))
        add(tfPath)
        add(btnGoPath)
    }

    fun updateDisplayPath(newPath : String) = SwingUtilities.invokeLater {
        tfPath.text = newPath
    }
}