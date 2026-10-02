/*
 * SPDX-License-Identifier: GPL-3.0-or-later
 * Copyright (c) 2026. The LibreFit Contributors
 *
 * LibreFit is subject to additional terms covering author attribution and trademark usage;
 * see the ADDITIONAL_TERMS.md and TRADEMARK_POLICY.md files in the project root.
 */

package org.librefit.ui.models

import com.google.common.truth.Truth.assertThat
import java.math.BigDecimal
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.collections.immutable.toImmutableList
import org.librefit.enums.userPreferences.UnitSystem
import org.librefit.models.Weight
import org.librefit.util.Formatter.getDecimalDigitsAsInteger

class InputModalBottomSheetStateTest {

    @Test
    fun `the wheel opens on the pound shown everywhere else`() {
        // 150 lb comes back as 149.99999999999997 lb, which used to open the wheel on 149.99
        val state = InputModalBottomSheetState.Weight.fromWeight(
            weight = Weight.pounds(150.0),
            unitSystem = UnitSystem.IMPERIAL
        )

        assertThat(state.integerWeight).isEqualTo(150)
        assertThat(state.decimalWeight).isEqualTo(0)
    }

    @Test
    fun `the wheel has no decimal part in pounds`() {
        val state = InputModalBottomSheetState.Weight.fromWeight(
            weight = Weight.pounds(135.0),
            unitSystem = UnitSystem.IMPERIAL
        )

        assertThat(state.hasDecimalPart).isFalse()
        assertThat(state.totalWeight).isEqualTo(135.0)
        assertThat(state.toWeight(UnitSystem.IMPERIAL).toInputText(UnitSystem.IMPERIAL))
            .isEqualTo("135")
    }

    @Test
    fun `a fractional pound opens the wheel on the rounded pound`() {
        val state = InputModalBottomSheetState.Weight.fromWeight(
            weight = Weight.pounds(137.5),
            unitSystem = UnitSystem.IMPERIAL
        )

        assertThat(state.integerWeight).isEqualTo(138)
    }

    @Test
    fun `every whole pound survives the wheel`() {
        (0..999).forEach { pounds ->
            val state = InputModalBottomSheetState.Weight.fromWeight(
                weight = Weight.pounds(pounds.toDouble()),
                unitSystem = UnitSystem.IMPERIAL
            )

            assertThat(state.integerWeight).isEqualTo(pounds)
            assertThat(state.toWeight(UnitSystem.IMPERIAL).toInputText(UnitSystem.IMPERIAL))
                .isEqualTo(pounds.toString())
        }
    }

    @Test
    fun `the wheel keeps its decimal part in kilograms`() {
        val state = InputModalBottomSheetState.Weight.fromWeight(
            weight = Weight.kilograms(61.15),
            unitSystem = UnitSystem.METRIC
        )

        assertThat(state.hasDecimalPart).isTrue()
        assertThat(state.integerWeight).isEqualTo(61)
        assertThat(state.decimalWeight).isEqualTo(15)
        assertThat(state.totalWeight).isEqualTo(61.15)
    }

    @Test
    fun `every twentieth of a kilogram survives the wheel`() {
        (0..19999).forEach { twentieths ->
            val kilograms = BigDecimal.valueOf(twentieths * 5L, 2).toDouble()

            val state = InputModalBottomSheetState.Weight.fromWeight(
                weight = Weight.kilograms(kilograms),
                unitSystem = UnitSystem.METRIC
            )

            assertThat(state.totalWeight).isEqualTo(kilograms)
        }
    }

    @Test
    fun `the wheel snaps kilograms to its steps as before`() {
        val state = InputModalBottomSheetState.Weight.fromWeight(
            weight = Weight.kilograms(61.23),
            unitSystem = UnitSystem.METRIC
        )

        assertThat(state.decimalWeight).isEqualTo(25)
    }

    @Test
    fun `a weight beyond the wheel opens on its largest value`() {
        val state = InputModalBottomSheetState.Weight.fromWeight(
            weight = Weight.pounds(1500.0),
            unitSystem = UnitSystem.IMPERIAL
        )

        assertThat(state.integerWeight).isEqualTo(999)
    }

    @Test
    fun `decimal digits are read from the decimal value`() {
        // 61.15 - 61 is 0.14999999999999858 in floating point, which used to give 14
        assertThat(61.15.getDecimalDigitsAsInteger()).isEqualTo(15)
        assertThat(2.3.getDecimalDigitsAsInteger()).isEqualTo(30)
        assertThat(61.5.getDecimalDigitsAsInteger()).isEqualTo(50)
        assertThat(135.0.getDecimalDigitsAsInteger()).isEqualTo(0)
        assertThat(61.999.getDecimalDigitsAsInteger()).isEqualTo(99)
        assertThat(Double.NaN.getDecimalDigitsAsInteger()).isEqualTo(0)
    }

    // ---------- MinutesSeconds ----------

