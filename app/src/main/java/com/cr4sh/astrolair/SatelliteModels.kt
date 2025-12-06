package com.cr4sh.astrolair

import java.time.Duration
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlin.math.PI
import kotlin.math.cos

data class SatellitePosition(
    val name: String,
    val x: Double // posizione lungo l'asse Est–Ovest, in raggi planetari
)

object SatelliteModels {

    // Epoch di riferimento per le fasi orbitali: arbitrario
    private val epoch: ZonedDateTime =
        ZonedDateTime.of(2025, 1, 1, 0, 0, 0, 0, ZoneId.of("UTC"))

    private data class SatelliteParams(
        val name: String,
        val periodDays: Double,
        val semiMajorAxisRadii: Double,
        val phase0: Double // frazione del periodo 0..1
    )

    // Valori indicativi per i 4 galileiani (periodo, semiasse in raggi gioviani)
    private val jupiterMoons = listOf(
        SatelliteParams("Io",        1.769,  5.9, 0.0),
        SatelliteParams("Europa",    3.551,  9.4, 0.25),
        SatelliteParams("Ganimede",  7.155, 15.0, 0.5),
        SatelliteParams("Callisto", 16.689, 26.3, 0.75)
    )

    // Saturno: alcuni satelliti principali (periodo, semiasse in raggi saturniani)
    private val saturnMoons = listOf(
        SatelliteParams("Mimas",     0.942,  3.1, 0.0),
        SatelliteParams("Encelado",  1.370,  3.9, 0.15),
        SatelliteParams("Teti",      1.888,  4.9, 0.3),
        SatelliteParams("Dione",     2.737,  6.3, 0.45),
        SatelliteParams("Rea",       4.518,  8.7, 0.6),
        SatelliteParams("Titano",   15.945, 20.3, 0.75)
    )

    fun jupiterSatellitesAt(time: ZonedDateTime): List<SatellitePosition> {
        val days = daysSinceEpoch(time)
        return jupiterMoons
            .map { p -> computePosition(p, days) }
            .sortedBy { it.x }
    }

    fun saturnSatellitesAt(time: ZonedDateTime): List<SatellitePosition> {
        val days = daysSinceEpoch(time)
        return saturnMoons
            .map { p -> computePosition(p, days) }
            .sortedBy { it.x }
    }

    private fun daysSinceEpoch(time: ZonedDateTime): Double {
        val diff = Duration.between(epoch, time.withZoneSameInstant(ZoneId.of("UTC")))
        return diff.toHours() / 24.0
    }

    private fun computePosition(
        p: SatelliteParams,
        daysSinceEpoch: Double
    ): SatellitePosition {
        val fracOrbit = ((daysSinceEpoch / p.periodDays) + p.phase0)
            .mod(1.0)
        val angle = 2.0 * PI * fracOrbit
        val x = p.semiMajorAxisRadii * cos(angle) // asse Est–Ovest
        return SatellitePosition(p.name, x)
    }

    private fun Double.mod(m: Double): Double {
        var r = this % m
        if (r < 0) r += m
        return r
    }
}
