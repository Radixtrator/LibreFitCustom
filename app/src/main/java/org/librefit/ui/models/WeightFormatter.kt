/*
 * SPDX-License-Identifier: GPL-3.0-or-later
 * Copyright (c) 2026. The LibreFit Contributors
 *
 * LibreFit is subject to additional terms covering author attribution and trademark usage;
 * see the ADDITIONAL_TERMS.md and TRADEMARK_POLICY.md files in the project root.
 */

package org.librefit.ui.models

import android.icu.text.MeasureFormat
import android.icu.util.Measure
import android.icu.util.MeasureUnit
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import org.librefit.R
import org.librefit.enums.userPreferences.UnitSystem
import org.librefit.models.Weight
import org.librefit.models.Weight.Companion.NUMBER_OF_DECIMAL_DIGITS
import org.librefit.nav.LocalUnitSystem
import org.librefit.util.Formatter
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.Locale


/**
 * It returns the value of [Weight] instance in current [UnitSystem] followed by its unit, e.g.
 * "61.25 kg" or "135 lb".
 *
 * @param numberOfDecimalDigits The number of decimal digits to round to. By default, it is the
 * precision of the current [UnitSystem], refer to [Weight.decimalDigits], so pounds are shown as
 * whole numbers.
 */
@Composable
fun Weight.formatToText(
    numberOfDecimalDigits: Int? = null
): String {
    val unitSystem = LocalUnitSystem.current

    return remember(this, unitSystem, numberOfDecimalDigits) {
        // Automatically translates "kg"/"lbs" depending on device language
        val format = MeasureFormat.getInstance(Locale.getDefault(), MeasureFormat.FormatWidth.SHORT)

        val value = roundedValue(
            unitSystem = unitSystem,
            numberOfDecimalDigits = numberOfDecimalDigits ?: Weight.decimalDigits(unitSystem)
        )
        format.format(
            Measure(
                value,
                when (unitSystem) {
                    UnitSystem.METRIC -> MeasureUnit.KILOGRAM
                    UnitSystem.IMPERIAL -> MeasureUnit.POUND
                }
            )
        )
    }
}

/**
 * It returns value of [Weight] instance in current [UnitSystem] without any suffix or unit
 */
@Composable
fun Weight.doubleValue(): Double {
    val unitSystem = LocalUnitSystem.current

    return remember(this, unitSystem) {
        this.doubleValue(unitSystem)
    }
}

/**
 * It returns value of [Weight] instance in current [UnitSystem] as string without any suffix or
 * unit, nor trailing zeros. Refer to [toInputText]
 */
@Composable
fun Weight.doubleValueAsString(): String {
    val unitSystem = LocalUnitSystem.current

    return remember(this, unitSystem) {
        this.toInputText(unitSystem)
    }
}

/**
 * It returns value of [Weight] instance based on passed [unitSystem], rounded half up to
 * [numberOfDecimalDigits]. Refer to [Weight.roundedValue]
 *
 * @param numberOfDecimalDigits By default, it is the precision of [unitSystem], refer to
 * [Weight.decimalDigits], so pounds are whole numbers.
 */
fun Weight.doubleValue(
    unitSystem: UnitSystem,
    numberOfDecimalDigits: Int = Weight.decimalDigits(unitSystem)
): Double {
    return roundedValue(unitSystem, numberOfDecimalDigits)
}

fun Double.toWeight(unitSystem: UnitSystem): Weight {
    return Weight.auto(this, unitSystem)
}

@Composable
fun Double.toWeight(): Weight {
    val unitSystem = LocalUnitSystem.current
    return remember(this, unitSystem) {
        this.toWeight(unitSystem)
    }
}

/**
 * Caps [decimalCount], the number of decimal digits a chart or a label would show a weight with,
 * to the precision of the current [UnitSystem] (refer to [Weight.decimalDigits]): it is left as it
 * is in kilograms, while pounds are always shown as whole numbers.
 */
@Composable
fun weightDecimalCount(decimalCount: Int = NUMBER_OF_DECIMAL_DIGITS): Int {
    val unitSystem = LocalUnitSystem.current

    return decimalCount.coerceAtMost(Weight.decimalDigits(unitSystem))
}

