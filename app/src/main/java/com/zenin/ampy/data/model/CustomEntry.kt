package com.zenin.ampy.data.model

import kotlinx.serialization.Serializable

@Serializable
data class CustomEntry(
    val path: String,
    val title: String,
    val unit: String,
    val scale: Int = 0
)

