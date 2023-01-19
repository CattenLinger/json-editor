package net.catten.property.editor.app

import net.catten.property.editor.framework.PropertyRegistry
import org.slf4j.LoggerFactory
import java.io.File
import java.io.FileWriter

class UIApplicationRegistry(env: UIApplicationEnvironment, val configurationFileName : String = "app.properties") : PropertyRegistry() {
    private val logger = LoggerFactory.getLogger(this::class.java)

//    var appEditorViewSplitPanePosition by integer("app.editor.split_pane.position").withDefault(100)
//    var appEditorViewValueTableKeyWidth by integer("app.editor.value_editor.key.width").withDefault(100)
//    var appEditorViewValueTableColumWidth by register(
//        "app.editor.value_editor.table.column_widths",
//        { it.joinToString(",") { int -> int.toString() } },
//        { it.split(",").map { str -> str.toInt() } }
//    ).withDefault(listOf(15,15))

    val configurationFile = env.configDir?.let { File(it, configurationFileName) }

    init { initConfig() }

    private fun initConfig() {
        val file = configurationFile ?: return
        if (!(file.exists() && file.isFile)) return logger.info("Application settings '{}' does not exists. Will not load.", file.absolutePath)

        logger.info("Loading application settings from '{}'.", configurationFile.absolutePath)
        deserialize(file.readLines().mapNotNull {
            when (val pos = it.indexOf('=')) {
                -1 -> null
                else -> it.substring(0, pos) to it.substring(pos + 1)
            }
        }.toMap())
    }

    internal fun saveConfig() {
        val file = configurationFile ?: return
        if (!(file.parentFile.exists() && file.parentFile.isDirectory))
            return logger.info("Directory '{}' does not exists. Application settings will not be saved.", file.parentFile.absolutePath)

        logger.info("Saving application settings to '{}'.", configurationFile.absolutePath)
        FileWriter(file).use { writer -> serialize().entries.forEach { writer.write("${it.key}=${it.value}\n") } }
    }
}