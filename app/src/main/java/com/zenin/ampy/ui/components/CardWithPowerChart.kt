package com.zenin.ampy.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zenin.ampy.data.model.BatteryInfo
import com.zenin.ampy.data.util.formatTemperatureAsInt
import com.zenin.ampy.nativelib.AmpyChartNative
import com.zenin.ampy.nativelib.PowerStats
import com.zenin.ampy.ui.screen.NormalBatteryCard
import com.zenin.ampy.R

data class PowerDataPoint(
    val timestamp: Long,
    val power: Float,
    val temperature: Float = 0f
)

data class ChartPoint(
    val timestamp: Long,
    val power: Float,
    val temperature: Float
)

private const val CHART_DOWNSAMPLE_THRESHOLD = 720
private const val CHART_DOWNSAMPLE_BUCKETS = 360

private fun buildChartPoints(data: List<PowerDataPoint>): List<ChartPoint> {
    val full = data.map { ChartPoint(it.timestamp, it.power, it.temperature) }
    if (!AmpyChartNative.isAvailable || data.size <= CHART_DOWNSAMPLE_THRESHOLD) return full

    return try {
        val packed = AmpyChartNative.downsample(
            LongArray(data.size) { data[it].timestamp },
            FloatArray(data.size) { data[it].power },
            FloatArray(data.size) { data[it].temperature },
            CHART_DOWNSAMPLE_BUCKETS
        ) ?: return full
        if (packed.size < 3) return full

        val base = data.first().timestamp
        val result = ArrayList<ChartPoint>(packed.size / 3)
        var index = 0
        while (index + 2 < packed.size) {
            result.add(
                ChartPoint(
                    timestamp = base + packed[index].toLong(),
                    power = packed[index + 1],
                    temperature = packed[index + 2]
                )
            )
            index += 3
        }
        result.ifEmpty { full }
    } catch (t: Throwable) {
        full
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun CardWithPowerChart(
    info: BatteryInfo,
    powerData: List<PowerDataPoint>,
    isExpanded: Boolean,
    isCelsius: Boolean,
    powerStats: PowerStats? = null,
    onResetData: () -> Unit = {},
    onChartExpand: () -> Unit = {}
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            NormalBatteryCard(info)
            Spacer(modifier = Modifier.weight(1f))
            if (isExpanded) {
                Box(modifier = Modifier.padding(horizontal = 4.dp)) {
                    FilledTonalIconButton(
                        onClick = onResetData,
                        modifier = Modifier.size(36.dp),
                        shapes = IconButtonDefaults.shapes()
                    ) {
                        Icon(
                            imageVector = ImageVector.vectorResource(id = R.drawable.refresh_24dp_1f1f1f_fill0_wght400_grad0_opsz24),
                            contentDescription = "Reset Chart Data"
                        )
                    }
                }
            }
            Box(modifier = Modifier.padding(horizontal = 4.dp)) {
                FilledTonalIconButton(
                    onClick = onChartExpand,
                    modifier = Modifier.size(36.dp),
                    shapes = IconButtonDefaults.shapes()
                ) {
                    Icon(
                        imageVector = if (isExpanded) ImageVector.vectorResource(id = R.drawable.keyboard_arrow_up_24dp_1f1f1f_fill0_wght400_grad0_opsz24) else ImageVector.vectorResource(
                            id = R.drawable.keyboard_arrow_down_24dp_1f1f1f_fill0_wght400_grad0_opsz24
                        ),
                        contentDescription = if (isExpanded) "Collapse" else "Expand"
                    )
                }
            }
        }
        if (isExpanded) {
            Spacer(modifier = Modifier.height(4.dp))
            PowerChart(
                data = powerData,
                isCelsius = isCelsius,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
                    .padding(horizontal = 4.dp)
            )
            powerStats?.let { stats ->
                Text(
                    text = stringResource(
                        R.string.power_chart_stats,
                        stats.average,
                        stats.peak,
                        stats.samples
                    ),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 8.dp, top = 2.dp)
                )
            }
        }
    }
}

