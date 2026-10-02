/*
 * SPDX-License-Identifier: GPL-3.0-or-later
 * Copyright (c) 2024-2026. The LibreFit Contributors
 *
 * LibreFit is subject to additional terms covering author attribution and trademark usage;
 * see the ADDITIONAL_TERMS.md and TRADEMARK_POLICY.md files in the project root.
 */

package org.librefit.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.material3.ButtonGroup
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CheckableDropdownMenuItem
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenuGroup
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.DropdownMenuPopup
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ExposedDropdownMenu
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TriStateCheckbox
import androidx.compose.material3.rememberSliderState
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.Wallpapers
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.collectLatest
import org.librefit.R
import org.librefit.enums.InfoMode
import org.librefit.enums.PreviousPerformanceSet
import org.librefit.enums.SetMode
import org.librefit.enums.exercise.Equipment
import org.librefit.enums.userPreferences.ThemeMode
import org.librefit.enums.userPreferences.UnitSystem
import org.librefit.models.Weight
import org.librefit.nav.LocalUnitSystem
import org.librefit.ui.components.dialogs.AmrapRepsDialog
import org.librefit.ui.components.dialogs.SetSettingsDialog
import org.librefit.ui.components.modalBottomSheets.BarbellCalculatorModalBottomSheet
import org.librefit.ui.components.modalBottomSheets.InputModalBottomSheet
import org.librefit.ui.models.InputModalBottomSheetState
import org.librefit.ui.models.UiExercise
import org.librefit.ui.models.UiExerciseDC
import org.librefit.ui.models.UiExerciseWithSets
import org.librefit.ui.models.UiSet
import org.librefit.ui.models.autoUnitSuffix
import org.librefit.ui.models.formatToText
import org.librefit.ui.models.isRepresentedBy
import org.librefit.ui.models.normalizeWeightInput
import org.librefit.ui.models.parseWeightInput
import org.librefit.ui.models.toInputText
import org.librefit.ui.models.weightKeyboardType
import org.librefit.ui.models.withAmrap
import org.librefit.ui.theme.LibreFitTheme
import org.librefit.ui.theme.supersetBlue
import org.librefit.ui.theme.supersetBlueAlternate
import org.librefit.util.Formatter
import org.librefit.util.textFieldTransformations.TimeInputTransformation
import org.librefit.util.textFieldTransformations.TimeOutputTransformation
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.seconds

/**
 * A custom [ElevatedCard] designed to display an [UiExerciseWithSets] with a uniform appearance across
 * the app.
 *
 * Every card can be folded down to its header with the arrow next to its menu, or by tapping the
 * header of a folded card, leaving only a one-line summary of its sets. The fold outlives the card
 * scrolling out of the list and configuration changes.
 *
 * When the exercise belongs to a superset, i.e. it has a [UiExercise.supersetGroupId], the card is
 * tinted blue, labeled as such and marked with a stripe along its start edge, which links it to the
 * neighbouring cards of the same superset. Refer to [supersetLink].
 *
 * @param modifier A [Modifier] that should be passed as `Modifier.animateItem` to enable
 * animation for the card within the list.
 * @param animatedVisibilityScope Used for image's animation transition
 * @param exerciseWithSets An instance of [UiExerciseWithSets] containing all the relevant information
 * required for the card display.
 * @param previousPerformances When not null and not empty, it displays the performances of previous set next
 * to the associated set. The strings should be already formatted and ready to be displayed.
 * @param addSet A lambda function invoked when the "Add set" button is clicked.
 * @param onDetail A lambda function triggered when the exercise's name or image is clicked, which should open
 * the [org.librefit.ui.screens.infoExercise.InfoExerciseScreen].
 * @param onDelete A lambda function executed when the *Delete* icon is clicked, it should result in
 * the removal of the card.
 * @param isCollapsed When `true`, the card collapses its editable body to provide clearer reorder feedback. So it's true only when reordering one of exercises in the list.
 * It hides the body whatever the fold chosen by the user, without changing it, and swaps the fold
 * arrow and the menu for a drag handle.
 * @param dragHandleModifier Modifier applied to the optional drag handle.
 * @param supersetLink Where the card stands within its superset, as computed by [supersetLinks]
 * over the whole list. It picks the shade of blue of the superset and links the card to the
 * neighbouring ones of the same superset, so they read as a single block. When `null`, an exercise
 * with a [UiExercise.supersetGroupId] is still painted as part of a superset, just on its own.
 * @param listSpacing The spacing between the items of the list holding the card, which is the gap
 * the stripe linking it to the previous card of the same superset spans. It defaults to the one of
 * [LibreFitLazyColumn].
 * @param onReorderRequest A lambda triggered when the `reorder` option from dropdown menu is pressed.
 * @param isDragging when `true`, it applies a shadow to further emphasize with a shadow that the card is dragged.
 * @param useScrollWheelForInput If `true`, [InputModalBottomSheet] appears instead of keyboard
 * @param dismissScrollWheelInputAutomatically If both this and [useScrollWheelForInput] are `true`, the [InputModalBottomSheet] will be dismissed automatically after first edit.
 * @param showExercisesImages If `true`, it shows image of exercise
 * @param updateExerciseNotes A function to update notes based on [UiExercise.id]. For more details, refer to
 * [org.librefit.ui.screens.workout.WorkoutScreenViewModel.updateExerciseNotes] and
 * [org.librefit.ui.screens.editWorkout.EditWorkoutScreenViewModel.updateExerciseNotes].
 * @param updateExerciseRestTime A function to update rest time based on [UiExercise.id]. For more details, refer to
 * [org.librefit.ui.screens.workout.WorkoutScreenViewModel.updateExerciseRestTime] and
 * [org.librefit.ui.screens.editWorkout.EditWorkoutScreenViewModel.updateExerciseRestTime].
 * @param updateExerciseSetMode A function to update the set mode based on.
 * For more details, refer to [org.librefit.ui.screens.workout.WorkoutScreenViewModel.updateExerciseSetMode]
 * and [org.librefit.ui.screens.editWorkout.EditWorkoutScreenViewModel.updateExerciseSetMode].
 * @param updateSetLoad A function to update load based on [UiSet.id]. For more details, refer to
 * [org.librefit.ui.screens.workout.WorkoutScreenViewModel.updateSetLoad] and
 * [org.librefit.ui.screens.editWorkout.EditWorkoutScreenViewModel.updateSetLoad].
 * @param updateSetReps A function to update reps based on [UiSet.id]. For more details, refer to
 * [org.librefit.ui.screens.workout.WorkoutScreenViewModel.updateSetReps] and
 * [org.librefit.ui.screens.editWorkout.EditWorkoutScreenViewModel.updateSetReps].
 * @param updateSetTime A function to update time based on [UiSet.id].. For more details, refer to
 * [org.librefit.ui.screens.workout.WorkoutScreenViewModel.updateSetTime] and
 * [org.librefit.ui.screens.editWorkout.EditWorkoutScreenViewModel.updateSetTime].
 * @param updateExerciseWeightIncrement A function to update the weight increment based on [UiExercise.id].
 * For more details, refer to [org.librefit.ui.screens.workout.WorkoutScreenViewModel.updateExerciseWeightIncrement]
 * and [org.librefit.ui.screens.editWorkout.EditWorkoutScreenViewModel.updateExerciseWeightIncrement].
 * @param updateSetCompleted A function to update completed state based on [UiSet.id]. For more details, refer to
 * [org.librefit.ui.screens.workout.WorkoutScreenViewModel.updateSetCompleted] and
 * [org.librefit.ui.screens.editWorkout.EditWorkoutScreenViewModel.updateSetCompleted].
 * @param updateSetFailed A function to flag a set as performed but short of the planned repetitions,
 * based on [UiSet.id]. For more details, refer to
 * [org.librefit.ui.screens.workout.WorkoutScreenViewModel.updateSetFailed] and
 * [org.librefit.ui.screens.editWorkout.EditWorkoutScreenViewModel.updateSetFailed].
 * @param updateSetIsAmrap A function to flag a set as an AMRAP one, based on [UiSet.id]. For more
 * details, refer to [org.librefit.ui.screens.workout.WorkoutScreenViewModel.updateSetIsAmrap] and
 * [org.librefit.ui.screens.editWorkout.EditWorkoutScreenViewModel.updateSetIsAmrap].
 * @param updateSetTargetReps A function to update the repetitions an AMRAP set is planned for,
 * based on [UiSet.id]. For more details, refer to
 * [org.librefit.ui.screens.workout.WorkoutScreenViewModel.updateSetTargetReps] and
 * [org.librefit.ui.screens.editWorkout.EditWorkoutScreenViewModel.updateSetTargetReps].
 * @param deleteSet A function called when the user removes the set from its dialog, or swipes
 * it away outside a workout, where a swipe has nothing to mark.
 * @param showInfo A lambda function executed when info icon next to "type of set" or "rest time" text
 * is clicked. The passed parameter is used by [org.librefit.ui.components.modalBottomSheets.InfoModalBottomSheet] to show the relevant information.
 * @param idSetWithRunningStopwatch The ID of the set whose stopwatch is currently active. This ensures
 * only one timer runs at a time. The composable will display a running stopwatch for the
 * set matching this ID. Pass null if no timer is active. This parameter is only used when [workout] is `true`.
 * @param updateIdSetWithRunningStopwatch A callback invoked when the user interacts with a set with a
 * running stopwatch. It provides the ID of the set that should become active, or null to stop the current timer.
 * This parameter is only used when [workout] is `true`.
 * @param workout A Boolean flag indicating whether a checkbox should be displayed next to each set.
 * When `true`, swiping a set marks it as done or as missed reps instead of removing it, and the
 * card folds itself away once every set is completed, unfolding again when one of them is undone.
 * Unless [editMode] says otherwise, it also leaves out the notes, the rest time and the weight
 * increase of the exercise, which are settings of the routine rather than something to fill in
 * while training.
 * @param editMode When `true`, the card shows the notes, the rest time and the weight increase of
 * the exercise, so they can be changed. It defaults to the opposite of [workout], but the editing of
 * a past workout shows them next to the checkboxes of its sets.
 * @param applyPreviousSetPerformance Triggered when the user clicks the previous set performance
 * (on the left to the set counter) * and should update the current set with the values of the previous set.
 */
