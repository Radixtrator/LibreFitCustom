/*
 * SPDX-License-Identifier: GPL-3.0-or-later
 * Copyright (c) 2026. The LibreFit Contributors
 *
 * LibreFit is subject to additional terms covering author attribution and trademark usage;
 * see the ADDITIONAL_TERMS.md and TRADEMARK_POLICY.md files in the project root.
 */

package org.librefit.ui.screens.settings

import android.content.Context
import android.text.format.DateUtils
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import org.librefit.R
import org.librefit.db.repository.UserPreferencesRepository
import org.librefit.helpers.WorkoutBackup
import org.librefit.models.BackupFile
import org.librefit.ui.components.HeadlineText

private const val TAG = "BackupSettings"

/**
 * What the backup settings show and do, refer to [backupSettingItems] and [WorkoutBackup].
 *
 * @property automaticBackupOn Whether the automatic backups are turned on
 * @property automaticBackupDescription Where they go and when the last one was written
 */
class BackupSettingsState(
    val automaticBackupOn: Boolean,
    val automaticBackupDescription: String,
    val onExport: () -> Unit,
    val onImport: () -> Unit,
    val onAutomaticBackupClick: () -> Unit,
)

@Composable
fun rememberBackupSettingsState(
    workoutBackup: WorkoutBackup = koinInject(),
    userPreferences: UserPreferencesRepository = koinInject(),
): BackupSettingsState {
    val context = LocalContext.current
    // Strings are read through the resources of the composition, which follow configuration changes
    val resources = LocalResources.current
    val coroutineScope = rememberCoroutineScope()

    val autoBackupFolder by userPreferences.autoBackupFolder.collectAsStateWithLifecycle()
    val lastAutoBackup by userPreferences.lastAutoBackup.collectAsStateWithLifecycle()
    val autoBackupFailed by userPreferences.autoBackupFailed.collectAsStateWithLifecycle()

    var folderName by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(autoBackupFolder) {
        folderName = workoutBackup.automaticBackupFolderName()
    }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument(BackupFile.MIME_TYPE)
    ) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        coroutineScope.launch {
            val message = runCatching { workoutBackup.export(uri) }.fold(
                onSuccess = { R.string.backup_exported },
                onFailure = {
                    Log.w(TAG, "Could not export the backup", it)
                    R.string.backup_export_failed
                }
            )
            context.toast(resources.getString(message))
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        coroutineScope.launch {
            val message = runCatching { workoutBackup.import(uri) }.fold(
                onSuccess = { plan ->
                    resources.getString(
                        R.string.backup_imported,
                        plan.completedWorkoutCount,
                        plan.routineCount,
                        plan.measurements.size
                    ) + if (plan.skippedExercises > 0) {
                        "\n" + resources.getString(R.string.backup_imported_skipped, plan.skippedExercises)
                    } else ""
                },
                onFailure = {
                    Log.w(TAG, "Could not import the backup", it)
                    resources.getString(R.string.backup_import_failed)
                }
            )
            context.toast(message)
        }
    }

    val folderLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { folder ->
        folder ?: return@rememberLauncherForActivityResult
        coroutineScope.launch {
            val written = runCatching { workoutBackup.enableAutomaticBackup(folder) }
                .onFailure { Log.w(TAG, "Could not turn the automatic backups on", it) }
                .getOrDefault(false)
            if (!written) context.toast(resources.getString(R.string.automatic_backup_failed_desc))
        }
    }

    val automaticBackupOn = autoBackupFolder != null
    val automaticBackupDescription = when {
        !automaticBackupOn -> stringResource(R.string.automatic_backup_off_desc)
        autoBackupFailed -> stringResource(R.string.automatic_backup_failed_desc)
        else -> stringResource(
            R.string.automatic_backup_on_desc,
            folderName ?: stringResource(R.string.automatic_backup_chosen_folder),
            lastAutoBackup?.let {
                DateUtils.getRelativeTimeSpanString(
                    it, System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS
                ).toString()
            } ?: stringResource(R.string.automatic_backup_not_yet)
        )
    }

    return BackupSettingsState(
        automaticBackupOn = automaticBackupOn,
        automaticBackupDescription = automaticBackupDescription,
        onExport = { exportLauncher.launch(WorkoutBackup.exportFileName()) },
        // Some file managers do not tell JSON files apart, so any file can be picked
        onImport = { importLauncher.launch(arrayOf(BackupFile.MIME_TYPE, "*/*")) },
        onAutomaticBackupClick = {
            if (automaticBackupOn) {
                coroutineScope.launch { workoutBackup.disableAutomaticBackup() }
            } else {
                folderLauncher.launch(null)
            }
        }
    )
}

/** The backup section of the settings: export, import and automatic backups */
fun LazyListScope.backupSettingItems(state: BackupSettingsState) {
    item { HeadlineText(text = stringResource(R.string.backup)) }

    item {
        SettingItem(
            isChecked = state.automaticBackupOn,
            onClick = state.onAutomaticBackupClick,
            icon = painterResource(R.drawable.ic_history),
            settingName = stringResource(R.string.automatic_backup),
            settingDesc = state.automaticBackupDescription
        )
    }

    item {
        SettingItem(
            onClick = state.onExport,
            icon = painterResource(R.drawable.ic_upload),
            settingName = stringResource(R.string.export_backup),
            settingDesc = stringResource(R.string.export_backup_desc)
        )
    }

    item {
        SettingItem(
            onClick = state.onImport,
            icon = painterResource(R.drawable.ic_download),
            settingName = stringResource(R.string.import_backup),
            settingDesc = stringResource(R.string.import_backup_desc)
        )
    }
}

private fun Context.toast(message: String) =
    Toast.makeText(this, message, Toast.LENGTH_LONG).show()
