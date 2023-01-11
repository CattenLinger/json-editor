package net.catten.property.editor.utils

import javax.swing.table.TableColumn
import javax.swing.table.TableColumnModel

fun <TCM : TableColumnModel> TCM.forEachColumnIndexed(block : (Int, TableColumn) -> Unit) {
    val iterator = columns.iterator()
    var count = 0
    while(iterator.hasNext()) {
        block(count, iterator.next())
        count++
    }
}