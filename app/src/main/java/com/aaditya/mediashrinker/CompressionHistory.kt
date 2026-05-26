package com.aaditya.mediashrinker

data class CompressionHistory(

    val imageUri: String,

    val originalSize: String,

    val compressedSize: String,

    val reducedPercent: String
)