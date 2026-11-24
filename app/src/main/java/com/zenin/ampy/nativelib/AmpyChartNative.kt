package com.zenin.ampy.nativelib

/**
 * JNI bridge to libampy_chart.so.
 *
 * Downsampling is optional at runtime: if the library cannot be loaded,
 * callers draw the original point series.
 */
object AmpyChartNative {
    private val loaded: Boolean = try {
        System.loadLibrary("ampy_chart")
        true
    } catch (t: Throwable) {
        false
    }

    val isAvailable: Boolean get() = loaded

    external fun downsample(
        timestamps: LongArray,
        power: FloatArray,
        temperature: FloatArray,
        buckets: Int
    ): FloatArray?
}
