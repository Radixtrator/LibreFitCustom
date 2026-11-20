/*
 * SPDX-License-Identifier: GPL-3.0-or-later
 * Copyright (c) 2026. The LibreFit Contributors
 *
 * LibreFit is subject to additional terms covering author attribution and trademark usage;
 * see the ADDITIONAL_TERMS.md and TRADEMARK_POLICY.md files in the project root.
 */

package org.librefit.ui.models

import kotlinx.collections.immutable.toImmutableList
import org.librefit.db.entity.Set
import org.librefit.db.relations.ExerciseWithSets
import org.librefit.enums.SetMode
import org.librefit.util.WeightProgression

/**
 * Fills the sets of an exercise about to be trained with what was done in its [previous] session,
 * set by set, so a workout starts where the last one left off:
 *
 * - The load is the one lifted last time, raised by [WeightProgression] when every set of that
 *   session was completed without being marked as failed. Doubling the planned repetitions of an
 *   AMRAP set doubles the raise, refer to [WeightProgression.incrementMultiplier].
 * - The repetitions and the duration are the ones performed last time. An AMRAP set is planned for
 *   its target instead, and a set that was failed last time keeps the repetitions it is planned
 *   for, since what was reached then is not a plan to repeat.
 * - A set that was AMRAP last time stays AMRAP, with the same target, so the flag is remembered
 *   for the exercise even when it was set during the workout rather than in the routine.
 *
 * A set is left as it is when the previous session has no completed set at its position, when the
 * exercise was trained with another [SetMode] back then, or when [shouldPrefill] rejects it.
 */
fun UiExerciseWithSets.prefilledFrom(
    previous: ExerciseWithSets?,
    shouldPrefill: (UiSet) -> Boolean = { true }
): UiExerciseWithSets {
    if (previous == null || previous.exercise.setMode != exercise.setMode) return this

    val sessionSuccessful = WeightProgression.isSessionSuccessful(previous.sets)
    val incrementMultiplier = WeightProgression.incrementMultiplier(previous.sets)

    return copy(
        sets = sets.mapIndexed { index, set ->
            val previousSet = previous.sets.getOrNull(index)
            if (previousSet == null || !previousSet.completed || !shouldPrefill(set)) {
                return@mapIndexed set
            }

            val load = WeightProgression.suggestedLoad(
                previousLoad = previousSet.load,
                increment = exercise.weightIncrement,
                sessionSuccessful = sessionSuccessful,
                incrementMultiplier = incrementMultiplier,
                decreasing = exercise.setMode == SetMode.ASSISTED_BODYWEIGHT
            ) ?: previousSet.load

            when (exercise.setMode) {
                SetMode.LOAD, SetMode.BODYWEIGHT_WITH_LOAD, SetMode.ASSISTED_BODYWEIGHT -> {
                    val planned = set.withAmrapFrom(previousSet)
                    planned.copy(load = load, reps = repsToPlan(planned, previousSet))
                }

                SetMode.BODYWEIGHT -> set.copy(reps = repsToPlan(set, previousSet))

                SetMode.DURATION -> set.copy(elapsedTime = previousSet.elapsedTime)
            }
        }.toImmutableList()
    )
}

/**
 * Keeps the AMRAP flag of [previousSet] on the set. A target already planned for the set wins over
 * the one of the previous session.
 */
private fun UiSet.withAmrapFrom(previousSet: Set): UiSet {
    if (!previousSet.isAmrap) return this
    return copy(
        isAmrap = true,
        targetReps = if (isAmrap && targetReps > 0) targetReps else previousSet.targetReps
    )
}

private fun repsToPlan(set: UiSet, previousSet: Set): Int = when {
    set.isAmrap -> if (set.targetReps > 0) set.targetReps else set.reps
    previousSet.failed -> set.reps
    else -> previousSet.reps
}