@OptIn(
    ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class,
    ExperimentalMaterial3ExpressiveApi::class
)
@Composable
fun SharedTransitionScope.ExerciseCard(
    modifier: Modifier = Modifier,
    animatedVisibilityScope: AnimatedVisibilityScope,
    exerciseWithSets: UiExerciseWithSets,
    previousPerformances: List<PreviousPerformanceSet>? = null,
    workout: Boolean = false,
    editMode: Boolean = !workout,
    idSetWithRunningStopwatch: Long? = null,
    addSet: (Long) -> Unit,
    onDetail: (Long, String) -> Unit,
    onDelete: (Long) -> Unit,
    isCollapsed: Boolean = false,
    dragHandleModifier: Modifier = Modifier,
    supersetLink: SupersetLink? = null,
    listSpacing: Dp = 15.dp,
    isDragging: Boolean,
    useScrollWheelForInput: Boolean,
    dismissScrollWheelInputAutomatically: Boolean,
    showExercisesImages: Boolean?,
    defaultBarWeight: Double?,
    onReorderRequest: () -> Unit,
    deleteSet: (Long) -> Unit,
    updateExerciseNotes: (String, Long) -> Unit,
    updateExerciseRestTime: (Int, Long) -> Unit,
    updateExerciseSetMode: (SetMode, Long) -> Unit,
    updateExerciseSupersetGroup: (Long?, Long) -> Unit,
    updateExerciseWeightIncrement: (Weight, Long) -> Unit,
    updateSetTime: (Int, Long) -> Unit,
    updateSetReps: (Int, Long) -> Unit,
    updateSetLoad: (Weight, Long) -> Unit,
    updateSetCompleted: (Boolean, Long) -> Unit,
    updateSetFailed: (Boolean, Long) -> Unit,
    updateSetIsAmrap: (Boolean, Long) -> Unit,
    updateSetTargetReps: (Int, Long) -> Unit,
    showInfo: (InfoMode) -> Unit,
    updateIdSetWithRunningStopwatch: (Long?) -> Unit = {},
    applyPreviousSetPerformance: (Long) -> Unit = {},
    saveDefaultBarWeight: (Double) -> Unit,
) {
    val unit = autoUnitSuffix()

    val elevation by animateDpAsState(
        targetValue = if (isDragging) 10.dp else 0.dp,
        label = "drag_elevation"
    )

    var showMenu by rememberSaveable { mutableStateOf(false) }

    // A finished exercise has nothing left to fill in, so it folds away to keep the rest of the
    // workout in reach. Outside a workout no set is ever completed, so nothing folds there on its own
    val isExerciseDone = workout && exerciseWithSets.sets.isNotEmpty() &&
            exerciseWithSets.sets.all { it.completed }
    // Any card can be folded by hand with the arrow in its header. Being saved, the fold outlives
    // the card scrolling out of the list
    var savedFold by rememberSaveable { mutableStateOf(isExerciseDone) }
    var wasExerciseDone by rememberSaveable { mutableStateOf(isExerciseDone) }
    // Finishing the exercise folds it away and undoing one of its sets brings it back, whatever the
    // arrow was left at. Until the effect below catches up with such a change, the card already
    // follows it, so it does not flash open for a frame
    val isFolded = if (wasExerciseDone == isExerciseDone) savedFold else isExerciseDone
    LaunchedEffect(isExerciseDone) {
        if (wasExerciseDone != isExerciseDone) {
            wasExerciseDone = isExerciseDone
            savedFold = isExerciseDone
        }
    }
    val isBodyHidden = isCollapsed || isFolded
    val foldArrowRotation by animateFloatAsState(
        targetValue = if (isFolded) 0f else 180f,
        label = "animated_rotation_for_fold_arrow"
    )

    // The exercises of a superset are performed back to back, so their cards are painted as a
    // single block. The links are left out while reordering, since the cards are on the move
    val isSuperset = exerciseWithSets.exercise.supersetGroupId != null
    val superset = supersetColors(groupIndex = supersetLink?.groupIndex ?: 0)
    val isLinkedToPrevious = isSuperset && !isCollapsed && supersetLink?.isLinkedToPrevious == true
    val isLinkedToNext = isSuperset && !isCollapsed && supersetLink?.isLinkedToNext == true
    val containerColor by animateColorAsState(
        targetValue = if (isSuperset) {
            superset.container
        } else {
            MaterialTheme.colorScheme.surfaceContainerLow
        },
        label = "animated_color_for_card_container"
    )
    val stripeColor by animateColorAsState(
        targetValue = if (isSuperset) superset.accent else superset.accent.copy(alpha = 0f),
        label = "animated_color_for_superset_stripe"
    )

    val cardShape = MaterialTheme.shapes.extraLarge
    val shape = remember(cardShape, isLinkedToPrevious, isLinkedToNext) {
        cardShape.linkedToNeighbours(
            toPrevious = isLinkedToPrevious,
            toNext = isLinkedToNext
        )
    }
    ElevatedCard(
        modifier = modifier
            .shadow(
                elevation = elevation,
                shape = shape
            )
            .then(
                if (isLinkedToPrevious) {
                    Modifier.supersetLinkToPrevious(color = stripeColor, length = listSpacing)
                } else Modifier
            ),
        shape = shape,
        colors = CardDefaults.elevatedCardColors(
            containerColor = containerColor,
            contentColor = MaterialTheme.colorScheme.onSurface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .supersetStripe(color = stripeColor)
                .padding(15.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .clip(MaterialTheme.shapes.medium)
                        // A folded card opens up when its header is tapped, while an open one
                        // leads to the details of the exercise
                        .clickable(
                            enabled = !isCollapsed,
                            onClickLabel = if (isFolded) stringResource(R.string.show) else null
                        ) {
                            if (isFolded) {
                                savedFold = false
                            } else {
                                onDetail(exerciseWithSets.exercise.id, exerciseWithSets.exerciseDC.id)
                            }
                        },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val model =
                        remember(exerciseWithSets.exerciseDC.images) { exerciseWithSets.exerciseDC.images.firstOrNull() }
                    if (showExercisesImages == true) {
                        AsyncImage(
                            model = model?.let { "file:///android_asset/${it}" },
                            fallback = painterResource(R.drawable.no_image),
                            contentDescription = exerciseWithSets.exerciseDC.name,
                            contentScale = ContentScale.Crop,
                            colorFilter = if (model == null) ColorFilter.tint(MaterialTheme.colorScheme.onSurfaceVariant) else null,
                            modifier = Modifier
                                .padding(end = 10.dp)
                                .sharedElement(
                                    sharedContentState = rememberSharedContentState(
                                        key = exerciseWithSets.exercise.id.toString() + exerciseWithSets.exerciseDC.id
                                    ),
                                    animatedVisibilityScope = animatedVisibilityScope
                                )
                                .size(50.dp)
                                .clip(MaterialTheme.shapes.medium)
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = exerciseWithSets.exerciseDC.name,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        // A folded card still tells how far the exercise is
                        val summary = if (workout) {
                            stringResource(R.string.completed_sets) + ": " +
                                    exerciseWithSets.sets.count { it.completed } + "/" +
                                    exerciseWithSets.sets.size
                        } else {
                            stringResource(R.string.sets_summary, exerciseWithSets.sets.size)
                        }
                        if (isSuperset) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                SupersetLabel(colors = superset)
                                AnimatedVisibility(
                                    visible = isBodyHidden,
                                    modifier = Modifier.weight(1f, fill = false)
                                ) {
                                    FoldedExerciseSummary(text = summary)
                                }
                            }
                        } else {
                            AnimatedVisibility(visible = isBodyHidden) {
                                FoldedExerciseSummary(text = summary)
                            }
                        }
                    }
                }
                // Reordering folds every card anyway, so the arrow makes room for the drag handle
                AnimatedVisibility(visible = !isCollapsed) {
                    IconButton(
                        onClick = { savedFold = !isFolded }
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_drop_down),
                            contentDescription = stringResource(
                                if (isFolded) R.string.show else R.string.hide
                            ),
                            modifier = Modifier.rotate(foldArrowRotation)
                        )
                    }
                }
                Column {
                    AnimatedContent(
                        targetState = isCollapsed,
                        label = "DragHandleTransition",
                    ) { isReordering ->
                        if (isReordering) {
                            IconButton(
                                modifier = dragHandleModifier,
                                onClick = {}
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_drag_handle),
                                    contentDescription = stringResource(R.string.reorder)
                                )
                            }
                        } else {
                            IconButton(
                                onClick = { showMenu = true }
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_more_options),
                                    contentDescription = stringResource(R.string.more_options)
                                )
                            }
                            DropdownMenuPopup(
                                expanded = showMenu,
                                onDismissRequest = { showMenu = false }) {
                                DropdownMenuGroup(
                                    shapes = MenuDefaults.groupShape(0, 1) // Top-level group shape
                                ) {
                                    // MenuDefaults.Label { Text("Header") }
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.reorder)) },
                                        leadingIcon = {
                                            Icon(
                                                painterResource(R.drawable.ic_reorder),
                                                stringResource(R.string.reorder)
                                            )
                                        },
                                        onClick = {
                                            onReorderRequest()
                                            showMenu = false
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                if (exerciseWithSets.exercise.supersetGroupId != null) {
                                                    stringResource(R.string.remove_superset)
                                                } else {
                                                    stringResource(R.string.add_superset)
                                                }
                                            )
                                        },
                                        leadingIcon = {
                                            Icon(
                                                painterResource(R.drawable.ic_more_options),
                                                stringResource(R.string.add_superset)
                                            )
                                        },
                                        onClick = {
                                            updateExerciseSupersetGroup(
                                                exerciseWithSets.exercise.supersetGroupId,
                                                exerciseWithSets.exercise.id
                                            )
                                            showMenu = false
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.delete)) },
                                        leadingIcon = {
                                            Icon(
                                                painterResource(R.drawable.ic_delete),
                                                stringResource(R.string.delete)
                                            )
                                        },
                                        onClick = {
                                            onDelete(exerciseWithSets.exercise.id)
                                            showMenu = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            AnimatedVisibility(visible = !isBodyHidden) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // The settings of the exercise belong to the routine, so they are left out while
                    // training rather than crowding the sets to fill in
                    if (editMode) {
                        OutlinedTextField(
                            shape = MaterialTheme.shapes.large,
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text(text = stringResource(id = R.string.notes)) },
                            value = exerciseWithSets.exercise.notes,
                            onValueChange = { updateExerciseNotes(it, exerciseWithSets.exercise.id) }
                        )

                        //Rest timer slider
                        Column {
                            // Hoist the slider state as single source of truth
                            val sliderState = rememberSliderState(
                                value = exerciseWithSets.exercise.restTime.toFloat(),
                                trackRange = 0f..300f,
                                steps = 19 // 20 intervals -> exact multiples of 15 natively
                            )

                            // Keep state synced if restTime is modified externally (e.g., from the ViewModel)
                            LaunchedEffect(exerciseWithSets.exercise.restTime) {
                                if (!sliderState.isDragging) {
                                    sliderState.value = exerciseWithSets.exercise.restTime.toFloat()
                                }
                            }

                            var showSlider by rememberSaveable { mutableStateOf(false) }
                            val haptic = LocalHapticFeedback.current

                            // Manually trigger haptics when the discrete step changes during a drag
                            LaunchedEffect(sliderState.value) {
                                if (sliderState.isDragging) {
                                    haptic.performHapticFeedback(HapticFeedbackType.SegmentFrequentTick)
                                }
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceAround,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    IconButton(
                                        // Read more at InfoModalBottomSheet
                                        onClick = { showInfo(InfoMode.REST_TIMER) }
                                    ) {
                                        Icon(
                                            painter = painterResource(R.drawable.ic_info),
                                            contentDescription = stringResource(R.string.info)
                                        )
                                    }
                                    Text(
                                        stringResource(R.string.rest_time) + ": " + sliderState.value.roundToInt()
                                                + " " + stringResource(R.string.seconds).replaceFirstChar { it.lowercase() }
                                    )
                                }
                                IconToggleButton(
                                    checked = showSlider,
                                    onCheckedChange = {
                                        showSlider = it
                                        haptic.performHapticFeedback(if (it) HapticFeedbackType.ToggleOn else HapticFeedbackType.ToggleOff)
                                    }
                                ) {
                                    Icon(
                                        painter = painterResource(if (showSlider) R.drawable.ic_check else R.drawable.ic_edit),
                                        contentDescription = stringResource(if (showSlider) R.string.save else R.string.edit)
                                    )
                                }
                            }
                            AnimatedVisibility(visible = showSlider) {
                                Slider(
                                    state = sliderState,
                                    onValueChangeFinished = {
                                        // Only hit the ViewModel when the user finishes dragging/clicking
                                        updateExerciseRestTime(
                                            sliderState.value.roundToInt(),
                                            exerciseWithSets.exercise.id
                                        )
                                    }
                                )
                            }
                        }

                        HorizontalDivider()
                    }

                    // The type of set belongs to the routine like the other settings of the
                    // exercise, so it is only picked while editing
                    if (editMode) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceAround,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                modifier = Modifier.weight(0.5f),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                IconButton(
                                    // Refer to InfoModalBottomSheet to know the reason behind this value.
                                    // Do NOT change it.
                                    onClick = { showInfo(InfoMode.TYPE_OF_SET) }
                                ) {
                                    Icon(
                                        painter = painterResource(R.drawable.ic_info),
                                        contentDescription = stringResource(R.string.info) + ":"
                                    )
                                }
                                Text(stringResource(R.string.type_of_set))
                            }

                            var expanded by remember { mutableStateOf(false) }


                            // Type of set selector
                            ExposedDropdownMenuBox(
                                expanded = expanded,
                                onExpandedChange = { expanded = it },
                                modifier = Modifier
                                    .padding(start = 10.dp, end = 10.dp)
                                    .weight(0.5f)
                            ) {
                                OutlinedTextField(
                                    shape = MaterialTheme.shapes.large,
                                    readOnly = true,
                                    value = stringResource(Formatter.setModeToStringId(exerciseWithSets.exercise.setMode)),
                                    onValueChange = {},
                                    singleLine = true,
                                    trailingIcon = {
                                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                                    },
                                    modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
                                    colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
                                )
                                ExposedDropdownMenu(
                                    expanded = expanded,
                                    onDismissRequest = { expanded = false },
                                    // Allow DropdownMenuGroup to control styling, shape, and elevation
                                    containerColor = Color.Transparent,
                                    shadowElevation = 0.dp,
                                    border = null
                                ) {
                                    // Wrap items inside Expressive DropdownMenuGroup
                                    DropdownMenuGroup(
                                        shapes = MenuDefaults.groupShape(0, 1)
                                    ) {
                                        val itemCount = SetMode.entries.size

                                        SetMode.entries.forEachIndexed { index, mode ->
                                            val isSelected = mode == exerciseWithSets.exercise.setMode

                                            CheckableDropdownMenuItem(
                                                checked = isSelected,
                                                onCheckedChange = {
                                                    updateExerciseSetMode(
                                                        mode,
                                                        exerciseWithSets.exercise.id
                                                    )
                                                    expanded = false
                                                },
                                                text = {
                                                    Text(
                                                        text = stringResource(
                                                            Formatter.setModeToStringId(
                                                                mode
                                                            )
                                                        )
                                                    )
                                                },
                                                trailingContent = if (exerciseWithSets.exercise.setMode == mode) {
                                                    {
                                                        Icon(
                                                            painter = painterResource(R.drawable.ic_check),
                                                            contentDescription = stringResource(R.string.checkbox)
                                                        )
                                                    }
                                                } else null,
                                                // Expressive rounded shapes per item position in group
                                                shapes = MenuDefaults.itemShape(index, itemCount),
                                            )
                                        }

                                    }
                                }
                            }
                        }
                    }

                    // Progressive overload. It is meaningful only for the set modes carrying a load,
                    // and like the other settings of the exercise it is only changed while editing
                    if (editMode) {
                        AnimatedVisibility(visible = exerciseWithSets.exercise.setMode.hasLoad) {
                            WeightIncrementRow(
                                weightIncrement = exerciseWithSets.exercise.weightIncrement,
                                decreasing = exerciseWithSets.exercise.setMode == SetMode.ASSISTED_BODYWEIGHT,
                                onWeightIncrementChange = { newIncrement ->
                                    updateExerciseWeightIncrement(
                                        newIncrement,
                                        exerciseWithSets.exercise.id
                                    )
                                },
                                showInfo = showInfo
                            )
                        }
                    }

                    ElevatedCard(
                        shape = MaterialTheme.shapes.extraLarge,
                        colors = CardDefaults.elevatedCardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest
                        )
                    ) {
                        //Headline set
                        Row(
                            modifier = Modifier
                                .padding(10.dp)
                                .fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Spacer(Modifier)
                            if (previousPerformances != null) {
                                Text(
                                    text = stringResource(R.string.previous),
                                    color = MaterialTheme.colorScheme.secondary
                                )
                            }
                            if (exerciseWithSets.exercise.setMode == SetMode.DURATION) {
                                Text(
                                    text = stringResource(R.string.time),
                                    color = MaterialTheme.colorScheme.secondary
                                )
                            } else {
                                if (exerciseWithSets.exercise.setMode.hasLoad) {
                                    Text(
                                        text = stringResource(
                                            if (exerciseWithSets.exercise.setMode == SetMode.ASSISTED_BODYWEIGHT) {
                                                R.string.support
                                            } else R.string.load
                                        ) + " (" + unit + ")",
                                        color = MaterialTheme.colorScheme.secondary
                                    )
                                }
                                Text(
                                    text = stringResource(id = R.string.reps),
                                    color = MaterialTheme.colorScheme.secondary
                                )
                            }
                            if (workout) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_check),
                                    contentDescription = stringResource(R.string.done)
                                )
                            }
                        }

                        //Sets
                        Column(modifier = Modifier.animateContentSize()) {
                            exerciseWithSets.sets.forEachIndexed { i, set ->
                                key(set.id) {
                                    Set(
                                        i = i,
                                        set = set,
                                        previousSet = previousPerformances?.getOrNull(i),
                                        lastIndex = exerciseWithSets.sets.lastIndex,
                                        setMode = exerciseWithSets.exercise.setMode,
                                        isStopwatchRunning = idSetWithRunningStopwatch == null,
                                        isThisSetStopwatchRunning = idSetWithRunningStopwatch == set.id,
                                        workout = workout,
                                        useScrollWheelForInput = useScrollWheelForInput,
                                        dismissScrollWheelInputAutomatically = dismissScrollWheelInputAutomatically,
                                        deleteSet = deleteSet,
                                        updateIdSetWithRunningStopwatch = updateIdSetWithRunningStopwatch,
                                        updateSetTime = updateSetTime,
                                        updateSetReps = updateSetReps,
                                        updateSetLoad = updateSetLoad,
                                        updateSetCompleted = updateSetCompleted,
                                        updateSetFailed = updateSetFailed,
                                        updateSetIsAmrap = updateSetIsAmrap,
                                        updateSetTargetReps = updateSetTargetReps,
                                        applyPreviousSet = applyPreviousSetPerformance
                                    )
                                }
                            }
                        }
                    }

                    //Add set button + barbell calculator (if exercise requires barbell)

                    if (exerciseWithSets.exerciseDC.equipment != Equipment.BARBELL) {
                        LibreFitButton(
                            text = stringResource(id = R.string.add_set),
                            icon = painterResource(R.drawable.ic_add_circle),
                            onClick = { addSet(exerciseWithSets.exercise.id) },
                            elevated = false
                        )
                    } else {
                        var showBarbellCalculator by rememberSaveable { mutableStateOf(false) }

                        if (showBarbellCalculator) {
                            val lastSet = exerciseWithSets.sets.lastOrNull { !it.completed }
                                ?: exerciseWithSets.sets.lastOrNull()

                            BarbellCalculatorModalBottomSheet(
                                initialTargetWeight = lastSet?.load ?: Weight.auto(50.0),
                                defaultBarWeight = defaultBarWeight,
                                onSaveDefaultBarWeight = saveDefaultBarWeight
                            ) {
                                showBarbellCalculator = false
                            }
                        }

                        val interactionSources = remember { List(2) { MutableInteractionSource() } }
                        ButtonGroup(
                            overflowIndicator = {}
                        ) {
                            customItem(
                                buttonGroupContent = {
                                    OutlinedIconButton(
                                        onClick = {
                                            showBarbellCalculator = true
                                        },
                                        shapes = IconButtonDefaults.shapes(),
                                        interactionSource = interactionSources[0],
                                        modifier = Modifier.animateWidth(interactionSources[0])
                                    ) {
                                        Icon(
                                            painter = painterResource(R.drawable.ic_barbell),
                                            contentDescription = stringResource(R.string.barbell_calculator)
                                        )
                                    }
                                },
                                menuContent = {}
                            )
                            customItem(
                                buttonGroupContent = {
                                    LibreFitButton(
                                        text = stringResource(id = R.string.add_set),
                                        icon = painterResource(R.drawable.ic_add_circle),
                                        onClick = { addSet(exerciseWithSets.exercise.id) },
                                        elevated = false,
                                        interactionSource = interactionSources[1],
                                        modifier = Modifier
                                            .weight(1f)
                                            .animateWidth(interactionSources[1])
                                    )
                                },
                                menuContent = {}
                            )
                        }
                    }


                }
            }
        }
    }
}

