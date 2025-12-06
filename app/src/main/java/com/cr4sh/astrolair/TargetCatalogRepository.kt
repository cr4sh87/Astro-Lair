package com.cr4sh.astrolair

object TargetCatalogRepository {

    // Versione ridotta, giusto per testare il flusso UI
    private val targetsByCatalog: Map<String, List<TargetObject>> = mapOf(
        "Messier" to listOf(
            TargetObject(
                catalog = "Messier",
                code = "M31",
                name = "Galassia di Andromeda",
                type = "Galassia a spirale",
                magnitude = 3.4,
                surfaceBrightness = 22.0,
                ra = "00h 42m 44s",
                dec = "+41° 16′ 09″",
                constellation = "Andromeda"
            ),
            TargetObject(
                catalog = "Messier",
                code = "M42",
                name = "Nebulosa di Orione",
                type = "Nebulosa a emissione",
                magnitude = 4.0,
                surfaceBrightness = 21.0,
                ra = "05h 35m 17s",
                dec = "−05° 23′ 28″",
                constellation = "Orione"
            ),
            TargetObject(
                catalog = "Messier",
                code = "M13",
                name = "Ammasso globulare di Ercole",
                type = "Ammasso globulare",
                magnitude = 5.8,
                surfaceBrightness = 21.5,
                ra = "16h 41m 41s",
                dec = "+36° 27′ 37″",
                constellation = "Ercole"
            )
        ),
        "NGC" to listOf(
            TargetObject(
                catalog = "NGC",
                code = "NGC 7000",
                name = "Nebulosa Nord America",
                type = "Nebulosa a emissione",
                magnitude = 4.0,
                surfaceBrightness = 23.0,
                ra = "20h 58m 54s",
                dec = "+44° 19′ 00″",
                constellation = "Cigno"
            ),
            TargetObject(
                catalog = "NGC",
                code = "NGC 253",
                name = "Galassia Scultore",
                type = "Galassia a spirale",
                magnitude = 8.0,
                surfaceBrightness = 22.5,
                ra = "00h 47m 33s",
                dec = "−25° 17′ 18″",
                constellation = "Scultore"
            )
        )
    )

    fun getCatalogNames(): List<String> = targetsByCatalog.keys.sorted()

    fun getTargetsForCatalog(catalog: String): List<TargetObject> =
        targetsByCatalog[catalog] ?: emptyList()
}
