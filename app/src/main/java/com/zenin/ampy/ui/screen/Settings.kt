package com.zenin.ampy.ui.screen

import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.navigation.NavController
import com.zenin.ampy.R
import com.zenin.ampy.ui.components.AppScaffold
import com.zenin.ampy.ui.components.CustomSwitch
import com.zenin.ampy.vm.SettingsViewModel
import kotlin.math.roundToInt

@Composable
fun Settings(currentTitle: String, navController: NavController, settingsVM: SettingsViewModel) {
    AppScaffold(currentTitle, isTopLevel = true) {
        SettingsContent(navController, settingsVM)
    }
}

@Composable
private fun SettingsSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth().padding(top = 16.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 8.dp, end = 8.dp, bottom = 8.dp)
        )
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow
            ),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(content = content)
        }
    }
}

@Composable
private fun SettingsDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(start = 58.dp, end = 14.dp),
        thickness = 1.dp,
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
    )
}

@Composable
private fun SettingsRow(
    icon: Painter,
    title: String,
    modifier: Modifier = Modifier,
    supportingText: String? = null,
    trailing: (@Composable () -> Unit)? = null,
    onClick: (() -> Unit)? = null
) {
    RowCompat(
        modifier = modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        icon = icon,
        title = title,
        supportingText = supportingText,
        trailing = trailing,
        showChevron = onClick != null && trailing == null
    )
}

@Composable
private fun RowCompat(
    modifier: Modifier,
    icon: Painter,
    title: String,
    supportingText: String?,
    trailing: (@Composable () -> Unit)?,
    showChevron: Boolean
) {
    androidx.compose.foundation.layout.Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp)
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge)
            if (supportingText != null) {
                Text(
                    text = supportingText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        if (trailing != null) {
            Spacer(Modifier.width(8.dp))
            trailing()
        } else if (showChevron) {
            Icon(
                painter = painterResource(R.drawable.keyboard_arrow_down_24dp_1f1f1f_fill0_wght400_grad0_opsz24),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp).rotate(-90f)
            )
        }
    }
}

@Composable
fun SettingsContent(
    navController: NavController,
    settingsVM: SettingsViewModel,
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    val darkModeEnabled by settingsVM.darkModeEnabled.collectAsState()
    val followSystemTheme by settingsVM.followSystemTheme.collectAsState()
    val dailyHistoryEnabled by settingsVM.dailyHistoryEnabled.collectAsState()
    val refreshInterval by settingsVM.refreshInterval.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp)
            .verticalScroll(scrollState)
    ) {
        Spacer(Modifier.height(8.dp))

        SettingsSection(title = stringResource(R.string.ui)) {
            SettingsRow(
                icon = painterResource(R.drawable.palette_24dp_1f1f1f_fill0_wght400_grad0_opsz24),
                title = stringResource(R.string.follow_system_theme),
                trailing = {
                    CustomSwitch(
                        checked = followSystemTheme,
                        onCheckedChange = { settingsVM.setFollowSystem(it) }
                    )
                }
            )
            SettingsDivider()
            SettingsRow(
                icon = painterResource(R.drawable.dark_mode_24dp_1f1f1f_fill0_wght400_grad0_opsz24),
                title = stringResource(R.string.enable_dark_mode),
                trailing = {
                    CustomSwitch(
                        checked = darkModeEnabled,
                        onCheckedChange = { settingsVM.setDarkMode(it) },
                        enabled = !followSystemTheme
                    )
                }
            )
        }

        SettingsSection(title = stringResource(R.string.history)) {
            SettingsRow(
                icon = painterResource(R.drawable.library_books_24dp_1f1f1f_fill0_wght400_grad0_opsz24),
                title = stringResource(R.string.automatic_daily_charge_cycle_log),
                trailing = {
                    CustomSwitch(
                        checked = dailyHistoryEnabled,
                        onCheckedChange = { settingsVM.setDailyHistoryEnabled(it, context) }
                    )
                }
            )
        }

        SettingsSection(title = stringResource(R.string.battery_short)) {
            SettingsRow(
                icon = painterResource(R.drawable.speed_24dp_1f1f1f_fill0_wght400_grad0_opsz24),
                title = stringResource(R.string.battery_current),
                supportingText = stringResource(R.string.current_sign_info)
            )
            SettingsDivider()
            RefreshIntervalSettingsRow(
                refreshInterval = refreshInterval,
                onIntervalChange = { settingsVM.setRefreshInterval(it) }
            )
            SettingsDivider()
            SettingsRow(
                icon = painterResource(R.drawable.science_24dp_1f1f1f_fill0_wght400_grad0_opsz24),
                title = stringResource(R.string.get_from_logcat),
                onClick = { navController.navigate("battery_logcat_experiment") }
            )
        }

        SettingsSection(title = stringResource(R.string.other)) {
            SettingsRow(
                icon = painterResource(R.drawable.update_24dp_1f1f1f_fill0_wght400_grad0_opsz24),
                title = stringResource(R.string.check_for_updates),
                onClick = {
                    val intent = Intent(Intent.ACTION_VIEW).apply {
                        data = "https://github.com/zenin1504/Ampy/releases".toUri()
                    }
                    context.startActivity(intent)
                }
            )
            SettingsDivider()
            SettingsRow(
                icon = painterResource(R.drawable.info_24dp_1f1f1f_fill0_wght400_grad0_opsz24),
                title = stringResource(R.string.about),
                onClick = { navController.navigate("about") }
            )
        }

        Spacer(Modifier.height(16.dp))
    }
}

@Composable
fun RefreshIntervalSettingsRow(
    refreshInterval: Int,
    onIntervalChange: (Int) -> Unit,
) {
    var showDialog by remember { mutableStateOf(false) }
    val tickValues = listOf(200, 500, 800, 1000, 2000, 3000, 5000, 10000)
    val defaultIndex = tickValues.indexOf(1000).coerceAtLeast(0)

    var sliderIndex by remember(refreshInterval) {
        mutableIntStateOf(
            tickValues.indexOfFirst { it >= refreshInterval }
                .let { if (it == -1) tickValues.lastIndex else it }
        )
    }

    SettingsRow(
        icon = painterResource(R.drawable.hourglass_bottom_24dp_1f1f1f_fill0_wght400_grad0_opsz24),
        title = stringResource(R.string.set_refresh_interval_ms),
        supportingText = stringResource(R.string.current_value, tickValues[sliderIndex]),
        onClick = { showDialog = true }
    )

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text(stringResource(R.string.set_refresh_interval_ms)) },
            text = {
                Column {
                    Text(
                        text = stringResource(R.string.current_value, tickValues[sliderIndex]),
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = stringResource(R.string.oem_refresh_interval_limitation_description),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = stringResource(R.string.refresh_interval_battery_drain_description),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(Modifier.height(16.dp))
                    Slider(
                        value = sliderIndex.toFloat(),
                        onValueChange = { pos ->
                            val idx = pos.roundToInt().coerceIn(0, tickValues.lastIndex)
                            sliderIndex = idx
                            onIntervalChange(tickValues[idx])
                        },
                        valueRange = 0f..tickValues.lastIndex.toFloat(),
                        steps = tickValues.size - 2
                    )
                }
            },
            confirmButton = {
                Button(onClick = { showDialog = false }) {
                    Text(stringResource(R.string.close))
                }
            },
            dismissButton = {
                Button(onClick = {
                    sliderIndex = defaultIndex
                    onIntervalChange(tickValues[defaultIndex])
                    showDialog = false
                }) {
                    Text(stringResource(R.string.reset))
                }
            }
        )
    }
}