/**
 * The one line taking the place of the body of a folded [ExerciseCard]
 */
@Composable
private fun FoldedExerciseSummary(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
    )
}

/**
 * Where an exercise stands within its superset, i.e. among the exercises of the list sharing its
 * [UiExercise.supersetGroupId]. Refer to [supersetLinks] to compute it.
 *
 * @param groupIndex The order of the superset among the ones of the list. It alternates the shade
 * of blue the superset is painted with, so two supersets sitting next to each other are told apart.
 * @param isLinkedToPrevious `true` when the previous exercise of the list belongs to the same superset
 * @param isLinkedToNext `true` when the next exercise of the list belongs to the same superset
 */
@Immutable
data class SupersetLink(
    val groupIndex: Int,
    val isLinkedToPrevious: Boolean,
    val isLinkedToNext: Boolean
)

/**
 * Returns the [SupersetLink] of every exercise of the list, in the same order, or `null` for the
 * exercises not belonging to any superset.
 */
fun List<UiExerciseWithSets>.supersetLinks(): List<SupersetLink?> {
    val groupIds = mapNotNull { it.exercise.supersetGroupId }.distinct()

    return mapIndexed { i, exerciseWithSets ->
        exerciseWithSets.exercise.supersetGroupId?.let { groupId ->
            SupersetLink(
                groupIndex = groupIds.indexOf(groupId),
                isLinkedToPrevious = getOrNull(i - 1)?.exercise?.supersetGroupId == groupId,
                isLinkedToNext = getOrNull(i + 1)?.exercise?.supersetGroupId == groupId
            )
        }
    }
}

