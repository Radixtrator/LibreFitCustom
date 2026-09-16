/*
 * SPDX-License-Identifier: GPL-3.0-or-later
 * Copyright (c) 2026. The LibreFit Contributors
 *
 * LibreFit is subject to additional terms covering author attribution and trademark usage;
 * see the ADDITIONAL_TERMS.md and TRADEMARK_POLICY.md files in the project root.
 */

package org.librefit.ui.components.dialogs

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.librefit.R
import org.librefit.enums.userPreferences.ThemeMode
import org.librefit.ui.theme.LibreFitTheme

/**
 * The dialog holding the settings that belong to a single set rather than to the whole exercise,
 * i.e. whether the set is an *AMRAP* one, the repetitions it is planned for, and its removal.
 *
 * The choice is kept locally and handed over only when confirmed, so half-made edits never reach
 * the workout and the toggle cannot flip back while the target is still being dialled in. Removing
 * the set is the exception: it takes effect at once, since there is nothing left to confirm.
 *
 * @param isAmrap Refer to [org.librefit.db.entity.Set.isAmrap].
 * @param targetReps Refer to [org.librefit.db.entity.Set.targetReps].
 * @param isAmrapAvailable Whether the set mode carries a load, the only case in which beating the
 * target can be rewarded with a heavier next session. When `false` the AMRAP settings are left out
 * and the dialog is merely the way to remove the set.
 * @param onDelete Invoked when the user removes the set, leaving the remaining settings untouched.
 * @param onConfirm Invoked with the chosen flag and target. The target is never negative.
 * @param onDismiss Invoked when the user backs out, leaving the set untouched.
 */
@Composable
fun SetSettingsDialog(
    isAmrap: Boolean,
    targetReps: Int,
    isAmrapAvailable: Boolean,
    onDelete: () -> Unit,
    onConfirm: (Boolean, Int) -> Unit,
    onDismiss: () -> Unit
) {
    val haptic = LocalHapticFeedback.current

    var amrapEnabled by rememberSaveable(isAmrap) { mutableStateOf(isAmrap) }
    var target by rememberSaveable(targetReps) { mutableIntStateOf(targetReps) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(R.string.set)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                if (isAmrapAvailable) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = stringResource(R.string.amrap))
                        Switch(
                            checked = amrapEnabled,
                            onCheckedChange = {
                                amrapEnabled = it
                                haptic.performHapticFeedback(
                                    if (it) HapticFeedbackType.ToggleOn else HapticFeedbackType.ToggleOff
                                )
                            }
                        )
                    }

                    // The target is the baseline the performed repetitions are compared against, so
                    // it is meaningless until the set is an AMRAP one. It keeps its place instead
                    // of expanding into it: a dialog window resizes in steps rather than smoothly,
                    // which left the row clipped halfway through the animation
                    RepsStepper(
                        label = stringResource(R.string.target_reps),
                        value = target,
                        onValueChange = { target = it },
                        enabled = amrapEnabled
                    )
                }

                // Swiping a set marks how it went during a workout, so its removal lives here
                TextButton(
                    onClick = onDelete,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_delete),
                        contentDescription = null
                    )
                    Text(
                        text = stringResource(R.string.delete_set),
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                    onConfirm(amrapEnabled, if (amrapEnabled) target else 0)
                }
            ) {
                Text(text = stringResource(R.string.ok_dialog))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.cancel_dialog))
            }
        }
    )
}

/**
 * A labelled row stepping an amount of repetitions up and down, never below zero.
 *
 * @param label The name of the value being stepped.
 * @param value The current amount of repetitions.
 * @param onValueChange Invoked with the new amount. It is never negative.
 * @param enabled When `false` the row is dimmed and no longer steps, which is how a value that
 * does not apply yet is shown without the layout changing size underneath the user.
 */
@Composable
internal fun RepsStepper(
    label: String,
    value: Int,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val decreaseDescription = stringResource(R.string.decrease)
    val increaseDescription = stringResource(R.string.increase)

    val contentAlpha by animateFloatAsState(
        targetValue = if (enabled) 1f else 0.38f,
        label = "animated_alpha_for_reps_stepper"
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .background(MaterialTheme.colorScheme.surfaceContainerHighest)
            .alpha(contentAlpha)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label)
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(
                onClick = { onValueChange((value - 1).coerceAtLeast(0)) },
                enabled = enabled && value > 0,
                modifier = Modifier
                    .size(40.dp)
                    .semantics { contentDescription = decreaseDescription }
            ) {
                Text(
                    text = "-",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Text(
                text = value.toString(),
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center,
                modifier = Modifier.width(48.dp)
            )
            IconButton(
                onClick = { onValueChange(value + 1) },
                enabled = enabled,
                modifier = Modifier
                    .size(40.dp)
                    .semantics { contentDescription = increaseDescription }
            ) {
                Text(
                    text = "+",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Preview
@Composable
private fun SetSettingsDialogPreview() {
    LibreFitTheme(dynamicColor = false, themeMode = ThemeMode.DARK) {
        SetSettingsDialog(
            isAmrap = true,
            targetReps = 8,
            isAmrapAvailable = true,
            onDelete = {},
            onConfirm = { _, _ -> },
            onDismiss = {})
    }
}
