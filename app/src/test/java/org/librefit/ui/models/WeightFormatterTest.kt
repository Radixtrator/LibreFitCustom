/*
 * SPDX-License-Identifier: GPL-3.0-or-later
 * Copyright (c) 2026. The LibreFit Contributors
 *
 * LibreFit is subject to additional terms covering author attribution and trademark usage;
 * see the ADDITIONAL_TERMS.md and TRADEMARK_POLICY.md files in the project root.
 */

package org.librefit.ui.models

import androidx.compose.ui.text.input.KeyboardType
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.librefit.enums.userPreferences.UnitSystem
import org.librefit.models.Weight
import org.librefit.util.Formatter

class WeightFormatterTest {

    /**
     * Types [text] one character at a time in a weight field following the pattern documented in
     * [isRepresentedBy]: every keystroke writes the parsed weight back, and the text is rebuilt
     * from the weight only when it does not represent it any more.
     *
     * @return The text left in the field and the resulting weight
     */
    private fun type(text: String, unitSystem: UnitSystem): Pair<String, Weight> {
        var field = ""
        var weight = Weight.zero()

        text.forEach { char ->
            field = normalizeWeightInput(field + char, unitSystem)
            weight = Weight.auto(parseWeightInput(field, unitSystem) ?: 0.0, unitSystem)

            if (!weight.isRepresentedBy(field, unitSystem)) {
                field = weight.toInputText(unitSystem)
            }
        }

        return field to weight
    }

    @Test
    fun `doubleValue shows pounds as whole numbers and kilograms with two decimals`() {
        assertThat(Weight.pounds(150.0).doubleValue(UnitSystem.IMPERIAL)).isEqualTo(150.0)
        assertThat(Weight.pounds(137.5).doubleValue(UnitSystem.IMPERIAL)).isEqualTo(138.0)
        assertThat(Weight.kilograms(61.25).doubleValue(UnitSystem.METRIC)).isEqualTo(61.25)
        assertThat(Weight.kilograms(2.3).doubleValue(UnitSystem.METRIC)).isEqualTo(2.3)
    }

    @Test
    fun `input text has no decimals in pounds`() {
        assertThat(Weight.pounds(135.0).toInputText(UnitSystem.IMPERIAL)).isEqualTo("135")
        assertThat(Weight.pounds(150.0).toInputText(UnitSystem.IMPERIAL)).isEqualTo("150")
        assertThat(Weight.pounds(137.5).toInputText(UnitSystem.IMPERIAL)).isEqualTo("138")
        assertThat(Weight.pounds(1234567.0).toInputText(UnitSystem.IMPERIAL)).isEqualTo("1234567")
        assertThat(Weight.zero().toInputText(UnitSystem.IMPERIAL)).isEqualTo("0")
    }

    @Test
    fun `input text has no trailing zeros in kilograms`() {
        assertThat(Weight.kilograms(61.25).toInputText(UnitSystem.METRIC)).isEqualTo("61.25")
        assertThat(Weight.kilograms(60.5).toInputText(UnitSystem.METRIC)).isEqualTo("60.5")
        assertThat(Weight.kilograms(60.0).toInputText(UnitSystem.METRIC)).isEqualTo("60")
        assertThat(Weight.kilograms(100.0).toInputText(UnitSystem.METRIC)).isEqualTo("100")
        assertThat(Weight.zero().toInputText(UnitSystem.METRIC)).isEqualTo("0")
    }

    @Test
    fun `pound input keeps whole numbers only`() {
        assertThat(normalizeWeightInput("135.5", UnitSystem.IMPERIAL)).isEqualTo("135")
        assertThat(normalizeWeightInput("1,5", UnitSystem.IMPERIAL)).isEqualTo("1")
        assertThat(normalizeWeightInput("1a3", UnitSystem.IMPERIAL)).isEqualTo("13")
        assertThat(normalizeWeightInput("1350", UnitSystem.IMPERIAL)).isEqualTo("135")
        assertThat(normalizeWeightInput(".", UnitSystem.IMPERIAL)).isEmpty()
        assertThat(normalizeWeightInput("", UnitSystem.IMPERIAL)).isEmpty()
    }

    @Test
    fun `pound input drops leading zeros`() {
        assertThat(normalizeWeightInput("05", UnitSystem.IMPERIAL)).isEqualTo("5")
        assertThat(normalizeWeightInput("0135", UnitSystem.IMPERIAL)).isEqualTo("135")
        assertThat(normalizeWeightInput("000", UnitSystem.IMPERIAL)).isEqualTo("0")
    }

