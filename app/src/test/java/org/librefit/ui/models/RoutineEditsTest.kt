/*
 * SPDX-License-Identifier: GPL-3.0-or-later
 * Copyright (c) 2026. The LibreFit Contributors
 *
 * LibreFit is subject to additional terms covering author attribution and trademark usage;
 * see the ADDITIONAL_TERMS.md and TRADEMARK_POLICY.md files in the project root.
 */

package org.librefit.ui.models

import kotlinx.collections.immutable.toImmutableList
import org.librefit.enums.SetMode
import org.librefit.models.Weight
import kotlin.test.Test
import kotlin.test.assertEquals

class RoutineEditsTest {

    private fun set(id: Long, load: Double = 50.0, reps: Int = 5, completed: Boolean = false) =
        UiSet(id = id, load = Weight.kilograms(load), reps = reps, completed = completed)

    private fun exercise(id: Long, dc: String, vararg sets: UiSet, restTime: Int = 90) =
        UiExerciseWithSets(
            exercise = UiExercise(id = id, idExerciseDC = dc, restTime = restTime),
            sets = sets.toList().toImmutableList(),
            exerciseDC = UiExerciseDC(id = dc)
        )

    private fun List<UiExerciseWithSets>.ids() = map { it.exercise.id }

    private val routine = listOf(
        exercise(1, "squat", set(11), set(12)),
        exercise(2, "bench", set(21), set(22))
    )

    @Test
    fun `nothing changes when the routine was saved as it was`() {
        val workout = routine.map { it.copy(sets = it.sets.map { s -> s.copy(load = Weight.kilograms(60.0)) }.toImmutableList()) }

        assertEquals(workout, workout.withRoutineEdits(before = routine, after = routine))
    }

    @Test
    fun `exercise settings follow the routine`() {
        val after = listOf(
            routine[0].copy(exercise = routine[0].exercise.copy(restTime = 180, setMode = SetMode.BODYWEIGHT)),
            routine[1]
        )

        val result = routine.withRoutineEdits(before = routine, after = after)

        assertEquals(180, result[0].exercise.restTime)
        assertEquals(SetMode.BODYWEIGHT, result[0].exercise.setMode)
        assertEquals(90, result[1].exercise.restTime)
    }

    @Test
    fun `only the edited values of a set are taken, and completed sets are kept`() {
        // The workout started from the previous session, so its loads differ from the routine
        val workout = listOf(
            exercise(1, "squat", set(11, load = 100.0, completed = true), set(12, load = 100.0)),
            routine[1]
        )
        val after = listOf(
            exercise(1, "squat", set(11, reps = 3), set(12, reps = 3)),
            routine[1]
        )

        val result = workout.withRoutineEdits(before = routine, after = after)

        assertEquals(5, result[0].sets[0].reps)
        assertEquals(3, result[0].sets[1].reps)
        assertEquals(Weight.kilograms(100.0), result[0].sets[1].load)
    }

    @Test
    fun `sets added to the routine are added, removed ones go unless completed`() {
        val workout = listOf(
            exercise(1, "squat", set(11, completed = true), set(12)),
            exercise(2, "bench", set(21, completed = true), set(22))
        )
        val after = listOf(
            exercise(1, "squat", set(11), set(12), set(13)),
            exercise(2, "bench")
        )

        val result = workout.withRoutineEdits(before = routine, after = after)

        assertEquals(listOf(11L, 12L, 13L), result[0].sets.map { it.id })
        assertEquals(listOf(21L), result[1].sets.map { it.id })
    }

    @Test
    fun `exercises added to the routine are inserted after their predecessor`() {
        val after = listOf(routine[0], exercise(3, "row", set(31)), routine[1])

        val result = routine.withRoutineEdits(before = routine, after = after)

        assertEquals(listOf(1L, 3L, 2L), result.ids())
        assertEquals(listOf(0, 1, 2), result.map { it.exercise.position })
    }

    @Test
    fun `an exercise removed from the routine stays once trained`() {
        val workout = listOf(
            routine[0],
            exercise(2, "bench", set(21, completed = true), set(22))
        )
        val after = listOf(routine[0])

        assertEquals(listOf(1L, 2L), workout.withRoutineEdits(before = routine, after = after).ids())
        assertEquals(listOf(1L), routine.withRoutineEdits(before = routine, after = after).ids())
    }

    @Test
    fun `the workout follows a new order of the routine`() {
        val after = listOf(routine[1], routine[0])

        assertEquals(listOf(2L, 1L), routine.withRoutineEdits(before = routine, after = after).ids())
    }

    @Test
    fun `an exercise removed from the workout is not brought back by an edit`() {
        val workout = listOf(routine[1])
        val after = listOf(
            routine[0].copy(exercise = routine[0].exercise.copy(restTime = 30)),
            routine[1]
        )

        assertEquals(listOf(2L), workout.withRoutineEdits(before = routine, after = after).ids())
    }

    @Test
    fun `a resumed workout is paired by exercise and set position`() {
        // A workout resumed after the app was closed has ids of its own
        val workout = listOf(
            exercise(101, "squat", set(111, completed = true), set(112)),
            exercise(102, "bench", set(121), set(122))
        )
        val after = listOf(
            exercise(1, "squat", set(11), set(12, reps = 8)),
            exercise(2, "bench", set(21), set(22), restTime = 60)
        )

        val result = workout.withRoutineEdits(before = routine, after = after)

        assertEquals(listOf(101L, 102L), result.ids())
        assertEquals(8, result[0].sets[1].reps)
        assertEquals(60, result[1].exercise.restTime)
    }

    @Test
    fun `sets coming from the routine are not completed`() {
        val after = listOf(
            exercise(1, "squat", set(11), set(12), set(13, completed = true)),
            routine[1]
        )

        val result = routine.withRoutineEdits(before = routine, after = after)

        assertEquals(false, result[0].sets[2].completed)
    }

    @Test
    fun `an empty workout stays empty`() {
        assertEquals(
            emptyList(),
            emptyList<UiExerciseWithSets>().withRoutineEdits(before = emptyList(), after = emptyList())
        )
    }
}
