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
