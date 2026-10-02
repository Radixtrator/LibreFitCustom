/*
 * SPDX-License-Identifier: GPL-3.0-or-later
 * Copyright (c) 2026. The LibreFit Contributors
 *
 * LibreFit is subject to additional terms covering author attribution and trademark usage;
 * see the ADDITIONAL_TERMS.md and TRADEMARK_POLICY.md files in the project root.
 */

package org.librefit.models

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.librefit.enums.userPreferences.UnitSystem
import java.math.BigDecimal

class WeightTest {

    @Test
    fun `pounds are shown with no decimals and kilograms with two`() {
        assertThat(Weight.decimalDigits(UnitSystem.IMPERIAL)).isEqualTo(0)
        assertThat(Weight.decimalDigits(UnitSystem.METRIC))
            .isEqualTo(Weight.NUMBER_OF_DECIMAL_DIGITS)
    }

    @Test
    fun `common plate loads in pounds survive the round trip through kilograms`() {
        listOf(135.0, 45.0, 225.0, 5.0).forEach { pounds ->
            assertThat(Weight.pounds(pounds).roundedValue(UnitSystem.IMPERIAL)).isEqualTo(pounds)
        }
    }

    @Test
    fun `loads coming back slightly short of the pound are rounded rather than floored`() {
        // 150 lb comes back as 149.99999999999997 lb and 29 lb as 28.999999999999996 lb
        assertThat(Weight.pounds(150.0).inPounds).isLessThan(150.0)
        assertThat(Weight.pounds(29.0).inPounds).isLessThan(29.0)

        assertThat(Weight.pounds(150.0).roundedValue(UnitSystem.IMPERIAL)).isEqualTo(150.0)
        assertThat(Weight.pounds(29.0).roundedValue(UnitSystem.IMPERIAL)).isEqualTo(29.0)
    }

    @Test
    fun `every whole pound up to two thousand is shown as it was entered`() {
        (0..2000).forEach { pounds ->
            assertThat(Weight.pounds(pounds.toDouble()).roundedValue(UnitSystem.IMPERIAL))
                .isEqualTo(pounds.toDouble())
        }
    }

    @Test
    fun `fractions of a pound are rounded half up to whole pounds`() {
        assertThat(Weight.pounds(137.5).roundedValue(UnitSystem.IMPERIAL)).isEqualTo(138.0)
        assertThat(Weight.pounds(136.5).roundedValue(UnitSystem.IMPERIAL)).isEqualTo(137.0)
        assertThat(Weight.pounds(2.5).roundedValue(UnitSystem.IMPERIAL)).isEqualTo(3.0)
        assertThat(Weight.pounds(0.5).roundedValue(UnitSystem.IMPERIAL)).isEqualTo(1.0)
        assertThat(Weight.pounds(0.4).roundedValue(UnitSystem.IMPERIAL)).isEqualTo(0.0)
    }

    @Test
    fun `explicit decimals still show fractions of a pound`() {
        assertThat(Weight.pounds(137.5).roundedValue(UnitSystem.IMPERIAL, 2)).isEqualTo(137.5)
    }

    @Test
    fun `kilograms keep their two decimals`() {
        assertThat(Weight.kilograms(61.25).roundedValue(UnitSystem.METRIC)).isEqualTo(61.25)
        assertThat(Weight.kilograms(80.0).roundedValue(UnitSystem.METRIC)).isEqualTo(80.0)
    }

    @Test
    fun `every hundredth of a kilogram up to a thousand is shown as it was entered`() {
        // Flooring value * 100 used to show 2.3 kg as 2.29 and 1.15 kg as 1.14
        (0..100000).forEach { hundredths ->
            val kilograms = BigDecimal.valueOf(hundredths.toLong(), 2).toDouble()

            assertThat(Weight.kilograms(kilograms).roundedValue(UnitSystem.METRIC))
                .isEqualTo(kilograms)
        }
    }

    @Test
    fun `zero is shown as zero in both unit systems`() {
        assertThat(Weight.zero().roundedValue(UnitSystem.IMPERIAL)).isEqualTo(0.0)
        assertThat(Weight.zero().roundedValue(UnitSystem.METRIC)).isEqualTo(0.0)
    }

