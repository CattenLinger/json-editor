package net.catten.property.editor.framework

/** Try to get value from a provider, null if failed */
fun <T> tryGet(onError : (Exception) -> Unit = {}, block: () -> T) : T? = try { block() } catch (e : Exception) { onError(e); null }