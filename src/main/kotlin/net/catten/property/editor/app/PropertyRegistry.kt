package net.catten.property.editor.app

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.*
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

abstract class PropertyRegistry(private val storage: MutableMap<String, String> = mutableMapOf()) {
    private val registeredFields = mutableMapOf<String, TypedPropertyFieldBase<out Any?>>()
    private val logger: Logger = LoggerFactory.getLogger(this::class.java)

    fun addObserver(key : String, observer : (key : String, oldValue : Any?, newValue : Any?) -> Unit) : Boolean {
        val field = registeredFields[key] ?: return false
        field.observers.add(observer)
        return true
    }

    fun removeObserver(key: String, observer: (key: String, oldValue: Any?, newValue: Any?) -> Unit) : Boolean {
        val field = registeredFields[key] ?: return false
        return field.observers.remove(observer)
    }

    protected fun <T> register(key : String, serialize: (T) -> String, deserialize: (String) -> T): TypedPropertyFieldNullable<T> {
        if (registeredFields.containsKey(key))
            logger.warn("Key '{}' was registered. Duplicated registration will replace the origin one.", key)

        val field = TypedPropertyFieldNullable(key, this, serialize, deserialize)
        registeredFields[key] = field
        return field
    }

    abstract class TypedPropertyFieldBase<T> internal constructor(
        val key: String, internal val registry: PropertyRegistry,
        internal val serialize: (T) -> String, internal val deserialize: (String) -> T
    ) {
        internal val observers: MutableList<(String, Any?, Any?) -> Unit> by lazy { LinkedList() }

        protected fun notifyChange(key: String, oldValue : T?, newValue : T?) {
            observers.forEach { it(key, oldValue, newValue) }
        }
    }

    class TypedPropertyFieldNotNull<T>(
        key: String, registry: PropertyRegistry, serialize: (T) -> String, deserialize: (String) -> T, val defaultValue: T
    ) : TypedPropertyFieldBase<T>(key, registry, serialize, deserialize), ReadWriteProperty<PropertyRegistry, T> {

        override fun getValue(thisRef: PropertyRegistry, property: KProperty<*>): T {
            val str = registry.storage[key] ?: return defaultValue
            return deserialize(str)
        }

        override fun setValue(thisRef: PropertyRegistry, property: KProperty<*>, value: T) {
            val oldValue = registry.storage[key]?.let(deserialize)
            registry.storage[key] = serialize(value)
            notifyChange(key, oldValue, value)
        }
    }

    class TypedPropertyFieldNullable<T>(
        key: String, registry: PropertyRegistry, serialize: (T) -> String, deserialize: (String) -> T
    ) : TypedPropertyFieldBase<T>(key, registry, serialize, deserialize), ReadWriteProperty<PropertyRegistry, T?> {
        override fun getValue(thisRef: PropertyRegistry, property: KProperty<*>): T? {
            val str = registry.storage[key] ?: return null
            return deserialize(str)
        }

        override fun setValue(thisRef: PropertyRegistry, property: KProperty<*>, value: T?) {
            val oldValue = registry.storage[key]?.let(deserialize)
            if (value == null) {
                registry.storage.remove(key)
                notifyChange(key, oldValue, null)
                return
            }

            thisRef.storage[key] = serialize(value)
            notifyChange(key, oldValue, value)
        }

        fun withDefault(default: T) = TypedPropertyFieldNotNull(key, registry, serialize, deserialize, default).also {
            registry.registeredFields[key] = it
        }
    }

    fun validate() {
        for ((key, field) in registeredFields.entries) {
            val value = storage[key] ?: continue
            try {
                field.deserialize(value)
            } catch (e: Exception) {
                throw PropertyTypeMismatchingException("Invalid value for key '${key}'.", e)
            }
        }
    }

    fun values(): Map<String, String> = storage.toMap()

    fun putAll(map: Map<String, String>) = storage.putAll(map).also { validate() }

    fun replaceWith(map: Map<String, String>) {
        storage.clear()
        storage.putAll(map)
        validate()
    }

    class PropertyTypeMismatchingException(message: String, cause: Throwable? = null) : Exception(message, cause)

    protected fun integer(key: String) = register(key, { it.toString() }, { it.toInt() })

    protected fun string(key: String) = register(key, { it }, { it })

    protected fun boolean(key: String) = register(key, { it.toString() }, { it.toBoolean() })

    protected fun float(key: String) = register(key,  { it.toString() }, { it.toFloat() })

    protected fun double(key: String) = register(key,  { it.toString() }, { it.toDouble() })

    protected fun long(key: String) = register(key,  { it.toString() }, { it.toLong() })
}