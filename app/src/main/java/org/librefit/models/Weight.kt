/*
 * SPDX-License-Identifier: GPL-3.0-or-later
 * Copyright (c) 2026. The LibreFit Contributors
 *
 * LibreFit is subject to additional terms covering author attribution and trademark usage;
 * see the ADDITIONAL_TERMS.md and TRADEMARK_POLICY.md files in the project root.
 */

package org.librefit.models

import androidx.annotation.FloatRange
import androidx.compose.runtime.Composable
import kotlinx.serialization.Serializable
import org.librefit.enums.userPreferences.UnitSystem
import org.librefit.models.Weight.Companion.MAX_WEIGHT_IN_KILOGRAMS
import org.librefit.models.Weight.Companion.MIN_WEIGHT_IN_KILOGRAMS
import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.math.ulp
import kotlin.math.withSign
import org.librefit.nav.LocalUnitSystem

/**
 * A type-safe wrapper for weight values, represented in kilograms.
 * 
 * This class uses [JvmInline] to provide a zero-cost abstraction, meaning that at runtime,
 * it is represented by the underlying [Double] value, preventing unnecessary object allocations.
 * It enforces business constraints on weight range during both construction and factory-based instantiation.
 *
 * Values entered in pounds are converted to kilograms on the way in and back on the way out, which
 * leaves some floating point noise behind (150 lb comes back as 149.99999999999997 lb). Anything
 * shown to the user therefore goes through [roundedValue], which rounds to the precision of the
 * unit system in use, refer to [decimalDigits].
 *
 * @see <a href="https://kotlinlang.org/docs/inline-classes.html">Kotlin Inline Value Classes Documentation</a>
 * @property inKilograms The underlying weight value in kilograms.
 */
