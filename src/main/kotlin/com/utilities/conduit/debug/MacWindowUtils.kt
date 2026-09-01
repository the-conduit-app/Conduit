package com.utilities.conduit.debug

import com.sun.jna.Library
import com.sun.jna.Native
import com.sun.jna.Pointer
import com.utilities.conduit.utils.AppUtils

private interface MacWindowLibrary : Library {
    fun macwindow_hide_title(window: Pointer?)
}

private val macWindow = Native.load(
    AppUtils.getNativeLibDir("libmacwindow.dylib"),
    MacWindowLibrary::class.java
)

object MacWindowUtils {

    fun hideTitle(windowHandle: Long) {
        macWindow.macwindow_hide_title(Pointer(windowHandle))
    }
}
