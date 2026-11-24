package com.zenin.ampy.data.model

import androidx.annotation.StringRes
import com.zenin.ampy.R

enum class BatteryInfoType(
    val key: String,
    @StringRes val titleRes: Int
) {
    LEVEL                    ("Level",                     R.string.battery_level),
    TEMP                     ("Temperature",               R.string.battery_temperature),
    STATUS                   ("Status",                    R.string.battery_status),
    HEALTH                   ("Health",                    R.string.battery_health),
    CYCLE_COUNT              ("Cycle Count",               R.string.battery_cycle_count),
    VOLTAGE                  ("Voltage",                   R.string.battery_voltage),
    CURRENT                  ("Current",                   R.string.battery_current),
    POWER                    ("Power",                     R.string.battery_power)
}
