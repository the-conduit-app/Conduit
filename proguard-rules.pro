-dontoptimize
  
# JNA uses JNI/reflection internally.
-keep class com.sun.jna.** { *; }
-keep class com.sun.jna.platform.** { *; }
-keep class com.sun.jna.ptr.** { *; }
-keep class com.sun.jna.win32.** { *; }

-keepclasseswithmembers class * {
    native <methods>;
}

-keep interface com.utilities.conduit.portals.ConduitLib {
    *;
}

-keep interface com.utilities.conduit.portals.ConduitTokenCallback {
    *;
}

-keep interface com.utilities.conduit.ui.MacWindowLibrary {
    *;
}

-keep interface com.utilities.conduit.ui.MagnificationCallback {
    *;
}

