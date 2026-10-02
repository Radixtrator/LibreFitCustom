/*
 * SPDX-License-Identifier: GPL-3.0-or-later
 * Copyright (c) 2026. The LibreFit Contributors
 *
 * LibreFit is subject to additional terms covering author attribution and trademark usage;
 * see the ADDITIONAL_TERMS.md and TRADEMARK_POLICY.md files in the project root.
 */

package org.librefit.ui.models

import kotlinx.collections.immutable.toImmutableList
import kotlin.test.Test
import kotlin.test.assertEquals

class SupersetRestTest {

    private fun exercise(
        id: Long,
        restTime: Int,
        groupId: Long?,
        vararg completed: Boolean
    ) = UiExerciseWithSets(
        exercise = UiExercise(id = id, restTime = restTime, supersetGroupId = groupId),
        sets = completed.mapIndexed { i, done -> UiSet(id = id * 10 + i, completed = done) }
            .toImmutableList()
    )

    @Test
    fun `an exercise outside of a superset rests for its own time`() {
        val exercises = listOf(exercise(1, 90, null, true, false))

        assertEquals(90, exercises.restTimeAfterCompleting(10))
    }

    @Test
    fun `the first exercise of a superset gives no rest`() {
        val exercises = listOf(
            exercise(1, 60, 7, true, false),
            exercise(2, 90, 7, false, false)
        )

        assertEquals(0, exercises.restTimeAfterCompleting(10))
    }

    @Test
    fun `the last exercise of a round rests for the longest time of the superset`() {
        val exercises = listOf(
            exercise(1, 60, 7, true, false),
            exercise(2, 90, 7, true, false),
            exercise(3, 30, 7, true, false)
        )

        assertEquals(90, exercises.restTimeAfterCompleting(30))
    }

    @Test
    fun `rounds are counted by set position`() {
        // Round one is done, but in round two only the first exercise is
        val exercises = listOf(
            exercise(1, 60, 7, true, true),
            exercise(2, 90, 7, true, false)
        )

        assertEquals(0, exercises.restTimeAfterCompleting(11))
    }

    @Test
    fun `an exercise with fewer sets does not hold back the later rounds`() {
        val exercises = listOf(
            exercise(1, 60, 7, true, true, true),
            exercise(2, 90, 7, true, true)
        )

        assertEquals(90, exercises.restTimeAfterCompleting(12))
    }

    @Test
    fun `other supersets do not count`() {
        val exercises = listOf(
            exercise(1, 60, 7, true),
            exercise(2, 90, 7, true),
            exercise(3, 120, 8, false)
        )

        assertEquals(90, exercises.restTimeAfterCompleting(20))
    }
}