    @Test
    fun `kilogram input is normalized as before`() {
        listOf("61,255", "61.", "1234.567", ".5", "abc").forEach { text ->
            assertThat(normalizeWeightInput(text, UnitSystem.METRIC))
                .isEqualTo(Formatter.normalizeNumericString(text))
        }
    }

    @Test
    fun `parsing pound input ignores decimals`() {
        assertThat(parseWeightInput("135.7", UnitSystem.IMPERIAL)).isEqualTo(135.0)
        assertThat(parseWeightInput("0", UnitSystem.IMPERIAL)).isEqualTo(0.0)
        assertThat(parseWeightInput("", UnitSystem.IMPERIAL)).isNull()
    }

    @Test
    fun `parsing kilogram input keeps two decimals`() {
        assertThat(parseWeightInput("61.25", UnitSystem.METRIC)).isEqualTo(61.25)
        assertThat(parseWeightInput("90.3", UnitSystem.METRIC)).isEqualTo(90.3)
        assertThat(parseWeightInput("", UnitSystem.METRIC)).isNull()
    }

    @Test
    fun `a weight is represented by its text at the displayed precision`() {
        assertThat(Weight.pounds(150.0).isRepresentedBy("150", UnitSystem.IMPERIAL)).isTrue()
        assertThat(Weight.pounds(137.5).isRepresentedBy("138", UnitSystem.IMPERIAL)).isTrue()
        assertThat(Weight.kilograms(61.0).isRepresentedBy("61.", UnitSystem.METRIC)).isTrue()
        assertThat(Weight.kilograms(61.2).isRepresentedBy("61.20", UnitSystem.METRIC)).isTrue()
        assertThat(Weight.pounds(140.0).isRepresentedBy("135", UnitSystem.IMPERIAL)).isFalse()
    }

    @Test
    fun `an empty or incomplete text represents zero`() {
        assertThat(Weight.zero().isRepresentedBy("", UnitSystem.IMPERIAL)).isTrue()
        assertThat(Weight.zero().isRepresentedBy("0.", UnitSystem.METRIC)).isTrue()
        assertThat(Weight.pounds(5.0).isRepresentedBy("", UnitSystem.IMPERIAL)).isFalse()
    }

    @Test
    fun `a text left over from another unit system does not represent the weight`() {
        assertThat(Weight.kilograms(61.25).isRepresentedBy("61.25", UnitSystem.IMPERIAL))
            .isFalse()
    }

    @Test
    fun `typing any whole number keeps the typed text`() {
        UnitSystem.entries.forEach { unitSystem ->
            (0..999).forEach { number ->
                val (field, weight) = type(number.toString(), unitSystem)

                assertThat(field).isEqualTo(number.toString())
                assertThat(weight.doubleValue(unitSystem)).isEqualTo(number.toDouble())
            }
        }
    }

    @Test
    fun `typing decimals in kilograms keeps the typed text`() {
        assertThat(type("61.25", UnitSystem.METRIC).first).isEqualTo("61.25")
        assertThat(type("0.5", UnitSystem.METRIC).first).isEqualTo("0.5")
    }

    @Test
    fun `typing a separator in pounds is ignored`() {
        val (field, weight) = type("135.5", UnitSystem.IMPERIAL)

        // The separator is dropped, and the last digit does not fit in the three allowed
        assertThat(field).isEqualTo("135")
        assertThat(weight.doubleValue(UnitSystem.IMPERIAL)).isEqualTo(135.0)
    }

    @Test
    fun `a change from somewhere else rebuilds the text`() {
        val field = "135"
        val stepped = Weight.pounds(135.0).stepLoad(up = true, unitSystem = UnitSystem.IMPERIAL)

        assertThat(stepped.isRepresentedBy(field, UnitSystem.IMPERIAL)).isFalse()
        assertThat(stepped.toInputText(UnitSystem.IMPERIAL)).isEqualTo("140")
    }

    @Test
    fun `pounds are typed on a keyboard without decimal separator`() {
        assertThat(weightKeyboardType(UnitSystem.IMPERIAL)).isEqualTo(KeyboardType.Number)
        assertThat(weightKeyboardType(UnitSystem.METRIC)).isEqualTo(KeyboardType.Decimal)
    }
}
