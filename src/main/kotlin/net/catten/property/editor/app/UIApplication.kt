package net.catten.property.editor.app

import net.catten.property.editor.framework.SimpleProcessInformation
import net.catten.property.editor.utils.UIAppCriticalErrorMessage
import net.catten.property.editor.utils.addWindowClosingListener
import net.catten.property.editor.utils.promptSwingDialog
import net.catten.property.editor.utils.tryDo
import org.slf4j.LoggerFactory
import java.awt.Desktop
import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import java.nio.channels.FileLock
import java.nio.charset.StandardCharsets
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import java.util.*
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import javax.swing.JFrame
import kotlin.system.exitProcess

class UIApplication private constructor(val commandLineArguments: Array<String>, val environment: UIApplicationEnvironment) {
    // standalone mode: no system ui integration, allow multiple instance,

    val logger = LoggerFactory.getLogger(this::class.java)

    private val applicationClosing = AtomicBoolean(false)
    private val mainWindowLists = LinkedList<JFrame>()

    val ioExecutor: Executor = Executors.newWorkStealingPool()

    fun registerMainWindow(showNow: Boolean = true, mainFrameProvider: (UIApplication) -> JFrame) {
        if (applicationClosing.get()) return logger.warn("Application is closing but mainWindow creation incomes.")
        mainFrameProvider(this).also { mainWindowLists.add(it) }.apply {
            addWindowClosingListener { e ->
                val window = e.window
                e.window.dispose()
                mainWindowLists.remove(window)
                logger.info("A main window was closed.")
                onAnyMainWindowCloses()
            }
            isVisible = showNow
        }
    }

    private fun onAnyMainWindowCloses() {
        if (applicationClosing.get()) return

        if (mainWindowLists.isEmpty()) {
            applicationClosing.set(true)
            logger.info("All window closed. Application Exit.")
            registry.saveConfig()
            /* May needed to do some resource disposing actions */
            exitProcess(0)
        }
    }

    val registry = UIApplicationRegistry(environment)

    val currentProcessInfo by lazy { tryDo { ProcessHandle.current().pid() } }

    val instanceProcessInfo = registry.configurationFile?.let {
        UIApplicationInstanceProcessInformation.fromPidFilePath(Path.of(it.absolutePath, ".pid"))
    }

    class UIApplicationInstanceProcessInformation private constructor(val processId : Long, val processIdFilePath : Path, val processIdFileLock: FileLock?) {
        companion object {
            fun fromPidFilePath(path : Path) : UIApplicationInstanceProcessInformation? {
                val pidFile = path.toFile()
                // Try to lock the pid file
                val pidFileLock = tryDo { FileChannel.open(path, StandardOpenOption.WRITE).tryLock() }

                // if failed to get current process id, consider as not supported
                val currentProcessInfo = SimpleProcessInformation.current() ?: return null

                // When could not acquire the pid file lock
                if(pidFileLock == null) {
                    // if pid file not exists or not a file, consider as not supported
                    if(!pidFile.isFile) return null

                    // read the shared instance pid. if pid file content is invalid, consider as not supported
                    val instancePid = String(pidFile.readBytes(), StandardCharsets.UTF_8).toLongOrNull() ?: return null

                    // check the shared process instance. if failed to get the process handle, consider as not supported
                    val sharedInstanceProcess = SimpleProcessInformation.of(instancePid) ?: return null

                    // return the shared instance process status
                    return UIApplicationInstanceProcessInformation(instancePid, pidFile.toPath(), null)
                }

                return try {
                    FileChannel.open(path, StandardOpenOption.WRITE).tryLock()?.let { lock ->
                        val currentProcessId = ProcessHandle.current().pid()
                        val channel = lock.channel().apply {
                            val content = currentProcessId.toString().toByteArray()
                            truncate(content.size.toLong())
                            write(ByteBuffer.wrap(content))
                        }
                        Runtime.getRuntime().addShutdownHook(Thread { channel.close() })
                        UIApplicationInstanceProcessInformation(currentProcessId, path, lock)
                    }
                } catch (e : Exception) {
                    null
                }
            }
        }
    }

    companion object {
        operator fun invoke(args: Array<String> = emptyArray(), env: UIApplicationEnvironment = UIApplicationEnvironment(), block: UIApplication.() -> Unit) {
            try {
                block(UIApplication(args, env))
            } catch (e: Exception) {
                UIAppCriticalErrorMessage(
                    "Critical Error",
                    "Application bootstrap meets an critical error.\n${e.message}",
                    e
                ).promptSwingDialog(0)
            }
        }
    }
}