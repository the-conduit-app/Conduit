package com.utilities.conduit.debug

import com.sun.jna.Pointer
import com.utilities.conduit.portals.LlmPortal
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.sin

// Stress test helper object.
object CpuHammer {
    suspend fun run(durationMs: Long = 5_000) = withContext(Dispatchers.Default) {
        coroutineScope {
            val workers = Runtime.getRuntime().availableProcessors()

            println("Cpu hammer starting with $workers workers for ${durationMs/1000}s")
            repeat(workers) {
                launch(Dispatchers.Default) {
                    val end = System.nanoTime() + durationMs * 1_000_000
                    var x = 0.123456789

                    while (System.nanoTime() < end) {
                        x = x * 1.0000001 + 0.0000001
                        x = sin(x)
                        //println("x = $x")
                    }
                    println("Cpu hammer done with $workers workers for ${durationMs/1000}s")
                }
            }
        }
    }
}

// Session stress test
object SessionHammer {

    suspend fun run(conduitPtr: Pointer, modelPath: String) =
        withContext(Dispatchers.IO) {
            repeat(100) { round ->
                Trace.log("SESSION HAMMER: round ${round + 1}")

                coroutineScope {
                    repeat(32) { index ->
                        launch {
                            try {
                                Trace.log("SESSION HAMMER: creating $index session=$modelPath")

                                val session = LlmPortal.initialize(
                                    conduitPtr,
                                    modelPath
                                )

                                Trace.log("SESSION HAMMER: created $index session=$session")

                                LlmPortal.freeSession(conduitPtr, session)
                                Trace.log("SESSION HAMMER: Freed $index session=$session")

                            } catch (e: Exception) {
                                Trace.log(
                                    "SESSION HAMMER: FAILED $index: ${e.message}"
                                )
                            }
                        }
                    }
                }
            }

            Trace.log("SESSION HAMMER: DONE")
        }
}
