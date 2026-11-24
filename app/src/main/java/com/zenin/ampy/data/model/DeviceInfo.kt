package com.zenin.ampy.data.model

data class DeviceInfo(
    val manufacturer: String,
    val model: String,
    val androidVersion: String,
    val apiLevel: Int,
    val batteryTechnology: String
)
