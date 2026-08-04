package com.utilities.conduit

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.sun.jna.Pointer
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File
import java.io.FileNotFoundException

/**
 * Runtime state shared by all Experts using the same GGUF model.
 * Ensures the model is loaded only once and owns the shared session pointer
 * and loading state.
 */
enum class ModelStatus { NONE, LOADING, READY, GENERATING, FAILED }

class ModelState internal constructor(val modelPath: String) {
    val mutex = Mutex()

    // Status
    @Volatile
    private var _liveStatus = ModelStatus.NONE
    val liveStatus: ModelStatus
        get() = _liveStatus
    var status by mutableStateOf(ModelStatus.NONE)
        private set
    fun updateStatus(newStatus: ModelStatus) {
        _liveStatus = newStatus
        status = newStatus
    }

    // LLM Session pointer
    @Volatile
    private var _sessionPtr: Pointer? = null
    val p: Pointer?
        get() = _sessionPtr
    fun setSession(ptr: Pointer?) {
        _sessionPtr = ptr
    }

    // -----------------------------------------------------------------------------

    suspend fun initialize() {
        if (modelPath.startsWith(":")) { // Internal experts like :simpleEcho
            updateStatus(ModelStatus.READY)
            return
        }

        mutex.withLock {
            when (liveStatus) {
                ModelStatus.LOADING    -> return
                ModelStatus.READY      -> return
                ModelStatus.FAILED     -> return
                ModelStatus.GENERATING -> return
                ModelStatus.NONE       -> { updateStatus(ModelStatus.LOADING) }
            }
        }
        println("AppState.initialize($modelPath) called") ////

        try {
            val absPath = AppUtils.getAbsolutePathString(modelPath)
            if (!File(absPath).exists()) {
                throw FileNotFoundException("Model not found at: $absPath")
            }
            println("Calling conduit_llm_get_session") ////
            val session = LlmPortal.initialize(absPath)
            println("Exited conduit_llm_get_session")

            setSession(session)
            updateStatus(ModelStatus.READY)
        } catch (e: Exception) {
            updateStatus(ModelStatus.FAILED)
            e.printStackTrace()
        }
        println("AppState.initialize($modelPath) finished") ////
    }

    fun shutdown() {
        Trace.log("ModelState.shutdown($modelPath) ENTER")

        val ptr = _sessionPtr
        if (ptr != null) {
            Trace.log("Calling conduit_llm_free_session")
            LlmPortal.freeSession(ptr)
            Trace.log("Returned from conduit_llm_free_session")
            setSession(null)
        } else {
            Trace.log("modelState was already null")
        }

        updateStatus(ModelStatus.NONE)
        Trace.log("Status set to NONE")

        Trace.log("ModelState.shutdown($modelPath) EXIT")
    }

    suspend fun retryInitialization() {
        if (liveStatus != ModelStatus.FAILED)
            return

        shutdown()
        initialize()
    }
}
