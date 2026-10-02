/*
 * SPDX-License-Identifier: GPL-3.0-or-later
 * Copyright (c) 2026. The LibreFit Contributors
 *
 * LibreFit is subject to additional terms covering author attribution and trademark usage;
 * see the ADDITIONAL_TERMS.md and TRADEMARK_POLICY.md files in the project root.
 */

package org.librefit.ui.screens.home

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.librefit.MainDispatcherRule
import org.librefit.db.entity.Workout
import org.librefit.db.relations.WorkoutWithExercisesAndSets
import org.librefit.db.repository.UserPreferencesRepository
import org.librefit.db.repository.WorkoutRepository
import org.librefit.enums.WorkoutState
import org.librefit.ui.models.UiWorkout
import org.librefit.ui.models.mappers.toEntity
import org.librefit.ui.models.mappers.toUi
import org.librefit.ui.models.moveRoutine
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomeScreenViewModelTest {
    // MainDispatcherRule to control coroutine execution
    private val mainDispatcherRule = MainDispatcherRule()

    // The mock repositories
    private lateinit var userPreferencesRepository: UserPreferencesRepository
    private lateinit var workoutRepository: WorkoutRepository

    // The class under test
    private lateinit var viewModel: HomeScreenViewModel

    // A controllable flow to simulate the routines stored in the database
    private lateinit var routinesFlow: MutableStateFlow<List<Workout>>

    // Captured objects
    private val routineIdsInNewOrder = slot<List<Long>>()

    // Test data
    private val routines = listOf(
        Workout(id = 1, title = "Upper body", state = WorkoutState.ROUTINE, position = 0),
        Workout(id = 2, title = "Lower body", state = WorkoutState.ROUTINE, position = 1),
        Workout(id = 3, title = "Tempo run", state = WorkoutState.ROUTINE, position = 2)
    )

    @BeforeTest
    fun setUp() {
        mainDispatcherRule.setUp()

        // Arrange: Create the mocks for the repositories
        userPreferencesRepository = mockk()
        workoutRepository = mockk()
        routinesFlow = MutableStateFlow(routines)

        every { userPreferencesRepository.requestPermissionsNextTime } returns MutableStateFlow(true)
        every { workoutRepository.routines } returns routinesFlow
        every { workoutRepository.runningWorkoutsWithExercisesAndSets } returns MutableStateFlow(emptyList<WorkoutWithExercisesAndSets>())
        every { workoutRepository.completedWorkouts } returns MutableStateFlow(emptyList<Workout>())

        // Arrange: Saving an order rewrites the positions and the database emits the routines in it
        coEvery { workoutRepository.updateRoutinesOrder(capture(routineIdsInNewOrder)) } answers {
            val routinesById = routinesFlow.value.associateBy { it.id }
            routinesFlow.value = routineIdsInNewOrder.captured.mapIndexed { index, id ->
                routinesById.getValue(id).copy(position = index)
            }
        }

        viewModel = HomeScreenViewModel(userPreferencesRepository, workoutRepository)
    }

    @AfterTest
    fun tearDown() {
        mainDispatcherRule.tearDown()
    }

    @Test
    fun `routines are emitted in the order of the repository`() = runTest {
        viewModel.routines.test {
            val emitted = awaitItem()

            assertThat(emitted.map { it.id }).containsExactly(1L, 2L, 3L).inOrder()
            assertThat(emitted.map { it.position }).containsExactly(0, 1, 2).inOrder()
        }
    }

    @Test
    fun `when routines are reordered - the new order is saved and emitted`() = runTest {
        viewModel.routines.test {
            // Consume the routines in their initial order
            awaitItem()

            // Act: Move the last routine to the top
            viewModel.reorderRoutines(listOf(3L, 1L, 2L))

            // Assert: The routines come back from the database in the new order
            val reordered = awaitItem()
            assertThat(reordered.map { it.id }).containsExactly(3L, 1L, 2L).inOrder()
            assertThat(reordered.map { it.position }).containsExactly(0, 1, 2).inOrder()
        }

        coVerify(exactly = 1) { workoutRepository.updateRoutinesOrder(listOf(3L, 1L, 2L)) }
    }

    @Test
    fun `when routines are dropped in the same order - nothing is saved`() = runTest {
        viewModel.routines.test {
            awaitItem()

            // Act: The order is the one already shown
            viewModel.reorderRoutines(listOf(1L, 2L, 3L))

            expectNoEvents()
        }

        coVerify(exactly = 0) { workoutRepository.updateRoutinesOrder(any()) }
    }

    @Test
    fun `moveRoutine moves the routine in place of the target and rewrites positions`() {
        val uiRoutines = routines.map { it.toUi() }

        val moved = uiRoutines.moveRoutine(fromKey = 1L, toKey = 3L)

        assertThat(moved.map { it.id }).containsExactly(2L, 3L, 1L).inOrder()
        assertThat(moved.map { it.position }).containsExactly(0, 1, 2).inOrder()
    }

    @Test
    fun `moveRoutine ignores keys of items that are not routines`() {
        val uiRoutines = routines.map { it.toUi() }

        // E.g. the header items of the home screen, whose keys are not routine ids
        assertThat(uiRoutines.moveRoutine(fromKey = 1L, toKey = "header")).isSameInstanceAs(uiRoutines)
        assertThat(uiRoutines.moveRoutine(fromKey = 4L, toKey = 1L)).isSameInstanceAs(uiRoutines)
        assertThat(uiRoutines.moveRoutine(fromKey = 2L, toKey = 2L)).isSameInstanceAs(uiRoutines)
    }

    @Test
    fun `position survives mapper round trip`() {
        val uiWorkout = UiWorkout(id = 99L, state = WorkoutState.ROUTINE, position = 4)

        val entity = uiWorkout.toEntity()
        val roundTrip = entity.toUi()

        assertThat(entity.position).isEqualTo(4)
        assertThat(roundTrip.position).isEqualTo(4)
    }
}
