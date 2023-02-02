package net.catten.property.editor.framework

import net.catten.property.editor.utils.tryDo

interface SimpleProcessInformation  {

    val processId: Long
    val command : String
    val arguments : Array<String>

    companion object {
        private class SimpleProcessInformationImpl(override val processId: Long, override val command: String, override val arguments: Array<String>) :
            SimpleProcessInformation

        fun from(handle : ProcessHandle) : SimpleProcessInformation? {
            val pid = tryGet { handle.pid() } ?: return null
            val info = tryGet { handle.info() } ?: return null
            val command = tryGet { info.command().orElse(null) } ?: return null
            val arguments = tryGet { info.arguments().orElse(emptyArray()) } ?: return null

            return SimpleProcessInformationImpl(pid, command, arguments)
        }

        fun of(pid : Long) = tryDo { ProcessHandle.of(pid).map(Companion::from).orElse(null) }

        fun current() = tryDo { from(ProcessHandle.current()) }
    }
}