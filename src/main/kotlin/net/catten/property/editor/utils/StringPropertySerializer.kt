package net.catten.property.editor.utils

interface StringPropertySerializer<T> {
    fun serialize(value : T) : String
    fun deserialize(raw : String) : T
}