/*
 * SPDX-License-Identifier: GPL-3.0-or-later
 * Copyright (c) 2026. The LibreFit Contributors
 *
 * LibreFit is subject to additional terms covering author attribution and trademark usage;
 * see the ADDITIONAL_TERMS.md and TRADEMARK_POLICY.md files in the project root.
 */

package org.librefit.ui.models

import kotlinx.collections.immutable.toImmutableList

/**
 * Carries into a running workout the edits made to its routine while the workout is going on.
 *
 * Only what changed between [before] and [after] is applied, so whatever the workout already holds
 * (the loads it started from, the sets typed in, an exercise removed for today) is kept unless the
 * very same thing was edited in the routine. What has been trained is never touched:
 *
 * - The notes, rest time, weight increase, set mode and superset of an exercise follow the routine.
 * - A set edited in the routine takes its new values, unless it has already been completed.
 * - Sets and exercises added to the routine are added to the workout as well.
 * - Sets and exercises removed from the routine are removed from the workout, unless they have
 *   been completed (for an exercise, unless any of its sets has).
 * - When the routine has been reordered, the workout follows the new order.
 *
 * A workout started from a routine shares the ids of its exercises and sets, which is how they
 * are paired. A workout resumed after the app was closed has ids of its own, so its exercises are
 * paired by exercise and occurrence and their sets by position.
 *
 * @receiver The exercises of the running workout
 * @param before The exercises of the routine before the edit
 * @param after The exercises of the routine after the edit
 */
fun List<UiExerciseWithSets>.withRoutineEdits(
    before: List<UiExerciseWithSets>,
    after: List<UiExerciseWithSets>
): List<UiExerciseWithSets> {
    if (before == after) return this

    // Routine exercise id -> workout exercise id
    val pairs = pairExercises(workout = this, routine = before)
    val beforeById = before.associateBy { it.exercise.id }
    val afterIds = after.map { it.exercise.id }.toSet()

    val result = toMutableList()

    // Removed from the routine
    before.filter { it.exercise.id !in afterIds }.forEach { removed ->
        val workoutId = pairs[removed.exercise.id] ?: return@forEach
        result.removeAll { it.exercise.id == workoutId && it.sets.none { set -> set.completed } }
    }

    // Edited in the routine
    after.forEach { edited ->
        val previous = beforeById[edited.exercise.id] ?: return@forEach
        val workoutId = pairs[edited.exercise.id] ?: return@forEach
        val index = result.indexOfFirst { it.exercise.id == workoutId }
        if (index != -1) {
            result[index] = result[index].withExerciseEdits(before = previous, after = edited)
        }
    }

    // Reordered in the routine. The exercises paired with the routine are sorted by their new
    // order within the slots they already take, so the ones the routine knows nothing about stay
    val beforeOrder = before.map { it.exercise.id }.filter { it in afterIds }
    val afterOrder = after.map { it.exercise.id }.filter { it in beforeById }
    if (beforeOrder != afterOrder) {
        val rank = afterOrder.withIndex()
            .mapNotNull { (index, routineId) -> pairs[routineId]?.let { it to index } }
            .toMap()
        val slots = result.indices.filter { result[it].exercise.id in rank }
        val sorted = slots.map { result[it] }.sortedBy { rank.getValue(it.exercise.id) }
        slots.zip(sorted).forEach { (slot, exercise) -> result[slot] = exercise }
    }

    // Added to the routine, right after the exercise that precedes them there
    after.forEachIndexed { index, added ->
        if (added.exercise.id in beforeById) return@forEachIndexed

        val precedingWorkoutId = after.take(index).asReversed().firstNotNullOfOrNull { routine ->
            val workoutId = pairs[routine.exercise.id] ?: routine.exercise.id
            workoutId.takeIf { id -> result.any { it.exercise.id == id } }
        }
        val insertAt = precedingWorkoutId
            ?.let { id -> result.indexOfFirst { it.exercise.id == id } + 1 }
            ?: 0

        result.add(insertAt, added.asPlanned())
    }

    return result.withNormalizedExercisePositions()
}

