/*
 * SPDX-License-Identifier: GPL-3.0-or-later
 * Copyright (c) 2026. The LibreFit Contributors
 *
 * LibreFit is subject to additional terms covering author attribution and trademark usage;
 * see the ADDITIONAL_TERMS.md and TRADEMARK_POLICY.md files in the project root.
 */

package org.librefit.models

import kotlinx.serialization.Serializable
import org.librefit.db.entity.Workout
import org.librefit.db.relations.ExerciseWithSets

@Serializable
data class RoutineFile(
    val version: Int = 1,
    val workout: Workout = Workout(),
    val exercisesWithSets: List<ExerciseWithSets> = emptyList()
)
