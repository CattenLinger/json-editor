package net.catten.property.editor.app.views.editor

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.JsonNodeType

abstract class ConfigValue(val name : String) {
    abstract val value : Any?
    abstract val typeName : String

    class Text(override val value : String) : ConfigValue("text") {
        override val typeName: String = "string"
    }
    class Number(override val value : kotlin.Number) : ConfigValue("number") {
        override val typeName: String = "number"
    }

    class Boolean(override val value: kotlin.Boolean) : ConfigValue("boolean") {
        override val typeName: String = "boolean_$value"
    }

    object Null : ConfigValue("null") {
        override val value: Any? = null
        override val typeName: String = "null"
    }

    companion object {
        fun wrap(json : JsonNode) : ConfigValue = when(json.nodeType) {
            JsonNodeType.BOOLEAN -> Boolean(json.booleanValue())
            JsonNodeType.NULL -> Null
            JsonNodeType.NUMBER -> Number(json.numberValue())
            JsonNodeType.STRING -> Text(json.textValue())
            else -> throw IllegalArgumentException("Unsupported value type: ${json.nodeType}")
        }
    }
}