/*
 * SPDX-License-Identifier: GPL-3.0-or-later
 * Copyright (c) 2026. The LibreFit Contributors
 *
 * LibreFit is subject to additional terms covering author attribution and trademark usage;
 * see the ADDITIONAL_TERMS.md and TRADEMARK_POLICY.md files in the project root.
 */

package org.librefit.ui.models

import kotlinx.collections.immutable.toImmutableList
import org.librefit.db.entity.Exercise
import org.librefit.db.entity.Set
import org.librefit.db.relations.ExerciseWithSets
import org.librefit.enums.SetMode
import org.librefit.enums.userPreferences.UnitSystem
import org.librefit.models.Weight
import kotlin.math.roundToInt
import kotlin.test.Test
import kotlin.test.assertEquals

class PreviousSessionPrefillTest {

    private val increment = Weight.pounds(5.0)

    /** The routine plans 3 sets of 5 at 135 lb */
    private fun planned(
        setMode: SetMode = SetMode.LOAD,
        weightIncrement: Weight = increment,
        amrapLast: Boolean = false
    ) = UiExerciseWithSets(
        exercise = UiExercise(setMode = setMode, weightIncrement = weightIncrement),
        sets = List(3) { index ->
            UiSet(
                id = index.toLong(),
                load = Weight.pounds(135.0),
                reps = 5,
                elapsedTime = 30,
                isAmrap = amrapLast && index == 2,
                targetReps = if (amrapLast && index == 2) 5 else 0
            )
        }.toImmutableList()
    )

    private fun previous(vararg sets: Set, setMode: SetMode = SetMode.LOAD) =
        ExerciseWithSets(exercise = Exercise(setMode = setMode), sets = sets.toList())

    private fun done(load: Double, reps: Int, failed: Boolean = false, time: Int = 0) = Set(
        load = Weight.pounds(load),
        reps = reps,
        elapsedTime = time,
        completed = true,
        failed = failed
    )

    private fun amrap(load: Double, reps: Int, targetReps: Int = 5) = Set(
        load = Weight.pounds(load),
        reps = reps,
        completed = true,
        isAmrap = true,
        targetReps = targetReps
    )

    /** Loads are stored in kilograms, so they are compared in pounds to the hundredth */
    private fun UiExerciseWithSets.pounds() =
        sets.map { (it.load.valueIn(UnitSystem.IMPERIAL) * 100).roundToInt() / 100.0 }

    @Test
    fun `a clean session adds the increase to the last loads`() {
        val result = planned().prefilledFrom(
            previous(done(185.0, 5), done(185.0, 5), done(185.0, 6))
        )

        assertEquals(List(3) { 190.0 }, result.pounds())
        assertEquals(listOf(5, 5, 6), result.sets.map { it.reps })
    }

    @Test
    fun `a failed set keeps the last loads and the planned reps of that set`() {
        val result = planned().prefilledFrom(
            previous(done(185.0, 5), done(185.0, 5), done(185.0, 3, failed = true))
        )

        assertEquals(List(3) { 185.0 }, result.pounds())
        assertEquals(listOf(5, 5, 5), result.sets.map { it.reps })
    }

    @Test
    fun `an unchecked set means no increase`() {
        val result = planned().prefilledFrom(
            previous(done(185.0, 5), done(185.0, 5), Set(load = Weight.pounds(185.0), reps = 5))
        )

        assertEquals(
            listOf(185.0, 185.0, 135.0),
            result.pounds()
        )
    }

    @Test
    fun `doubling the reps of an AMRAP set doubles the increase`() {
        val result = planned(amrapLast = true).prefilledFrom(
            previous(done(185.0, 5), done(185.0, 5), amrap(185.0, 10))
        )

        assertEquals(List(3) { 195.0 }, result.pounds())
        // The AMRAP set is planned for its target again, not for what was reached
        assertEquals(listOf(5, 5, 5), result.sets.map { it.reps })
    }

    @Test
    fun `beating an AMRAP set by less than double gives the plain increase`() {
        val result = planned(amrapLast = true).prefilledFrom(
            previous(done(185.0, 5), done(185.0, 5), amrap(185.0, 9))
        )

        assertEquals(List(3) { 190.0 }, result.pounds())
    }

    @Test
    fun `without an increase the last loads are taken as they are`() {
        val result = planned(weightIncrement = Weight.zero()).prefilledFrom(
            previous(done(185.0, 5), done(185.0, 5), done(185.0, 5))
        )

        assertEquals(List(3) { 185.0 }, result.pounds())
    }

    @Test
    fun `sets beyond the previous session keep the plan`() {
        val result = planned().prefilledFrom(previous(done(185.0, 5)))

        assertEquals(
            listOf(190.0, 135.0, 135.0),
            result.pounds()
        )
    }

    @Test
    fun `durations are taken from the previous session`() {
        val result = planned(setMode = SetMode.DURATION).prefilledFrom(
            previous(done(0.0, 0, time = 45), setMode = SetMode.DURATION)
        )

        assertEquals(listOf(45, 30, 30), result.sets.map { it.elapsedTime })
    }

    @Test
    fun `a session trained with another set mode is ignored`() {
        val plan = planned()

        assertEquals(
            plan,
            plan.prefilledFrom(previous(done(0.0, 12), setMode = SetMode.BODYWEIGHT))
        )
    }

    @Test
    fun `only the accepted sets are filled`() {
        val result = planned().prefilledFrom(
            previous(done(185.0, 5), done(185.0, 5), done(185.0, 5))
        ) { it.id == 2L }

        assertEquals(
            listOf(135.0, 135.0, 190.0),
            result.pounds()
        )
    }
}
