package net.catten.property.editor.utils

import java.awt.Window
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import javax.swing.SwingUtilities
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

fun swingInvokeLater(block : () -> Unit) = SwingUtilities.invokeLater(block)

fun swingInvokeAndWait(block : () -> Unit) = SwingUtilities.invokeAndWait(block)

object MouseEventListeners {
    fun mousePressed(handler : (MouseEvent) -> Unit) = object : MouseAdapter() {
        override fun mousePressed(e: MouseEvent) = handler(e)
    }
}

fun Window.addWindowClosingListener(block : (WindowEvent) -> Unit) = addWindowListener(object : WindowAdapter() {
    override fun windowClosing(e: WindowEvent) = block(e)
})

fun <T> ReadWriteProperty<Any?, T?>.withDefault(defaultValueProvider : () -> T) = object : ReadWriteProperty<Any?, T> {
    var origin : T? by this@withDefault

    override fun getValue(thisRef: Any?, property: KProperty<*>): T {
        return origin ?: defaultValueProvider()
    }

    override fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
        origin = value
    }
}

fun <T> tryDo(onError : (Exception) -> Unit = {}, block: () -> T) : T? = try { block() } catch (e : Exception) { onError(e); null }