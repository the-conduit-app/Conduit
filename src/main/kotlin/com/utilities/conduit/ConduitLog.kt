package com.utilities.conduit

import com.utilities.conduit.utils.AppUtils
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

object ConduitLog {
    private const val MAX_LINES = 100
    private val logFile = File(AppUtils.getAppDir(), "conduit.log")

    private val timestamp = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS")

    @Synchronized
    fun error(message: String, throwable: Throwable? = null) {
        write("ERROR", message, throwable)
    }

    @Synchronized
    fun info(message: String) {
        write("INFO", message)
    }

    private fun write(level: String, message: String, throwable: Throwable? = null) {
        try {
            val entry = buildString {
                append(LocalDateTime.now().format(timestamp))
                append(" ")
                append(level)
                append(" ")
                append(message)

                if (throwable != null) {
                    append('\n')
                    val sw = StringWriter()
                    throwable.printStackTrace(PrintWriter(sw))
                    append(sw.toString())
                }
                append('\n')
            }

            val lines = if (logFile.exists()) { logFile.readLines().toMutableList() } else { mutableListOf() }
            lines.addAll(entry.lines())
            val retained = lines.takeLast(MAX_LINES)
            logFile.writeText(retained.joinToString("\n") + "\n")

        } catch (_: Exception) {
            // Logging must never cause another failure.
        }
    }
}
