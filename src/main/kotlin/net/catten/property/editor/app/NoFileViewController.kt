package net.catten.property.editor.app

import net.catten.property.editor.utils.UIProperties
import java.awt.Component
import java.awt.Dimension
import javax.swing.Box
import javax.swing.BoxLayout
import javax.swing.JButton
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.SwingUtilities

class NoFileViewController private constructor() {
    val frame = JPanel()

    private fun addComponent(com : JComponent) {
        com.alignmentX = Component.CENTER_ALIGNMENT
        frame.add(com)
    }

    var onCreateEmptyFile : (() -> Unit)? = null
    var onOpenFile : (() -> Unit)? = null

    init {
        frame.layout = BoxLayout(frame, BoxLayout.Y_AXIS)
        frame.add(Box.createVerticalGlue())
        addComponent(JLabel("No file opened"))
        frame.add(Box.createRigidArea(Dimension(0, 20)))
        addComponent(JButton("Create an empty file").apply {
            putClientProperty(UIProperties.BUTTON_ROLE, BUTTON_ROLE_CREATE_EMPTY_FILE)
            addActionListener { event ->
                if(event.source != this) return@addActionListener
                onCreateEmptyFile?.let { SwingUtilities.invokeLater(it) }
            }
        })
        addComponent(JLabel("or"))
        addComponent(JButton("Open a file").apply {
            putClientProperty(UIProperties.BUTTON_ROLE, BUTTON_ROLE_OPEN_FILE)
            addActionListener { event ->
                if(event.source != this) return@addActionListener
                onOpenFile?.let { SwingUtilities.invokeLater(it) }
            }
        })
        frame.add(Box.createVerticalGlue())
    }

    companion object {
        val instance = NoFileViewController()
        const val BUTTON_ROLE_CREATE_EMPTY_FILE = "create_empty_file"
        const val BUTTON_ROLE_OPEN_FILE = "open_file"
    }
}