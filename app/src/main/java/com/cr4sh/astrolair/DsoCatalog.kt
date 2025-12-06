package com.cr4sh.astrolair

data class DsoCatalog(
    val version: Int,
    val generatedAt: String,
    val objects: List<DsoObject>
)

data class DsoObject(
    val id: String,
    val catalog: String,
    val code: String,
    val number: Int?,
    val ngc: String?,
    val name: String,
    val type: String,
    val constellation: String,
    val raDeg: Double?,
    val decDeg: Double?,
    val mag: Double?,
    val surfaceBrightness: Double?,
    val imageUrl: String?
)
