package com.zenin.ampy.vm

import android.app.Application
import android.content.Intent
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.zenin.ampy.data.model.BatteryInfoType
import com.zenin.ampy.data.repository.BatteryInfoRepository
import com.zenin.ampy.data.repository.PrefsRepository
import com.zenin.ampy.service.BatteryMonitorService
import com.zenin.ampy.service.FloatingWindowService
import com.zenin.ampy.service.MonitorWatchdog
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BatteryMonitorSettingsViewModel @Inject constructor(
    application: Application,
    private val batteryRepo: BatteryInfoRepository = BatteryInfoRepository(application),
    private val prefsRepo: PrefsRepository = PrefsRepository(application)
) : AndroidViewModel(application) {

    private val _isMonitoring = MutableStateFlow(false)
    val isMonitoring: StateFlow<Boolean> = _isMonitoring.asStateFlow()
    private val _isFloatingWindowOn = MutableStateFlow(false)
    val isFloatingWindowOn: StateFlow<Boolean> = _isFloatingWindowOn.asStateFlow()
    private val appContext = getApplication<Application>()

    fun startMonitor() {
        viewModelScope.launch {
            // Persist first so even an immediate process death can be restored.
            prefsRepo.setMonitorEnabled(true)
            MonitorWatchdog.schedulePeriodic(appContext)
            try {
                ContextCompat.startForegroundService(
                    appContext,
                    Intent(appContext, BatteryMonitorService::class.java)
                )
            } catch (e: Exception) {
                // Android can refuse a background FGS start; the watchdog
                // alarm will retry while the monitor preference stays on.
            }
            _isMonitoring.value = true
        }
    }

    fun stopMonitor() {
        Intent(appContext, BatteryMonitorService::class.java).also { intent ->
            appContext.stopService(intent)
        }
        MonitorWatchdog.cancel(appContext)
        _isMonitoring.value = false
        viewModelScope.launch {
            prefsRepo.setMonitorEnabled(false)
        }
    }

    fun canScheduleExactAlarms(): Boolean = MonitorWatchdog.canScheduleExactAlarms(appContext)

    /** Re-arm the watchdog after the user returns from exact-alarm settings. */
    fun onKeepAliveSettingsReturned() {
        viewModelScope.launch {
            if (prefsRepo.monitorEnabled.first()) {
                MonitorWatchdog.cancel(appContext)
                MonitorWatchdog.schedulePeriodic(appContext)
                MonitorWatchdog.restartSoon(appContext)
            }
        }
    }

    fun startFloatingWindow() {
        Intent(appContext, FloatingWindowService::class.java).also { intent ->
            appContext.startService(intent)
        }
        _isFloatingWindowOn.value = true
    }

    fun stopFloatingWindow() {
        Intent(appContext, FloatingWindowService::class.java).also { intent ->
            appContext.stopService(intent)
        }
        _isFloatingWindowOn.value = false
    }


    // Holds the list of all available BatteryInfo titles for selection
    private val _availableEntries = MutableStateFlow<List<BatteryInfoType>>(emptyList())
    val availableEntries: StateFlow<List<BatteryInfoType>> = _availableEntries

    // Holds the set of user-selected titles (entries) from preferences
    val visibleEntries: StateFlow<Set<BatteryInfoType>> =
        prefsRepo.visibleEntriesFlow
            .stateIn(viewModelScope, SharingStarted.Companion.Eagerly, emptySet())

    init {
        viewModelScope.launch {
            prefsRepo.monitorEnabled.collect { enabled ->
                _isMonitoring.value = enabled
            }
        }
        viewModelScope.launch {
            _availableEntries.value =
                batteryRepo.getAvailableBatteryInfo().map { it.type }.distinct()
        }
    }

    // Update the set of visible entries in DataStore
    fun setVisibleEntries(newEntries: Set<BatteryInfoType>) {
        viewModelScope.launch {
            prefsRepo.setVisibleEntries(newEntries)
        }
    }
}
