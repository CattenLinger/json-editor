package net.catten.property.editor.app.views.editor

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.JsonNodeType
import com.formdev.flatlaf.extras.FlatSVGUtils
import net.catten.property.editor.app.UIApplication
import net.catten.property.editor.utils.CustomTreeCellRenderer
import net.catten.property.editor.utils.swingInvokeLater
import org.slf4j.LoggerFactory
import java.awt.BorderLayout
import java.io.File
import java.util.*
import javax.swing.*
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeCellRenderer
import javax.swing.tree.DefaultTreeModel
import javax.swing.tree.TreePath

class JsonEditorView(val app: UIApplication) {

    private val logger = LoggerFactory.getLogger(this::class.java)

    var file: File? = null
        private set

    private val treeNode = DefaultMutableTreeNode(ConfigValueContainer.JsonRoot())

    private val treeView = JTree(treeNode).apply {
        fun DefaultTreeCellRenderer.setRootIconByType(name: String) {
            when (name) {
                "json" -> icon = loadSvgIcon("json")
                "yaml" -> icon = loadSvgIcon("yaml")
            }
        }

        cellRenderer = CustomTreeCellRenderer.forValue {
            require(it is DefaultMutableTreeNode) { "DefaultMutableTreeNode required but got unsupported node type: ${it::class.java}" }
            when (val value = it.userObject as ConfigValueContainer) {
                is ConfigValueContainer.Root -> (value.title ?: "<${value.format.name} root>").also {
                    setRootIconByType(value.format.name)
                }

                is ConfigValueContainer.Container -> (value.title ?: "")
                    .also { icon = loadSvgIcon(value.type.displayName) }

                else -> error("unknown node type '${value}'(${value::class.java})")
            }
        }

        addTreeSelectionListener { event ->
            val jsonPath = event.path.toJsonPath()
            logger.info("Selected path: {}", jsonPath)
            viewPathNavigator.updateDisplayPath(jsonPath)
            viewValueEditor.setCurrentEditingNode(event.path.lastPathComponent as DefaultMutableTreeNode)
        }
    }

    private val viewPathNavigator = JsonPathNavigator()

    private val viewValueEditor = JsonValueEditorView(app).also {
        it.containerSelectedListeners.add(this::onContainerValueSelected)
        it.rowSelectUpdateListeners.add(this::onRowSelectionUpdate)
    }

    private fun onRowSelectionUpdate(rows: List<JsonValueEditorView.ContainerTableModel.RowModel>) {
        if (rows.isEmpty()) return
        if (rows.count() > 1) return viewPathNavigator.updateMultipleDisplayPath(rows.size)
        val row = rows.first()
        val treeSelected = treeView.selectionPath ?: return
        viewPathNavigator.updateDisplayPath(treeSelected.toJsonPath() + "." + row.key.escapeIfContainsReservedSymbol())
    }

    private fun onContainerValueSelected(key: String) {
        val selected = treeView.selectionPath ?: return

        val path = selected.path
        val last = (path.last() as DefaultMutableTreeNode)
        val target = last.children().toList().firstOrNull {
            when (val obj = (it as DefaultMutableTreeNode).userObject) {
                is ConfigValueContainer.Container -> key == obj.key
                else -> false
            }
        } ?: return
        swingInvokeLater {
            val oldExpandPolicy = treeView.expandsSelectedPaths
            treeView.expandsSelectedPaths = true
            treeView.selectionModel.clearSelection()
            treeView.selectionModel.addSelectionPath(selected.pathByAddingChild(target))
            treeView.expandsSelectedPaths = oldExpandPolicy
        }
    }

    private val splitPane = JSplitPane(
        JSplitPane.HORIZONTAL_SPLIT,
        JScrollPane(treeView).apply { setViewportView(treeView) },
        viewValueEditor.frame
    ).apply {
        addPropertyChangeListener(JSplitPane.DIVIDER_LOCATION_PROPERTY) {
            app.registry.appEditorViewSplitPanePosition = (it.newValue as Number).toInt()
        }
        SwingUtilities.invokeLater { dividerLocation = app.registry.appEditorViewSplitPanePosition }
    }

    val frame = JPanel(BorderLayout()).apply {
        add(viewPathNavigator.frame, BorderLayout.NORTH)
        add(splitPane, BorderLayout.CENTER)
    }

    fun loadFile(file: File, callback: (Result<JsonNode>) -> Unit) = app.ioExecutor.execute {
        val format = when (val extension = file.extension) {
            "yaml", "yml" -> ConfigValueContainerFormat.Yaml
            "json" -> ConfigValueContainerFormat.Json
            else -> return@execute callback(Result.failure(Exception("Unsupported extension: $extension")))
        }

        try {
            callback(Result.success(loadObject(file, format)))
        } catch (e: Exception) {
            return@execute callback(Result.failure(e))
        }
    }

