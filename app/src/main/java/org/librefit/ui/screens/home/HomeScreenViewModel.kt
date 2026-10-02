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
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import org.librefit.db.repository.UserPreferencesRepository
import org.librefit.db.repository.WorkoutRepository
import org.librefit.models.RoutineFile
import org.librefit.ui.models.mappers.toEntity
import org.librefit.ui.models.mappers.toUi

class HomeScreenViewModel(
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

    /**
     * Persists the order the user gave to the routines by dragging them in [HomeScreen].
     * [routineIds] lists the ids of every routine from the first to the last one. Nothing is
     * written when the order is the one already shown, e.g. when a routine is dropped where it was
     * picked up.
     */
    fun reorderRoutines(routineIds: List<Long>) {
        if (routineIds == routines.value.map { it.id }) return

        viewModelScope.launch {
            workoutRepository.updateRoutinesOrder(routineIds)
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

}