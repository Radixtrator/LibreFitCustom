/*
 * SPDX-License-Identifier: GPL-3.0-or-later
 * Copyright (c) 2026. The LibreFit Contributors
 *
 * LibreFit is subject to additional terms covering author attribution and trademark usage;
 * see the ADDITIONAL_TERMS.md and TRADEMARK_POLICY.md files in the project root.
 */

package org.librefit.models

import kotlinx.serialization.Serializable
import org.librefit.db.entity.Exercise
import org.librefit.db.entity.ExerciseDC
import org.librefit.db.entity.LocalDateTimeSerializer
import org.librefit.db.entity.Measurement
import org.librefit.db.entity.Set
import org.librefit.db.entity.Workout
import java.time.LocalDateTime

/**
 * A backup of everything the user entered in LibreFit: the completed workouts, the routines, the
 * body measurements and the custom exercises. Refer to [org.librefit.helpers.WorkoutBackup].
 *
 * The exercises of the workouts point to their [ExerciseDC] by id only: the exercises of the
 * dataset ship with the app, and the custom ones are in [customExercises].
 *
 * @property format Always [org.librefit.helpers.WorkoutBackup.FORMAT]. It has no default, so a JSON
 * file that is not a backup, e.g. an exported routine, fails to load instead of loading empty.
 * @property version The version of this format, raised when it changes in a way older versions of
 * the app cannot read.
 * @property appVersion The version name of the app that wrote the backup, for reference only.
 * @property created When the backup was written.
 */
@Serializable
data class BackupFile(
    val format: String,
    val version: Int = CURRENT_VERSION,
    val appVersion: String = "",
    @Serializable(with = LocalDateTimeSerializer::class)
    val created: LocalDateTime = LocalDateTime.now(),
    val workouts: List<BackupWorkout> = emptyList(),
    val measurements: List<BackupMeasurement> = emptyList(),
    val customExercises: List<ExerciseDC> = emptyList(),
) {
    companion object {
        const val CURRENT_VERSION = 1

        /** The type backups are written with and picked by when they are imported */
        const val MIME_TYPE = "application/json"
    }
}

/** A completed workout or a routine with its exercises and their sets */
@Serializable
data class BackupWorkout(
    val workout: Workout,
    val exercises: List<BackupExercise> = emptyList(),
)

@Serializable
data class BackupExercise(
    val exercise: Exercise,
    val sets: List<Set> = emptyList(),
)

/** A [Measurement], which is not serializable itself */
@Serializable
data class BackupMeasurement(
    val bodyWeight: Weight = Weight.zero(),
    val bodyFatPercentage: Int = 0,
    val muscleMassPercentage: Int = 0,
    @Serializable(with = LocalDateTimeSerializer::class)
    val date: LocalDateTime = LocalDateTime.now(),
    val notes: String = "",
) {
    fun toEntity() = Measurement(
        bodyWeight = bodyWeight,
        bodyFatPercentage = bodyFatPercentage,
        muscleMassPercentage = muscleMassPercentage,
        date = date,
        notes = notes
    )

    companion object {
        fun from(measurement: Measurement) = BackupMeasurement(
            bodyWeight = measurement.bodyWeight,
            bodyFatPercentage = measurement.bodyFatPercentage,
            muscleMassPercentage = measurement.muscleMassPercentage,
            date = measurement.date,
            notes = measurement.notes
        )
    }
}
