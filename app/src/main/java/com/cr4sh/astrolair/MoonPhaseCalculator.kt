package com.cr4sh.astrolair

import java.time.Duration
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlin.math.abs

/**
 * Calcolatore semplice di fase lunare.
 *
 * Restituisce un valore tra 0.0 e 1.0:
 *  0.0 ~ Luna nuova
 *  0.25 ~ Primo quarto
 *  0.5 ~ Luna piena
 *  0.75 ~ Ultimo quarto
 */
object MoonPhaseCalculator {

    private const val SYNODIC_MONTH = 29.53058867

    private val referenceNewMoon: ZonedDateTime =
        ZonedDateTime.of(2000, 1, 6, 18, 14, 0, 0, ZoneId.of("UTC"))

    fun phaseFraction(dateTime: ZonedDateTime): Double {
        val dtUtc = dateTime.withZoneSameInstant(ZoneId.of("UTC"))
        val diff = Duration.between(referenceNewMoon, dtUtc)
        val days = diff.toHours() / 24.0

        var phase = (days % SYNODIC_MONTH) / SYNODIC_MONTH
        if (phase < 0) {
            phase += 1.0
        }
        return phase
    }

    fun describePhase(phase: Double): String {
        return when {
            isNear(phase, 0.0) || isNear(phase, 1.0) ->
                "Luna Nuova"
            isNear(phase, 0.25) ->
                "Primo Quarto"
            isNear(phase, 0.5) ->
                "Luna Piena"
            isNear(phase, 0.75) ->
                "Ultimo Quarto"
            phase in 0.0..0.25 ->
                "Falce Crescente"
            phase in 0.25..0.5 ->
                "Gibbosa Crescente"
            phase in 0.5..0.75 ->
                "Gibbosa Calante"
            else ->
                "Falce Calante"
        }
    }

    private fun isNear(value: Double, target: Double, tolerance: Double = 0.03): Boolean {
        val diff = abs(value - target)
        return diff <= tolerance || diff >= 1.0 - tolerance
    }
}
