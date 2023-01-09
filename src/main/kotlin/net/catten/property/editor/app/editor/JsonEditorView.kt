package net.catten.property.editor.app.editor

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.formdev.flatlaf.extras.FlatSVGUtils
import net.catten.property.editor.components.JsonNodeWrapper
import net.catten.property.editor.utils.CustomTreeCellRenderer
import org.slf4j.LoggerFactory
import java.awt.BorderLayout
import java.io.File
import java.util.WeakHashMap
import javax.swing.ImageIcon
import javax.swing.JPanel
import javax.swing.JScrollPane
import javax.swing.JSplitPane
import javax.swing.JTree
import javax.swing.SwingUtilities
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeCellRenderer
import javax.swing.tree.TreePath

class JsonEditorView {

    private val logger = LoggerFactory.getLogger(this::class.java)

    var file: File? = null
        private set

    private val treeModel = DefaultMutableTreeNode(JsonNodeWrapperTreeModel.Root("root"))

    private val treeView = JTree(treeModel).apply {
        fun JsonNodeWrapper.Entry<*>.renderValue() : String {
            return when(value) {
                is JsonNodeWrapper.Container<*> -> ""
                else -> ": ${value.json}"
            }
        }

        fun DefaultTreeCellRenderer.setRootIconByType(name : String) {
            when(name) {
                "json" -> icon = loadSvgIcon("json")
                "yaml" -> icon = loadSvgIcon("yaml")
            }
        }

        fun DefaultTreeCellRenderer.setNodeIconByType(name : String, value : JsonNodeWrapper) {
            when(name) {
                "boolean" -> icon = loadSvgIcon("boolean_${value.json.asBoolean()}")
                "number" -> icon = loadSvgIcon("number")
                "string" -> icon = loadSvgIcon("string")
                "array" -> icon = loadSvgIcon("list")
                "object" -> icon = loadSvgIcon("object")
            }
        }

        cellRenderer = CustomTreeCellRenderer.forValue {
            require(it is DefaultMutableTreeNode) { "unsupported node type: ${it::class.java}" }
            when(val value = it.userObject) {
                is JsonNodeWrapperTreeModel.Root -> value.displayName.also { setRootIconByType(value.displayType) }

                is JsonNodeWrapperTreeModel.Entry -> when(value.entry.key) {
                    is Number -> "[${value.entry.key}]${value.entry.renderValue()}"
                    else -> "${value.entry.key}${value.entry.renderValue()}"
                }.also { setNodeIconByType(value.displayType, value.entry.value) }

                is JsonNodeWrapperTreeModel.Value -> value.displayName.also { setNodeIconByType(value.displayType, value.value) }

                else -> error("unknown node type '${value}'(${value::class.java})")
            }
        }

        addTreeSelectionListener { event ->
            logger.info("Selected path: {}", event.path)
            viewPathNavigator.updateDisplayPath(event.path.toJsonPath())
            viewValueEditor.setCurrentEditingNode(event.path.lastPathComponent as DefaultMutableTreeNode)
        }
    }

    private val viewPathNavigator = JsonPathNavigator()

    private val viewValueEditor = JsonValueEditorView()

    private val splitPane = JSplitPane(
        JSplitPane.HORIZONTAL_SPLIT,
        JScrollPane(treeView).apply {
            setViewportView(treeView)
        },
        viewValueEditor.frame
    ).apply {
        setDividerLocation(0.418)
    }

    val frame = JPanel(BorderLayout()).apply {
        add(viewPathNavigator.frame, BorderLayout.NORTH)
        add(splitPane, BorderLayout.CENTER)
    }

    fun loadFile(file: File, callback: (Result<JsonNode>) -> Unit) = try {
        val yaml = yaml.readTree(file)
        loadObject(file.name, yaml)
        callback(Result.success(yaml))
    } catch (e: Exception) {
        callback(Result.failure(e))
    }

    private fun loadObject(name: String, json: JsonNode) {
        val tree = treeView
        tree.isRootVisible = false
        val root = treeModel
        root.removeAllChildren()
        (root.userObject as JsonNodeWrapperTreeModel.Root).apply {
            displayName = name
            displayType = "yaml"
        }
        if (json.isEmpty) return
        val wrapper = JsonNodeWrapper.wrap(json)
        buildJsonTree(wrapper, root)
        tree.isRootVisible = true
        SwingUtilities.invokeLater {
            treeView.revalidate()
            treeView.repaint()
            tree.expandPath(TreePath(root))
        }
    }

    private fun buildJsonTree(wrapper: JsonNodeWrapper, current: DefaultMutableTreeNode) {
        when (wrapper) {
            is JsonNodeWrapper.Value -> current.add(DefaultMutableTreeNode(JsonNodeWrapperTreeModel.Value(wrapper)))
            is JsonNodeWrapper.Container<*> -> buildJsonContainerTree(wrapper, current)
        }
    }

    private fun buildJsonContainerTree(wrapper: JsonNodeWrapper.Container<*>, current: DefaultMutableTreeNode) {
        wrapper.children.forEach {
            val model = JsonNodeWrapperTreeModel.Entry(it)
            val child = DefaultMutableTreeNode(model)
            when(val value = it.value) {
                is JsonNodeWrapper.Container<*> -> buildJsonContainerTree(value, child)
                is JsonNodeWrapper.Value -> model.apply {
                    displayType = value.json.nodeType.name.lowercase()
                    displayName = value.json.toString()
                }
            }
            current.add(child)
        }
    }

    fun onSave() {
        when(treeModel.childCount) {
            0 -> return
            1 -> when(val child = (treeModel.firstChild as DefaultMutableTreeNode).userObject) {
                is JsonNodeWrapperTreeModel.Value -> println(child.value.toString())
                is JsonNodeWrapperTreeModel.Entry -> buildJsonObjectFromEntry(child)
            }
            else -> {
                val child = (treeModel.firstChild as DefaultMutableTreeNode)
                child.children().asSequence().filterIsInstance<JsonNodeWrapperTreeModel.Entry>()
            }
        }
        val child = treeModel.firstChild as? DefaultMutableTreeNode ?: return
        when(val root = child.userObject) {
            is JsonNodeWrapperTreeModel.Value -> println(root.value)
        }
    }

    private fun buildJsonObjectFromEntry(entry : JsonNodeWrapperTreeModel.Entry) : JsonNode {
        TODO()
    }

    companion object {
        private val yaml by lazy { ObjectMapper(YAMLFactory()) }
        private val json by lazy { ObjectMapper() }
        val instance by lazy { JsonEditorView() }

        private val reservedKeyChars = Regex("[.\\[\\]]")

        /* Svg Cache */
        private val svgIconCache = WeakHashMap<String, ImageIcon>()
        private fun DefaultTreeCellRenderer.loadSvgIcon(name : String) = svgIconCache.computeIfAbsent("/assets/icons/$name.svg") {
            ImageIcon(FlatSVGUtils.svg2image(it, icon.iconWidth, icon.iconHeight))
        }

        private fun TreePath.toJsonPath() = path.filterIsInstance<DefaultMutableTreeNode>().joinToString(".") { node ->
            when (val obj = node.userObject) {
                is JsonNodeWrapperTreeModel.Entry -> when (val key = obj.entry.key) {
                    is Number -> "[$key]"
                    else -> key.toString().let { if (it.contains(reservedKeyChars)) "\"$it\"" else it }
                }

                else -> ""
            }
        }
    }
}