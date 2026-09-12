package com.utilities.conduit.ui

import com.sun.jna.Library
import com.sun.jna.Native
import com.sun.jna.Pointer
import com.utilities.conduit.utils.AppUtils

private interface MacWindowLibrary : Library {
    fun macwindow_hide_title(window: Pointer?)
    fun macwindow_style_titlebar(window: Pointer?)
}

private val macWindowLib = Native.load(
    AppUtils.getNativeLibDir("libmacwindow.dylib"),
    MacWindowLibrary::class.java
)

object MacWindowUtils {

    fun hideTitle(windowHandle: Long) {
        macWindowLib.macwindow_hide_title(Pointer(windowHandle))
    }

    fun styleTitlebar(windowHandle: Long) {
        macWindowLib.macwindow_style_titlebar(Pointer(windowHandle))
    }
}
