package com.zenin.ampy.data.util

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import com.zenin.ampy.R
import com.zenin.ampy.data.model.HistoryInfo
import com.zenin.ampy.data.repository.HistoryInfoRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun getStatusString(status: Int, context: Context): String = when (status) {
    BatteryManager.BATTERY_STATUS_CHARGING -> context.getString(R.string.charging)
    BatteryManager.BATTERY_STATUS_DISCHARGING -> context.getString(R.string.discharging)
    BatteryManager.BATTERY_STATUS_FULL -> context.getString(R.string.full)
    else -> context.getString(R.string.not_charging)
}

fun getHealthString(health: Int, context: Context): String = when (health) {
    BatteryManager.BATTERY_HEALTH_GOOD -> context.getString(R.string.good)
    BatteryManager.BATTERY_HEALTH_OVERHEAT -> context.getString(R.string.overheat)
    BatteryManager.BATTERY_HEALTH_DEAD -> context.getString(R.string.dead)
    BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE -> context.getString(R.string.overvolt)
    else -> context.getString(R.string.unknown)
}

fun getBoolString(boolVal: Boolean, context: Context): String = when (boolVal) {
    true -> context.getString(R.string.yes)
    false -> context.getString(R.string.no)
}

fun Double.formatClean(fractionDigits: Int = 2): String {
    return if (this % 1.0 == 0.0) {
        String.format(Locale.getDefault(), "%d", this.toLong())
    } else {
        String.format(Locale.getDefault(), "%.${fractionDigits}f", this)
    }
}

fun Double.formatWithUnit(unit: String, fractionDigits: Int = 2): String =
    "${this.formatClean(fractionDigits)} $unit"

fun convertCelsiusToFahrenheit(celsius: Double): Double = celsius * 9 / 5 + 32

fun formatTemperature(temperature: Int, isCelsius: Boolean): String {
    val tempInCelsius = temperature / 10.0
    return if (isCelsius) {
        String.format(Locale.getDefault(), "%.1f°C", tempInCelsius)
    } else {
        val tempInFahrenheit = convertCelsiusToFahrenheit(tempInCelsius)
        String.format(Locale.getDefault(), "%.1f°F", tempInFahrenheit)
    }
}

fun formatTemperatureAsInt(temperature: Float, isCelsius: Boolean): String {
    return if (isCelsius) {
        "${temperature.toInt()}°C"
    } else {
        "${temperature.toInt()}°F"
    }
}

suspend fun saveCycleCountToHistory(
    context: Context,
    historyInfoRepository: HistoryInfoRepository
) {
    withContext(Dispatchers.IO) {
        val intent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val cycleCount = intent?.getIntExtra(BatteryManager.EXTRA_CYCLE_COUNT, -1) ?: -1
        val date = System.currentTimeMillis()
        val dateString = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(date))
        val newInfo = HistoryInfo(
            date = date,
            dateString = dateString,
            cycleCount = cycleCount.toString()
        )
        historyInfoRepository.insertOrUpdate(newInfo)
    }
}