/**
 * The colours a card of a superset is painted with, refer to [supersetColors]
 *
 * @param container The colour of the card itself
 * @param accent The colour of the stripe running along the card and linking it to its neighbours
 * @param label The colour of the text of [SupersetLabel]
 */
@Immutable
internal data class SupersetColors(
    val container: Color,
    val accent: Color,
    val label: Color
)

/**
 * Blends [supersetBlue], or [supersetBlueAlternate] for every other superset, into [base], so a
 * card of a superset is tinted gently whatever the colour scheme, dynamic colours included. On dark
 * surfaces the blue is lightened, so it keeps standing out.
 *
 * @param groupIndex Refer to [SupersetLink.groupIndex]
 * @param base The colour the card has outside of a superset
 */
@Composable
internal fun supersetColors(
    groupIndex: Int,
    base: Color = MaterialTheme.colorScheme.surfaceContainerLow
): SupersetColors = remember(groupIndex, base) {
    val blue = if (groupIndex % 2 == 0) supersetBlue else supersetBlueAlternate

    if (base.luminance() < 0.5f) {
        SupersetColors(
            container = lerp(base, blue, 0.18f),
            accent = lerp(blue, Color.White, 0.4f),
            label = lerp(blue, Color.White, 0.6f)
        )
    } else {
        SupersetColors(
            container = lerp(base, blue, 0.12f),
            accent = blue,
            label = lerp(blue, Color.Black, 0.35f)
        )
    }
}

