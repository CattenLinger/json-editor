package net.catten.property.editor

import com.formdev.flatlaf.FlatIntelliJLaf
import com.formdev.flatlaf.extras.FlatInspector
import net.catten.property.editor.app.PropertyEditorMainViewController
import net.catten.property.editor.utils.UIAppCriticalErrorMessage
import net.catten.property.editor.utils.UIApplication
import net.catten.property.editor.utils.promptSwingDialog
import javax.swing.SwingUtilities

fun main(args: Array<String>) = try {
    setupFlatLaf()
    val uiApplication = UIApplication(args)
    SwingUtilities.invokeLater { PropertyEditorMainViewController.create(uiApplication) }
} catch (e: Exception) {
    UIAppCriticalErrorMessage(
        "Critical Error",
        "Application bootstrap meets an critical error.\n${e.message}",
        e
    ).promptSwingDialog(0)
}

private fun setupFlatLaf() {
    FlatIntelliJLaf.setup()
    FlatInspector.install("ctrl shift alt X")
}