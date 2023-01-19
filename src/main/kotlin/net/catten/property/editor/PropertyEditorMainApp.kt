package net.catten.property.editor

import com.formdev.flatlaf.FlatIntelliJLaf
import com.formdev.flatlaf.extras.FlatInspector
import com.formdev.flatlaf.util.SystemInfo
import net.catten.property.editor.app.UIApplication
import net.catten.property.editor.app.UIApplicationEnvironment
import net.catten.property.editor.app.views.PropertyEditorMainViewController
import net.catten.property.editor.utils.swingInvokeAndWait
import java.io.File
import java.lang.RuntimeException

fun main(args: Array<String>) = UIApplication(args) {


    swingInvokeAndWait {
        setupFlatLaf()
        val mainView = PropertyEditorMainViewController(this).also { view -> registerMainWindow { view.frame } }
        if (args.isNotEmpty()) {
            val file = File(args[0])
            if (file.isFile && file.exists()) mainView.loadFile(file)
        }
    }
}

private fun setupFlatLaf() {
    if (SystemInfo.isMacOS) {
        System.setProperty("apple.laf.useScreenMenuBar", "true")
        System.setProperty("apple.awt.application.name", "Property Editor")
    }

    FlatIntelliJLaf.setup()
    FlatInspector.install("ctrl shift alt X")
}