private fun UiExerciseWithSets.withExerciseEdits(
    before: UiExerciseWithSets,
    after: UiExerciseWithSets
): UiExerciseWithSets {
    val b = before.exercise
    val a = after.exercise
    val editedExercise = exercise.copy(
        notes = if (a.notes != b.notes) a.notes else exercise.notes,
        restTime = if (a.restTime != b.restTime) a.restTime else exercise.restTime,
        weightIncrement = if (a.weightIncrement != b.weightIncrement) a.weightIncrement else exercise.weightIncrement,
        setMode = if (a.setMode != b.setMode) a.setMode else exercise.setMode,
        supersetGroupId = if (a.supersetGroupId != b.supersetGroupId) a.supersetGroupId else exercise.supersetGroupId
    )

    if (before.sets == after.sets) return copy(exercise = editedExercise)

    // Routine set id -> workout set id
    val pairs = pairSets(workout = sets, routine = before.sets)
    val beforeById = before.sets.associateBy { it.id }
    val afterIds = after.sets.map { it.id }.toSet()

    val editedSets = sets.toMutableList()

    before.sets.filter { it.id !in afterIds }.forEach { removed ->
        val workoutId = pairs[removed.id] ?: return@forEach
        editedSets.removeAll { it.id == workoutId && !it.completed }
    }

    after.sets.forEach { edited ->
        val previous = beforeById[edited.id]
        if (previous == null) {
            editedSets.add(edited.asPlanned())
            return@forEach
        }
        val workoutId = pairs[edited.id] ?: return@forEach
        val index = editedSets.indexOfFirst { it.id == workoutId }
        if (index != -1 && !editedSets[index].completed) {
            editedSets[index] = editedSets[index].withSetEdits(before = previous, after = edited)
        }
    }

    return copy(exercise = editedExercise, sets = editedSets.toImmutableList())
}

private fun UiSet.withSetEdits(before: UiSet, after: UiSet): UiSet = copy(
    load = if (after.load != before.load) after.load else load,
    reps = if (after.reps != before.reps) after.reps else reps,
    elapsedTime = if (after.elapsedTime != before.elapsedTime) after.elapsedTime else elapsedTime,
    isAmrap = if (after.isAmrap != before.isAmrap) after.isAmrap else isAmrap,
    targetReps = if (after.targetReps != before.targetReps) after.targetReps else targetReps
)

private fun UiExerciseWithSets.asPlanned() = copy(sets = sets.map { it.asPlanned() }.toImmutableList())

private fun UiSet.asPlanned() = copy(completed = false, failed = false)

/**
 * Pairs every exercise of the [routine] with its counterpart in the [workout], from the routine
 * exercise id to the workout exercise id. Exercises are paired by id first, then by exercise and
 * occurrence among the ones left.
 */
private fun pairExercises(
    workout: List<UiExerciseWithSets>,
    routine: List<UiExerciseWithSets>
): Map<Long, Long> {
    val workoutIds = workout.map { it.exercise.id }.toSet()
    val pairs = routine
        .filter { it.exercise.id in workoutIds }
        .associate { it.exercise.id to it.exercise.id }

    val leftInWorkout = workout.filter { it.exercise.id !in pairs.values }
    val leftInRoutine = routine.filter { it.exercise.id !in pairs }

    return pairs + leftInRoutine.mapIndexedNotNull { index, exercise ->
        val occurrence = leftInRoutine.take(index).count { it.exerciseDC.id == exercise.exerciseDC.id }
        leftInWorkout
            .filter { it.exerciseDC.id == exercise.exerciseDC.id }
            .getOrNull(occurrence)
            ?.let { exercise.exercise.id to it.exercise.id }
    }
}

/**
 * Pairs every set of the [routine] with its counterpart in the [workout], from the routine set id
 * to the workout set id. Sets are paired by id first, then by position among the ones left.
 */
private fun pairSets(workout: List<UiSet>, routine: List<UiSet>): Map<Long, Long> {
    val workoutIds = workout.map { it.id }.toSet()
    val pairs = routine.filter { it.id in workoutIds }.associate { it.id to it.id }

    val leftInWorkout = workout.filter { it.id !in pairs.values }
    val leftInRoutine = routine.filter { it.id !in pairs }

    return pairs + leftInRoutine.zip(leftInWorkout).associate { (r, w) -> r.id to w.id }
}
