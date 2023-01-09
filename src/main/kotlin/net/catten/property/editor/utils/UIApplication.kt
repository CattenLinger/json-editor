package net.catten.property.editor.utils

import java.util.concurrent.ConcurrentHashMap

class UIApplication(val commandLineArguments : Array<String>) {

    val registry = Registry()

    class Registry {
        private val cache = ConcurrentHashMap<String, String>()

        private val serializers = ConcurrentHashMap<Class<*>, StringPropertySerializer<*>>()

        fun <T> registerSerializer(clazz : Class<T>, serializer: StringPropertySerializer<T>) { serializers[clazz] = serializer }
        inline fun <reified T> registerSerializer(serializer: StringPropertySerializer<T>) = registerSerializer(T::class.java, serializer)

    }
}