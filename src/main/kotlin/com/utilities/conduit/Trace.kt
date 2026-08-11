package com.utilities.conduit

import java.util.concurrent.atomic.AtomicInteger

// Old style debugging helper
object Trace {
    private val seq = AtomicInteger(0)

    fun log(message: String) {
        val timestamp = System.currentTimeMillis()
        println("${seq.incrementAndGet()}. $timestamp " + "${Thread.currentThread().name}: $message")
    }
}