@Serializable
@JvmInline
value class Weight private constructor(
    @param:FloatRange(
        from = MIN_WEIGHT_IN_KILOGRAMS,
        to = MAX_WEIGHT_IN_KILOGRAMS
    ) val inKilograms: Double
) : Comparable<Weight> {

    /**
     * The weight value represented in pounds (lbs).
     */
    val inPounds: Double get() = inKilograms * KILOGRAMS_TO_POUNDS

    override fun compareTo(other: Weight): Int = inKilograms.compareTo(other.inKilograms)

    operator fun plus(other: Weight): Weight = kilograms(
        (this.inKilograms + other.inKilograms).coerceIn(
            MIN_WEIGHT_IN_KILOGRAMS,
            MAX_WEIGHT_IN_KILOGRAMS
        )
    )

    operator fun minus(other: Weight): Weight = kilograms(
        (this.inKilograms - other.inKilograms).coerceIn(
            MIN_WEIGHT_IN_KILOGRAMS,
            MAX_WEIGHT_IN_KILOGRAMS
        )
    )

    operator fun times(multiplier: Double): Weight = kilograms(
        (this.inKilograms * multiplier).coerceIn(MIN_WEIGHT_IN_KILOGRAMS, MAX_WEIGHT_IN_KILOGRAMS)
    )

    operator fun div(scalar: Double): Weight = kilograms(
        (this.inKilograms / scalar).coerceIn(MIN_WEIGHT_IN_KILOGRAMS, MAX_WEIGHT_IN_KILOGRAMS)
    )

    operator fun div(other: Weight): Double = this.inKilograms / other.inKilograms

    /**
     * The weight value expressed in the unit of [unitSystem], i.e. [inKilograms] or [inPounds].
     *
     * It is the raw value, conversion noise included: use [roundedValue] for anything shown to the
     * user or compared with what the user typed.
     */
    fun valueIn(unitSystem: UnitSystem): Double = when (unitSystem) {
        UnitSystem.METRIC -> inKilograms
        UnitSystem.IMPERIAL -> inPounds
    }

    /**
     * The weight value expressed in the unit of [unitSystem] and rounded half up to
     * [numberOfDecimalDigits], which by default is the precision the app shows that unit with
     * (refer to [decimalDigits]).
     *
     * ```
     * Weight.pounds(150.0).roundedValue(UnitSystem.IMPERIAL)    // 150.0, not 149.99999999999997
     * Weight.pounds(137.5).roundedValue(UnitSystem.IMPERIAL)    // 138.0
     * Weight.kilograms(61.25).roundedValue(UnitSystem.METRIC)   // 61.25
     * ```
     */
    fun roundedValue(
        unitSystem: UnitSystem,
        numberOfDecimalDigits: Int = decimalDigits(unitSystem)
    ): Double = roundHalfUp(valueIn(unitSystem), numberOfDecimalDigits)

    /**
     * Moves this weight by [deltaInPounds].
     *
     * - [UnitSystem.METRIC]: the delta is converted to kilograms and added as it is, so a step of
     * 2.5 lb moves the weight by about 1.134 kg. It is how the metric load steppers have always
     * behaved, refer to [stepLoad].
     * - [UnitSystem.IMPERIAL]: the delta is added in pounds to the current value snapped onto the
     * hundredths of a pound, and the result is snapped as well. Repeated steps therefore stay on
     * the values the user steps through (135, 137.5, 140...) instead of drifting with the
     * conversion noise (184.99999999999994 lb after twenty steps of 2.5 lb from 135 lb).
     *
     * The result is kept within [MIN_WEIGHT_IN_KILOGRAMS] and [MAX_WEIGHT_IN_KILOGRAMS], so
     * stepping below zero gives zero.
     */
    fun stepBy(deltaInPounds: Double, unitSystem: UnitSystem): Weight = when (unitSystem) {
        UnitSystem.METRIC -> fromValueClamped(
            inKilograms + deltaInPounds * POUNDS_TO_KILOGRAMS,
            unitSystem
        )

        UnitSystem.IMPERIAL -> fromValueClamped(
            roundHalfUp(
                roundHalfUp(inPounds, NUMBER_OF_DECIMAL_DIGITS) + deltaInPounds,
                NUMBER_OF_DECIMAL_DIGITS
            ),
            unitSystem
        )
    }

    /**
     * Moves this weight by [delta], expressed in the unit of [unitSystem], on the grid that unit is
     * shown with (refer to [decimalDigits]): the current value is rounded first, the way the user
     * sees it, and so is the result. For instance, pounds being shown as whole numbers, 137.5 lb
     * stepped by 5 gives 143 lb.
     *
     * @param maxValueInUnit The largest value accepted, in the unit of [unitSystem]. It must not be
     * negative. The result is never negative either, nor outside the range accepted by [Weight].
     */
    fun stepInUnit(
        delta: Double,
        unitSystem: UnitSystem,
        maxValueInUnit: Double = Double.POSITIVE_INFINITY
    ): Weight {
        val stepped = roundHalfUp(roundedValue(unitSystem) + delta, decimalDigits(unitSystem))

        return fromValueClamped(stepped.coerceIn(0.0, maxValueInUnit), unitSystem)
    }

    /**
     * Moves this load one step [up] or down, as the -/+ buttons next to a set do.
     *
     * - [UnitSystem.METRIC]: by [METRIC_LOAD_STEP_IN_POUNDS] converted to kilograms (about
     * 1.134 kg), exactly as it has always been, refer to [stepBy].
     * - [UnitSystem.IMPERIAL]: by [IMPERIAL_LOAD_STEP_IN_POUNDS], i.e. a 2.5 lb plate on each side
     * of the bar, landing on whole pounds: the current value is rounded to the closest pound
     * first, so 135 lb goes to 140 lb and 137.5 lb (shown as 138 lb) goes to 143 lb.
     *
     * The result is never negative.
     */
    fun stepLoad(up: Boolean, unitSystem: UnitSystem): Weight = when (unitSystem) {
        UnitSystem.METRIC -> stepBy(
            deltaInPounds = if (up) METRIC_LOAD_STEP_IN_POUNDS else -METRIC_LOAD_STEP_IN_POUNDS,
            unitSystem = unitSystem
        )

        UnitSystem.IMPERIAL -> stepInUnit(
            delta = if (up) IMPERIAL_LOAD_STEP_IN_POUNDS else -IMPERIAL_LOAD_STEP_IN_POUNDS,
            unitSystem = unitSystem
        )
    }

    /**
     * Moves this weight increment, refer to [org.librefit.db.entity.Exercise.weightIncrement], one
     * step [up] or down by [incrementStep], on the grid of [unitSystem] (refer to [stepInUnit]).
     *
     * @param maxValueInUnit The largest increment accepted, in the unit of [unitSystem].
     */
    fun stepIncrement(
        up: Boolean,
        unitSystem: UnitSystem,
        maxValueInUnit: Double = Double.POSITIVE_INFINITY
    ): Weight {
        val step = incrementStep(unitSystem)

        return stepInUnit(
            delta = if (up) step else -step,
            unitSystem = unitSystem,
            maxValueInUnit = maxValueInUnit
        )
    }

    companion object {
        /** Minimum allowable weight in kilograms. */
        const val MIN_WEIGHT_IN_KILOGRAMS = 0.0

        /** Maximum allowable weight in kilograms. */
        const val MAX_WEIGHT_IN_KILOGRAMS = 999999.0

        /** Standard conversion factor for pounds to kilograms (1 lb ≈ 0.45359237 kg). */
        const val POUNDS_TO_KILOGRAMS = 0.45359237

        /** Standard conversion factor for kilograms to pounds (1 kg ≈ 2.20462262 lbs). */
        const val KILOGRAMS_TO_POUNDS = 2.2046226218487757

        /**
         * The number of decimal digits kilograms are shown and entered with. Refer to
         * [decimalDigits]
         */
        const val NUMBER_OF_DECIMAL_DIGITS = 2

        /**
         * The number of decimal digits pounds are shown and entered with: none, as a fraction of a
         * pound is below anything a gym can load. Refer to [decimalDigits]
         */
        const val NUMBER_OF_DECIMAL_DIGITS_IN_POUNDS = 0

        /**
         * The step of the load steppers in metric. It is expressed in pounds, as it has always
         * been, so it moves the load by about 1.134 kg. Refer to [stepLoad]
         */
        const val METRIC_LOAD_STEP_IN_POUNDS = 2.5

        /**
         * The step of the load steppers in imperial, i.e. a 2.5 lb plate on each side of the bar.
         * Refer to [stepLoad]
         */
        const val IMPERIAL_LOAD_STEP_IN_POUNDS = 5.0

        /**
         * The step of the weight increment steppers in metric, i.e. the smallest plate commonly
         * available. Refer to [incrementStep]
         */
        const val METRIC_INCREMENT_STEP_IN_KILOGRAMS = 1.25

        /**
         * The step of the weight increment steppers in imperial: pounds are whole numbers, and
         * 5 lb is a 2.5 lb plate on each side of the bar. Refer to [incrementStep]
         */
        const val IMPERIAL_INCREMENT_STEP_IN_POUNDS = 5.0

        /**
         * How far a value may fall short of a rounding boundary, in units in the last place, and
         * still be rounded as if it were on it. Converting between kilograms and pounds, or
         * summing a few hundred loads, leaves values a handful of ulps away from the decimal the
         * user entered, while this tolerance amounts to roughly 1e-13 of the value: far too little
         * to affect a value genuinely below a boundary, such as 61.2449999 kg.
         */
        private const val ROUNDING_TOLERANCE_IN_ULPS = 512.0

        /**
         * The number of decimal digits weights are shown and entered with in [unitSystem]:
         * [NUMBER_OF_DECIMAL_DIGITS] in kilograms and [NUMBER_OF_DECIMAL_DIGITS_IN_POUNDS], i.e.
         * whole numbers, in pounds. Every place turning a weight into text relies on it, refer to
         * [roundedValue].
         */
        fun decimalDigits(unitSystem: UnitSystem): Int = when (unitSystem) {
            UnitSystem.METRIC -> NUMBER_OF_DECIMAL_DIGITS
            UnitSystem.IMPERIAL -> NUMBER_OF_DECIMAL_DIGITS_IN_POUNDS
        }

        /**
         * The step of the weight increment steppers, expressed in the unit of [unitSystem]:
         * [METRIC_INCREMENT_STEP_IN_KILOGRAMS] or [IMPERIAL_INCREMENT_STEP_IN_POUNDS]. Refer to
         * [stepIncrement]
         */
        fun incrementStep(unitSystem: UnitSystem): Double = when (unitSystem) {
            UnitSystem.METRIC -> METRIC_INCREMENT_STEP_IN_KILOGRAMS
            UnitSystem.IMPERIAL -> IMPERIAL_INCREMENT_STEP_IN_POUNDS
        }

        /**
         * Rounds [value] half up, i.e. ties away from zero, to [numberOfDecimalDigits], as a
         * person would do on the decimal number it stands for rather than on its binary
         * approximation.
         *
         * ```
         * roundHalfUp(149.99999999999997, 0) // 150.0
         * roundHalfUp(137.5, 0)              // 138.0
         * roundHalfUp(0.4, 0)                // 0.0
         * roundHalfUp(1.005, 2)              // 1.01, although 1.005 is 1.00499999999999989... in binary
         * roundHalfUp(61.2449999, 2)         // 61.24
         * ```
         *
         * @throws IllegalArgumentException if [numberOfDecimalDigits] is negative
         * @return The rounded value, or [value] itself when it is not finite
         */
        fun roundHalfUp(value: Double, numberOfDecimalDigits: Int): Double {
            require(numberOfDecimalDigits >= 0) {
                "numberOfDecimalDigits must not be negative. Actual: $numberOfDecimalDigits"
            }
            if (!value.isFinite()) return value
            // It also turns -0.0 into 0.0, which would otherwise be shown with its sign
            if (value == 0.0) return 0.0

            // The value is pushed away from zero by the tolerance, so a value sitting a few ulps
            // short of a boundary because of the conversion noise is rounded as the decimal it
            // stands for
            val nudged = value + (value.ulp * ROUNDING_TOLERANCE_IN_ULPS).withSign(value)

            return BigDecimal(nudged)
                .setScale(numberOfDecimalDigits, RoundingMode.HALF_UP)
                .toDouble()
        }

        /**
         * Creates a [Weight] from [value], expressed in the unit of [unitSystem], bringing it back
         * within [MIN_WEIGHT_IN_KILOGRAMS] and [MAX_WEIGHT_IN_KILOGRAMS] instead of throwing.
         * Within that range, the result is exactly the one of [auto].
         */
        private fun fromValueClamped(value: Double, unitSystem: UnitSystem): Weight {
            val valueInKilograms = when (unitSystem) {
                UnitSystem.METRIC -> value
                UnitSystem.IMPERIAL -> value * POUNDS_TO_KILOGRAMS
            }

            return kilograms(
                valueInKilograms.coerceIn(MIN_WEIGHT_IN_KILOGRAMS, MAX_WEIGHT_IN_KILOGRAMS)
            )
        }

        /**
         * Factory method to create a [Weight] instance from a kilogram value.
         * 
         * @param value The weight in kilograms.
         * @throws IllegalArgumentException if the provided [value] is outside the permitted [MIN_WEIGHT_IN_KILOGRAMS] and [MAX_WEIGHT_IN_KILOGRAMS] range.
         * @return A valid [Weight] instance.
         */
        fun kilograms(
            @FloatRange(from = MIN_WEIGHT_IN_KILOGRAMS, to = MAX_WEIGHT_IN_KILOGRAMS) value: Double
        ): Weight {
            require(value in MIN_WEIGHT_IN_KILOGRAMS..MAX_WEIGHT_IN_KILOGRAMS) {
                "Weight must be between $MIN_WEIGHT_IN_KILOGRAMS and $MAX_WEIGHT_IN_KILOGRAMS. Actual: $value"
            }

            return Weight(value)
        }

        /**
         * Factory method to create a [Weight] instance from a pound value.
         * 
         * @param value The weight in pounds.
         * @throws IllegalArgumentException if the resulting kilogram conversion is outside the permitted range.
         * @return A valid [Weight] instance.
         */
        fun pounds(
            @FloatRange(
                from = MIN_WEIGHT_IN_KILOGRAMS * KILOGRAMS_TO_POUNDS,
                to = MAX_WEIGHT_IN_KILOGRAMS * KILOGRAMS_TO_POUNDS
            )
            value: Double
        ): Weight {
            val inKg = value * POUNDS_TO_KILOGRAMS
            return kilograms(inKg)
        }

        /**
         * Factory method to create a [Weight] instance from [value] automatically parsed based on [unitSystem]
         *
         * @param value The weight value
         * @param unitSystem The unit system value
         * @throws IllegalArgumentException if the resulting kilogram conversion is outside the permitted range.
         * @return A valid [Weight] instance.
         */
        fun auto(value: Double, unitSystem: UnitSystem): Weight {
            return when (unitSystem) {
                UnitSystem.METRIC -> kilograms(value)
                UnitSystem.IMPERIAL -> pounds(value)
            }
        }

        /**
         * Factory method to create a [Weight] instance from [value] automatically parsed based on current [UnitSystem]
         *
         * @param value The weight value
         * @throws IllegalArgumentException if the resulting kilogram conversion is outside the permitted range.
         * @return A valid [Weight] instance.
         */
        @Composable
        fun auto(value: Double): Weight {
            val unitSystem = LocalUnitSystem.current

            return when (unitSystem) {
                UnitSystem.METRIC -> kilograms(value)
                UnitSystem.IMPERIAL -> pounds(value)
            }
        }

        /**
         * Factory method to create a [Weight] instance with value zero kilograms/pounds
         *
         * @return A valid [Weight] instance.
         */
        fun zero(): Weight {
            return kilograms(0.0)
        }
    }
}