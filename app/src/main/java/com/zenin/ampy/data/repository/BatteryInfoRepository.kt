package com.zenin.ampy.data.repository

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import androidx.datastore.preferences.core.edit
import com.zenin.ampy.data.model.BatteryInfo
import com.zenin.ampy.data.model.BatteryInfoType
import com.zenin.ampy.data.util.DUAL_BATTERY_KEY
import com.zenin.ampy.data.util.MULTIPLIER_MAGNITUDE_KEY
import com.zenin.ampy.data.util.MULTIPLY_KEY
import com.zenin.ampy.R
import com.zenin.ampy.data.util.IS_CELSIUS
import com.zenin.ampy.data.util.dataStore
import com.zenin.ampy.data.util.formatTemperature
import com.zenin.ampy.data.util.formatWithUnit
import com.zenin.ampy.data.util.getHealthString
import com.zenin.ampy.data.util.getStatusString
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import kotlin.math.abs
import kotlin.math.pow

class BatteryInfoRepository @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    private val batteryManager get() =
        context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
    private val settings by lazy { context.dataStore }

    private val calibFlow = settings.data
        .map { prefs ->
            val isMult = prefs[MULTIPLY_KEY] != false
            val mag = prefs[MULTIPLIER_MAGNITUDE_KEY] ?: 0
            if (isMult) 10.0.pow(mag.toDouble()) else 1 / 10.0.pow(mag.toDouble())
        }

    private val dualBattFlow: Flow<Int> = settings.data
        .map { prefs -> if (prefs[DUAL_BATTERY_KEY] == true) 2 else 1 }

    val isDualBattFlow: Flow<Boolean> = dualBattFlow.map { it == 2 }

    suspend fun setDualBatt(enabled: Boolean) {
        settings.edit { it[DUAL_BATTERY_KEY] = enabled }
    }

    suspend fun setMultiplierPrefs(isMult: Boolean, mag: Int) {
        settings.edit { prefs ->
            prefs[MULTIPLY_KEY] = isMult
            prefs[MULTIPLIER_MAGNITUDE_KEY] = mag
        }
    }

    val isMultiplyFlow: Flow<Boolean> = settings.data
        .map { prefs -> prefs[MULTIPLY_KEY] != false }

    val selectedMagnitudeFlow: Flow<Int> = settings.data
        .map { prefs -> prefs[MULTIPLIER_MAGNITUDE_KEY] ?: 0 }

    val isCelsiusFlow: Flow<Boolean> = settings.data
        .map { prefs -> prefs[IS_CELSIUS] ?: true }

    suspend fun getBasicBatteryInfo(): List<BatteryInfo> = withContext(Dispatchers.IO) {
        val intent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val level = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, 0) ?: 0
        val temperature = intent?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -999) ?: 0
        val status = intent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val health = intent?.getIntExtra(BatteryManager.EXTRA_HEALTH, 0) ?: 0
        val cycleCount = intent?.getIntExtra(BatteryManager.EXTRA_CYCLE_COUNT, -1) ?: -1
        val isCelsius = isCelsiusFlow.first()

        listOf(
            BatteryInfo(BatteryInfoType.LEVEL, "$level%", false),
            BatteryInfo(BatteryInfoType.STATUS, getStatusString(status, context), false),
            BatteryInfo(BatteryInfoType.HEALTH, getHealthString(health, context), false),
            BatteryInfo(BatteryInfoType.CYCLE_COUNT, cycleCount.toString(), false),
            BatteryInfo(BatteryInfoType.TEMP, formatTemperature(temperature, isCelsius), false)
        )
    }

    suspend fun getVoltCurrPwr(): List<BatteryInfo> = withContext(Dispatchers.IO) {
        val calibMultiplier = calibFlow.first()
        val dualBattMultiplier = dualBattFlow.first()
        val intent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val voltage = intent?.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0) ?: 0
        val status = intent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val isCharging = batteryManager.isCharging() ||
            status == BatteryManager.BATTERY_STATUS_CHARGING ||
            status == BatteryManager.BATTERY_STATUS_FULL
        val magnitude =
            abs(batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW) * calibMultiplier)
        val current = when {
            magnitude == 0.0 -> 0.0
            isCharging -> magnitude
            else -> -magnitude
        }
        val power = current * voltage * dualBattMultiplier / 1_000_000.0

        listOf(
            BatteryInfo(BatteryInfoType.VOLTAGE, "$voltage mV", false),
            BatteryInfo(BatteryInfoType.CURRENT, current.formatWithUnit("mA"), false),
            BatteryInfo(BatteryInfoType.POWER, power.formatWithUnit("W"), false)
        )
    }

    suspend fun getAvailableBatteryInfo(): List<BatteryInfo> = withContext(Dispatchers.IO) {
        getBasicBatteryInfo() + getVoltCurrPwr()
    }
}
