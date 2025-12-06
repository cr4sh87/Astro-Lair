package com.cr4sh.astrolair

data class EquipmentConfig(
    var primaryName: String = "",
    var primaryFocalLengthMm: Double = 800.0,
    var primaryFocalRatio: Double = 4.0,
    var secondaryName: String = "",
    var imagingCamera: String = "",
    var guideCamera: String = ""
)

object EquipmentManager {
    // Default tagliati sul tuo setup reale
    var config: EquipmentConfig = EquipmentConfig(
        primaryName = "Newton 200/800",
        primaryFocalLengthMm = 800.0,
        primaryFocalRatio = 4.0,
        secondaryName = "",
        imagingCamera = "CCD Moravian",
        guideCamera = "ZWO ASI120MM"
    )
}