    @Test
    fun `roundHalfUp rounds conversion noise as the decimal it stands for`() {
        assertThat(Weight.roundHalfUp(149.99999999999997, 0)).isEqualTo(150.0)
        assertThat(Weight.roundHalfUp(134.99999999999997, 0)).isEqualTo(135.0)
        // Noise just below a tie
        assertThat(Weight.roundHalfUp(137.49999999999997, 0)).isEqualTo(138.0)
        // 1.005 and 2.675 are slightly below the tie in binary
        assertThat(Weight.roundHalfUp(1.005, 2)).isEqualTo(1.01)
        assertThat(Weight.roundHalfUp(2.675, 2)).isEqualTo(2.68)
        assertThat(Weight.roundHalfUp(61.245, 2)).isEqualTo(61.25)
    }

    @Test
    fun `roundHalfUp leaves values genuinely below the tie alone`() {
        assertThat(Weight.roundHalfUp(61.2449999, 2)).isEqualTo(61.24)
        assertThat(Weight.roundHalfUp(61.2449999999, 2)).isEqualTo(61.24)
        assertThat(Weight.roundHalfUp(137.4, 0)).isEqualTo(137.0)
        assertThat(Weight.roundHalfUp(0.4, 0)).isEqualTo(0.0)
    }

    @Test
    fun `roundHalfUp rounds ties away from zero`() {
        assertThat(Weight.roundHalfUp(0.5, 0)).isEqualTo(1.0)
        assertThat(Weight.roundHalfUp(2.5, 0)).isEqualTo(3.0)
        assertThat(Weight.roundHalfUp(-2.5, 0)).isEqualTo(-3.0)
    }

    @Test
    fun `roundHalfUp handles zero and values that are not finite`() {
        assertThat(Weight.roundHalfUp(0.0, 2)).isEqualTo(0.0)
        // No negative zero, which would be shown with its sign
        assertThat(1.0 / Weight.roundHalfUp(-0.0, 0)).isPositiveInfinity()
        assertThat(Weight.roundHalfUp(Double.NaN, 2)).isNaN()
    }

    @Test
    fun `roundHalfUp handles large values`() {
        assertThat(Weight.roundHalfUp(999999.994, 2)).isEqualTo(999999.99)
        assertThat(Weight.roundHalfUp(999999.995, 2)).isEqualTo(1000000.0)
        assertThat(Weight.roundHalfUp(123456.785, 2)).isEqualTo(123456.79)
        assertThat(
            Weight.kilograms(Weight.MAX_WEIGHT_IN_KILOGRAMS).roundedValue(UnitSystem.IMPERIAL)
        ).isEqualTo(2204620.0)
    }

