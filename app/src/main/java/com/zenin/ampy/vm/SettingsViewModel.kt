package com.zenin.ampy.vm

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.zenin.ampy.data.repository.PrefsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    application: Application
) : AndroidViewModel(application) {

    private val prefs = PrefsRepository(application)

    val refreshInterval = prefs.refreshInterval.stateIn(
        viewModelScope, SharingStarted.Companion.WhileSubscribed(5000), initialValue = 1000
    )

    val darkModeEnabled = prefs.darkModeEnabled.stateIn(
        viewModelScope, SharingStarted.Companion.WhileSubscribed(5000), initialValue = false
    )
    val followSystemTheme = prefs.followSystemTheme.stateIn(
        viewModelScope, SharingStarted.Companion.WhileSubscribed(5000), initialValue = true
    )

    fun setRefreshInterval(rate: Int) = viewModelScope.launch {
        prefs.setRefreshInterval(rate)
    }

    fun setDarkMode(enabled: Boolean) = viewModelScope.launch {
        prefs.setDarkMode(enabled)
    }

    fun setFollowSystem(enabled: Boolean) = viewModelScope.launch {
        prefs.setFollowSystemTheme(enabled)
        if (enabled) prefs.setDarkMode(false)
    }

    val isPowerChartExpanded: StateFlow<Boolean> = prefs.isPowerChartExpanded
        .stateIn(viewModelScope, SharingStarted.Eagerly, true)

    fun setPowerChartExpanded(expanded: Boolean) = viewModelScope.launch {
        prefs.setPowerChartExpanded(expanded)
    }

    val dailyHistoryEnabled = prefs.dailyHistoryEnabled.stateIn(
        viewModelScope, SharingStarted.Eagerly, initialValue = false
    )

    fun setDailyHistoryEnabled(enabled: Boolean, context: Context) {
        viewModelScope.launch {
            prefs.setDailyHistoryEnabled(context, enabled)
        }
    }

    val isCelsius: StateFlow<Boolean> = prefs.isCelsiusFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, initialValue = true)

    fun setIsCelsius(isCelsius: Boolean) {
        viewModelScope.launch {
            prefs.setIsCelsius(isCelsius)
        }
    }
}