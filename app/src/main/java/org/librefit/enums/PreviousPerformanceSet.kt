/*
 * SPDX-License-Identifier: GPL-3.0-or-later
 * Copyright (c) 2025-2026. The LibreFit Contributors
 *
 * LibreFit is subject to additional terms covering author attribution and trademark usage;
 * see the ADDITIONAL_TERMS.md and TRADEMARK_POLICY.md files in the project root.
 */

package org.librefit.enums

import org.librefit.models.Weight

/**
 * It is used to display the previous of a set in a [org.librefit.ui.components.ExerciseCard]
 *
 * @property reps The repetitions performed in the previous session.
 * @property load The load lifted in the previous session.
 * @property time The time elapsed in the previous session.
 * @property suggestedLoad The load to aim for in the current session. It is not null only when the
 * exercise has a [org.librefit.db.entity.Exercise.weightIncrement] configured and every set of the
 * previous session was completed without being marked as [org.librefit.db.entity.Set.failed].
 * Refer to [org.librefit.util.WeightProgression].
 */
data class PreviousPerformanceSet(
    val reps: Int = 0,
    val load: Weight = Weight.zero(),
    val time: Int = 0,
    val suggestedLoad: Weight? = null
)