    private fun containerTypeOf(json: JsonNode): ConfigValueContainer.Type = when (val type = json.nodeType) {
        JsonNodeType.OBJECT -> ConfigValueContainer.Type.Dictionary
        JsonNodeType.ARRAY -> ConfigValueContainer.Type.Array
        else -> error("Unsupported container type: $type")
    }

    private fun loadObject(file: File, format: ConfigValueContainerFormat): JsonNode {
        // Read the object tree from json/yaml
        val tree = format.objectMapper.readTree(file)

        // Check container type
        val containerType = if (tree.isEmpty) ConfigValueContainer.Type.Dictionary else containerTypeOf(tree)

        treeView.isRootVisible = false
        treeNode.removeAllChildren()

        val container = ConfigValueContainer.Root(format, containerType).also { it.file = file }
        treeNode.userObject = container

        if (!tree.isEmpty) buildTreeStructure(treeNode, tree)

        treeView.isRootVisible = true

        SwingUtilities.invokeLater {
            (treeView.model as DefaultTreeModel).reload()

            treeView.revalidate()
            treeView.repaint()
            treeView.expandPath(TreePath(treeNode))
        }
        return tree
    }

    private fun JsonNode.isSimpleValue(): Boolean = when (nodeType) {
        JsonNodeType.BOOLEAN, JsonNodeType.NULL, JsonNodeType.NUMBER, JsonNodeType.STRING -> true
        else -> false
    }

    private fun buildTreeStructure(treeModel: DefaultMutableTreeNode, root: JsonNode) {
        val current = treeModel.userObject as ConfigValueContainer
        when (current.type) {
            ConfigValueContainer.Type.Dictionary -> for ((key, value) in root.fields()) {
                if (value.isSimpleValue()) current.values[key] = ConfigValue.wrap(value) else {
                    val container = ConfigValueContainer.Container(containerTypeOf(value)).also { it.key = key }
                    treeModel.add(DefaultMutableTreeNode(container).also { buildTreeStructure(it, value) })
                }
            }

            ConfigValueContainer.Type.Array -> root.forEachIndexed { index, value ->
                if (value.isSimpleValue()) current.values[index.toString()] = ConfigValue.wrap(value) else {
                    val container = ConfigValueContainer.Container(containerTypeOf(value)).also { it.key = index.toString() }
                    treeModel.add(DefaultMutableTreeNode(container).also { buildTreeStructure(it, value) })
                }
            }
        }
    }

    fun onSave() {
//        when (treeNode.childCount) {
//            0 -> return
//            1 -> when (val child = (treeNode.firstChild as DefaultMutableTreeNode).userObject) {
//                is JsonNodeWrapperTreeModel.Value -> println(child.value.toString())
//                is JsonNodeWrapperTreeModel.Entry -> buildJsonObjectFromEntry(child)
//            }
//
//            else -> {
//                val child = (treeNode.firstChild as DefaultMutableTreeNode)
//                child.children().asSequence().filterIsInstance<JsonNodeWrapperTreeModel.Entry>()
//            }
//        }
//        val child = treeNode.firstChild as? DefaultMutableTreeNode ?: return
//        when (val root = child.userObject) {
//            is JsonNodeWrapperTreeModel.Value -> println(root.value)
//        }
    }

//    private fun buildJsonObjectFromEntry(entry: JsonNodeWrapperTreeModel.Entry): JsonNode {
//        TODO()
//    }

    companion object {
        /* Svg Cache */
        private val svgIconCache = WeakHashMap<String, ImageIcon>()
        private fun DefaultTreeCellRenderer.loadSvgIcon(name: String) = svgIconCache.computeIfAbsent("/assets/icons/$name.svg") {
            ImageIcon(FlatSVGUtils.svg2image(it, icon.iconWidth, icon.iconHeight))
        }

        private fun TreePath.toJsonPath(): String = path
            .filterIsInstance<DefaultMutableTreeNode>()
            .map { it.userObject }
            .filterIsInstance<ConfigValueContainer.Container>()
            .mapNotNull { it.key }
            .joinToString(".") { it.escapeIfContainsReservedSymbol() }

        private fun String.escapeIfContainsReservedSymbol(): String {
            var str = this
            if (str.contains("\"")) str = str.replace("\"", "\\\"")
            if (str.contains(".")) str = "\"$str\""
            return str
        }
    }
}