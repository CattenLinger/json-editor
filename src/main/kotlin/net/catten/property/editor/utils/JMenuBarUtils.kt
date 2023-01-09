package net.catten.property.editor.utils

import javax.swing.JMenu
import javax.swing.JMenuBar
import javax.swing.JMenuItem

fun jMenuBar(menuBar : JMenuBar = JMenuBar(), builder : JMenuBar.() -> Unit) : JMenuBar {
    menuBar.builder()
    return menuBar
}

fun JMenuBar.menu(title : String, builder : JMenu.() -> Unit) : JMenu {
    val menu = JMenu(title)
    menu.builder()
    add(menu)
    return menu
}

fun JMenu.separator() = addSeparator()

fun JMenu.menuItem(title : String, builder : JMenuItem.() -> Unit) : JMenuItem {
    val item = JMenuItem(title)
    item.builder()
    add(item)
    return item
}