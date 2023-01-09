package net.catten.property.editor.components

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.JsonNodeType
import com.fasterxml.jackson.databind.node.MissingNode
import com.fasterxml.jackson.databind.node.POJONode
import java.io.Serializable

interface JsonNodeWrapper {
    val json : JsonNode

    /* Values */

    open class Value(override val json: JsonNode) : JsonNodeWrapper {
        override fun toString(): String = json.toString()
    }

    object Undefined : Value(MissingNode.getInstance()) {
        override fun toString(): String = "<undefined>"
    }

    class Binary(json : JsonNode) : Value(json) {
        val data: ByteArray by lazy { json.binaryValue() }
    }

    class RawObject(json : JsonNode) : Value(json) {
        val raw: Any by lazy {
            require(json is POJONode) { "${json::class.java.name} is not a POJONode!" };
            json.pojo
        }
    }

    /* Containers */

    abstract class Container<K : Serializable>(override val json : JsonNode) : JsonNodeWrapper {
        abstract val children : List<Entry<K>>
        override fun toString(): String = "<${json.nodeType.name.lowercase()}>"
    }

    class Array(json: JsonNode) : Container<Int>(json) {
        override val children = json.mapIndexed { index, value -> Entry(index, wrap(value), this) }
    }

    class Object(json : JsonNode) : Container<String>(json) {
        override val children = json.fields().asSequence().map { (key, value) -> Entry(key, wrap(value), this) }.toList()
    }

    /* Container entry */

    class Entry<K : Serializable>(val key : K, val value: JsonNodeWrapper, val container : Container<K>) {
        override fun toString(): String = "${if (container is Array) String.format("[% 4d]", key) else "$key:"} $value"
    }

    companion object {
        fun wrap(json : JsonNode) : JsonNodeWrapper = when(json.nodeType!!) {
            /* Containers */
            JsonNodeType.ARRAY -> Array(json)
            JsonNodeType.OBJECT -> Object(json)

            /* value nodes */
            JsonNodeType.BOOLEAN,
            JsonNodeType.NULL,
            JsonNodeType.NUMBER,
            JsonNodeType.STRING -> Value(json)

            /* Special nodes */
            JsonNodeType.BINARY -> Binary(json)
            JsonNodeType.POJO -> RawObject(json)
            JsonNodeType.MISSING -> Undefined
        }
    }
}