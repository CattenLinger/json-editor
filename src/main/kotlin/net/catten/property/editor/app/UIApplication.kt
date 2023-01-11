package net.catten.property.editor.app

import org.slf4j.LoggerFactory
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import java.io.File
import java.io.FileWriter
import java.util.*
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import javax.swing.JFrame
import kotlin.system.exitProcess

class UIApplication(val commandLineArguments: Array<String>) {
    val logger = LoggerFactory.getLogger(this::class.java)

    private val applicationClosing = AtomicBoolean(false)
    private val mainWindowLists = LinkedList<JFrame>()

    val ioExecutor: Executor = Executors.newWorkStealingPool()

    fun registerMainWindow(provider: (UIApplication) -> JFrame, showNow: Boolean = true) {
        if (applicationClosing.get()) {
            logger.warn("Application is closing but mainWindow creation incomes.")
            return
        }

        val frame = provider(this)
        mainWindowLists.add(frame)

        frame.addWindowListener(object : WindowAdapter() {
            override fun windowClosing(e: WindowEvent) {
                val window = e.window
                e.window.dispose()
                mainWindowLists.remove(window)
                logger.info("A main window was closed.")
                onAnyMainWindowCloses()
            }
        })

        frame.isVisible = showNow
    }

    private fun onAnyMainWindowCloses() {
        if (applicationClosing.get()) return
        if (mainWindowLists.isEmpty()) {
            applicationClosing.set(true)
            logger.info("All window closed. Application Exit.")
            registry.saveConfig()
            /* May needed to do some resource disposing actions */
            exitProcess(0)
        }
    }

    class Environment : EnvironmentProperties() {
        val configDir by provider {
            property("net.catten.PropertyEditor.configDir")
            environment("PROPERTY_EDITOR_CONFIG_DIR")
            ifNotFoundThenLog("Application settings will not be persistent.")
        }
    }

    val environment = Environment()

    class Registry(env: Environment) : PropertyRegistry() {
        private val logger = LoggerFactory.getLogger(this::class.java)

        var appEditorViewSplitPanePosition by integer("app.editor.split_pane.position").withDefault(100)
        var appEditorViewValueTableKeyWidth by integer("app.editor.value_editor.key.width").withDefault(100)
        var appEditorViewValueTableColumWidth by register(
            "app.editor.value_editor.table.column_widths",
            { it.joinToString(",") { int -> int.toString() } },
            { it.split(",").map { str -> str.toInt() } }
        ).withDefault(listOf(15,15))

        private val configFile = env.configDir?.let { File(it, "app.properties") }

        init {
            initConfig()
        }

        private fun initConfig() {
            val file = configFile ?: return
            if (!(file.exists() && file.isFile)) return logger.info("Application settings '{}' does not exists. Will not load.", file.absolutePath)

            logger.info("Loading application settings from '{}'.", configFile.absolutePath)
            replaceWith(file.readLines().mapNotNull {
                when (val pos = it.indexOf('=')) {
                    -1 -> null
                    else -> it.substring(0, pos) to it.substring(pos + 1)
                }
            }.toMap())
        }

        internal fun saveConfig() {
            val file = configFile ?: return
            if (!(file.parentFile.exists() && file.parentFile.isDirectory))
                return logger.info("Directory '{}' does not exists. Application settings will not be saved.", file.parentFile.absolutePath)

            logger.info("Saving application settings to '{}'.", configFile.absolutePath)
            FileWriter(file).use { writer -> values().entries.forEach { writer.write("${it.key}=${it.value}\n") } }
        }
    }

    val registry = Registry(environment)
}