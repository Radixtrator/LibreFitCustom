/*
 * SPDX-License-Identifier: GPL-3.0-or-later
 * Copyright (c) 2026. The LibreFit Contributors
 *
 * LibreFit is subject to additional terms covering author attribution and trademark usage;
 * see the ADDITIONAL_TERMS.md and TRADEMARK_POLICY.md files in the project root.
 */

package org.librefit.ui.models

/**
 * Returns the rest time, in seconds, to start once the set with id [setId] has been completed, or
 * zero when there is no rest to take.
 *
 * An exercise outside of a superset rests for its own [UiExercise.restTime]. A superset is trained
 * in rounds instead: the n-th set of every exercise in it, one after the other, and only then a
 * rest. So completing a set gives no rest while another exercise of the same superset still has its
 * set in that round to do, and the rest that closes the round is the longest one among the
 * exercises of the superset. An exercise with fewer sets than the others simply has no part in the
 * later rounds.
 */
fun List<UiExerciseWithSets>.restTimeAfterCompleting(setId: Long): Int {
    val exerciseWithSets = find { e -> e.sets.any { it.id == setId } } ?: return 0
    val groupId = exerciseWithSets.exercise.supersetGroupId
        ?: return exerciseWithSets.exercise.restTime

    val round = exerciseWithSets.sets.indexOfFirst { it.id == setId }
    val superset = filter { it.exercise.supersetGroupId == groupId }

    val isRoundDone = superset.all { member -> member.sets.getOrNull(round)?.completed ?: true }

    return if (isRoundDone) superset.maxOf { it.exercise.restTime } else 0
}

/**
 * Where to go once the set with id [setId] has been completed without a rest, i.e. in the middle of
 * a round of a superset, refer to [restTimeAfterCompleting].
 *
 * @property exerciseId The [UiExercise.id] of the exercise to train next
 * @property setIndex The index of the set to do there, the round being trained
 */
data class NextInSuperset(val exerciseId: Long, val setIndex: Int)

/**
 * Returns the exercise of the same superset that still has its set of the round of [setId] to do,
 * looking at the ones after the exercise of [setId] first and then wrapping around, or `null` when
 * the set is not in a superset or its round is done.
 */
fun List<UiExerciseWithSets>.nextInSupersetRound(setId: Long): NextInSuperset? {
    val exerciseWithSets = find { e -> e.sets.any { it.id == setId } } ?: return null
    val groupId = exerciseWithSets.exercise.supersetGroupId ?: return null

    val round = exerciseWithSets.sets.indexOfFirst { it.id == setId }
    val superset = filter { it.exercise.supersetGroupId == groupId }
    val position = superset.indexOf(exerciseWithSets)

    val next = (superset.drop(position + 1) + superset.take(position))
        .firstOrNull { member -> member.sets.getOrNull(round)?.completed == false }
        ?: return null

    return NextInSuperset(exerciseId = next.exercise.id, setIndex = round)
}
