/*
 * SPDX-License-Identifier: GPL-3.0-or-later
 * Copyright (c) 2026. The LibreFit Contributors
 *
 * LibreFit is subject to additional terms covering author attribution and trademark usage;
 * see the ADDITIONAL_TERMS.md and TRADEMARK_POLICY.md files in the project root.
 */

package org.librefit.util

import kotlinx.serialization.SerializationException
import org.librefit.db.entity.Exercise
import org.librefit.db.entity.ExerciseDC
import org.librefit.db.entity.Measurement
import org.librefit.db.entity.Set
import org.librefit.db.entity.Workout
import org.librefit.enums.WorkoutState
import org.librefit.helpers.WorkoutBackup
import org.librefit.models.BackupExercise
import org.librefit.models.BackupFile
import org.librefit.models.BackupMeasurement
import org.librefit.models.BackupWorkout
import org.librefit.models.RoutineFile
import org.librefit.models.Weight
import java.time.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class BackupMergeTest {

    private val monday = LocalDateTime.of(2026, 11, 2, 18, 30)
    private val wednesday = LocalDateTime.of(2026, 11, 4, 18, 30)

    private fun completed(routineId: Long, at: LocalDateTime) = Workout(
        id = 7,
        routineId = routineId,
        state = WorkoutState.COMPLETED,
        created = at,
        completed = at.plusHours(1)
    )

    private fun routine(routineId: Long) = Workout(id = 3, routineId = routineId, state = WorkoutState.ROUTINE)

    private fun backupWorkout(workout: Workout, vararg exerciseIds: String) = BackupWorkout(
        workout = workout,
        exercises = exerciseIds.map { id ->
            BackupExercise(
                exercise = Exercise(idExerciseDC = id),
                sets = listOf(Set(load = Weight.kilograms(80.0), reps = 8, completed = true))
            )
        }
    )

    private fun backup(
        workouts: List<BackupWorkout> = emptyList(),
        measurements: List<BackupMeasurement> = emptyList(),
        customExercises: List<ExerciseDC> = emptyList(),
    ) = BackupFile(
        format = WorkoutBackup.FORMAT,
        workouts = workouts,
        measurements = measurements,
        customExercises = customExercises
    )

    @Test
    fun `everything is imported into an empty app`() {
        val plan = planBackupImport(
            backup = backup(
                workouts = listOf(
                    backupWorkout(routine(1), "Bench_Press"),
                    backupWorkout(completed(1, monday), "Bench_Press"),
                    backupWorkout(completed(1, wednesday), "Bench_Press")
                ),
                measurements = listOf(BackupMeasurement(date = monday))
            ),
            existingWorkouts = emptyList(),
            existingMeasurements = emptyList(),
            existingExerciseIds = setOf("Bench_Press")
        )

        assertEquals(2, plan.completedWorkoutCount)
        assertEquals(1, plan.routineCount)
        assertEquals(1, plan.measurements.size)
        assertEquals(0, plan.skippedExercises)
    }

    @Test
    fun `what is already in the app is skipped, so a second import adds nothing`() {
        val plan = planBackupImport(
            backup = backup(
                workouts = listOf(
                    backupWorkout(routine(1)),
                    backupWorkout(completed(1, monday)),
                    backupWorkout(completed(1, wednesday))
                ),
                measurements = listOf(
                    BackupMeasurement(date = monday),
                    BackupMeasurement(date = wednesday)
                )
            ),
            // The ids differ on the other phone, only the content is compared
            existingWorkouts = listOf(
                routine(1).copy(id = 40, title = "Renamed since"),
                completed(1, monday).copy(id = 41)
            ),
            existingMeasurements = listOf(Measurement(id = 9, date = monday)),
            existingExerciseIds = emptySet()
        )

        assertEquals(listOf(wednesday), plan.workouts.map { it.workout.created })
        assertEquals(listOf(wednesday), plan.measurements.map { it.date })
    }

    @Test
    fun `a completed workout of the same routine at another moment is a different workout`() {
        val plan = planBackupImport(
            backup = backup(workouts = listOf(backupWorkout(completed(1, wednesday)))),
            existingWorkouts = listOf(completed(1, monday), completed(2, wednesday)),
            existingMeasurements = emptyList(),
            existingExerciseIds = emptySet()
        )

        assertEquals(1, plan.completedWorkoutCount)
    }

    @Test
    fun `running workouts are never imported`() {
        val running = completed(1, monday).copy(state = WorkoutState.RUNNING)

        val plan = planBackupImport(
            backup = backup(workouts = listOf(backupWorkout(running))),
            existingWorkouts = emptyList(),
            existingMeasurements = emptyList(),
            existingExerciseIds = emptySet()
        )

        assertTrue(plan.workouts.isEmpty())
    }

    @Test
    fun `custom exercises are added unless already there, and make their exercises restorable`() {
        val custom = ExerciseDC(id = "my_curl", name = "My curl", isCustomExercise = true)
        val edited = ExerciseDC(id = "my_row", name = "My row", isCustomExercise = true)

        val plan = planBackupImport(
            backup = backup(
                workouts = listOf(backupWorkout(completed(1, monday), "my_curl", "my_row")),
                customExercises = listOf(custom, edited)
            ),
            existingWorkouts = emptyList(),
            existingMeasurements = emptyList(),
            existingExerciseIds = setOf("my_row")
        )

        // The one already in the app keeps the edits made since the backup
        assertEquals(listOf(custom), plan.customExercises)
        assertEquals(2, plan.workouts.single().exercises.size)
    }

    @Test
    fun `exercises that no longer exist are left out and counted`() {
        val plan = planBackupImport(
            backup = backup(
                workouts = listOf(backupWorkout(completed(1, monday), "Bench_Press", "Removed_Lift"))
            ),
            existingWorkouts = emptyList(),
            existingMeasurements = emptyList(),
            existingExerciseIds = setOf("Bench_Press")
        )

        assertEquals(
            listOf("Bench_Press"),
            plan.workouts.single().exercises.map { it.exercise.idExerciseDC }
        )
        assertEquals(1, plan.skippedExercises)
    }

    @Test
    fun `a backup reads back as it was written`() {
        val original = backup(
            workouts = listOf(
                backupWorkout(routine(1), "Bench_Press"),
                backupWorkout(completed(1, monday), "Bench_Press")
            ),
            measurements = listOf(
                BackupMeasurement(bodyWeight = Weight.kilograms(81.5), bodyFatPercentage = 18, date = monday)
            ),
            customExercises = listOf(ExerciseDC(id = "my_curl", name = "My curl", isCustomExercise = true))
        )

        val text = WorkoutBackup.json.encodeToString(BackupFile.serializer(), original)

        assertEquals(original, WorkoutBackup.json.decodeFromString(BackupFile.serializer(), text))
    }

    @Test
    fun `an exported routine is not taken for a backup`() {
        val routineFile = WorkoutBackup.json.encodeToString(
            RoutineFile.serializer(),
            RoutineFile(workout = routine(1))
        )

        assertFailsWith<SerializationException> {
            WorkoutBackup.json.decodeFromString(BackupFile.serializer(), routineFile)
        }
    }
}
