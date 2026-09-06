/*
 * SPDX-License-Identifier: GPL-3.0-or-later
 * Copyright (c) 2026. The LibreFit Contributors
 *
 * LibreFit is subject to additional terms covering author attribution and trademark usage;
 * see the ADDITIONAL_TERMS.md and TRADEMARK_POLICY.md files in the project root.
 */

package org.librefit.ui.components.dialogs

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
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
 * i.e. whether the set is an *AMRAP* one and the repetitions it is planned for.
 *
 * The choice is kept locally and handed over only when confirmed, so half-made edits never reach
 * the workout and the toggle cannot flip back while the target is still being dialled in.
 *
 * @param isAmrap Refer to [org.librefit.db.entity.Set.isAmrap].
 * @param targetReps Refer to [org.librefit.db.entity.Set.targetReps].
 * @param onConfirm Invoked with the chosen flag and target. The target is never negative.
 * @param onDismiss Invoked when the user backs out, leaving the set untouched.
 */
@Composable
fun AmrapSetDialog(
    isAmrap: Boolean,
    targetReps: Int,
    onConfirm: (Boolean, Int) -> Unit,
    onDismiss: () -> Unit
) {
    val haptic = LocalHapticFeedback.current

    var amrapEnabled by rememberSaveable(isAmrap) { mutableStateOf(isAmrap) }
    var target by rememberSaveable(targetReps) { mutableIntStateOf(targetReps) }

    val decreaseDescription = stringResource(R.string.decrease)
    val increaseDescription = stringResource(R.string.increase)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(R.string.amrap)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
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

                // The target is the baseline the performed repetitions are compared against, so it
                // is meaningless until the set is an AMRAP one
                AnimatedVisibility(visible = amrapEnabled) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(MaterialTheme.shapes.large)
                            .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = stringResource(R.string.target_reps))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = { target = (target - 1).coerceAtLeast(0) },
                                enabled = target > 0,
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
                                text = target.toString(),
                                style = MaterialTheme.typography.titleLarge,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.width(48.dp)
                            )
                            IconButton(
                                onClick = { target += 1 },
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

@Preview
@Composable
private fun AmrapSetDialogPreview() {
    LibreFitTheme(dynamicColor = false, themeMode = ThemeMode.DARK) {
        AmrapSetDialog(isAmrap = true, targetReps = 8, onConfirm = { _, _ -> }, onDismiss = {})
    }
}
