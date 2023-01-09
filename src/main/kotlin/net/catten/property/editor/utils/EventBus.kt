package net.catten.property.editor.utils

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import java.awt.EventQueue
import java.lang.ref.WeakReference
import java.util.WeakHashMap
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.locks.ReentrantLock

object EventBus {
    private val scope = CoroutineScope(Dispatchers.Main)

    val listeners = ConcurrentHashMap<String, ListenerHub>()

    open class Event(val source : Any)

    class ListenerHub() {
        private val map = WeakHashMap<String, suspend (Event) -> Unit>()

    }
}