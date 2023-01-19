package net.catten.property.editor.framework

import java.util.*
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

open class PropertyRegistry {
    private val registeredFields: MutableMap<PropertyKey<*>, PropertyValue<*>> = mutableMapOf()
    private val values = mutableMapOf<String, Any?>()

    open class PropertyKey<T>(val name: String, val serializer: (T) -> String, val deserializer: (String) -> T)

    @FunctionalInterface
    interface PropertyValueUpdateObserver<T> {
        fun onValueUpdate(key: PropertyKey<T>, newValue: T?, oldValue: T?)
    }

    private fun <T> notifyValueUpdate(source: PropertyValue<T>, oldValue: T?, newValue: T?) {
        source.observers.forEach { it.onValueUpdate(source.key, oldValue, newValue) }
    }

    class PropertyValue<T>(private val registry: PropertyRegistry, val key: PropertyKey<T>) : ReadWriteProperty<Any?, T?> {
        @Suppress("UNCHECKED_CAST")
        private var value: T?
            get() = registry.values[key.name] as T?
            set(value) { registry.values[key.name] = value }

        internal val observers = LinkedList<PropertyValueUpdateObserver<T>>()

        fun serialize() = value?.let(key.serializer)
        fun deserialize(string: String?) {
            value = string?.let(key.deserializer)
        }

        override fun getValue(thisRef: Any?, property: KProperty<*>): T? = value

        override fun setValue(thisRef: Any?, property: KProperty<*>, value: T?) {
            val oldValue = this.value
            if (oldValue == value) return

            this.value = value
            registry.notifyValueUpdate(this, oldValue, value)
        }
    }

    operator fun <T> get(key: PropertyKey<T>): PropertyValue<T> = register(key)

    private fun <T> assertKeyIsOwnedByValue(key: PropertyKey<T>, value: PropertyValue<*>): PropertyValue<T> {
        if (value.key != key) throw IllegalStateException("Provided key instance and the registered are not the same one")
        @Suppress("UNCHECKED_CAST")
        return value as PropertyValue<T>
    }

    fun <T> addPropertyObserver(key: PropertyKey<T>, observer: PropertyValueUpdateObserver<T>): PropertyValueUpdateObserver<T>? {
        val field = registeredFields[key]?.let { assertKeyIsOwnedByValue(key, it) } ?: return null
        field.observers.add(observer)
        return observer
    }

    fun <T> removePropertyObserver(key: PropertyKey<T>, observer: PropertyValueUpdateObserver<T>): Boolean {
        val field = registeredFields[key]?.let { assertKeyIsOwnedByValue(key, it) } ?: return false
        return field.observers.remove(observer)
    }

    @Synchronized
    protected fun <T> register(key: PropertyKey<T>): PropertyValue<T> = when (val value = registeredFields[key]) {
        null -> {
            if (registeredFields.keys.count { it.name == key.name } > 0) error("Duplicated key name registration: ${key.name}")
            PropertyValue(this, key).also { registeredFields[key] = it }
        }

        else -> assertKeyIsOwnedByValue(key, value)
    }

    fun deserialize(map: Map<String, String>) {
        for ((key, field) in registeredFields.entries) {
            val value = map[key.name] ?: continue
            try {
                field.deserialize(value)
            } catch (e: Exception) {
                throw PropertyTypeMismatchingException("Invalid value '${value}' for key '${key}'.", e)
            }
        }
    }

    fun serialize(): Map<String, String?> = registeredFields.entries.associate { (key, value) -> key.name to value.serialize() }.toMap()

    class PropertyTypeMismatchingException(message: String, cause: Throwable? = null) : Exception(message, cause)
}