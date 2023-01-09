package net.catten.property.editor.utils

import javax.swing.JOptionPane
import kotlin.system.exitProcess

data class UIAppCriticalErrorMessage(val title : String, val message : String, val exception : Exception?)

fun UIAppCriticalErrorMessage.promptSwingDialog(exitStatus : Int = 1) {
    JOptionPane.showMessageDialog(
        null,
        message,
        title,
        JOptionPane.ERROR_MESSAGE
    )
    exitProcess(exitStatus)
}