/**
 * The badge telling that the exercise of a card is part of a superset
 */
@Composable
internal fun SupersetLabel(
    colors: SupersetColors,
    modifier: Modifier = Modifier
) {
    Text(
        text = stringResource(R.string.superset),
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.SemiBold,
        color = colors.label,
        maxLines = 1,
        modifier = modifier
            .clip(CircleShape)
            .background(colors.accent.copy(alpha = 0.2f))
            .padding(horizontal = 8.dp, vertical = 2.dp)
    )
}

/**
 * The width of the stripe running along the start edge of the cards of a superset
 */
internal val SUPERSET_STRIPE_WIDTH = 5.dp

/**
 * The rounding left to the corners of a card joining another card of the same superset
 */
internal val SUPERSET_LINKED_CORNER_SIZE = 8.dp

/**
 * Returns the receiver with the corners joining another card of the same superset tightened, so
 * the cards of a superset read as a single block.
 *
 * @param toPrevious When `true`, the top corners are tightened
 * @param toNext When `true`, the bottom corners are tightened
 */
internal fun CornerBasedShape.linkedToNeighbours(
    toPrevious: Boolean,
    toNext: Boolean
): CornerBasedShape {
    if (!toPrevious && !toNext) return this

    val linkedCorner = CornerSize(SUPERSET_LINKED_CORNER_SIZE)
    return copy(
        topStart = if (toPrevious) linkedCorner else topStart,
        topEnd = if (toPrevious) linkedCorner else topEnd,
        bottomEnd = if (toNext) linkedCorner else bottomEnd,
        bottomStart = if (toNext) linkedCorner else bottomStart
    )
}

/**
 * Paints the stripe of a superset along the start edge of the receiver, which has to be clipped to
 * the shape of the card. When the receiver is the padded content of the card, [outsetStart] and
 * [outsetVertical] reach past the padding up to the edges of the card: anything drawn past them is
 * clipped away. Nothing is drawn when [color] is fully transparent.
 */
internal fun Modifier.supersetStripe(
    color: Color,
    outsetStart: Dp = 0.dp,
    outsetVertical: Dp = 0.dp
): Modifier = drawBehind {
    if (color.alpha == 0f) return@drawBehind

    val width = SUPERSET_STRIPE_WIDTH.toPx()
    val start = outsetStart.toPx()
    val vertical = outsetVertical.toPx()
    drawRect(
        color = color,
        topLeft = Offset(
            x = if (layoutDirection == LayoutDirection.Ltr) -start else size.width + start - width,
            y = -vertical
        ),
        size = Size(width = width, height = size.height + 2 * vertical)
    )
}

/**
 * Carries the stripe of a superset across the gap of [length] above the receiver, joining the card
 * to the previous one of the same superset. It reaches [SUPERSET_LINKED_CORNER_SIZE] into both cards
 * to fill the notches their tightened corners leave next to the stripes, so it has to be applied
 * outside of the clip of the card, where it is drawn behind the card itself.
 */
internal fun Modifier.supersetLinkToPrevious(
    color: Color,
    length: Dp
): Modifier = drawBehind {
    if (color.alpha == 0f) return@drawBehind

    val width = SUPERSET_STRIPE_WIDTH.toPx()
    val gap = length.toPx()
    val overlap = SUPERSET_LINKED_CORNER_SIZE.toPx()
    drawRect(
        color = color,
        topLeft = Offset(
            x = if (layoutDirection == LayoutDirection.Ltr) 0f else size.width - width,
            y = -gap - overlap
        ),
        size = Size(width = width, height = gap + 2 * overlap)
    )
}

/**
 * The control letting the user configure [UiExercise.weightIncrement], i.e. how much load is added
 * to the suggestion for the next session once every set of the exercise has been completed without
 * being flagged as missed reps. A value of zero disables the progression.
 *
 * @param weightIncrement The currently configured increment.
 * @param onWeightIncrementChange Invoked with the new increment. It is never negative.
 * @param showInfo Refer to [org.librefit.ui.components.modalBottomSheets.InfoModalBottomSheet]
 */
@Composable
private fun WeightIncrementRow(
    weightIncrement: Weight,
    decreasing: Boolean,
    onWeightIncrementChange: (Weight) -> Unit,
    showInfo: (InfoMode) -> Unit
) {
    val unitSystem = LocalUnitSystem.current
    val unit = autoUnitSuffix()

    // Typing writes the increment back on every keystroke, so the text is kept as it is typed
    // rather than rebuilt from the stored value, which would turn "1" into "1.0" at once
    var incrementValue by rememberSaveable {
        mutableStateOf(weightIncrement.toInputText(unitSystem))
    }
    // The text is rebuilt only when the increment was changed from somewhere else, i.e. the
    // steppers or a change of unit system
    LaunchedEffect(weightIncrement, unitSystem) {
        if (!weightIncrement.isRepresentedBy(incrementValue, unitSystem)) {
            incrementValue = weightIncrement.toInputText(unitSystem)
        }
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(
                // Read more at InfoModalBottomSheet
                onClick = { showInfo(InfoMode.WEIGHT_INCREASE) }
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_info),
                    contentDescription = stringResource(R.string.info)
                )
            }
            // For an assisted exercise progressing means less support, so the step is shown as the
            // amount it goes down by, refer to WeightProgression.suggestedLoad
            Text(
                stringResource(if (decreasing) R.string.support_decrease else R.string.weight_increase) +
                        " (" + unit + ")"
            )
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(
                onClick = {
                    onWeightIncrementChange(
                        weightIncrement.stepIncrement(
                            up = false,
                            unitSystem = unitSystem,
                            maxValueInUnit = MAX_WEIGHT_INCREMENT
                        )
                    )
                },
                modifier = Modifier.size(28.dp)
            ) {
                Text("-", color = MaterialTheme.colorScheme.onSurface)
            }
            OutlinedTextField(
                shape = MaterialTheme.shapes.large,
                modifier = Modifier.width(80.dp),
                value = incrementValue,
                onValueChange = { string ->
                    // Pounds are whole numbers, so only digits are kept for them
                    incrementValue = normalizeWeightInput(string, unitSystem)

                    onWeightIncrementChange(
                        Weight.auto(
                            (parseWeightInput(incrementValue, unitSystem) ?: 0.0)
                                .coerceIn(0.0, MAX_WEIGHT_INCREMENT),
                            unitSystem
                        )
                    )
                },
                singleLine = true,
                prefix = if (decreasing) {
                    { Text("\u2212") }
                } else null,
                keyboardOptions = KeyboardOptions(keyboardType = weightKeyboardType(unitSystem)),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedBorderColor = Color.Transparent,
                    focusedBorderColor = Color.Transparent,
                    disabledBorderColor = Color.Transparent
                )
            )
            IconButton(
                onClick = {
                    onWeightIncrementChange(
                        weightIncrement.stepIncrement(
                            up = true,
                            unitSystem = unitSystem,
                            maxValueInUnit = MAX_WEIGHT_INCREMENT
                        )
                    )
                },
                modifier = Modifier.size(28.dp)
            ) {
                Text("+", color = MaterialTheme.colorScheme.onSurface)
            }
        }
    }
}

/**
 * The largest increment that can be configured, expressed in the unit of the current
 * [UnitSystem]. Anything above it is a typo rather than an intention, and [Weight] would throw for
 * values outside of its own range. The steppers keep within it as well: they move the increment by
 * [Weight.Companion.incrementStep] on the grid of the unit, so pounds stay whole numbers, refer to
 * [Weight.stepIncrement].
 */
