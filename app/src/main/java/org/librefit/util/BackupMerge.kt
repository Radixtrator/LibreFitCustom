/*
 * SPDX-License-Identifier: GPL-3.0-or-later
 * Copyright (c) 2026. The LibreFit Contributors
 *
 * LibreFit is subject to additional terms covering author attribution and trademark usage;
 * see the ADDITIONAL_TERMS.md and TRADEMARK_POLICY.md files in the project root.
 */

package org.librefit.util

import org.librefit.db.entity.ExerciseDC
import org.librefit.db.entity.Measurement
import org.librefit.db.entity.Workout
import org.librefit.enums.WorkoutState
import org.librefit.models.BackupFile
import org.librefit.models.BackupMeasurement
import org.librefit.models.BackupWorkout

/**
 * What importing a [BackupFile] adds, refer to [planBackupImport].
 *
 * @property skippedExercises How many exercises of the imported workouts were left out, because
 * their exercise is neither in this app nor in the backup.
 */
data class BackupImportPlan(
    val workouts: List<BackupWorkout>,
    val measurements: List<BackupMeasurement>,
    val customExercises: List<ExerciseDC>,
    val skippedExercises: Int,
) {
    val completedWorkoutCount get() = workouts.count { it.workout.state == WorkoutState.COMPLETED }
    val routineCount get() = workouts.count { it.workout.state == WorkoutState.ROUTINE }
}

/**
 * Works out what importing [backup] adds to what is already in the app, so importing never
 * replaces or deletes anything and importing the same backup twice adds nothing the second time:
 *
 * - A routine is already there when a routine with the same [Workout.routineId] is, e.g. after
 *   restoring a backup on the phone it was made on. A completed workout is already there when one
 *   of the same routine was completed and created at the same moments.
 * - A measurement is already there when one was taken at the same moment.
 * - A custom exercise already there is kept as it is, with the edits made since the backup.
 * - An exercise of a workout is left out when its exercise is neither in the app nor among the
 *   custom exercises of the backup, e.g. one removed from the dataset since, as the workout could
 *   not be saved with it.
 *
 * Running workouts are never imported.
 */
fun planBackupImport(
    backup: BackupFile,
    existingWorkouts: List<Workout>,
    existingMeasurements: List<Measurement>,
    existingExerciseIds: Set<String>,
): BackupImportPlan {
    val workoutKeys = existingWorkouts.mapTo(mutableSetOf()) { it.importKey() }
    val workouts = backup.workouts
        .filter { it.workout.state != WorkoutState.RUNNING }
        // add() is false for a workout already there, or one already taken from this backup
        .filter { workoutKeys.add(it.workout.importKey()) }

    val customExercises = backup.customExercises
        .filter { it.id !in existingExerciseIds }
        .distinctBy { it.id }
    val availableExerciseIds = existingExerciseIds + customExercises.map { it.id }

    var skippedExercises = 0
    val restorableWorkouts = workouts.map { backupWorkout ->
        val (kept, skipped) = backupWorkout.exercises
            .partition { it.exercise.idExerciseDC in availableExerciseIds }
        skippedExercises += skipped.size
        backupWorkout.copy(exercises = kept)
    }

    val measurementDates = existingMeasurements.mapTo(mutableSetOf()) { it.date }
    val measurements = backup.measurements.filter { measurementDates.add(it.date) }

    return BackupImportPlan(
        workouts = restorableWorkouts,
        measurements = measurements,
        customExercises = customExercises,
        skippedExercises = skippedExercises
    )
}

private fun Workout.importKey(): Any = when (state) {
    WorkoutState.ROUTINE -> Pair(state, routineId)
    else -> listOf(state, routineId, created, completed)
}
