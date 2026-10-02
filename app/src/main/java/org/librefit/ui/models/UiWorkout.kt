/*
 * SPDX-License-Identifier: GPL-3.0-or-later
 * Copyright (c) 2025-2026. The LibreFit Contributors
 *
 * LibreFit is subject to additional terms covering author attribution and trademark usage;
 * see the ADDITIONAL_TERMS.md and TRADEMARK_POLICY.md files in the project root.
 */

package org.librefit.ui.models

import androidx.compose.runtime.Immutable
import org.librefit.enums.WorkoutState
import java.time.LocalDateTime
import kotlin.random.Random

/**
 * The [org.librefit.db.entity.Workout] model used only by the ui. The difference is the use
 * of [Immutable] annotation
 *
 * @see [org.librefit.db.entity.Workout]
 */
@Immutable
data class UiWorkout(
    val id: Long = 0,
    val routineId: Long = Random.nextLong(),
    val notes: String = "",
    val title: String = "",
    val state: WorkoutState = WorkoutState.COMPLETED,
    val timeElapsed: Int = 0,
    val created: LocalDateTime = LocalDateTime.now(),
    val completed: LocalDateTime = LocalDateTime.now(),
    val position: Int = 0
)

/**
 * Returns a copy of this list of routines where the routine with the id [fromKey] takes the place of
 * the routine with the id [toKey], with every [UiWorkout.position] rewritten to match the new order.
 *
 * The keys are the ones of the lazy list items in [org.librefit.ui.screens.home.HomeScreen], which
 * are the routine ids. The list is returned unchanged when the keys are the same or when one of
 * them is not a routine of the list, e.g. when a routine is dragged over the header of the screen.
 */
fun List<UiWorkout>.moveRoutine(fromKey: Any, toKey: Any): List<UiWorkout> {
    val fromIndex = indexOfFirst { it.id == fromKey }
    val toIndex = indexOfFirst { it.id == toKey }
    if (fromIndex == -1 || toIndex == -1 || fromIndex == toIndex) return this

    return toMutableList()
        .apply {
            add(toIndex, removeAt(fromIndex))
        }
        .mapIndexed { index, routine -> routine.copy(position = index) }
}