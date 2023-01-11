package net.catten.property.editor.utils

import javax.swing.JPanel

fun jPanel(block : JPanel.() -> Unit) : JPanel {
    return JPanel().apply(block)
}