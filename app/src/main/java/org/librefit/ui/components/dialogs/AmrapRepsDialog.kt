/*
 * SPDX-License-Identifier: GPL-3.0-or-later
 * Copyright (c) 2026. The LibreFit Contributors
 *
 * LibreFit is subject to additional terms covering author attribution and trademark usage;
 * see the ADDITIONAL_TERMS.md and TRADEMARK_POLICY.md files in the project root.
 */

package org.librefit.ui.components.dialogs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.librefit.R
import org.librefit.enums.userPreferences.ThemeMode
import androidx.compose.ui.tooling.preview.Preview
import org.librefit.ui.theme.LibreFitTheme

/**
 * The dialog asking how far an *AMRAP* set was actually taken, shown when such a set is ticked off
 * during a workout.
 *
 * The point of the set is to beat the target rather than to hit the planned repetitions, so the
 * performed amount is only known once the set is over and is asked for instead of being typed into
 * the row beforehand.
 *
 * @param reps The repetitions currently logged for the set, which the dialog starts from.
 * @param targetReps Refer to [org.librefit.db.entity.Set.targetReps]. It is shown as the amount to
 * beat when there is one, i.e. when it is greater than zero.
 * @param onConfirm Invoked with the performed repetitions. They are never negative.
 * @param onDismiss Invoked when the user backs out, leaving the set neither logged nor completed.
 */
@Composable
fun AmrapRepsDialog(
    reps: Int,
    targetReps: Int,
    onConfirm: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val haptic = LocalHapticFeedback.current

    var performedReps by rememberSaveable(reps) { mutableIntStateOf(reps) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(R.string.amrap)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(text = stringResource(R.string.amrap_reps_prompt))
                RepsStepper(
                    label = stringResource(R.string.reps),
                    value = performedReps,
                    onValueChange = { performedReps = it }
                )
                if (targetReps > 0) {
                    Text(
                        text = stringResource(R.string.amrap_target_hint, targetReps),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                    onConfirm(performedReps)
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
private fun AmrapRepsDialogPreview() {
    LibreFitTheme(dynamicColor = false, themeMode = ThemeMode.DARK) {
        AmrapRepsDialog(reps = 8, targetReps = 8, onConfirm = {}, onDismiss = {})
    }
}
