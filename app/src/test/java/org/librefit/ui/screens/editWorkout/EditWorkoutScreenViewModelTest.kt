/*
 * SPDX-License-Identifier: GPL-3.0-or-later
 * Copyright (c) 2025-2026. The LibreFit Contributors
 *
 * LibreFit is subject to additional terms covering author attribution and trademark usage;
 * see the ADDITIONAL_TERMS.md and TRADEMARK_POLICY.md files in the project root.
 */

package org.librefit.ui.screens.editWorkout

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.librefit.enums.userPreferences.UnitSystem
import org.librefit.models.Weight
import org.librefit.ui.models.UiExercise
import org.librefit.ui.models.UiExerciseWithSets
import org.librefit.ui.models.UiSet
import org.librefit.ui.models.doubleValue
import org.librefit.ui.models.mappers.toEntity
import org.librefit.ui.models.mappers.toUi
import org.librefit.ui.models.moveExercise
import org.librefit.ui.models.withAmrap
import org.librefit.ui.models.withNormalizedExercisePositions

class EditWorkoutScreenViewModelTest {

    private val exercises = listOf(
        UiExerciseWithSets(exercise = UiExercise(id = 11L, position = 0)),
        UiExerciseWithSets(exercise = UiExercise(id = 22L, position = 1)),
        UiExerciseWithSets(exercise = UiExercise(id = 33L, position = 2))
    )

    @Test
    fun `moveExercise reorders list and rewrites positions`() {
        val reordered = exercises.moveExercise(fromIndex = 0, toIndex = 2)

        assertThat(reordered.map { it.exercise.id }).containsExactly(22L, 33L, 11L).inOrder()
        assertThat(reordered.map { it.exercise.position }).containsExactly(0, 1, 2).inOrder()
    }

    @Test
    fun `moveExercise ignores invalid indices`() {
        val reordered = exercises.moveExercise(fromIndex = -1, toIndex = 2)

        assertThat(reordered).isEqualTo(exercises)
    }

    @Test
    fun `withNormalizedExercisePositions rewrites positions sequentially`() {
        val normalized = listOf(
            UiExerciseWithSets(exercise = UiExercise(id = 22L, position = 99)),
            UiExerciseWithSets(exercise = UiExercise(id = 11L, position = 44))
        ).withNormalizedExercisePositions()

        assertThat(normalized.map { it.exercise.id }).containsExactly(22L, 11L).inOrder()
        assertThat(normalized.map { it.exercise.position }).containsExactly(0, 1).inOrder()
    }

    @Test
    fun `superset group id survives mapper round trip`() {
        val uiExercise = UiExercise(id = 99L, supersetGroupId = 42L)

        val entity = uiExercise.toEntity()
        val roundTrip = entity.toUi()

        assertThat(entity.supersetGroupId).isEqualTo(42L)
        assertThat(roundTrip.supersetGroupId).isEqualTo(42L)
    }

    @Test
    fun `weight step helper increments by two and a half pounds in imperial`() {
        val stepped = Weight.auto(10.0, UnitSystem.IMPERIAL).stepBy(2.5, UnitSystem.IMPERIAL)

        assertThat(stepped.doubleValue(UnitSystem.IMPERIAL)).isWithin(0.001).of(12.5)
    }

    @Test
    fun `weight increment survives mapper round trip`() {
        val uiExercise = UiExercise(id = 99L, weightIncrement = Weight.kilograms(2.5))

        val entity = uiExercise.toEntity()
        val roundTrip = entity.toUi()

        assertThat(entity.weightIncrement).isEqualTo(Weight.kilograms(2.5))
        assertThat(roundTrip.weightIncrement).isEqualTo(Weight.kilograms(2.5))
    }

    @Test
    fun `failed flag survives mapper round trip`() {
        val uiSet = UiSet(id = 7L, failed = true)

        val entity = uiSet.toEntity()
        val roundTrip = entity.toUi()

        assertThat(entity.failed).isTrue()
        assertThat(roundTrip.failed).isTrue()
    }

    @Test
    fun `amrap flag and target reps survive mapper round trip`() {
        val uiSet = UiSet(id = 7L, isAmrap = true, targetReps = 8)

        val entity = uiSet.toEntity()
        val roundTrip = entity.toUi()

        assertThat(entity.isAmrap).isTrue()
        assertThat(entity.targetReps).isEqualTo(8)
        assertThat(roundTrip.isAmrap).isTrue()
        assertThat(roundTrip.targetReps).isEqualTo(8)
    }

    @Test
    fun `marking a set as amrap takes the planned reps as its target`() {
        val uiSet = UiSet(reps = 5)

        val amrap = uiSet.withAmrap(true)

        assertThat(amrap.isAmrap).isTrue()
        assertThat(amrap.targetReps).isEqualTo(5)
    }

    @Test
    fun `marking a set as amrap keeps a target the user has already set`() {
        val uiSet = UiSet(reps = 5, targetReps = 8)

        val amrap = uiSet.withAmrap(true)

        assertThat(amrap.targetReps).isEqualTo(8)
    }

    @Test
    fun `clearing the amrap flag keeps the target for when it is set again`() {
        val uiSet = UiSet(reps = 5, isAmrap = true, targetReps = 8)

        val notAmrap = uiSet.withAmrap(false)

        assertThat(notAmrap.isAmrap).isFalse()
        assertThat(notAmrap.targetReps).isEqualTo(8)
    }
}
