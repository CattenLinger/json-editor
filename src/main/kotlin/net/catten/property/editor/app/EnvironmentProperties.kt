package net.catten.property.editor.app

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.LinkedList
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

abstract class EnvironmentProperties {
    private val logger: Logger = LoggerFactory.getLogger(this::class.java)

    protected fun provider(config : PropertyProvider.Configuration.() -> Unit) : PropertyProvider {
        return PropertyProvider(config)
    }

    fun PropertyProvider.Configuration.ifNotFoundThenLog(append : String = "") {
        whenNotFound = { src -> logger.info("{} not found. {}", src.joinToString(" or ") { """"${it.keyName}(${it.sourceName})"""" } , append) }
    }

    fun PropertyProvider.Configuration.ifNotFoundThenLog(provider : (List<PropertySource>) -> String) {
        whenNotFound = { src -> logger.info("{}", provider(src)) }
    }

    fun PropertyProvider.Configuration.ifNotFoundThen(action : (List<PropertySource>) -> Unit) {
        whenNotFound = action
    }

    class PropertyProvider(config : Configuration.() -> Unit) : ReadOnlyProperty<Any?, String?> {
        private val sources : List<PropertySource>
        private val whenNotFound : (List<PropertySource>) -> Unit

        private var cache : String? = null
        private val cached = AtomicBoolean(false)

        override fun getValue(thisRef: Any?, property: KProperty<*>): String? {
            if(cached.get()) return cache
            if(!reloadValue()) whenNotFound(sources)
            return cache
        }

        fun reloadValue(): Boolean {
            for(provider in sources) {
                cache = provider.provide() ?: continue
                cached.set(true)
                return true
            }
            cached.set(true)
            return false
        }

        init {
            val c = Configuration().apply(config)
            sources = c.providers
            whenNotFound = c.whenNotFound ?: { /* Do nothing */ }
        }

        class Configuration {
            internal val providers = LinkedList<PropertySource>()
            var whenNotFound : ((List<PropertySource>) -> Unit)? = null

            fun property(key : String) = providers.add(SystemProperty(key))
            fun environment(key : String) = providers.add(EnvProperty(key))

            private class SystemProperty(key : String) : PropertySource(key) {
                override val sourceName = "System Property"
                override fun provide(): String? = System.getProperty(keyName)
            }

            private class EnvProperty(key: String) : PropertySource(key) {
                override val sourceName = "Environment Variable"
                override fun provide(): String? = System.getenv(keyName)
            }
        }
    }

    abstract class PropertySource(val keyName : String) {
        abstract fun provide() : String?
        abstract val sourceName : String
    }
}