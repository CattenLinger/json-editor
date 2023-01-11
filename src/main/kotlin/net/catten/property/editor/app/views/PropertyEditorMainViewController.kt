package net.catten.property.editor.app.views

import net.catten.property.editor.app.UIApplication
import net.catten.property.editor.app.views.editor.JsonEditorView
import net.catten.property.editor.utils.*
import org.slf4j.LoggerFactory
import java.awt.BorderLayout
import java.awt.Dimension
import java.io.File
import javax.imageio.ImageIO
import javax.swing.JFileChooser
import javax.swing.JFrame
import javax.swing.JMenuItem
import javax.swing.SwingUtilities
import javax.swing.filechooser.FileNameExtensionFilter
import kotlin.properties.Delegates.notNull
import kotlin.properties.Delegates.observable

class PropertyEditorMainViewController (val app: UIApplication) {
    private val logger = LoggerFactory.getLogger(this::class.java)

    private var defaultOptionDirectory = File(".")

    val frame = JFrame().apply {
        title = DEFAULT_APP_TITLE
        setLocationRelativeTo(null)
        iconImage = ImageIO.read(ResourceLoader.getResourceUrl("assets/icons/app_icon.png"))
    }

    var title by observable(DEFAULT_APP_TITLE) { _, _, n -> swingInvokeLater { frame.title = n } }
        private set

    private var openMenuItem: JMenuItem by notNull()
    private var exitMenuItem: JMenuItem by notNull()

    private val fileChooser by lazy {
        JFileChooser().apply {
            fileFilter = FileNameExtensionFilter("Json Or Yaml File", "json", "yaml", "yml")
            isMultiSelectionEnabled = false
            currentDirectory = defaultOptionDirectory
        }
    }

    private val jsonEditorView = JsonEditorView(app)

    private fun onOpenFile() = SwingUtilities.invokeLater {
        when (val result = fileChooser.showOpenDialog(frame)) {
            JFileChooser.APPROVE_OPTION -> {
                val file = fileChooser.selectedFile
                logger.info("user selected file {}", file)
                loadFile(file)
            }
            else -> logger.info("command canceled by user, result: {}", result)
        }
    }

    fun loadFile(file : File) = jsonEditorView.loadFile(file) {
        if (it.isSuccess) swingInvokeLater {
            val noFile = NoFileViewController.instance
            noFile.frame.isEnabled = false
            logger.info("Loaded file '{}'.", file.absolutePath)
            frame.remove(noFile.frame)

            frame.add(jsonEditorView.frame, BorderLayout.CENTER)
            noFile.frame.isEnabled = true
            frame.revalidate()
            frame.repaint()
        } else {
            logger.error("load failed",it.exceptionOrNull())
        }
    }

    private fun onSaveAs() {
        jsonEditorView.onSave()
    }

    private fun buildMainMenu() = jMenuBar {
        menu("Files") {
            openMenuItem = menuItem("Open...") {
                addActionListener {
                    if (it.source != this) return@addActionListener
                    onOpenFile()
                }
            }
            menuItem("Save As...") {
                addActionListener {
                    if(it.source != this) return@addActionListener
                    onSaveAs()
                }
            }
            separator()
            exitMenuItem = menuItem("Exit") { }
        }
    }

    init {
        initialization()
    }

    private fun initialization() {
        frame.jMenuBar = buildMainMenu()
        frame.size = Dimension(640, 480)

        val noFileView = NoFileViewController.instance
        noFileView.onCreateEmptyFile = { logger.info("User create empty file") }
        noFileView.onOpenFile = { onOpenFile() }
        frame.add(noFileView.frame, BorderLayout.CENTER)

        logger.info("Initialization invoked.")
    }

    companion object {
        private const val DEFAULT_APP_TITLE = "Property Editor"
    }
}