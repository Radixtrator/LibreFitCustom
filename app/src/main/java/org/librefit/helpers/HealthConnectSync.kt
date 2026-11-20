/*
 * SPDX-License-Identifier: GPL-3.0-or-later
 * Copyright (c) 2026. The LibreFit Contributors
 *
 * LibreFit is subject to additional terms covering author attribution and trademark usage;
 * see the ADDITIONAL_TERMS.md and TRADEMARK_POLICY.md files in the project root.
 */

package org.librefit.helpers

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.net.toUri
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.metadata.Metadata
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.librefit.db.entity.Workout
import org.librefit.db.repository.UserPreferencesRepository
import org.librefit.enums.WorkoutState
import java.time.ZoneId

/**
 * Writes the completed workouts to Health Connect, so other health and fitness apps (e.g. Google
 * Fit, Samsung Health, Fitbit) see them as strength training sessions.
 *
 * Nothing is written unless the user turned it on in settings and granted the permission, refer
 * to [UserPreferencesRepository.healthConnectSyncOn]. Each workout is written with its id as the
 * client record id, so writing it again after an edit replaces it instead of adding a copy, and
 * deleting it from LibreFit deletes it from Health Connect too.
 *
 * Health Connect is part of Android 14+; on Android 9 to 13 it is a separate app.
 */
class HealthConnectSync(
    context: Context,
    private val userPreferences: UserPreferencesRepository,
    private val applicationScope: CoroutineScope,
) {
    companion object {
        private const val TAG = "HealthConnectSync"
        private const val PROVIDER_PACKAGE = "com.google.android.apps.healthdata"

        /** The permissions LibreFit asks Health Connect for */
        val PERMISSIONS: Set<String> = setOf(
            HealthPermission.getWritePermission(ExerciseSessionRecord::class)
        )

        /** Health Connect refuses sessions without length, so a workout lasts at least this */
        private const val MIN_DURATION_SECONDS = 60L

        /** How many workouts are written at once when syncing the whole history */
        private const val BATCH_SIZE = 100

        private fun clientRecordId(workoutId: Long) = "librefit-workout-$workoutId"
    }

    enum class Availability { AVAILABLE, UPDATE_REQUIRED, UNAVAILABLE }

    private val appContext = context.applicationContext

    private var cachedClient: HealthConnectClient? = null

    /**
     * The Health Connect client, or `null` while Health Connect is not available. It is looked up
     * again until found, since Health Connect can be installed or updated while LibreFit runs.
     */
    private val client: HealthConnectClient?
        get() = cachedClient ?: if (availability() == Availability.AVAILABLE) {
            runCatching { HealthConnectClient.getOrCreate(appContext) }.getOrNull()
                ?.also { cachedClient = it }
        } else {
            null
        }

    fun availability(): Availability =
        when (runCatching { HealthConnectClient.getSdkStatus(appContext) }.getOrNull()) {
            HealthConnectClient.SDK_AVAILABLE -> Availability.AVAILABLE
            HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED -> Availability.UPDATE_REQUIRED
            else -> Availability.UNAVAILABLE
        }

    /** Whether every permission in [PERMISSIONS] is granted */
    suspend fun hasPermissions(): Boolean {
        val client = client ?: return false
        return runCatching {
            client.permissionController.getGrantedPermissions().containsAll(PERMISSIONS)
        }.getOrDefault(false)
    }

    /** Opens the app store page of Health Connect, to install or update it on Android 13 and lower */
    fun openProviderInStore(context: Context) {
        val intent = Intent(Intent.ACTION_VIEW)
            .setPackage("com.android.vending")
            .setData(
                "market://details?id=$PROVIDER_PACKAGE&url=healthconnect%3A%2F%2Fonboarding".toUri()
            )
            .putExtra("overlay", true)
            .putExtra("callerId", appContext.packageName)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try {
            context.startActivity(intent)
        } catch (_: ActivityNotFoundException) {
            context.startActivity(
                Intent(Intent.ACTION_VIEW, "market://details?id=$PROVIDER_PACKAGE".toUri())
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }
    }

    /**
     * Writes [workout] to Health Connect in the background, replacing what was written for it
     * before. Workouts that are not completed are ignored.
     */
    fun sync(workout: Workout) {
        if (workout.state != WorkoutState.COMPLETED || workout.id == 0L) return
        if (!userPreferences.healthConnectSyncOn.value) return

        applicationScope.launch { write(listOf(workout)) }
    }

    /** Writes all [workouts] to Health Connect, e.g. the whole history when sync is turned on */
    suspend fun syncAll(workouts: List<Workout>) {
        workouts
            .filter { it.state == WorkoutState.COMPLETED && it.id != 0L }
            .chunked(BATCH_SIZE)
            .forEach { write(it) }
    }

    /** Deletes what was written for [workout] from Health Connect, in the background */
    fun remove(workout: Workout) {
        if (workout.state != WorkoutState.COMPLETED || workout.id == 0L) return
        if (!userPreferences.healthConnectSyncOn.value) return

        applicationScope.launch {
            val client = client ?: return@launch
            runCatching {
                client.deleteRecords(
                    ExerciseSessionRecord::class,
                    recordIdsList = emptyList(),
                    clientRecordIdsList = listOf(clientRecordId(workout.id))
                )
            }.onFailure { Log.w(TAG, "Could not delete workout ${workout.id}", it) }
        }
    }

    private suspend fun write(workouts: List<Workout>) {
        val client = client ?: return
        if (workouts.isEmpty()) return
        runCatching {
            client.insertRecords(workouts.map { it.toRecord() })
        }.onFailure { Log.w(TAG, "Could not write ${workouts.size} workout(s)", it) }
    }

    private fun Workout.toRecord(): ExerciseSessionRecord {
        val zone = ZoneId.systemDefault()
        val end = completed.atZone(zone).toInstant()
        val start = end.minusSeconds(timeElapsed.toLong().coerceAtLeast(MIN_DURATION_SECONDS))

        return ExerciseSessionRecord(
            startTime = start,
            startZoneOffset = zone.rules.getOffset(start),
            endTime = end,
            endZoneOffset = zone.rules.getOffset(end),
            metadata = Metadata.manualEntry(
                clientRecordId = clientRecordId(id),
                // A newer version replaces the record written before for the same workout
                clientRecordVersion = System.currentTimeMillis()
            ),
            exerciseType = ExerciseSessionRecord.EXERCISE_TYPE_STRENGTH_TRAINING,
            title = title.ifBlank { null },
            notes = notes.ifBlank { null }
        )
    }
}