@Composable
fun PowerChart(
    data: List<PowerDataPoint>,
    isCelsius: Boolean,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val textColor = MaterialTheme.colorScheme.onSurface
    val powerLineColor = MaterialTheme.colorScheme.primary
    val temperatureLineColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f)
    val gridColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
    val chartPoints = remember(data) { buildChartPoints(data) }

    Canvas(modifier = modifier) {
        val canvasWidth = size.width
        val canvasHeight = size.height
        val topPadding = with(density) { 12.dp.toPx() }
        val bottomPadding = with(density) { 32.dp.toPx() }

        // Handle empty data case - show empty chart with grid
        val isEmpty = data.isEmpty()

        // Calculate data ranges from the original series so the time axis stays stable.
        val minTime = if (isEmpty) 0L else data.first().timestamp
        val maxTime = if (isEmpty) 30000L else data.last().timestamp
        val rawMaxPower = if (isEmpty) 1f else chartPoints.maxOfOrNull { it.power } ?: 1f
        // Nice tick step (always 5 ticks: 0..4)
        val stepCandidates = listOf(
            0.05f, 0.1f, 0.2f, 0.25f, 0.5f,
            1f, 2f, 2.5f, 5f, 10f, 20f, 25f, 50f, 100f
        )
        val desiredStepBase = (rawMaxPower / 4f).coerceAtLeast(0.01f)
        val powerStep = stepCandidates.firstOrNull { it >= desiredStepBase } ?: stepCandidates.last()
        val maxPower = powerStep * 4f
        // We intentionally allow maxPower > rawMaxPower to land on round ticks
        val rawMinTemp = chartPoints.minOfOrNull { it.temperature } ?: 0f
        val rawMaxTemp = chartPoints.maxOfOrNull { it.temperature } ?: 1f

        // Handle temperature range
        val tempMargin = kotlin.math.max((rawMaxTemp - rawMinTemp) * 0.1f, 5f)
        val minTemp = kotlin.math.floor(rawMinTemp - tempMargin).toFloat()
        val maxTemp = kotlin.math.ceil(rawMaxTemp + tempMargin).toFloat()
        val adjustedTempRange = maxTemp - minTemp

        // Handle time range
        val rawTimeRange = maxTime - minTime
        val displayTimeRange = if (rawTimeRange < 30000) {
            kotlin.math.max(rawTimeRange, 30000)
        } else rawTimeRange

        // Text paint for Y-axis labels
        val textPaint = android.graphics.Paint().apply {
            color = textColor.toArgb()
            textSize = with(density) { 10.sp.toPx() }
            isAntiAlias = true
            textAlign = android.graphics.Paint.Align.RIGHT
        }

        // Calculate maximum width needed for left Y-axis labels
        var maxLeftLabelWidth = 0f
        for (i in 0..4) {
            val powerValue = maxPower - powerStep * i
            val base = if (powerStep < 1f) String.format("%.1f", powerValue) else powerValue.toInt().toString()
            val text = "$base W"
            val textWidth = textPaint.measureText(text)
            maxLeftLabelWidth = kotlin.math.max(maxLeftLabelWidth, textWidth)
        }

        // Text paint for right Y-axis labels
        val rightTextPaint = android.graphics.Paint().apply {
            color = textColor.toArgb()
            textSize = with(density) { 10.sp.toPx() }
            isAntiAlias = true
            textAlign = android.graphics.Paint.Align.LEFT
        }

        // Calculate maximum width needed for right Y-axis labels
        var maxRightLabelWidth = 0f
        for (i in 0..4) {
            val tempValue = maxTemp - (adjustedTempRange * i / 4)
            val text = formatTemperatureAsInt(tempValue, isCelsius)
            val textWidth = rightTextPaint.measureText(text)
            maxRightLabelWidth = kotlin.math.max(maxRightLabelWidth, textWidth)
        }

        // Dynamic padding based on label widths
        val leftPadding = maxLeftLabelWidth + with(density) { 12.dp.toPx() }
        val rightPadding = maxRightLabelWidth + with(density) { 12.dp.toPx() }

        // Drawing area
        val chartLeft = leftPadding
        val chartRight = canvasWidth - rightPadding
        val chartTop = topPadding
        val chartBottom = canvasHeight - bottomPadding
        val chartWidth = chartRight - chartLeft
        val chartHeight = chartBottom - chartTop

        // Draw grid lines and Y-axis labels
        for (i in 0..4) {
            val y = chartTop + (chartHeight * i / 4)
            val powerValue = maxPower - powerStep * i
            val tempValue = maxTemp - (adjustedTempRange * i / 4)

            // Grid line
            drawLine(
                color = gridColor,
                start = Offset(chartLeft, y),
                end = Offset(chartRight, y),
                strokeWidth = 0.5.dp.toPx()
            )

            // Left Y-axis label (Power)
            drawIntoCanvas { canvas ->
                val base = if (powerStep < 1f) String.format("%.1f", powerValue) else powerValue.toInt().toString()
                val text = "$base W"
                canvas.nativeCanvas.drawText(
                    text,
                    chartLeft - with(density) { 8.dp.toPx() },
                    y + with(density) { 3.dp.toPx() },
                    textPaint
                )
            }

            // Right Y-axis label (Temp)
            drawIntoCanvas { canvas ->
                val text = formatTemperatureAsInt(tempValue, isCelsius)
                canvas.nativeCanvas.drawText(
                    text,
                    chartRight + with(density) { 8.dp.toPx() },
                    y + with(density) { 3.dp.toPx() },
                    rightTextPaint
                )
            }
        }

        // Text paint for X-axis labels
        val xAxisTextPaint = android.graphics.Paint().apply {
            color = textColor.toArgb()
            textSize = with(density) { 10.sp.toPx() }
            isAntiAlias = true
            textAlign = android.graphics.Paint.Align.CENTER
        }

        // Use actual data timestamps for X-axis ticks
        val tickIndices = if (isEmpty) {
            emptyList()
        } else {
            when {
                data.size < 30 -> {
                    // Show only last tick to prevent overlap in early stages
                    listOf(data.lastIndex)
                }
                else -> {
                    // Show 5 evenly spaced ticks once we have enough data
                    val maxTicks = 5
                    val step = (data.size - 1).toFloat() / (maxTicks - 1)
                    (0 until maxTicks).map { i ->
                        when (i) {
                            maxTicks - 1 -> data.lastIndex
                            else -> (i * step).toInt()
                        }
                    }
                }
            }
        }

        // Draw X-axis labels (only if not empty)
        if (!isEmpty) {
            tickIndices.forEach { index ->
                val point = data[index]
                val relativeTimestamp = point.timestamp - minTime
                val x = chartLeft + (chartWidth * relativeTimestamp / displayTimeRange.coerceAtLeast(1))
                val elapsedSeconds = relativeTimestamp / 1000

                drawIntoCanvas { canvas ->
                    val text = when {
                        elapsedSeconds >= 60 -> {
                            val minutes = (elapsedSeconds / 60).toInt()
                            val seconds = (elapsedSeconds % 60).toInt()
                            if (seconds == 0) "${minutes}m" else "${minutes}m${seconds}s"
                        }
                        else -> "${elapsedSeconds}s"
                    }
                    canvas.nativeCanvas.drawText(
                        text,
                        x,
                        chartBottom + with(density) { 20.dp.toPx() },
                        xAxisTextPaint
                    )
                }
            }
        }

        // Draw power line (only if not empty and has multiple points)
        if (!isEmpty && chartPoints.size >= 2) {
            val powerPath = Path()
            chartPoints.forEachIndexed { index, point ->
                val relativeTimestamp = point.timestamp - minTime
                val x = chartLeft + (chartWidth * relativeTimestamp / displayTimeRange.coerceAtLeast(1))
                val y = chartBottom - (chartHeight * point.power / maxPower.coerceAtLeast(0.0001f))

                if (index == 0) {
                    powerPath.moveTo(x, y)
                } else {
                    powerPath.lineTo(x, y)
                }
            }

            val lineWidth = when {
                chartPoints.size > 100 -> 0.8.dp.toPx()
                chartPoints.size > 30 -> 1.2.dp.toPx()
                else -> 1.5.dp.toPx()
            }

            drawPath(
                path = powerPath,
                color = powerLineColor,
                style = Stroke(width = lineWidth)
            )
        }

        // Draw temperature line (only if not empty and has multiple points)
        if (!isEmpty && chartPoints.size >= 2) {
            val tempPath = Path()
            chartPoints.forEachIndexed { index, point ->
                val relativeTimestamp = point.timestamp - minTime
                val x = chartLeft + (chartWidth * relativeTimestamp / displayTimeRange.coerceAtLeast(1))
                val y = chartBottom - (chartHeight * (point.temperature - minTemp) / adjustedTempRange.coerceAtLeast(0.01f))

                if (index == 0) {
                    tempPath.moveTo(x, y)
                } else {
                    tempPath.lineTo(x, y)
                }
            }

            val lineWidth = when {
                chartPoints.size > 100 -> 0.8.dp.toPx()
                chartPoints.size > 30 -> 1.2.dp.toPx()
                else -> 1.5.dp.toPx()
            }

            drawPath(
                path = tempPath,
                color = temperatureLineColor,
                style = Stroke(width = lineWidth)
            )
        }

        // Draw power data points (only if not empty)
        if (!isEmpty) {
            val pointRadius = when {
                chartPoints.size > 100 -> 0f
                chartPoints.size > 50 -> 0.3.dp.toPx()
                chartPoints.size > 30 -> 0.6.dp.toPx()
                else -> 1.2.dp.toPx()
            }

            if (pointRadius > 0) {
                chartPoints.forEach { point ->
                    val relativeTimestamp = point.timestamp - minTime
                    val x = chartLeft + (chartWidth * relativeTimestamp / displayTimeRange.coerceAtLeast(1))
                    val y = chartBottom - (chartHeight * point.power / maxPower.coerceAtLeast(0.0001f))

                    drawCircle(
                        color = powerLineColor,
                        radius = pointRadius,
                        center = Offset(x, y)
                    )
                }
            }
        }

        // Draw temperature data points (only if not empty)
        if (!isEmpty) {
            val pointRadius = when {
                chartPoints.size > 100 -> 0f
                chartPoints.size > 50 -> 0.3.dp.toPx()
                chartPoints.size > 30 -> 0.6.dp.toPx()
                else -> 1.2.dp.toPx()
            }

            if (pointRadius > 0) {
                chartPoints.forEach { point ->
                    val relativeTimestamp = point.timestamp - minTime
                    val x = chartLeft + (chartWidth * relativeTimestamp / displayTimeRange.coerceAtLeast(1))
                    val y = chartBottom - (chartHeight * (point.temperature - minTemp) / adjustedTempRange.coerceAtLeast(0.01f))

                    drawCircle(
                        color = temperatureLineColor,
                        radius = pointRadius,
                        center = Offset(x, y)
                    )
                }
            }
        }
    }
}
