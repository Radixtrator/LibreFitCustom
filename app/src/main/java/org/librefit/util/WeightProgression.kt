/*
 * SPDX-License-Identifier: GPL-3.0-or-later
 * Copyright (c) 2026. The LibreFit Contributors
 *
 * LibreFit is subject to additional terms covering author attribution and trademark usage;
 * see the ADDITIONAL_TERMS.md and TRADEMARK_POLICY.md files in the project root.
 */

package org.librefit.util

import org.librefit.db.entity.Set
import org.librefit.models.Weight

/**
 * Progressive overload helper. It decides whether the load of an exercise should go up in the next
 * session, and by how much, based on how the previous session went and on the
 * [org.librefit.db.entity.Exercise.weightIncrement] configured for that exercise.
 */
object WeightProgression {

    /**
     * The smallest multiplier, i.e. the plain increment configured for the exercise.
     */
    const val MIN_INCREMENT_MULTIPLIER = 1

    /**
     * The largest multiplier that can be earned on an AMRAP set. Beyond this point the reported
     * repetitions are far likelier to be a typo than a performance, and honouring them would
     * suggest a jump nobody can actually lift.
     */
    const val MAX_INCREMENT_MULTIPLIER = 4

    /**
     * A session counts as successful only when it actually has sets and every single one of them
     * has been completed without being marked as [Set.failed], meaning the user hit the planned
     * amount of repetitions on all of them.
     */
    fun isSessionSuccessful(sets: List<Set>): Boolean {
        return sets.isNotEmpty() && sets.all { it.completed && !it.failed }
    }

    /**
     * How many times the configured increment has been earned by the AMRAP sets of a session.
     *
     * An AMRAP set is a set the user takes to the limit instead of stopping at the planned amount
     * of repetitions, so beating the plan is the signal that the load was too light: doubling the
     * planned repetitions doubles the increment, tripling them triples it, and so on up to
     * [MAX_INCREMENT_MULTIPLIER].
     *
     * When a session holds more than one AMRAP set, the smallest multiplier wins, in line with the
     * all-or-nothing rule of [isSessionSuccessful]: the bonus is earned only when *every* AMRAP set
     * of the exercise showed it. A session without AMRAP sets, or one whose AMRAP sets have no
     * planned amount to compare against, keeps the plain increment.
     */
    fun incrementMultiplier(sets: List<Set>): Int {
        val multipliers = sets
            .filter { it.isAmrap && it.targetReps > 0 }
            .map { (it.reps / it.targetReps).coerceIn(MIN_INCREMENT_MULTIPLIER, MAX_INCREMENT_MULTIPLIER) }

        return multipliers.minOrNull() ?: MIN_INCREMENT_MULTIPLIER
    }

    /**
     * Returns the load to suggest for the next session, or `null` when there is nothing to suggest,
     * i.e. the exercise has no increment configured or the previous session was not successful.
     *
     * @param previousLoad The load lifted in the previous session.
     * @param increment Refer to [org.librefit.db.entity.Exercise.weightIncrement]
     * @param sessionSuccessful Refer to [isSessionSuccessful]
     * @param incrementMultiplier Refer to [incrementMultiplier]
     */
    fun suggestedLoad(
        previousLoad: Weight,
        increment: Weight,
        sessionSuccessful: Boolean,
        incrementMultiplier: Int = MIN_INCREMENT_MULTIPLIER
    ): Weight? {
        if (!sessionSuccessful || increment <= Weight.zero()) return null

        val multiplier = incrementMultiplier
            .coerceIn(MIN_INCREMENT_MULTIPLIER, MAX_INCREMENT_MULTIPLIER)

        return previousLoad + increment * multiplier.toDouble()
    }
}
