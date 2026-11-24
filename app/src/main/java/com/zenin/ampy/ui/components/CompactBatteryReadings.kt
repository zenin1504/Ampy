package com.zenin.ampy.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.zenin.ampy.R
import com.zenin.ampy.data.model.BatteryInfo
import com.zenin.ampy.data.model.BatteryInfoType

private data class TileAction(
    val icon: ImageVector,
    val contentDescription: String,
    val onClick: () -> Unit
)

@Composable
private fun CompactReadingTile(
    info: BatteryInfo,
    modifier: Modifier = Modifier,
    action: TileAction? = null
) {
    val label = info.customTitle ?: stringResource(info.type.titleRes)

    Card(
        modifier = modifier.heightIn(min = 76.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = info.value,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (action != null) {
                IconButton(
                    onClick = action.onClick,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = action.icon,
                        contentDescription = action.contentDescription,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun CompactBatteryReadings(
    items: List<BatteryInfo>,
    modifier: Modifier = Modifier,
    onSwapTemperature: () -> Unit,
    onShowCycleInfo: () -> Unit,
    onShowVoltageInfo: () -> Unit,
    onCalibrateCurrent: () -> Unit = {}
) {
    val infoIcon = ImageVector.vectorResource(
        R.drawable.info_24dp_1f1f1f_fill0_wght400_grad0_opsz24
    )
    val swapIcon = ImageVector.vectorResource(
        R.drawable.swap_horiz_24dp_1f1f1f_fill0_wght400_grad0_opsz24
    )
    val editIcon = ImageVector.vectorResource(
        R.drawable.edit_24dp_1f1f1f_fill0_wght400_grad0_opsz24
    )

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        items.chunked(2).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                row.forEach { info ->
                    CompactReadingTile(
                        info = info,
                        modifier = Modifier.weight(1f),
                        action = when (info.type) {
                            BatteryInfoType.TEMP -> TileAction(
                                swapIcon,
                                stringResource(info.type.titleRes),
                                onSwapTemperature
                            )
                            BatteryInfoType.CYCLE_COUNT -> TileAction(
                                infoIcon,
                                stringResource(info.type.titleRes),
                                onShowCycleInfo
                            )
                            BatteryInfoType.VOLTAGE -> TileAction(
                                infoIcon,
                                stringResource(info.type.titleRes),
                                onShowVoltageInfo
                            )
                            BatteryInfoType.CURRENT -> TileAction(
                                editIcon,
                                stringResource(info.type.titleRes),
                                onCalibrateCurrent
                            )
                            else -> null
                        }
                    )
                }
                if (row.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}
