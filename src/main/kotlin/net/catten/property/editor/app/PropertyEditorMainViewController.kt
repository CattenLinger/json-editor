package net.catten.property.editor.app

import net.catten.property.editor.app.editor.JsonEditorView
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
import kotlin.math.log
import kotlin.properties.Delegates.notNull
import kotlin.properties.Delegates.observable

class PropertyEditorMainViewController private constructor(val app: UIApplication) {
    private val logger = LoggerFactory.getLogger(this::class.java)

    private var defaultOptionDirectory = File(".")

    private val frame = JFrame().apply {
        title = DEFAULT_APP_TITLE
        iconImage = ImageIO.read(ResourceLoader.getResourceUrl("assets/icons/app_icon.png"))
    }

    var title by observable(DEFAULT_APP_TITLE) { _, _, n -> SwingUtilities.invokeLater { frame.title = n } }
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

    private fun onOpenFile() = SwingUtilities.invokeLater {
        when (val result = fileChooser.showOpenDialog(frame)) {
            JFileChooser.APPROVE_OPTION -> {
                val editor = JsonEditorView.instance
                val noFile = NoFileViewController.instance
                logger.info("user selected file {}", fileChooser.selectedFile)
                noFile.frame.isEnabled = false
                editor.loadFile(fileChooser.selectedFile) {
                    if (it.isSuccess) {
                        frame.remove(NoFileViewController.instance.frame)
                        frame.add(editor.frame, BorderLayout.CENTER)
                        SwingUtilities.invokeLater {
                            frame.revalidate()
                            frame.repaint()
                        }
                    } else {
                        logger.error("load failed",it.exceptionOrNull())
                    }
                }
                NoFileViewController.instance.frame.isEnabled = true
            }

            else -> logger.info("command canceled by user, result: {}", result)
        }
    }

    private fun onSaveAs() {
        JsonEditorView.instance.onSave()

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
        fun create(app: UIApplication) {
            val appFrame = PropertyEditorMainViewController(app).apply { initialization() }

            with(appFrame.frame) {
                setLocationRelativeTo(null)
                defaultCloseOperation = JFrame.EXIT_ON_CLOSE
                isVisible = true
            }
        }

        private const val DEFAULT_APP_TITLE = "Property Editor"
    }
}