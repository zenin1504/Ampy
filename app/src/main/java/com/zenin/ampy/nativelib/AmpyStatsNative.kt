package com.zenin.ampy.nativelib

data class PowerStats(
    val average: Float,
    val peak: Float,
    val samples: Int
)

/**
 * JNI bridge to libampy_stats.so.
 *
 * The native side keeps a fixed-size rolling window and maintains sum, min
 * and max in amortized O(1) per sample, so the dashboard does not need to
 * rescan the whole chart history on every refresh.
 */
object AmpyStatsNative {
    private val loaded: Boolean = try {
        System.loadLibrary("ampy_stats")
        true
    } catch (t: Throwable) {
        false
    }

    val isAvailable: Boolean get() = loaded

    private external fun create(capacity: Int): Long
    private external fun push(handle: Long, value: Float): FloatArray?
    private external fun reset(handle: Long)
    private external fun destroy(handle: Long)

    fun createStats(capacity: Int): Long = if (loaded) {
        try {
            create(capacity)
        } catch (t: Throwable) {
            -1L
        }
    } else {
        -1L
    }

    fun pushStats(handle: Long, value: Float): PowerStats? {
        if (!loaded || handle <= 0L) return null
        return try {
            val packed = push(handle, value) ?: return null
            if (packed.size < 5) return null
            PowerStats(
                average = packed[1],
                peak = packed[3],
                samples = packed[4].toInt()
            )
        } catch (t: Throwable) {
            null
        }
    }

    fun resetStats(handle: Long) {
        if (!loaded || handle <= 0L) return
        try {
            reset(handle)
        } catch (t: Throwable) {
            // Ignore: the dashboard falls back to Kotlin-side statistics.
        }
    }

    fun destroyStats(handle: Long) {
        if (!loaded || handle <= 0L) return
        try {
            destroy(handle)
        } catch (t: Throwable) {
            // Nothing useful to do while disposing.
        }
    }
}
