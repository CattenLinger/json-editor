package net.catten.property.editor.app.views.editor

import java.io.File
import kotlin.properties.Delegates

@Suppress("FunctionName")
abstract class ConfigValueContainer(val type : Type) {
    abstract val title : String

    var values : MutableMap<String, ConfigValue> = mutableMapOf()

    enum class Type(val displayName : String) {
        Dictionary("object"), Array("list")
    }

    class Root(val format : ConfigValueContainerFormat, type : Type) : ConfigValueContainer(type) {
        var file : File? by Delegates.observable(null) { _,_, new -> /* Do nothing now */ }

        override val title: String
            get() = file?.name ?: "<un-named ${format.name.lowercase()} file>"
    }

    class Container(type : Type) : ConfigValueContainer(type) {
        var key : String? = null

        override val title: String
            get() = key ?: "<un-named ${type.displayName.lowercase()}>"
    }

    companion object {
        fun JsonRoot(type : Type = Type.Dictionary) = Root(ConfigValueContainerFormat.Json, type)

        fun YamlRoot(type : Type = Type.Dictionary) = Root(ConfigValueContainerFormat.Yaml, type)
    }
}