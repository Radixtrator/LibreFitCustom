/*
 * SPDX-License-Identifier: GPL-3.0-or-later
 * Copyright (c) 2026. The LibreFit Contributors
 *
 * LibreFit is subject to additional terms covering author attribution and trademark usage;
 * see the ADDITIONAL_TERMS.md and TRADEMARK_POLICY.md files in the project root.
 */

package org.librefit.util

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.librefit.db.entity.Set
import org.librefit.models.Weight

class WeightProgressionTest {

    private val increment = Weight.kilograms(2.5)

    private fun set(completed: Boolean, failed: Boolean = false) =
        Set(load = Weight.kilograms(80.0), reps = 8, completed = completed, failed = failed)

    private fun amrapSet(performedReps: Int, targetReps: Int = 8) = Set(
        load = Weight.kilograms(80.0),
        reps = performedReps,
        completed = true,
        isAmrap = true,
        targetReps = targetReps
    )

    @Test
    fun `session with every set completed is successful`() {
        val sets = listOf(set(completed = true), set(completed = true))

        assertThat(WeightProgression.isSessionSuccessful(sets)).isTrue()
    }

    @Test
    fun `session with an unchecked set is not successful`() {
        val sets = listOf(set(completed = true), set(completed = false))

        assertThat(WeightProgression.isSessionSuccessful(sets)).isFalse()
    }

    @Test
    fun `session with a set short of the planned reps is not successful`() {
        val sets = listOf(set(completed = true), set(completed = true, failed = true))

        assertThat(WeightProgression.isSessionSuccessful(sets)).isFalse()
    }

    @Test
    fun `session without sets is not successful`() {
        assertThat(WeightProgression.isSessionSuccessful(emptyList())).isFalse()
    }

    @Test
    fun `load goes up after a successful session`() {
        val suggested = WeightProgression.suggestedLoad(
            previousLoad = Weight.kilograms(80.0),
            increment = increment,
            sessionSuccessful = true
        )

        assertThat(suggested).isEqualTo(Weight.kilograms(82.5))
    }

    @Test
    fun `load stays the same after a session that was not successful`() {
        val suggested = WeightProgression.suggestedLoad(
            previousLoad = Weight.kilograms(80.0),
            increment = increment,
            sessionSuccessful = false
        )

        assertThat(suggested).isNull()
    }

    @Test
    fun `a zero increment disables the progression`() {
        val suggested = WeightProgression.suggestedLoad(
            previousLoad = Weight.kilograms(80.0),
            increment = Weight.zero(),
            sessionSuccessful = true
        )

        assertThat(suggested).isNull()
    }

    @Test
    fun `a session without amrap sets keeps the plain increment`() {
        val sets = listOf(set(completed = true), set(completed = true))

        assertThat(WeightProgression.incrementMultiplier(sets)).isEqualTo(1)
    }

    @Test
    fun `an amrap set stopped at the planned reps keeps the plain increment`() {
        val sets = listOf(set(completed = true), amrapSet(performedReps = 8))

        assertThat(WeightProgression.incrementMultiplier(sets)).isEqualTo(1)
    }

    @Test
    fun `doubling the planned reps of an amrap set doubles the increment`() {
        val sets = listOf(set(completed = true), amrapSet(performedReps = 16))

        assertThat(WeightProgression.incrementMultiplier(sets)).isEqualTo(2)
    }

    @Test
    fun `beating the plan without reaching the double keeps the plain increment`() {
        val sets = listOf(set(completed = true), amrapSet(performedReps = 15))

        assertThat(WeightProgression.incrementMultiplier(sets)).isEqualTo(1)
    }

    @Test
    fun `tripling the planned reps of an amrap set triples the increment`() {
        val sets = listOf(amrapSet(performedReps = 24))

        assertThat(WeightProgression.incrementMultiplier(sets)).isEqualTo(3)
    }

    @Test
    fun `an implausible amount of reps does not multiply the increment beyond the cap`() {
        val sets = listOf(amrapSet(performedReps = 800))

        assertThat(WeightProgression.incrementMultiplier(sets))
            .isEqualTo(WeightProgression.MAX_INCREMENT_MULTIPLIER)
    }

    @Test
    fun `the smallest multiplier wins when a session holds several amrap sets`() {
        val sets = listOf(amrapSet(performedReps = 24), amrapSet(performedReps = 16))

        assertThat(WeightProgression.incrementMultiplier(sets)).isEqualTo(2)
    }

    @Test
    fun `an amrap set without a planned amount keeps the plain increment`() {
        val sets = listOf(amrapSet(performedReps = 16, targetReps = 0))

        assertThat(WeightProgression.incrementMultiplier(sets)).isEqualTo(1)
    }

    @Test
    fun `a set is only an amrap when flagged as such`() {
        val sets = listOf(amrapSet(performedReps = 16).copy(isAmrap = false))

        assertThat(WeightProgression.incrementMultiplier(sets)).isEqualTo(1)
    }

    @Test
    fun `an amrap set coming short of the plan is not a successful session`() {
        val sets = listOf(amrapSet(performedReps = 6).copy(failed = true))

        assertThat(WeightProgression.isSessionSuccessful(sets)).isFalse()
    }

    @Test
    fun `the load goes up by the multiplied increment`() {
        val suggested = WeightProgression.suggestedLoad(
            previousLoad = Weight.kilograms(80.0),
            increment = increment,
            sessionSuccessful = true,
            incrementMultiplier = 2
        )

        assertThat(suggested).isEqualTo(Weight.kilograms(85.0))
    }

    @Test
    fun `a multiplier does not make the load go up after a session that was not successful`() {
        val suggested = WeightProgression.suggestedLoad(
            previousLoad = Weight.kilograms(80.0),
            increment = increment,
            sessionSuccessful = false,
            incrementMultiplier = 2
        )

        assertThat(suggested).isNull()
    }
}