private const val MAX_WEIGHT_INCREMENT = 1000.0

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun Set(
    i: Int,
    set: UiSet,
    previousSet: PreviousPerformanceSet? = null,
    lastIndex: Int,
    setMode: SetMode,
    isStopwatchRunning: Boolean,
    isThisSetStopwatchRunning: Boolean,
    workout: Boolean,
    useScrollWheelForInput: Boolean,
    dismissScrollWheelInputAutomatically: Boolean,
    deleteSet: (Long) -> Unit,
    updateSetTime: (Int, Long) -> Unit,
    updateSetReps: (Int, Long) -> Unit,
    updateSetLoad: (Weight, Long) -> Unit,
    updateSetCompleted: (Boolean, Long) -> Unit,
    updateSetFailed: (Boolean, Long) -> Unit,
    updateSetIsAmrap: (Boolean, Long) -> Unit,
    updateSetTargetReps: (Int, Long) -> Unit,
    updateIdSetWithRunningStopwatch: (Long?) -> Unit,
    applyPreviousSet: (Long) -> Unit
) {
    val unitSystem = LocalUnitSystem.current
    val focusManager = LocalFocusManager.current

    // The progression rewards beating the target only where a load can actually go up
    val isAmrapAvailable = setMode.hasLoad

    val timeTextFieldState = rememberTextFieldState(
        initialText = Formatter.formateSecondsInMinutesAndSeconds(set.elapsedTime)
            .filter { it != ':' }
    )
    var repValue by rememberSaveable(set.reps) { mutableStateOf(set.reps.toString()) }
    // Keyed on the set only, as the text has to outlive the changes of load it causes itself: what
    // is typed is kept as it is, an empty field or a trailing separator ("60.") included, instead
    // of being rebuilt from the stored value, which would turn "1" into "1.0" at once
    var weightValue by rememberSaveable(set.id) {
        mutableStateOf(set.load.toInputText(unitSystem))
    }
    // Typing writes the load back on every keystroke, so the text is rebuilt only when the load
    // was changed from somewhere else: the steppers, the previous performance, the scroll wheel or
    // a change of unit system
    LaunchedEffect(set.load, unitSystem) {
        if (!set.load.isRepresentedBy(weightValue, unitSystem)) {
            weightValue = set.load.toInputText(unitSystem)
        }
    }

    // Sync elapsed time with time text field
    LaunchedEffect(set.elapsedTime) {
        val formatted =
            Formatter.formateSecondsInMinutesAndSeconds(set.elapsedTime).filter { it != ':' }
        if (timeTextFieldState.text.toString() != formatted) {
            timeTextFieldState.setTextAndPlaceCursorAtEnd(formatted)
        }
    }

    // Sync time text field with elapsed time
    LaunchedEffect(timeTextFieldState) {
        snapshotFlow { timeTextFieldState.text.toString() }.collectLatest { rawText ->
            val padded = rawText.padStart(4, '0')
            val seconds = padded.takeLast(2)
            val minutes = padded.dropLast(2).takeLast(2)
            val newValue = Formatter.parseTimeInputToSeconds(
                input = "$minutes:$seconds"
            )
            if (newValue != set.elapsedTime) {
                updateSetTime(newValue, set.id)
            }
        }
    }

    val swipeToDismissBoxState = rememberSwipeToDismissBoxState()

    // Ticking off an AMRAP set asks how far it actually went: such a set is about beating the
    // target rather than hitting the planned repetitions, which is only known once it is over
    var showAmrapRepsDialog by remember { mutableStateOf(false) }

    // A set is logged the same way whether it is ticked off or swiped
    val stopStopwatchIfRunning = {
        if (isThisSetStopwatchRunning) updateIdSetWithRunningStopwatch(null)
    }
    val markSetCompleted = {
        if (set.isAmrap && isAmrapAvailable) {
            showAmrapRepsDialog = true
        } else {
            updateSetCompleted(true, set.id)
        }
    }
    val clearSet = {
        updateSetFailed(false, set.id)
        updateSetCompleted(false, set.id)
    }

    var inputModalBottomSheetState by remember { mutableStateOf<InputModalBottomSheetState?>(null) }
    var inputSetId by rememberSaveable { mutableStateOf<Long?>(null) }

    inputModalBottomSheetState?.let {
        InputModalBottomSheet(
            state = it,
            onValueChange = { newState ->
                inputModalBottomSheetState = newState
                inputSetId?.let { id ->
                    when (newState) {
                        is InputModalBottomSheetState.Weight -> {
                            updateSetLoad(newState.toWeight(unitSystem), id)
                        }

                        is InputModalBottomSheetState.Reps -> {
                            updateSetReps(newState.reps, id)
                        }

                        is InputModalBottomSheetState.MinutesSeconds -> {
                            updateSetTime(newState.totalSeconds, id)
                        }

                        else -> error("newState in ExerciseCard should not have this value: $newState")
                    }
                }
            },
            onDismiss = {
                inputModalBottomSheetState = null
                inputSetId = null
            },
            dismissAutomatically = dismissScrollWheelInputAutomatically
        )
    }

    if (showAmrapRepsDialog) {
        AmrapRepsDialog(
            reps = set.reps,
            targetReps = set.targetReps,
            onConfirm = { performedReps ->
                showAmrapRepsDialog = false
                updateSetReps(performedReps, set.id)
                updateSetCompleted(true, set.id)
            },
            // Backing out leaves the set open, since it is not known how it went
            onDismiss = { showAmrapRepsDialog = false }
        )
    }

    val haptic = LocalHapticFeedback.current
    LaunchedEffect(swipeToDismissBoxState.currentValue) {
        if (swipeToDismissBoxState.currentValue != SwipeToDismissBoxValue.Settled) {
            haptic.performHapticFeedback(HapticFeedbackType.GestureThresholdActivate)
        }
    }

    // While training, the row is a log of what happened rather than a plan being written, so a
    // swipe marks how the set went and the row returns to its place. Removing it lives in the set
    // dialog instead. Outside a workout there is nothing to mark, so the swipe still removes it.
    // The settled value is the only key here: the box calls its onDismiss again on every
    // recomposition, which would toggle the set a second time
    LaunchedEffect(swipeToDismissBoxState.settledValue) {
        val swipe = swipeToDismissBoxState.settledValue
        if (!workout || swipe == SwipeToDismissBoxValue.Settled) return@LaunchedEffect

        // The row comes home before anything is logged: completing the last set folds the whole
        // card away, which would take this effect with it and leave the row remembered as swiped
        swipeToDismissBoxState.reset()

        stopStopwatchIfRunning()
        when (swipe) {
            // Swiping forwards logs the set as done, and takes it back when it already was
            SwipeToDismissBoxValue.StartToEnd ->
                if (set.completed) clearSet() else markSetCompleted()

            // Swiping backwards is the second tap of the checkbox: the set was trained, but short
            // of the planned repetitions
            else -> updateSetFailed(!set.failed, set.id)
        }
    }

    SwipeToDismissBox(
        state = swipeToDismissBoxState,
        // While training, the swipe marks how the set went instead, refer to the effect above
        onDismiss = { if (!workout) deleteSet(set.id) },
        backgroundContent = {
            val direction = swipeToDismissBoxState.dismissDirection
            // Swiping a set forwards is about to mark it as done, which is the only swipe not
            // carrying bad news
            val isMarkingAsDone = workout && direction == SwipeToDismissBoxValue.StartToEnd
            val cornerShape = remember(i, lastIndex) {
                RoundedCornerShape(
                    topStart = CornerSize(if (i == 0) 50 else 0),
                    topEnd = CornerSize(if (i == 0) 50 else 0),
                    bottomEnd = CornerSize(if (i == lastIndex) 50 else 0),
                    bottomStart = CornerSize(if (i == lastIndex) 50 else 0),
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(cornerShape)
                    .background(
                        when {
                            direction == SwipeToDismissBoxValue.Settled -> Color.Transparent
                            isMarkingAsDone -> MaterialTheme.colorScheme.tertiaryContainer
                            else -> MaterialTheme.colorScheme.errorContainer
                        }
                    )
                    .padding(start = 10.dp, end = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = when (direction) {
                    SwipeToDismissBoxValue.EndToStart -> Arrangement.End
                    SwipeToDismissBoxValue.Settled -> Arrangement.Start
                    SwipeToDismissBoxValue.StartToEnd -> Arrangement.Start
                }
            ) {
                val actionIcon = when {
                    !workout -> R.drawable.ic_delete
                    isMarkingAsDone -> R.drawable.ic_check
                    else -> R.drawable.ic_cancel
                }
                val actionDescription = when {
                    !workout -> R.string.delete
                    isMarkingAsDone -> R.string.done
                    else -> R.string.missed_reps
                }
                Icon(
                    painter = painterResource(actionIcon),
                    contentDescription = stringResource(actionDescription),
                    tint = if (isMarkingAsDone) {
                        MaterialTheme.colorScheme.onTertiaryContainer
                    } else {
                        MaterialTheme.colorScheme.onErrorContainer
                    }
                )
            }
        }
    ) {
        val backgroundColor by animateColorAsState(
            targetValue = when {
                set.failed -> MaterialTheme.colorScheme.errorContainer
                set.completed -> MaterialTheme.colorScheme.tertiaryContainer
                else -> MaterialTheme.colorScheme.surfaceContainerHighest
            },
            label = "animated_color_for_set_background"
        )
        val contentColor by animateColorAsState(
            targetValue = when {
                set.failed -> MaterialTheme.colorScheme.onErrorContainer
                set.completed -> MaterialTheme.colorScheme.onTertiaryContainer
                else -> MaterialTheme.colorScheme.onSurface
            },
            label = "animated_color_for_set_content"
        )
        Row(
            modifier = Modifier
                .clip(
                    RoundedCornerShape(
                        topStart = CornerSize(if (i == 0) 45 else 0),
                        topEnd = CornerSize(if (i == 0) 45 else 0),
                        bottomEnd = CornerSize(
                            if (i == lastIndex) 45 else 0
                        ),
                        bottomStart = CornerSize(
                            if (i == lastIndex) 45 else 0
                        ),
                    )
                )
                .background(backgroundColor)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            var showSetMenu by remember { mutableStateOf(false) }

            Box(modifier = Modifier.padding(start = 12.dp)) {
                Column(
                    modifier = Modifier
                        .clip(MaterialTheme.shapes.medium)
                        .clickable { showSetMenu = true }
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "${i + 1}",
                        color = contentColor
                    )
                    // An AMRAP set is taken to the limit, so the target to beat is worth showing.
                    // A set mode carrying no load ignores the flag, so it does not advertise it
                    // either, leaving no label the user cannot reach the menu to remove
                    if (set.isAmrap && isAmrapAvailable) {
                        Text(
                            text = stringResource(R.string.amrap),
                            color = contentColor,
                            style = MaterialTheme.typography.labelSmall
                        )
                        if (set.targetReps > 0) {
                            Text(
                                text = stringResource(R.string.amrap_target, set.targetReps),
                                color = MaterialTheme.colorScheme.primary,
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                }
                if (showSetMenu) {
                    SetSettingsDialog(
                        isAmrap = set.isAmrap,
                        targetReps = set.targetReps,
                        isAmrapAvailable = isAmrapAvailable,
                        onDelete = {
                            showSetMenu = false
                            deleteSet(set.id)
                        },
                        onConfirm = { isAmrap, targetReps ->
                            showSetMenu = false
                            updateSetIsAmrap(isAmrap, set.id)
                            updateSetTargetReps(targetReps, set.id)
                        },
                        onDismiss = { showSetMenu = false }
                    )
                }
            }

            previousSet?.let { values ->
                TextButton(
                    onClick = { applyPreviousSet(set.id) },
                    modifier = Modifier.wrapContentWidth()
                ) {
                    val (previousReps, previousLoad, previousTime) = values
                    val text = when (setMode) {
                        SetMode.LOAD -> "${previousLoad.formatToText()}\n* $previousReps"
                        SetMode.BODYWEIGHT -> "$previousReps"
                        SetMode.BODYWEIGHT_WITH_LOAD, SetMode.ASSISTED_BODYWEIGHT ->
                            "${previousLoad.formatToText()}\n* $previousReps"
                        SetMode.DURATION -> Formatter.formateSecondsInMinutesAndSeconds(previousTime)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = text,
                            color = contentColor,
                            textAlign = TextAlign.Center,
                        )
                        // The last session was fully completed, so the load goes up this time
                        values.suggestedLoad?.let { suggestedLoad ->
                            Text(
                                text = "\u2192 " + suggestedLoad.formatToText(),
                                color = MaterialTheme.colorScheme.primary,
                                style = MaterialTheme.typography.labelSmall,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }

            if (setMode == SetMode.DURATION) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (workout) {

                        IconButton(
                            enabled = (isStopwatchRunning || isThisSetStopwatchRunning)
                                    && !set.completed,
                            colors = IconButtonDefaults.iconButtonColors(
                                contentColor = contentColor
                            ),
                            onClick = {
                                val newId = if (isThisSetStopwatchRunning) null else set.id
                                updateIdSetWithRunningStopwatch(newId)
                            }
                        ) {
                            Icon(
                                painter = painterResource(
                                    if (isThisSetStopwatchRunning)
                                        R.drawable.ic_pause else R.drawable.ic_play_arrow
                                ),
                                contentDescription = if (isThisSetStopwatchRunning)
                                    stringResource(R.string.resume) else
                                    stringResource(R.string.pause)
                            )
                        }
                    }
                    //Time
                    Box {
                        OutlinedTextField(
                            shape = MaterialTheme.shapes.large,
                            modifier = Modifier.width(80.dp),
                            state = timeTextFieldState,
                            lineLimits = TextFieldLineLimits.SingleLine,
                            inputTransformation = TimeInputTransformation(),
                            outputTransformation = TimeOutputTransformation(),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Number,
                                imeAction = ImeAction.Done
                            ),
                            // The value is written as it is typed, so there is nothing left to
                            // confirm: the field just gets out of the way
                            onKeyboardAction = { focusManager.clearFocus() },
                            colors = OutlinedTextFieldDefaults.colors(
                                unfocusedBorderColor = Color.Transparent,
                                focusedBorderColor = Color.Transparent,
                                disabledBorderColor = Color.Transparent,
                                focusedTextColor = contentColor,
                                unfocusedTextColor = contentColor,
                            ),
                            readOnly = useScrollWheelForInput
                        )
                        if (useScrollWheelForInput) {
                            Box(
                                modifier = Modifier
                                    .matchParentSize()
                                    .clip(MaterialTheme.shapes.extraLarge)
                                    .clickable {
                                        set.elapsedTime.seconds.toComponents { _, minutes, seconds, _ ->
                                            inputModalBottomSheetState =
                                                InputModalBottomSheetState.MinutesSeconds(
                                                    minutes = minutes,
                                                    seconds = seconds
                                                )
                                        }
                                        inputSetId = set.id
                                    }
                            ) { }
                        }
                    }
                }
            } else {
                if (setMode.hasLoad) {
                    //Weight
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (!useScrollWheelForInput) {
                            IconButton(
                                onClick = {
                                    // 5 lb on whole pounds, or about 1.134 kg as it has always been
                                    val newLoad = set.load.stepLoad(
                                        up = false,
                                        unitSystem = unitSystem
                                    )
                                    updateSetLoad(newLoad, set.id)
                                },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Text("-")
                            }
                        }
                        Box {
                            OutlinedTextField(
                                shape = MaterialTheme.shapes.large,
                                modifier = Modifier.width(80.dp),
                                value = weightValue,
                                onValueChange = { string ->
                                    // Pounds are whole numbers, so only digits are kept for them
                                    weightValue = normalizeWeightInput(string, unitSystem)

                                    updateSetLoad(
                                        Weight.auto(
                                            parseWeightInput(weightValue, unitSystem) ?: 0.0,
                                            unitSystem
                                        ),
                                        set.id
                                    )
                                },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = weightKeyboardType(unitSystem),
                                    imeAction = ImeAction.Done
                                ),
                                keyboardActions = KeyboardActions(
                                    onDone = { focusManager.clearFocus() }
                                ),
                                colors = OutlinedTextFieldDefaults.colors(
                                    unfocusedBorderColor = Color.Transparent,
                                    focusedBorderColor = Color.Transparent,
                                    disabledBorderColor = Color.Transparent,
                                    focusedTextColor = contentColor,
                                    unfocusedTextColor = contentColor,
                                ),
                                readOnly = useScrollWheelForInput
                            )
                            if (useScrollWheelForInput) {
                                Box(
                                    modifier = Modifier
                                        .matchParentSize()
                                        .clip(MaterialTheme.shapes.extraLarge)
                                        .clickable {
                                            // The wheels open on the load as it is shown, e.g.
                                            // 150 lb rather than 149.99, and offer no decimal part
                                            // for pounds, which are whole numbers
                                            inputModalBottomSheetState =
                                                InputModalBottomSheetState.Weight.fromWeight(
                                                    weight = set.load,
                                                    unitSystem = unitSystem
                                                )
                                            inputSetId = set.id
                                        }
                                ) { }
                            }
                        }
                        if (!useScrollWheelForInput) {
                            IconButton(
                                onClick = {
                                    val newLoad = set.load.stepLoad(
                                        up = true,
                                        unitSystem = unitSystem
                                    )
                                    updateSetLoad(newLoad, set.id)
                                },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Text("+")
                            }
                        }
                    }
                }
                //Reps
                Box {
                    OutlinedTextField(
                        shape = MaterialTheme.shapes.large,
                        modifier = Modifier.width(80.dp),
                        value = repValue,
                        onValueChange = { string ->
                            repValue = Formatter.normalizeNumericString(string)
                            Formatter.parseIntegerFromString(repValue)?.let {
                                updateSetReps(it, set.id)
                            }
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = { focusManager.clearFocus() }
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedBorderColor = Color.Transparent,
                            focusedBorderColor = Color.Transparent,
                            disabledBorderColor = Color.Transparent,
                            focusedTextColor = contentColor,
                            unfocusedTextColor = contentColor,
                        ),
                        readOnly = useScrollWheelForInput
                    )
                    if (useScrollWheelForInput) {
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .clip(MaterialTheme.shapes.extraLarge)
                                .clickable {
                                    inputModalBottomSheetState = InputModalBottomSheetState.Reps(
                                        reps = repValue.toInt()
                                    )
                                    inputSetId = set.id
                                }
                        ) { }
                    }
                }
            }

            if (workout) {
                // The indeterminate state on its own does not tell what it stands for
                val missedRepsDescription = stringResource(R.string.missed_reps)

                // Off -> done -> done but short of the planned reps -> off again
                TriStateCheckbox(
                    modifier = if (set.failed) {
                        Modifier.semantics { stateDescription = missedRepsDescription }
                    } else Modifier,
                    state = when {
                        set.failed -> ToggleableState.Indeterminate
                        set.completed -> ToggleableState.On
                        else -> ToggleableState.Off
                    },
                    onClick = {
                        stopStopwatchIfRunning()
                        when {
                            set.failed -> clearSet()
                            set.completed -> updateSetFailed(true, set.id)
                            else -> markSetCompleted()
                        }
                    },
                    colors = CheckboxDefaults.colors(
                        checkedColor = if (set.failed) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.primary
                        }
                    )
                )
            }
        }
    }

}


