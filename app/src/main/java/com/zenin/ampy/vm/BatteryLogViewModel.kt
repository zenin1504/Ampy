package com.zenin.ampy.vm

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.zenin.ampy.data.model.DeviceInfo
import com.zenin.ampy.data.repository.BatteryLogRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BatteryLogViewModel @Inject constructor(
    application: Application,
    private val repo: BatteryLogRepository = BatteryLogRepository()
) : AndroidViewModel(application) {

    private val refreshIntervalMs: Long = 1000L

    private val designCapacityMah: String? by lazy { readDesignCapacityFromPowerProfile() }

    private val _deviceInfo = MutableStateFlow(readDeviceInfo())
    val deviceInfo: StateFlow<DeviceInfo> = _deviceInfo.asStateFlow()

    private val _latestLog = MutableStateFlow<Map<String, String>?>(null)
    val latestLog: StateFlow<Map<String, String>?> = _latestLog

    private val _batteryCapacityMah = MutableStateFlow<String?>(null)
    val batteryCapacityMah: StateFlow<String?> = _batteryCapacityMah.asStateFlow()

    init {
        viewModelScope.launch(Dispatchers.IO) {
            while (true) {
                try {
                    val parsed = repo.getParsedLogcatData()
                    _latestLog.value = parsed
                    _batteryCapacityMah.value = designCapacityMah ?: capacityFromLog(parsed)
                } catch (t: Throwable) {
                    Log.e("BatteryLogVM", "logcat error", t)
                    _latestLog.value = null
                    _batteryCapacityMah.value = null
                }
                delay(refreshIntervalMs)
            }
        }
    }

    private fun readDeviceInfo(): DeviceInfo {
        val intent = getApplication<Application>()
            .registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val technology = intent?.getStringExtra(BatteryManager.EXTRA_TECHNOLOGY).takeUnless { it.isNullOrBlank() }
            ?: "Unknown"
        return DeviceInfo(
            manufacturer = Build.MANUFACTURER ?: "Unknown",
            model = Build.MODEL ?: "Unknown",
            androidVersion = Build.VERSION.RELEASE ?: "Unknown",
            apiLevel = Build.VERSION.SDK_INT,
            batteryTechnology = technology
        )
    }

    @SuppressLint("PrivateApi")
    private fun readDesignCapacityFromPowerProfile(): String? = try {
        val profileClass = Class.forName("com.android.internal.os.PowerProfile")
        val profile = profileClass
            .getConstructor(Context::class.java)
            .newInstance(getApplication<Application>().applicationContext)
        val capacity = profileClass.getMethod("getBatteryCapacity").invoke(profile) as? Double
        if (capacity != null && capacity > 0 && capacity < 100_000) {
            capacity.toLong().toString()
        } else {
            null
        }
    } catch (t: Throwable) {
        null
    }

    private fun capacityFromLog(map: Map<String, String>?): String? {
        val raw = map?.get("totalBatteryCapacity") ?: map?.get("fcc") ?: return null
        val value = raw.toLongOrNull() ?: return null
        return (if (value > 100_000) value / 1000 else value).toString()
    }
}
