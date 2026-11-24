/*
 * SPDX-License-Identifier: GPL-3.0-or-later
 * Copyright (c) 2026. The LibreFit Contributors
 *
 * LibreFit is subject to additional terms covering author attribution and trademark usage;
 * see the ADDITIONAL_TERMS.md and TRADEMARK_POLICY.md files in the project root.
 */

package org.librefit.helpers

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import android.util.Log
import androidx.core.net.toUri
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import org.librefit.BuildConfig
import org.librefit.db.dao.DatasetDao
import org.librefit.db.dao.MeasurementDao
import org.librefit.db.dao.WorkoutDao
import org.librefit.db.entity.Workout
import org.librefit.db.repository.UserPreferencesRepository
import org.librefit.enums.WorkoutState
import org.librefit.models.BackupExercise
import org.librefit.models.BackupFile
import org.librefit.models.BackupMeasurement
import org.librefit.models.BackupWorkout
import org.librefit.util.BackupImportPlan
import org.librefit.util.planBackupImport
import java.io.IOException
import java.time.LocalDate
import kotlin.time.Duration.Companion.seconds

/**
 * Backs up and restores the completed workouts, the routines, the body measurements and the
 * custom exercises, as a [BackupFile].
 *
 * - [export] writes a backup to a file the user picks, and [import] adds the content of one to the
 *   app without replacing or deleting anything, refer to [planBackupImport].
 * - Automatic backups are written to a folder the user picks, a few seconds after a workout,
 *   routine or measurement is saved, edited or deleted. There is one file per day, named
 *   `LibreFit-backup-<date>.json` and overwritten during the day, and only the newest
 *   [KEPT_AUTOMATIC_BACKUPS] are kept. Picking a folder synced by another app, e.g. a cloud
 *   storage one, keeps the backups off the phone.
 */
