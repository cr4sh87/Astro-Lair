package com.cr4sh.astrolair

data class TargetObject(
    val catalog: String,
    val code: String,
    val name: String,
    val type: String,
    val magnitude: Double,
    val surfaceBrightness: Double?,
    val ra: String,
    val dec: String,
    val constellation: String
)