    @Test
    fun `steppedBy carries over from seconds to minutes on positive delta`() {
        val state = InputModalBottomSheetState.MinutesSeconds(minutes = 0, seconds = 50)

        val stepped = state.steppedBy(30)

        assertEquals(1, stepped.minutes)
        assertEquals(20, stepped.seconds)
    }

    @Test
    fun `steppedBy carries over from minutes to seconds on negative delta`() {
        val state = InputModalBottomSheetState.MinutesSeconds(minutes = 1, seconds = 5)

        val stepped = state.steppedBy(-30)

        assertEquals(0, stepped.minutes)
        assertEquals(35, stepped.seconds)
    }

    @Test
    fun `steppedBy clamps minutes seconds state at zero`() {
        val state = InputModalBottomSheetState.MinutesSeconds(minutes = 0, seconds = 10)

        val stepped = state.steppedBy(-30)

        assertEquals(
            InputModalBottomSheetState.MinutesSeconds(minutes = 0, seconds = 0),
            stepped
        )
    }

    @Test
    fun `steppedBy clamps minutes seconds state at maximum range`() {
        val state = InputModalBottomSheetState.MinutesSeconds(minutes = 59, seconds = 59)

        val stepped = state.steppedBy(30)

        assertEquals(
            InputModalBottomSheetState.MinutesSeconds(minutes = 59, seconds = 59),
            stepped
        )
    }

    @Test
    fun `steppedBy with zero delta returns an equal minutes seconds state`() {
        val state = InputModalBottomSheetState.MinutesSeconds(minutes = 3, seconds = 25)

        val stepped = state.steppedBy(0)

        assertEquals(state, stepped)
    }

    // ---------- HoursMinutesSeconds ----------

    @Test
    fun `steppedBy carries over across the hour boundary`() {
        val state =
            InputModalBottomSheetState.HoursMinutesSeconds(hours = 0, minutes = 59, seconds = 50)

        val stepped = state.steppedBy(30)

        assertEquals(1, stepped.hours)
        assertEquals(0, stepped.minutes)
        assertEquals(20, stepped.seconds)
    }

    @Test
    fun `steppedBy clamps hours minutes seconds state at zero`() {
        val state =
            InputModalBottomSheetState.HoursMinutesSeconds(hours = 0, minutes = 0, seconds = 10)

        val stepped = state.steppedBy(-30)

        assertEquals(
            InputModalBottomSheetState.HoursMinutesSeconds(
                hours = 0,
                minutes = 0,
                seconds = 0
            ),
            stepped
        )
    }

    @Test
    fun `steppedBy clamps hours minutes seconds state at maximum range`() {
        val state =
            InputModalBottomSheetState.HoursMinutesSeconds(hours = 23, minutes = 59, seconds = 59)

        val stepped = state.steppedBy(30)

        assertEquals(
            InputModalBottomSheetState.HoursMinutesSeconds(
                hours = 23,
                minutes = 59,
                seconds = 59
            ),
            stepped
        )
    }

    // ---------- Reps ----------

    @Test
    fun `steppedBy increments and decrements reps`() {
        val state = InputModalBottomSheetState.Reps(reps = 8)

        val incremented = state.steppedBy(5)
        val decremented = state.steppedBy(-5)

        assertEquals(13, incremented.reps)
        assertEquals(3, decremented.reps)
    }

    @Test
    fun `steppedBy clamps reps at range bounds`() {
        val decremented = InputModalBottomSheetState.Reps(reps = 2).steppedBy(-5)
        val incrementedFromMax = InputModalBottomSheetState.Reps(reps = 997).steppedBy(5)

        assertEquals(0, decremented.reps)
        assertEquals(999, incrementedFromMax.reps)
    }

    // ---------- Weight ----------

    @Test
    fun `steppedBy shifts the integer part and preserves the decimal part`() {
        val state = InputModalBottomSheetState.Weight.create(integerWeight = 62, decimalWeight = 30)

        val stepped = state.steppedBy(10)

        assertEquals(72, stepped.integerWeight)
        assertEquals(30, stepped.decimalWeight)
        assertEquals(72.3, stepped.totalWeight, absoluteTolerance = 1e-9)
    }

    @Test
    fun `steppedBy clamps weight at range bounds while keeping the decimal part`() {
        val state = InputModalBottomSheetState.Weight.create(integerWeight = 2, decimalWeight = 30)

        val stepped = state.steppedBy(-10)

        assertEquals(0, stepped.integerWeight)
        assertEquals(30, stepped.decimalWeight)
    }

    @Test
    fun `steppedBy snaps the shifted weight to the configured range steps`() {
        val state = InputModalBottomSheetState.Weight.create(
            integerWeight = 5,
            decimalWeight = 0,
            integerStep = 5,
            integerWeightRange = (0..999 step 5).toImmutableList()
        )

        val stepped = state.steppedBy(10)

        assertEquals(15, stepped.integerWeight)
    }
}