/**
 * Returns the text an editable weight field shows for this [Weight] in [unitSystem]: the value
 * rounded to the precision of [unitSystem] (refer to [Weight.decimalDigits]), with '.' as decimal
 * separator and without trailing zeros, so that [Formatter.normalizeNumericString] and
 * [Formatter.parseDoubleFromString] read it back as it is.
 *
 * ```
 * Weight.pounds(150.0).toInputText(UnitSystem.IMPERIAL)   // "150", not "149.99"
 * Weight.pounds(137.5).toInputText(UnitSystem.IMPERIAL)   // "138"
 * Weight.kilograms(61.25).toInputText(UnitSystem.METRIC)  // "61.25"
 * Weight.kilograms(60.5).toInputText(UnitSystem.METRIC)   // "60.5"
 * Weight.kilograms(60.0).toInputText(UnitSystem.METRIC)   // "60"
 * Weight.zero().toInputText(UnitSystem.METRIC)            // "0"
 * ```
 */
fun Weight.toInputText(unitSystem: UnitSystem): String {
    val numberOfDecimalDigits = Weight.decimalDigits(unitSystem)

    val text = BigDecimal.valueOf(roundedValue(unitSystem, numberOfDecimalDigits))
        .setScale(numberOfDecimalDigits, RoundingMode.HALF_UP)
        .toPlainString()

    // With a scale, there is always a separator, which stops the trimming of the integer part
    return if (numberOfDecimalDigits > 0) text.trimEnd('0').trimEnd('.') else text
}

/**
 * Sanitizes what the user typed in an editable weight field of [unitSystem]:
 * - [UnitSystem.METRIC]: as [Formatter.normalizeNumericString] does, i.e. up to 3 integer digits
 * and 2 decimal digits.
 * - [UnitSystem.IMPERIAL]: pounds are whole numbers, so only digits are kept, up to 3, and a
 * decimal separator is dropped together with whatever follows it ("135.5" gives "135"). Leading
 * zeros are dropped as well, so typing after the "0" of an empty load gives "5" rather than "05".
 */
fun normalizeWeightInput(text: String, unitSystem: UnitSystem): String {
    return when (unitSystem) {
        UnitSystem.METRIC -> Formatter.normalizeNumericString(text)
        UnitSystem.IMPERIAL -> {
            val digits = text
                .takeWhile { it != '.' && it != ',' }
                .filter { it.isDigit() }

            if (digits.isEmpty()) "" else digits.trimStart('0').take(3).ifEmpty { "0" }
        }
    }
}

/**
 * Parses what the user typed in an editable weight field of [unitSystem], after sanitizing it with
 * [normalizeWeightInput], so pounds are always whole numbers.
 *
 * @return The value expressed in the unit of [unitSystem], ready for [Weight.auto], or null when
 * there is nothing to parse, e.g. an empty field.
 */
fun parseWeightInput(text: String, unitSystem: UnitSystem): Double? {
    return Formatter.parseDoubleFromString(normalizeWeightInput(text, unitSystem))
}

/**
 * Whether [text], the content of an editable weight field, already shows this [Weight] in
 * [unitSystem]. Both are compared at the precision of [unitSystem] (refer to
 * [Weight.decimalDigits]), and an empty or incomplete text (e.g. "0.") counts as zero.
 *
 * A weight field writes the parsed value back on every keystroke, so it must replace its text
 * only when this returns false, i.e. when the weight has been changed from somewhere else (the
 * steppers, the scroll wheel, the previous performance, a change of unit system...). Rebuilding
 * the text from the weight on every change would overwrite what the user is typing.
 */
fun Weight.isRepresentedBy(text: String, unitSystem: UnitSystem): Boolean {
    val typed = text.trim().replace(',', '.').toDoubleOrNull() ?: 0.0
    val numberOfDecimalDigits = Weight.decimalDigits(unitSystem)

    return Weight.roundHalfUp(typed, numberOfDecimalDigits) ==
            roundedValue(unitSystem, numberOfDecimalDigits)
}

/**
 * Returns the keyboard to show for an editable weight field in [unitSystem]: digits only in
 * pounds, which are whole numbers, and digits with a decimal separator in kilograms.
 */
fun weightKeyboardType(unitSystem: UnitSystem): KeyboardType {
    return when (unitSystem) {
        UnitSystem.METRIC -> KeyboardType.Decimal
        UnitSystem.IMPERIAL -> KeyboardType.Number
    }
}

/**
 * Returns the correct string resource unit suffix based on current [UnitSystem]
 */
fun autoUnitSuffix(unitSystem: UnitSystem): Int {
    return when (unitSystem) {
        UnitSystem.METRIC -> R.string.kg
        UnitSystem.IMPERIAL -> R.string.lb
    }
}

/**
 * Returns the correct unit suffix based on current [UnitSystem]
 */
@Composable
fun autoUnitSuffix(): String {
    val unitSystem = LocalUnitSystem.current

    val id = rememberSaveable(unitSystem) { autoUnitSuffix(unitSystem) }

    return stringResource(id)
}