    @Test
    fun `roundHalfUp absorbs the noise of a sum of many loads`() {
        val volume = (1..200).sumOf { Weight.pounds(150.0).inPounds }

        assertThat(Weight.roundHalfUp(volume, 0)).isEqualTo(30000.0)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `roundHalfUp rejects a negative number of decimal digits`() {
        Weight.roundHalfUp(1.0, -1)
    }

    @Test
    fun `stepping a load in pounds moves by five pounds on whole pounds`() {
        var load = Weight.pounds(135.0)

        repeat(20) { i ->
            load = load.stepLoad(up = true, unitSystem = UnitSystem.IMPERIAL)

            assertThat(load.roundedValue(UnitSystem.IMPERIAL)).isEqualTo(140.0 + 5 * i)
            assertThat(load.inPounds).isWithin(1e-9).of(140.0 + 5 * i)
        }
        assertThat(load.roundedValue(UnitSystem.IMPERIAL)).isEqualTo(235.0)

        repeat(20) { load = load.stepLoad(up = false, unitSystem = UnitSystem.IMPERIAL) }
        assertThat(load.roundedValue(UnitSystem.IMPERIAL)).isEqualTo(135.0)
    }

    @Test
    fun `stepping a fractional load in pounds rounds it to the pound first`() {
        val load = Weight.pounds(137.5)

        assertThat(load.stepLoad(up = true, unitSystem = UnitSystem.IMPERIAL).inPounds)
            .isWithin(1e-9).of(143.0)
        assertThat(load.stepLoad(up = false, unitSystem = UnitSystem.IMPERIAL).inPounds)
            .isWithin(1e-9).of(133.0)
    }

    @Test
    fun `stepping a load down never goes below zero`() {
        assertThat(Weight.pounds(3.0).stepLoad(up = false, unitSystem = UnitSystem.IMPERIAL))
            .isEqualTo(Weight.zero())
        assertThat(Weight.zero().stepLoad(up = false, unitSystem = UnitSystem.IMPERIAL))
            .isEqualTo(Weight.zero())
        assertThat(Weight.zero().stepLoad(up = false, unitSystem = UnitSystem.METRIC))
            .isEqualTo(Weight.zero())
    }

    @Test
    fun `stepping a load in kilograms is unchanged`() {
        val step = Weight.METRIC_LOAD_STEP_IN_POUNDS * Weight.POUNDS_TO_KILOGRAMS

        (0..2000).forEach { i ->
            val load = Weight.kilograms(i / 20.0 + 1.2)

            assertThat(load.stepLoad(up = true, unitSystem = UnitSystem.METRIC))
                .isEqualTo(Weight.kilograms(load.inKilograms + step))
            assertThat(load.stepLoad(up = false, unitSystem = UnitSystem.METRIC))
                .isEqualTo(Weight.kilograms(load.inKilograms - step))
        }
        assertThat(Weight.kilograms(61.25).stepLoad(up = true, unitSystem = UnitSystem.METRIC))
            .isEqualTo(Weight.kilograms(61.25 + step))
    }

    @Test
    fun `stepBy two and a half pounds stays on the grid`() {
        var load = Weight.pounds(135.0)

        repeat(20) { i ->
            load = load.stepBy(2.5, UnitSystem.IMPERIAL)

            assertThat(load.roundedValue(UnitSystem.IMPERIAL, 2)).isEqualTo(137.5 + 2.5 * i)
        }
        assertThat(load.roundedValue(UnitSystem.IMPERIAL, 2)).isEqualTo(185.0)
    }

    @Test
    fun `stepBy below zero gives zero`() {
        assertThat(Weight.pounds(1.0).stepBy(-2.5, UnitSystem.IMPERIAL)).isEqualTo(Weight.zero())
    }

    @Test
    fun `weight increments step by five pounds or one and a quarter kilograms`() {
        var pounds = Weight.zero()
        var kilograms = Weight.zero()

        val shownPounds = (1..4).map {
            pounds = pounds.stepIncrement(up = true, unitSystem = UnitSystem.IMPERIAL)
            pounds.roundedValue(UnitSystem.IMPERIAL)
        }
        val shownKilograms = (1..4).map {
            kilograms = kilograms.stepIncrement(up = true, unitSystem = UnitSystem.METRIC)
            kilograms.roundedValue(UnitSystem.METRIC)
        }

        assertThat(shownPounds).containsExactly(5.0, 10.0, 15.0, 20.0).inOrder()
        assertThat(shownKilograms).containsExactly(1.25, 2.5, 3.75, 5.0).inOrder()
    }

    @Test
    fun `a legacy increment of two and a half pounds is shown and stepped as three pounds`() {
        val increment = Weight.pounds(2.5)

        assertThat(increment.roundedValue(UnitSystem.IMPERIAL)).isEqualTo(3.0)
        assertThat(
            increment.stepIncrement(up = true, unitSystem = UnitSystem.IMPERIAL)
                .roundedValue(UnitSystem.IMPERIAL)
        ).isEqualTo(8.0)
        assertThat(increment.stepIncrement(up = false, unitSystem = UnitSystem.IMPERIAL))
            .isEqualTo(Weight.zero())
    }

    @Test
    fun `weight increments stay within the given maximum`() {
        assertThat(
            Weight.pounds(998.0)
                .stepIncrement(up = true, unitSystem = UnitSystem.IMPERIAL, maxValueInUnit = 1000.0)
                .roundedValue(UnitSystem.IMPERIAL)
        ).isEqualTo(1000.0)
        assertThat(
            Weight.kilograms(999.5)
                .stepIncrement(up = true, unitSystem = UnitSystem.METRIC, maxValueInUnit = 1000.0)
                .roundedValue(UnitSystem.METRIC)
        ).isEqualTo(1000.0)
    }
}
