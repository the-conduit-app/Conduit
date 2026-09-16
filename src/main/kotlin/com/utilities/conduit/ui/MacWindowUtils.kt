package com.utilities.conduit.ui

import com.sun.jna.Callback
import com.sun.jna.Library
import com.sun.jna.Native
import com.sun.jna.Pointer
import com.utilities.conduit.utils.AppUtils

private interface MacWindowLibrary : Library {
    fun macwindow_hide_title(window: Pointer?)
    fun macwindow_style_titlebar(window: Pointer?)

    fun macwindow_set_magnification_callback(
        window: Pointer?,
        callback: MagnificationCallback?
    )
}
private fun interface MagnificationCallback : Callback {
    fun invoke(magnification: Double)
}

private val macWindowLib = Native.load(
    AppUtils.getNativeLibDir("libmacwindow.dylib"),
    MacWindowLibrary::class.java
)

object MacWindowUtils {
    private var magnificationCallback: MagnificationCallback? = null
    private var magnificationListener: ((Double) -> Unit)? = null

    fun hideTitle(windowHandle: Long) {
        macWindowLib.macwindow_hide_title(Pointer(windowHandle))
    }

    fun styleTitlebar(windowHandle: Long) {
        macWindowLib.macwindow_style_titlebar(Pointer(windowHandle))
    }

    fun setMagnificationCallback(windowHandle: Long) {
        magnificationCallback = MagnificationCallback { magnification ->
            magnificationListener?.invoke(magnification)
        }

        macWindowLib.macwindow_set_magnification_callback(Pointer(windowHandle), magnificationCallback)
    }

    fun setMagnificationListener(listener: ((Double) -> Unit)?) {
        magnificationListener = listener
    }
}
