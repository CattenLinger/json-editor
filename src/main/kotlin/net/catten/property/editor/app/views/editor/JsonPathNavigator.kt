package net.catten.property.editor.app.views.editor

import net.catten.property.editor.utils.swingInvokeLater
import javax.swing.*

class JsonPathNavigator {
    private val tfPath = JTextField()

//    val btnGoPath = JButton("Go")

    val frame = JPanel().apply {
        layout = BoxLayout(this, BoxLayout.X_AXIS)
        // https://stackoverflow.com/questions/5854005/setting-horizontal-and-vertical-margins
//        border = BorderFactory.createEmptyBorder(0, 3, 3, 3)
        border = BorderFactory.createEmptyBorder(3, 3, 3, 3)

        add(JLabel("Path: "))
        add(tfPath)

        tfPath.text = "Select a item..."
        tfPath.isEnabled = false
//        add(btnGoPath)
    }

    fun updateDisplayPath(newPath : String) = swingInvokeLater {
        tfPath.text = newPath
        tfPath.isEnabled = true
    }

    fun updateMultipleDisplayPath(count : Int) = swingInvokeLater {
        tfPath.isEnabled = false
        tfPath.text = "<Selected $count items...>"
    }
}