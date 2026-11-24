package com.zenin.ampy.vm

import android.app.Application
import android.content.Context
import android.os.BatteryManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.zenin.ampy.data.model.BatteryInfo
import com.zenin.ampy.data.repository.BatteryInfoRepository
import com.zenin.ampy.data.repository.HistoryInfoRepository
import com.zenin.ampy.data.repository.PrefsRepository
import com.zenin.ampy.data.util.saveCycleCountToHistory
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class BatteryInfoViewModel @Inject constructor(
    application: Application,
    private val batteryInfoRepository: BatteryInfoRepository = BatteryInfoRepository(application),
    private val prefsRepo: PrefsRepository = PrefsRepository(application),
    private val historyInfoRepository: HistoryInfoRepository = HistoryInfoRepository(application)
) : AndroidViewModel(application) {
    private val context = application.applicationContext

    val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager

    val isDualBatt: StateFlow<Boolean> = batteryInfoRepository.isDualBattFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    fun setDualBatt(enabled: Boolean) = viewModelScope.launch {
        batteryInfoRepository.setDualBatt(enabled)
    }

    val isMultiply: StateFlow<Boolean> = batteryInfoRepository.isMultiplyFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, true)

    val selectedMagnitude: StateFlow<Int> = batteryInfoRepository.selectedMagnitudeFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, 0)

    fun setMultiplierPrefs(isMultiply: Boolean, magnitude: Int) {
        viewModelScope.launch {
            batteryInfoRepository.setMultiplierPrefs(isMultiply, magnitude)
        }
    }

    suspend fun getDisplayBatteryInfo(): List<BatteryInfo> = withContext(Dispatchers.IO) {
        batteryInfoRepository.getAvailableBatteryInfo()
    }

    suspend fun saveCycleCount() {
        saveCycleCountToHistory(context, historyInfoRepository)
    }
}
