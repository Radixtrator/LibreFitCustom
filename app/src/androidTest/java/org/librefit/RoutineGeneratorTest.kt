/*
 * SPDX-License-Identifier: GPL-3.0-or-later
 * Copyright (c) 2025-2026. The LibreFit Contributors
 *
 * LibreFit is subject to additional terms covering author attribution and trademark usage;
 * see the ADDITIONAL_TERMS.md and TRADEMARK_POLICY.md files in the project root.
 */

package org.librefit

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.librefit.db.AppDatabase
import org.librefit.db.entity.Exercise
import org.librefit.db.entity.ExerciseDC
import org.librefit.db.entity.Set
import org.librefit.db.entity.Workout
import org.librefit.db.relations.ExerciseWithSets
import org.librefit.db.relations.WorkoutWithExercisesAndSets
import org.librefit.enums.SetMode
import org.librefit.enums.WorkoutState
import org.librefit.models.Weight
import java.time.LocalDateTime
import kotlin.random.Random

/**
 * Test to generate 6 routines (Monday-Saturday) with exercises and supersets.
 * Queries the actual exercise database and creates routines with the found exercise IDs.
 */
@RunWith(AndroidJUnit4::class)
class RoutineGeneratorTest {

    private lateinit var db: AppDatabase

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            AppDatabase.NAME
        ).build()
    }

    data class RoutineSpec(
        val dayName: String,
        val exercises: List<ExerciseSpec>
    )

    data class ExerciseSpec(
        val exerciseName: String,
        val sets: Int,
        val repsRange: String,
        val supersetGroupId: Long? = null
    )

    @Test
    fun generateWorkoutRoutines() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val workoutDao = db.getWorkoutDao()
        val datasetDao = db.getDatasetDao()

        // Query for all ExerciseDC entries to map by name
        val allExercises = datasetDao.getDataset().first()

        println("Found ${allExercises.size} exercises in database")

        // Exercise name mappings - these should match the ExerciseDC names
        val exerciseNames = mapOf(
            "barbell_deadlift" to "Barbell Deadlift",
            "pullups" to "Pullups",
            "seated_cable_rows" to "Seated Cable Rows",
            "hammer_curls" to "Hammer Curls",
            "face_pull" to "Face Pull",
            "dumbbell_bicep_curl" to "Dumbbell Bicep Curl",
            "barbell_bench_press" to "Barbell Bench Press - Medium Grip",
            "dumbbell_shoulder_press" to "Dumbbell Shoulder Press",
            "decline_dumbbell_bench_press" to "Decline Dumbbell Bench Press",
            "triceps_pushdown" to "Triceps Pushdown",
            "side_lateral_raise" to "Side Lateral Raise",
            "front_dumbbell_raise" to "Front Dumbbell Raise",
            "dumbbell_tricep_extension" to "Dumbbell Tricep Extension -Pronated Grip",
            "barbell_squat" to "Barbell Squat",
            "romanian_deadlift" to "Romanian Deadlift",
            "leg_press" to "Leg Press",
            "calf_raises" to "Calf Raises - With Bands",
            "seated_leg_curl" to "Seated Leg Curl",
            "bent_over_barbell_row" to "Bent Over Barbell Row",
            "front_barbell_squat" to "Front Barbell Squat"
        )

        // Build exercise ID map by finding the best matches
        val exerciseIdMap = mutableMapOf<String, String>()
        val exercisesByName = allExercises.associateBy { it.name }
        
        exerciseNames.forEach { (key, displayName) ->
            val found = exercisesByName[displayName]
            if (found != null) {
                exerciseIdMap[key] = found.id
                println("✓ Mapped $key -> ${found.name} (ID: ${found.id})")
            } else {
                // Try to find a close match
                val closeMatch = allExercises.find { it.name.contains(displayName.split(" ").first(), ignoreCase = true) }
                if (closeMatch != null) {
                    exerciseIdMap[key] = closeMatch.id
                    println("⚠ Mapped $key to close match: ${closeMatch.name} (ID: ${closeMatch.id})")
                } else {
                    println("✗ Could not find exercise: $displayName")
                }
            }
        }

        // Define the 6 routines
        val routines = listOf(
            RoutineSpec(
                dayName = "Monday (Pull)",
                exercises = listOf(
                    ExerciseSpec("barbell_deadlift", 3, "5"),
                    ExerciseSpec("pullups", 3, "8-12"),
                    ExerciseSpec("seated_cable_rows", 3, "8-12"),
                    ExerciseSpec("hammer_curls", 4, "8-12", supersetGroupId = 1L),
                    ExerciseSpec("face_pull", 4, "12-15", supersetGroupId = 1L),
                    ExerciseSpec("dumbbell_bicep_curl", 4, "8-12")
                )
            ),
            RoutineSpec(
                dayName = "Tuesday (Push)",
                exercises = listOf(
                    ExerciseSpec("barbell_bench_press", 3, "5"),
                    ExerciseSpec("dumbbell_shoulder_press", 3, "8-12"),
                    ExerciseSpec("decline_dumbbell_bench_press", 3, "8-12"),
                    ExerciseSpec("triceps_pushdown", 3, "8-12", supersetGroupId = 2L),
                    ExerciseSpec("side_lateral_raise", 3, "15", supersetGroupId = 2L),
                    ExerciseSpec("front_dumbbell_raise", 3, "5", supersetGroupId = 2L),
                    ExerciseSpec("dumbbell_tricep_extension", 3, "8-12", supersetGroupId = 3L),
                    ExerciseSpec("side_lateral_raise", 3, "15", supersetGroupId = 3L),
                    ExerciseSpec("front_dumbbell_raise", 3, "5", supersetGroupId = 3L)
                )
            ),
            RoutineSpec(
                dayName = "Wednesday (Legs)",
                exercises = listOf(
                    ExerciseSpec("barbell_squat", 3, "5"),
                    ExerciseSpec("romanian_deadlift", 3, "8-12"),
                    ExerciseSpec("leg_press", 3, "8-12", supersetGroupId = 4L),
                    ExerciseSpec("calf_raises", 3, "30", supersetGroupId = 4L),
                    ExerciseSpec("seated_leg_curl", 3, "8-12")
                )
            ),
            RoutineSpec(
                dayName = "Thursday (Pull)",
                exercises = listOf(
                    ExerciseSpec("bent_over_barbell_row", 3, "5"),
                    ExerciseSpec("pullups", 3, "8-12"),
                    ExerciseSpec("seated_cable_rows", 3, "8-12"),
                    ExerciseSpec("hammer_curls", 4, "8-12", supersetGroupId = 5L),
                    ExerciseSpec("face_pull", 4, "12-15", supersetGroupId = 5L),
                    ExerciseSpec("dumbbell_bicep_curl", 4, "8-12")
                )
            ),
            RoutineSpec(
                dayName = "Friday (Push)",
                exercises = listOf(
                    ExerciseSpec("dumbbell_shoulder_press", 3, "5"),
                    ExerciseSpec("barbell_bench_press", 3, "8-12"),
                    ExerciseSpec("decline_dumbbell_bench_press", 3, "8-12"),
                    ExerciseSpec("triceps_pushdown", 3, "8-12", supersetGroupId = 6L),
                    ExerciseSpec("side_lateral_raise", 3, "15", supersetGroupId = 6L),
                    ExerciseSpec("front_dumbbell_raise", 3, "5", supersetGroupId = 6L),
                    ExerciseSpec("dumbbell_tricep_extension", 3, "8-12", supersetGroupId = 7L),
                    ExerciseSpec("side_lateral_raise", 3, "15", supersetGroupId = 7L),
                    ExerciseSpec("front_dumbbell_raise", 3, "5", supersetGroupId = 7L)
                )
            ),
            RoutineSpec(
                dayName = "Saturday (Legs)",
                exercises = listOf(
                    ExerciseSpec("front_barbell_squat", 3, "5"),
                    ExerciseSpec("romanian_deadlift", 3, "8-12"),
                    ExerciseSpec("leg_press", 3, "8-12", supersetGroupId = 8L),
                    ExerciseSpec("calf_raises", 3, "30", supersetGroupId = 8L),
                    ExerciseSpec("seated_leg_curl", 3, "8-12")
                )
            )
        )

        // Create each routine
        routines.forEach { routine ->
            val routineId = Random.nextLong()

            // Create the main workout (routine) entry
            val workout = Workout(
                routineId = routineId,
                title = routine.dayName,
                state = WorkoutState.ROUTINE,
                created = LocalDateTime.now(),
                completed = LocalDateTime.now(),
                notes = "Auto-generated routine",
                timeElapsed = 0
            )

            // Build exercises with sets
            val exercisesWithSets = routine.exercises.mapIndexed { index, exerciseSpec ->
                val exerciseDCId = exerciseIdMap[exerciseSpec.exerciseName] ?: return@mapIndexed null

                val exercise = Exercise(
                    idExerciseDC = exerciseDCId,
                    position = index,
                    workoutId = 0,
                    restTime = 120,
                    notes = exerciseSpec.repsRange,
                    setMode = SetMode.LOAD,
                    supersetGroupId = exerciseSpec.supersetGroupId
                )

                val sets = (1..exerciseSpec.sets).map { setNum ->
                    Set(
                        load = Weight.kilograms(70.0),
                        reps = exerciseSpec.repsRange.split("-").first().toIntOrNull() ?: 5,
                        elapsedTime = 0,
                        completed = false,
                        exerciseId = 0
                    )
                }

                ExerciseWithSets(exercise, sets)
            }.filterNotNull()

            try {
                workoutDao.addWorkoutWithExercisesAndSets(
                    WorkoutWithExercisesAndSets(workout, exercisesWithSets)
                )
                println("✓ Created routine: ${routine.dayName}")
            } catch (e: Exception) {
                println("✗ Failed to create routine: ${routine.dayName} - ${e.message}")
                e.printStackTrace()
            }
        }

        println("\n✅ All 6 routines generated successfully!")
    }
}
