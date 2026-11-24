package com.zenin.ampy.data.repository

import android.os.Build
import android.util.Log
import com.zenin.ampy.data.parser.FallbackLogParser
import com.zenin.ampy.data.parser.LogParser
import com.zenin.ampy.data.parser.MotoLogParser
import com.zenin.ampy.data.parser.OPlusLogParser
import com.zenin.ampy.data.parser.XiaomiLogParser
import com.zenin.ampy.nativelib.AmpyLogNative
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class BatteryLogRepository @Inject constructor() {
    // Xiaomi is the primary parser. OnePlus/OPlus and Motorola parsers remain
    // as fallbacks so those devices keep working.
    private val defaultParsers = listOf(
        XiaomiLogParser(),
        OPlusLogParser(),
        FallbackLogParser()
    )

    private val allParsers: Map<String, List<LogParser>> = mapOf(
        "xiaomi" to defaultParsers,
        "oneplus" to defaultParsers,
        "oppo" to defaultParsers,
        "realme" to defaultParsers,
        "motorola" to listOf(XiaomiLogParser(), MotoLogParser(), FallbackLogParser())
    )

    private data class NativeSpec(val marker: String, val mode: Int)

    private fun LogParser.nativeSpec(): NativeSpec? = when (this) {
        is XiaomiLogParser -> NativeSpec("chargerType=", AmpyLogNative.MODE_XIAOMI)
        is OPlusLogParser -> NativeSpec("type", AmpyLogNative.MODE_OPLUS)
        is MotoLogParser -> NativeSpec("uevent", AmpyLogNative.MODE_MOTO)
        is FallbackLogParser -> NativeSpec("=", AmpyLogNative.MODE_FALLBACK)
        else -> null
    }

    internal fun parsersForManufacturer(manufacturer: String?): List<LogParser> =
        allParsers[manufacturer?.lowercase()] ?: defaultParsers

    private fun parsersForDevice(): List<LogParser> =
        parsersForManufacturer(Build.MANUFACTURER)

    suspend fun getParsedLogcatData(): Map<String, String>? =
        getParsedDataFromLines(fetchLiveLogcatLines())

    private suspend fun fetchLiveLogcatLines(): List<String> = withContext(Dispatchers.IO) {
        val parsers = parsersForDevice()
        val filters = parsers.map { "${it.tagFilter}:D" } + "*:S"
        val cmd = "logcat -d -v raw ${filters.joinToString(" ")}"
        try {
            // Android only returns log lines this app is allowed to read.
            Runtime.getRuntime().exec(cmd).inputStream.bufferedReader().useLines { it.toList() }
        } catch (e: Exception) {
            Log.w("BatteryLogRepository", "logcat unavailable: ${e.message}")
            emptyList()
        }
    }

    suspend fun getParsedDataFromLines(lines: List<String>): Map<String, String>? =
        withContext(Dispatchers.Default) {
            val parsers = parsersForDevice()
            Log.d("BatteryLogRepository", "Parsers = ${parsers.map { it::class.simpleName }}")

            if (AmpyLogNative.isAvailable) {
                val specs = parsers.mapNotNull { it.nativeSpec() }
                if (specs.size == parsers.size) {
                    try {
                        val nativeResult = AmpyLogNative.parseFirst(
                            lines.toTypedArray(),
                            specs.map { it.marker }.toTypedArray(),
                            IntArray(specs.size) { specs[it].mode }
                        )
                        // null here means "no matching line", not "native failed".
                        return@withContext nativeResult
                    } catch (t: Throwable) {
                        Log.w("BatteryLogRepository", "native parser failed: ${t.message}")
                    }
                }
            }

            lines.asReversed().firstNotNullOfOrNull { line ->
                parsers.firstNotNullOfOrNull { parser ->
                    parser.takeIf { it.matches(line) }
                        ?.parse(line)
                        ?.takeIf { it.isNotEmpty() }
                }
            }
        }
}
