package net.catten.property.editor

import com.formdev.flatlaf.FlatIntelliJLaf
import com.formdev.flatlaf.extras.FlatInspector
import com.formdev.flatlaf.util.SystemInfo
import net.catten.property.editor.app.UIApplication
import net.catten.property.editor.app.views.PropertyEditorMainViewController
import net.catten.property.editor.utils.UIAppCriticalErrorMessage
import net.catten.property.editor.utils.promptSwingDialog
import java.io.File
import javax.swing.SwingUtilities

fun main(args: Array<String>) = try {
    val uiApplication = UIApplication(args)
    SwingUtilities.invokeAndWait {
        setupFlatLaf()
        val mainView = PropertyEditorMainViewController(uiApplication)
        uiApplication.registerMainWindow({ mainView.frame })
        if (args.isNotEmpty()) {
            val file = File(args[0])
            if (file.isFile && file.exists()) mainView.loadFile(file)
        }
    }
} catch (e: Exception) {
    UIAppCriticalErrorMessage(
        "Critical Error",
        "Application bootstrap meets an critical error.\n${e.message}",
        e
    ).promptSwingDialog(0)
}

private fun setupFlatLaf() {
    if (SystemInfo.isMacOS) {
        System.setProperty("apple.laf.useScreenMenuBar", "true")
        System.setProperty("apple.awt.application.name", "Property Editor")
    }

    FlatIntelliJLaf.setup()
    FlatInspector.install("ctrl shift alt X")
}