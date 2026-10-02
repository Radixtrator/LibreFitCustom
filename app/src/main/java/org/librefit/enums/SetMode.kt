/*
 * SPDX-License-Identifier: GPL-3.0-or-later
 * Copyright (c) 2024-2026. The LibreFit Contributors
 *
 * LibreFit is subject to additional terms covering author attribution and trademark usage;
 * see the ADDITIONAL_TERMS.md and TRADEMARK_POLICY.md files in the project root.
 */

package org.librefit.enums

enum class SetMode {
    LOAD,
    BODYWEIGHT,
    BODYWEIGHT_WITH_LOAD,

    /**
     * A bodyweight exercise done on a machine taking part of the weight off, e.g. an assisted
     * pull-up. The load of its sets is that support, so it goes *down* as the user gets stronger,
     * refer to [org.librefit.util.WeightProgression.suggestedLoad]
     */
    ASSISTED_BODYWEIGHT,
    DURATION;
    //TODO: DURATION_WITH_DISTANCE

    /** Whether the sets of this mode carry a load the user enters */
    val hasLoad: Boolean
        get() = this == LOAD || this == BODYWEIGHT_WITH_LOAD || this == ASSISTED_BODYWEIGHT

    /**
     * The weight actually moved on each repetition, given the [load] of the set and the
     * [bodyWeight] of the user, both in the same unit
     */
    fun effectiveLoad(load: Double, bodyWeight: Double): Double = when (this) {
        LOAD -> load
        BODYWEIGHT -> bodyWeight
        BODYWEIGHT_WITH_LOAD -> load + bodyWeight
        ASSISTED_BODYWEIGHT -> (bodyWeight - load).coerceAtLeast(0.0)
        DURATION -> 0.0
    }
}