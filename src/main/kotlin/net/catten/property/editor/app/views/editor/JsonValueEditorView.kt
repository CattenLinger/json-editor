package net.catten.property.editor.app.views.editor

import com.formdev.flatlaf.extras.FlatSVGUtils
import net.catten.property.editor.app.UIApplication
import net.catten.property.editor.utils.MouseEventListeners
import net.catten.property.editor.utils.forEachColumnIndexed
import net.catten.property.editor.utils.jPanel
import net.catten.property.editor.utils.swingInvokeLater
import java.awt.BorderLayout
import java.awt.Component
import java.awt.Font
import java.awt.event.MouseEvent
import java.beans.PropertyChangeEvent
import java.util.*
import javax.swing.*
import javax.swing.table.AbstractTableModel
import javax.swing.table.DefaultTableCellRenderer
import javax.swing.tree.DefaultMutableTreeNode

class JsonValueEditorView(app: UIApplication) {

    val frame = jPanel {
        layout = BorderLayout()
    }

    private val tableModel = ContainerTableModel()

    class RowModelRenderer : DefaultTableCellRenderer() {
        init {
            horizontalTextPosition = JLabel.RIGHT
            verticalTextPosition = JLabel.CENTER
        }

        override fun getTableCellRendererComponent(
            table: JTable,
            value: Any?, isSelected: Boolean, hasFocus: Boolean,
            row: Int, column: Int
        ): Component {
            val label = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column) as JLabel
            val rowModel = (value as? ContainerTableModel.RowModel) ?: return label

            label.icon = svgCache.computeIfAbsent("/assets/icons/${rowModel.type}.svg") {
                ImageIcon(FlatSVGUtils.svg2image(it, table.rowHeight, table.rowHeight))
            }

            if (rowModel is ContainerTableModel.RowModel.ContainerRowModel) {
                label.font = label.font.deriveFont(Font.ITALIC)
                label.text = "<${rowModel.valuePreview}>"
                label.isEnabled = false
            } else {
                label.text = rowModel.valuePreview
                label.font = label.font.deriveFont(Font.PLAIN)
                label.isEnabled = true
            }

            return label
        }
    }

    class ContainerTableModel : AbstractTableModel() {
        abstract class RowModel(val key: String) {
            abstract val type: String
            abstract val valuePreview: String

            class ValueRowModel(key: String, val origin: ConfigValue) : RowModel(key) {
                override val valuePreview: String
                    get() = origin.value.toString()

                override val type: String
                    get() = origin.typeName
            }

            class ContainerRowModel(val container: ConfigValueContainer) : RowModel(container.title) {
                override val type: String
                    get() = container.type.displayName

                override val valuePreview: String
                    get() = "nested ${container.type.name.lowercase()}"
            }
        }

        private val contents = LinkedList<RowModel>()
        val rows : List<RowModel>
            get() = contents

        fun applyTreeNode(treeNode: DefaultMutableTreeNode) {
            val container = treeNode.userObject as ConfigValueContainer
            contents.clear()

            contents.addAll(container.values.map { (key, value) -> RowModel.ValueRowModel(key, value) })

            contents.addAll(treeNode.children().toList().map {
                ((it as DefaultMutableTreeNode).userObject as ConfigValueContainer).let { container1 -> RowModel.ContainerRowModel(container1) }
            })

            if (container.type == ConfigValueContainer.Type.Array) contents.sortBy { it.key.toInt() }
        }

        override fun getRowCount(): Int {
            return contents.size
        }

        private val columns = listOf("Key", "Value")
        override fun getColumnCount(): Int = columns.size

        override fun getColumnName(column: Int): String {
            return columns[column]
        }

        override fun getColumnClass(columnIndex: Int): Class<*> {
            return when (columnIndex) {
                1 -> RowModel::class.java
                else -> String::class.java
            }
        }

        override fun getValueAt(rowIndex: Int, columnIndex: Int): Any {
            val obj = contents[rowIndex]
            return when (columnIndex) {
                0 -> obj.key
                1 -> obj
                else -> throw IndexOutOfBoundsException()
            }
        }
    }

    val table = JTable().apply {
        fillsViewportHeight = true
        model = tableModel
        setDefaultRenderer(ContainerTableModel.RowModel::class.java, RowModelRenderer())

        fun onTableWidthChangeListener(event: PropertyChangeEvent) {
            if (event.propertyName == "width") {
//                app.registry.appEditorViewValueTableColumWidth = columnModel.columns.toList().map { it.width }
            }
        }

        columnModel.forEachColumnIndexed { index, column ->
            column.addPropertyChangeListener(::onTableWidthChangeListener)
//            column.preferredWidth = app.registry.appEditorViewValueTableColumWidth.getOrElse(index) { 15 }
        }

        selectionModel.addListSelectionListener { onTableSelectionChanged() }

        addMouseListener(MouseEventListeners.mousePressed(::onTableDoubleClickListener))

        swingInvokeLater { doLayout() }
    }

    private fun onTableDoubleClickListener(e : MouseEvent) {
        val table = e.source as JTable
        if (table.selectedRowCount != 1) return
        if(e.clickCount == 2 && table.selectedRowCount == 1) {
            val clickedRow = table.rowAtPoint(e.point).takeIf { it >= 0 } ?: return
            val row = tableModel.rows[clickedRow]
            if(row !is ContainerTableModel.RowModel.ContainerRowModel) return
            containerSelectedListeners.forEach { it(row.key) }
        }
    }

    val containerSelectedListeners = mutableListOf<(String) -> Unit>()

    private fun onTableSelectionChanged() {
        val selected = table.selectedRows
        if(selected.isEmpty()) return
        val selectedRows = selected.map { tableModel.rows[it] }
        rowSelectUpdateListeners.forEach { it(selectedRows) }
    }

    val rowSelectUpdateListeners = mutableListOf<(List<ContainerTableModel.RowModel>) -> Unit>()

    val scrollPane = JScrollPane(table).apply {
        setViewportView(table)
    }.also { frame.add(it, BorderLayout.CENTER) }

    fun setCurrentEditingNode(defaultMutableTreeNode: DefaultMutableTreeNode) {
        tableModel.applyTreeNode(defaultMutableTreeNode)
        swingInvokeLater {
            table.selectionModel.clearSelection()
            table.revalidate()
            table.repaint()
        }
    }

    companion object {
        private val svgCache = WeakHashMap<String, ImageIcon>()
    }
}