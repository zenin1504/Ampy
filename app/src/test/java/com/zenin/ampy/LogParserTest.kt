package com.zenin.ampy


import com.zenin.ampy.data.parser.FallbackLogParser
import com.zenin.ampy.data.parser.LogParser
import com.zenin.ampy.data.parser.MotoLogParser
import com.zenin.ampy.data.parser.OPlusLogParser
import com.zenin.ampy.data.parser.XiaomiLogParser
import com.zenin.ampy.data.repository.BatteryLogRepository
import kotlin.test.*                   // assertEquals, assertTrue…
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.*
import org.junit.jupiter.params.provider.MethodSource
import java.nio.file.Files

data class Sample(
    val name: String,
    val parser: LogParser,
    val resource: String,
    val keyToAssert: String,
)

class LogParserTest {

    companion object {

        @JvmStatic
        fun samples() = listOf(
            Sample(
                name = "OPlus",
                parser = OPlusLogParser(),
                resource = "logcats/oplus_sample",
                keyToAssert = "fcc"
            ),
            Sample(
                name = "Moto",
                parser = MotoLogParser(),
                resource = "logcats/moto_sample",
                keyToAssert = "POWER_SUPPLY_VOLTAGE_NOW"
            ),
            Sample(
                name = "Xiaomi",
                parser = XiaomiLogParser(),
                resource = "logcats/xiaomi_sample",
                keyToAssert = "batteryLevel"
            ),
//            Sample(
//                name = "Fallback",
//                parser = FallbackLogParser(),
//                resource = "logs/fallback_sample.log",
//                keyToAssert = "voltage_now"
//            )
        )
    }

    @ParameterizedTest(name = "{0} sample should be parsed")
    @MethodSource("samples")
    fun parseWholeFile(sample: Sample) {
        val (name, parser, res, key) = sample

        val url = javaClass.classLoader!!.getResource(res)
            ?: error("Resource $res not found")
        val lines = Files.lines(java.nio.file.Paths.get(url.toURI()))

        val maps = lines.filter(parser::matches)
            .map(parser::parse)
            .toList()

        assertTrue(maps.isNotEmpty(), "$name: no line matched")
        assertTrue(maps.any { it.containsKey(key) },
            "$name: key '$key' not found in parsed maps")
    }

    @Test
    fun xiaomiIsPrimaryParserEverywhere() {
        val repo = BatteryLogRepository()

        for (manufacturer in listOf("Xiaomi", "OnePlus", "oppo", "realme", "UnknownBrand", null)) {
            val parsers = repo.parsersForManufacturer(manufacturer)
            assertTrue(parsers.first() is XiaomiLogParser,
                "$manufacturer should try Xiaomi first")
            assertTrue(parsers.any { it is OPlusLogParser },
                "$manufacturer should keep OPlus as a fallback")
        }

        val motoParsers = repo.parsersForManufacturer("motorola")
        assertTrue(motoParsers.first() is XiaomiLogParser)
        assertTrue(motoParsers.any { it is MotoLogParser })
    }
}