class WorkoutBackup(
    context: Context,
    private val workoutDao: WorkoutDao,
    private val measurementDao: MeasurementDao,
    private val datasetDao: DatasetDao,
    private val userPreferences: UserPreferencesRepository,
    private val applicationScope: CoroutineScope,
) {
    companion object {
        private const val TAG = "WorkoutBackup"

        /** Marks a JSON file as a LibreFit backup, so other JSON files are refused */
        const val FORMAT = "librefit-backup"

        /** How many automatic backups, i.e. days, are kept in the folder */
        const val KEPT_AUTOMATIC_BACKUPS = 7

        /** Changes made in a row, e.g. a workout saved and then the routine updated, make one backup */
        private val AUTOMATIC_BACKUP_DELAY = 5.seconds

        private val BACKED_UP_STATES = listOf(WorkoutState.COMPLETED, WorkoutState.ROUTINE)

        private const val AUTOMATIC_FILE_PREFIX = "LibreFit-backup-"
        private val AUTOMATIC_FILE_NAME = Regex("""LibreFit-backup-\d{4}-\d{2}-\d{2}\.json""")

        private const val FOLDER_PERMISSIONS =
            Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION

        /** How backups are written and read. Unknown fields are ignored, for newer backups */
        internal val json = Json {
            ignoreUnknownKeys = true
            encodeDefaults = true
        }

        /** The name of the automatic backup of [date] */
        fun automaticFileName(date: LocalDate = LocalDate.now()) = "$AUTOMATIC_FILE_PREFIX$date.json"

        /**
         * The name suggested for a backup exported by hand. It differs from the automatic ones, so
         * an export saved in the backup folder is never deleted to keep the newest ones only.
         */
        fun exportFileName(date: LocalDate = LocalDate.now()) = "LibreFit-export-$date.json"
    }

    /** The file is not a LibreFit backup, or one written by a newer version of the app */
    class InvalidBackupException(message: String, cause: Throwable? = null) :
        Exception(message, cause)

    private val contentResolver = context.applicationContext.contentResolver

    /** Keeps a backup from being written while another one, or an import, is in progress */
    private val mutex = Mutex()

    private val pendingLock = Any()
    private var pendingAutomaticBackup: Job? = null

    /** Builds a backup of everything that is backed up, refer to [BackupFile] */
    suspend fun createBackup(): BackupFile = withContext(Dispatchers.IO) {
        val workouts = BACKED_UP_STATES
            .flatMap { workoutDao.getWorkoutsWithExercisesAndSetsByState(it).first() }
            .map { workout ->
                BackupWorkout(
                    workout = workout.workout,
                    exercises = workout.exercisesWithSets
                        .sortedBy { it.exercise.position }
                        .map { BackupExercise(exercise = it.exercise, sets = it.sets) }
                )
            }

        BackupFile(
            format = FORMAT,
            appVersion = BuildConfig.VERSION_NAME,
            workouts = workouts,
            measurements = measurementDao.getAllMeasurements().first()
                .map(BackupMeasurement::from),
            customExercises = datasetDao.getCustomExercises().first()
        )
    }

    /** Writes a backup to [uri], a file the user picked. It throws when it cannot be written */
    suspend fun export(uri: Uri) = mutex.withLock {
        withContext(Dispatchers.IO) { write(uri, createBackup()) }
    }

    /**
     * Adds the content of the backup at [uri] to the app, refer to [planBackupImport] for what is
     * added, and returns what was added.
     *
     * @throws InvalidBackupException when [uri] is not a backup this version can read
     * @throws IOException when [uri] cannot be read
     */
    suspend fun import(uri: Uri): BackupImportPlan = mutex.withLock {
        withContext(Dispatchers.IO + NonCancellable) {
            val text = contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                ?: throw IOException("Cannot read $uri")

            val backup = try {
                json.decodeFromString(BackupFile.serializer(), text)
            } catch (e: IllegalArgumentException) {
                // Thrown for invalid JSON and for JSON that is not a backup, e.g. a routine file
                throw InvalidBackupException("Not a LibreFit backup", e)
            }
            if (backup.format != FORMAT) throw InvalidBackupException("Not a LibreFit backup")
            if (backup.version > BackupFile.CURRENT_VERSION) {
                throw InvalidBackupException("Backup version ${backup.version} is not supported")
            }

            val plan = planBackupImport(
                backup = backup,
                existingWorkouts = WorkoutState.entries.flatMap { workoutDao.getWorkoutsByState(it).first() },
                existingMeasurements = measurementDao.getAllMeasurements().first(),
                existingExerciseIds = datasetDao.getDataset().first().mapTo(mutableSetOf()) { it.id }
            )

            // The custom exercises first, as the exercises of the workouts point to them
            plan.customExercises.forEach { datasetDao.upsertExercise(it) }
            workoutDao.importWorkouts(plan.workouts)
            plan.measurements.forEach { measurementDao.upsertMeasurement(it.toEntity()) }

            plan
        }
    }.also { onDataChanged() }

    /** To be called when [workout] was saved, edited or deleted, refer to [onDataChanged] */
    fun onWorkoutChanged(workout: Workout) {
        if (workout.state in BACKED_UP_STATES) onDataChanged()
    }

    /**
     * Writes an automatic backup a few seconds after the last change, when they are turned on.
     * Calling it again within those seconds postpones the backup, so a burst of changes makes one.
     */
    fun onDataChanged() {
        if (userPreferences.autoBackupFolder.value == null) return

        synchronized(pendingLock) {
            pendingAutomaticBackup?.cancel()
            pendingAutomaticBackup = applicationScope.launch {
                delay(AUTOMATIC_BACKUP_DELAY)
                writeAutomaticBackup()
            }
        }
    }

    /**
     * Turns the automatic backups on in [folder], a document tree the user picked, and writes the
     * first one straight away. Returns whether it could be written.
     */
    suspend fun enableAutomaticBackup(folder: Uri): Boolean {
        contentResolver.takePersistableUriPermission(folder, FOLDER_PERMISSIONS)
        userPreferences.saveAutoBackupFolder(folder.toString())
        return writeAutomaticBackup(folder)
    }

    /** Turns the automatic backups off. The backups already written are left in the folder */
    suspend fun disableAutomaticBackup() {
        synchronized(pendingLock) { pendingAutomaticBackup?.cancel() }
        val folder = userPreferences.autoBackupFolder.value
        userPreferences.saveAutoBackupFolder(null)
        folder?.let {
            runCatching { contentResolver.releasePersistableUriPermission(it.toUri(), FOLDER_PERMISSIONS) }
        }
    }

    /** The name of the folder of the automatic backups, or `null` when it cannot be read */
    suspend fun automaticBackupFolderName(): String? = withContext(Dispatchers.IO) {
        val folder = userPreferences.autoBackupFolder.value?.toUri() ?: return@withContext null
        runCatching {
            val document = DocumentsContract.buildDocumentUriUsingTree(
                folder, DocumentsContract.getTreeDocumentId(folder)
            )
            contentResolver.query(
                document, arrayOf(DocumentsContract.Document.COLUMN_DISPLAY_NAME), null, null, null
            )?.use { cursor -> if (cursor.moveToFirst()) cursor.getString(0) else null }
        }.getOrNull()
    }

    /**
     * Writes today's automatic backup to [folder] and deletes the oldest ones beyond
     * [KEPT_AUTOMATIC_BACKUPS]. The outcome is saved for the settings to show, refer to
     * [UserPreferencesRepository.lastAutoBackup]. Returns whether it was written.
     */
    suspend fun writeAutomaticBackup(
        folder: Uri? = userPreferences.autoBackupFolder.value?.toUri()
    ): Boolean {
        folder ?: return false

        // A backup started is finished even if another change comes in meanwhile, so the file is
        // never left half written
        return withContext(Dispatchers.IO + NonCancellable) {
            val succeeded = mutex.withLock {
                runCatching {
                    val backup = createBackup()
                    val folderDocument = DocumentsContract.buildDocumentUriUsingTree(
                        folder, DocumentsContract.getTreeDocumentId(folder)
                    )
                    val backups = listAutomaticBackups(folder)
                    val name = automaticFileName()
                    val file = backups[name]
                        ?: DocumentsContract.createDocument(
                            contentResolver, folderDocument, BackupFile.MIME_TYPE, name
                        )
                        ?: throw IOException("Cannot create $name")

                    write(file, backup)
                    deleteOldBackups(backups + (name to file))
                }.onFailure { Log.w(TAG, "Could not write the automatic backup", it) }.isSuccess
            }
            userPreferences.saveAutoBackupResult(succeeded, System.currentTimeMillis())
            succeeded
        }
    }

    private fun write(uri: Uri, backup: BackupFile) {
        // "wt" truncates the file, so a shorter backup leaves nothing of the previous one behind
        contentResolver.openOutputStream(uri, "wt")?.bufferedWriter()?.use { writer ->
            writer.write(json.encodeToString(BackupFile.serializer(), backup))
        } ?: throw IOException("Cannot write $uri")
    }

    /** The automatic backups in [folder], by file name */
    private fun listAutomaticBackups(folder: Uri): Map<String, Uri> {
        val children = DocumentsContract.buildChildDocumentsUriUsingTree(
            folder, DocumentsContract.getTreeDocumentId(folder)
        )
        val backups = mutableMapOf<String, Uri>()
        contentResolver.query(
            children,
            arrayOf(
                DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                DocumentsContract.Document.COLUMN_DISPLAY_NAME
            ),
            null, null, null
        )?.use { cursor ->
            while (cursor.moveToNext()) {
                val name = cursor.getString(1)
                if (name != null && AUTOMATIC_FILE_NAME.matches(name)) {
                    backups[name] = DocumentsContract.buildDocumentUriUsingTree(folder, cursor.getString(0))
                }
            }
        }
        return backups
    }

    private fun deleteOldBackups(backups: Map<String, Uri>) {
        // The date in the names sorts them from the newest to the oldest
        backups.entries
            .sortedByDescending { it.key }
            .drop(KEPT_AUTOMATIC_BACKUPS)
            .forEach { (name, uri) ->
                runCatching { DocumentsContract.deleteDocument(contentResolver, uri) }
                    .onFailure { Log.w(TAG, "Could not delete the old backup $name", it) }
            }
    }
}
