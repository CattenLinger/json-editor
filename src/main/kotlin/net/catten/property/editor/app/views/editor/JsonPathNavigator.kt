package net.catten.property.editor.app.views.editor

import net.catten.property.editor.utils.swingInvokeLater
import java.awt.event.KeyListener
import javax.swing.*
import javax.swing.text.DefaultEditorKit

class JsonPathNavigator {
    private val popupMenu = JPopupMenu()
    private val tfPath = JTextField()

    init {
        tfPath.isEditable = false
        val copyMenuItem = JMenuItem("Copy Path").apply {
            mnemonic = 'C'.code
            addActionListener(DefaultEditorKit.CopyAction())
        }
        popupMenu.add(copyMenuItem)
        tfPath.componentPopupMenu = popupMenu
        
    }

//    val btnGoPath = JButton("Go")

    val frame = JPanel().apply {
        layout = BoxLayout(this, BoxLayout.X_AXIS)
        // https://stackoverflow.com/questions/5854005/setting-horizontal-and-vertical-margins
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