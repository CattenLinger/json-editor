package net.catten.property.editor.utils

import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.SwingUtilities

fun swingInvokeLater(block : () -> Unit) = SwingUtilities.invokeLater(block)

fun swingInvokeAndWait(block : () -> Unit) = SwingUtilities.invokeAndWait(block)

object MouseEventListeners {
    fun mousePressed(handler : (MouseEvent) -> Unit) = object : MouseAdapter() {
        override fun mousePressed(e: MouseEvent) = handler(e)
    }
}