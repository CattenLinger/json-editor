package net.catten.property.editor.utils

import java.net.URL

object ResourceLoader {
    fun getResourceUrl(name : String) : URL? = Thread.currentThread().contextClassLoader.getResource(name)
}