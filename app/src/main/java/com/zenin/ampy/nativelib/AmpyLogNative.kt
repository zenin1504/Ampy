package com.zenin.ampy.nativelib

/**
 * JNI bridge to libampy_logparse.so.
 *
 * Parsing is optional at runtime: if the library cannot be loaded, callers
 * fall back to the existing Kotlin parsers.
 */
object AmpyLogNative {
    const val MODE_XIAOMI = 1
    const val MODE_OPLUS = 2
    const val MODE_FALLBACK = 3
    const val MODE_MOTO = 4

    private val loaded: Boolean = try {
        System.loadLibrary("ampy_logparse")
        true
    } catch (t: Throwable) {
        false
    }

    val isAvailable: Boolean get() = loaded

    external fun parseFirst(
        lines: Array<String>,
        markers: Array<String>,
        modes: IntArray
    ): Map<String, String>?
}
