package net.catten.property.editor.app.editor

import net.catten.property.editor.components.JsonNodeWrapper

interface JsonNodeWrapperTreeModel {
    var displayName: String
    var displayType: String
    val children: MutableList<JsonNodeWrapperTreeModel>

    class Root(name: String) : JsonNodeWrapperTreeModel {
        override val children: MutableList<JsonNodeWrapperTreeModel> = mutableListOf()
        override var displayType: String = "object"
        override var displayName: String = name
    }

    class Value(val value: JsonNodeWrapper.Value) : JsonNodeWrapperTreeModel {
        override var displayType: String = value.json.nodeType.name.lowercase()
        override var displayName: String = value.toString()
        override val children: MutableList<JsonNodeWrapperTreeModel> = mutableListOf()
    }

    class Entry(val entry: JsonNodeWrapper.Entry<*>) : JsonNodeWrapperTreeModel {
        override var displayType: String = entry.value.json.nodeType.name.lowercase()
        override var displayName: String = entry.key.toString()
        override val children: MutableList<JsonNodeWrapperTreeModel> by lazy {
            when (val value = entry.value) {
                is JsonNodeWrapper.Value -> mutableListOf(Value(value))
                is JsonNodeWrapper.Container<*> -> from(value)
                else -> throw IllegalArgumentException("unknown child type ${value::class.java}")
            }
        }

        companion object {
            private fun from(wrapper: JsonNodeWrapper.Container<*>): MutableList<JsonNodeWrapperTreeModel> {
                return wrapper.children.map { Entry(it) }.toMutableList()
            }
        }
    }
}