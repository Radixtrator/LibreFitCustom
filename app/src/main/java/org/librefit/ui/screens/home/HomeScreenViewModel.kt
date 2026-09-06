/*
 * SPDX-License-Identifier: GPL-3.0-or-later
 * Copyright (c) 2024-2026. The LibreFit Contributors
 *
 * LibreFit is subject to additional terms covering author attribution and trademark usage;
 * see the ADDITIONAL_TERMS.md and TRADEMARK_POLICY.md files in the project root.
 */

package org.librefit.ui.screens.home

import android.content.ContentResolver
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import org.librefit.db.relations.WorkoutWithExercisesAndSets
import org.librefit.db.repository.UserPreferencesRepository
import org.librefit.db.repository.WorkoutRepository
import org.librefit.enums.WorkoutState
import org.librefit.models.RoutineFile
import org.librefit.ui.models.mappers.toEntity
import org.librefit.ui.models.mappers.toUi
import java.time.LocalDateTime
import javax.inject.Inject
import kotlin.random.Random

@HiltViewModel
class HomeScreenViewModel @Inject constructor(
    private val userPreferences: UserPreferencesRepository,
    private val workoutRepository: WorkoutRepository
) : ViewModel() {
    val requestPermissionNextTime: StateFlow<Boolean> = userPreferences.requestPermissionsNextTime

    val routines = workoutRepository.routines
        .map { list -> list.map { it.toUi() } }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    val runningWorkout = workoutRepository.runningWorkoutsWithExercisesAndSets
        .map { list -> list.firstOrNull()?.workout?.toUi() }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = null
        )

    val showKeepAndroidOpen = userPreferences.showKeepAndroidOpen

    fun saveKeepOpenAndroidCheckbox(showAgain : Boolean) {
        viewModelScope.launch {
            userPreferences.saveShowKeepAndroidOpen(!showAgain)
        }
    }


    fun deleteRunningWorkout() {
        viewModelScope.launch {
            runningWorkout.value?.let {
                workoutRepository.deleteWorkout(it.toEntity())
            }
        }
    }

    fun exportRoutine(contentResolver: ContentResolver, uri: Uri, routineId: Long) {
        viewModelScope.launch {
            val routine = workoutRepository.getWorkoutWithExercisesAndSets(routineId)
            val payload = RoutineFile(
                workout = routine.workout.toEntity(),
                exercisesWithSets = routine.exercisesWithSets.map { it.toEntity() }.toList()
            )
            val json = Json { prettyPrint = true }
            contentResolver.openOutputStream(uri)?.bufferedWriter()?.use { writer ->
                writer.write(json.encodeToString(RoutineFile.serializer(), payload))
            }
        }
    }

    fun importRoutine(contentResolver: ContentResolver, uri: Uri) {
        viewModelScope.launch {
            val text = contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                ?: return@launch

            val payload = Json { ignoreUnknownKeys = true }
                .decodeFromString(RoutineFile.serializer(), text)

            val importedWorkout = WorkoutWithExercisesAndSets(
                workout = payload.workout.copy(
                    id = 0,
                    routineId = Random.nextLong(),
                    state = WorkoutState.ROUTINE,
                    created = LocalDateTime.now(),
                    completed = LocalDateTime.now()
                ),
                exercisesWithSets = payload.exercisesWithSets.map { exerciseWithSets ->
                    exerciseWithSets.copy(
                        exercise = exerciseWithSets.exercise.copy(id = 0, workoutId = 0),
                        sets = exerciseWithSets.sets.map { set -> set.copy(id = 0, exerciseId = 0) }
                    )
                }
            )

            workoutRepository.addWorkoutWithExercisesAndSets(importedWorkout)
        }
    }

}