@OptIn(ExperimentalSharedTransitionApi::class)
@Preview(wallpaper = Wallpapers.RED_DOMINATED_EXAMPLE)
@Composable
private fun ExerciseCardPreview() {
    val currentIdSetWithRunningSet = remember { mutableStateOf<Long?>(null) }

    val e = remember {
        mutableStateOf(
            UiExerciseWithSets(
                exercise = UiExercise(
                    notes = "This is a note!",
                    restTime = 90,
                    setMode = SetMode.LOAD,
                    supersetGroupId = 1L
                ),
                sets = persistentListOf(UiSet(completed = true), UiSet(elapsedTime = 100)),
                exerciseDC = UiExerciseDC(
                    name = "Exercise name",
                    images = persistentListOf("3_4_Sit-Up/0.jpg"),
                    equipment = Equipment.BARBELL
                )
            )
        )
    }

    val previousPerformances = e.value.sets.map { _ ->
        when (e.value.exercise.setMode) {
            SetMode.BODYWEIGHT -> PreviousPerformanceSet(reps = 10)
            SetMode.DURATION -> PreviousPerformanceSet(time = 124)
            SetMode.BODYWEIGHT_WITH_LOAD, SetMode.ASSISTED_BODYWEIGHT -> PreviousPerformanceSet(
                reps = 10,
                load = Weight.kilograms(12.0)
            )

            SetMode.LOAD -> PreviousPerformanceSet(reps = 10, load = Weight.kilograms(12.0))
        }
    }

    LibreFitTheme(dynamicColor = true, themeMode = ThemeMode.DARK) {
        SharedTransitionLayout {
            AnimatedVisibility(visible = true) {
                ExerciseCard(
                    animatedVisibilityScope = this,
                    exerciseWithSets = e.value,
                    previousPerformances = previousPerformances,
                    addSet = {
                        val newSets = e.value.sets.toMutableList() + UiSet()
                        e.value = e.value.copy(sets = newSets.toImmutableList())
                    },
                    onDetail = { _, _ -> },
                    onDelete = {},
                    deleteSet = { id ->
                        e.value = e.value.copy(sets = e.value.sets.filter { it.id != id }
                            .toImmutableList())
                        if (id == currentIdSetWithRunningSet.value) currentIdSetWithRunningSet.value =
                            null
                    },
                    showInfo = {},
                    idSetWithRunningStopwatch = currentIdSetWithRunningSet.value,
                    updateIdSetWithRunningStopwatch = { currentIdSetWithRunningSet.value = it },
                    workout = true,
                    isDragging = false,
                    useScrollWheelForInput = false,
                    dismissScrollWheelInputAutomatically = false,
                    showExercisesImages = false,
                    defaultBarWeight = null,
                    updateExerciseNotes = { notes, _ ->
                        e.value = e.value.copy(exercise = e.value.exercise.copy(notes = notes))
                    },
                    updateExerciseRestTime = { restTime, _ ->
                        e.value =
                            e.value.copy(exercise = e.value.exercise.copy(restTime = restTime))
                    },
                    updateExerciseSetMode = { setMode, _ ->
                        e.value = e.value.copy(exercise = e.value.exercise.copy(setMode = setMode))
                    },
                    updateExerciseSupersetGroup = { groupId, _ ->
                        e.value = e.value.copy(
                            exercise = e.value.exercise.copy(
                                supersetGroupId = if (groupId == null) 1L else null
                            )
                        )
                    },
                    updateExerciseWeightIncrement = { weightIncrement, _ ->
                        e.value = e.value.copy(
                            exercise = e.value.exercise.copy(weightIncrement = weightIncrement)
                        )
                    },
                    updateSetTime = { time, id ->
                        e.value = e.value.copy(
                            sets = e.value.sets.map {
                                if (it.id == id) it.copy(elapsedTime = time) else it
                            }.toImmutableList()
                        )
                    },
                    updateSetReps = { reps, id ->
                        e.value = e.value.copy(
                            sets = e.value.sets.map {
                                if (it.id == id) it.copy(reps = reps) else it
                            }.toImmutableList()
                        )
                    },
                    updateSetLoad = { load, id ->
                        e.value = e.value.copy(
                            sets = e.value.sets.map {
                                if (it.id == id) it.copy(load = load) else it
                            }.toImmutableList()
                        )
                    },
                    updateSetCompleted = { completed, id ->
                        e.value = e.value.copy(
                            sets = e.value.sets.map {
                                if (it.id == id) it.copy(completed = completed) else it
                            }.toImmutableList()
                        )
                    },
                    updateSetFailed = { failed, id ->
                        e.value = e.value.copy(
                            sets = e.value.sets.map {
                                if (it.id == id) {
                                    it.copy(failed = failed, completed = failed || it.completed)
                                } else it
                            }.toImmutableList()
                        )
                    },
                    updateSetIsAmrap = { isAmrap, id ->
                        e.value = e.value.copy(
                            sets = e.value.sets.map {
                                if (it.id == id) it.withAmrap(isAmrap) else it
                            }.toImmutableList()
                        )
                    },
                    updateSetTargetReps = { targetReps, id ->
                        e.value = e.value.copy(
                            sets = e.value.sets.map {
                                if (it.id == id) it.copy(targetReps = targetReps) else it
                            }.toImmutableList()
                        )
                    },
                    applyPreviousSetPerformance = { id ->
                        val index = e.value.sets.indexOfFirst { it.id == id }
                        previousPerformances.getOrNull(index)?.let { p ->
                            e.value = e.value.copy(
                                sets = e.value.sets.map { set ->
                                    if (set.id == id) {
                                        when (e.value.exercise.setMode) {
                                            SetMode.BODYWEIGHT -> set.copy(reps = p.reps)
                                            SetMode.LOAD -> set.copy(load = p.load)
                                            SetMode.DURATION -> set.copy(elapsedTime = p.time)
                                            SetMode.BODYWEIGHT_WITH_LOAD, SetMode.ASSISTED_BODYWEIGHT -> set.copy(
                                                load = p.load,
                                                reps = p.reps
                                            )
                                        }
                                    } else set
                                }.toImmutableList()
                            )
                        }
                    },
                    onReorderRequest = {},
                    saveDefaultBarWeight = {},
                )
            }
        }
    }
}