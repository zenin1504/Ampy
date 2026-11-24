package com.zenin.ampy.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private val DefaultTypography = Typography()

val AmpyTypography = Typography(
    headlineSmall = DefaultTypography.headlineSmall.copy(
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.sp
    ),
    titleLarge = DefaultTypography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
    titleMedium = DefaultTypography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
    titleSmall = DefaultTypography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
    bodyLarge = DefaultTypography.bodyLarge.copy(lineHeight = 22.sp),
    bodyMedium = DefaultTypography.bodyMedium.copy(lineHeight = 20.sp),
    labelLarge = DefaultTypography.labelLarge.copy(fontWeight = FontWeight.SemiBold)
)
