/*
 * SPDX-License-Identifier: GPL-3.0-or-later
 * Copyright (c) 2024-2026. The LibreFit Contributors
 *
 * LibreFit is subject to additional terms covering author attribution and trademark usage;
 * see the ADDITIONAL_TERMS.md and TRADEMARK_POLICY.md files in the project root.
 */

package org.librefit.services

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.os.PowerManager
import android.os.SystemClock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import org.librefit.db.repository.UserPreferencesRepository
import org.librefit.enums.WorkoutServiceActions
import org.librefit.helpers.NotificationHelper
import org.librefit.helpers.SoundPlayer
import org.librefit.services.WorkoutService.Companion.EXTRA_ADD_TEN_SECONDS
import org.librefit.services.WorkoutService.Companion.EXTRA_INITIAL_REST_TIME
import org.librefit.services.WorkoutService.Companion.EXTRA_IS_FOCUSED
import org.librefit.services.WorkoutService.Companion.isStopwatchPaused
import org.librefit.services.WorkoutService.Companion.restTime
import org.librefit.services.WorkoutService.Companion.timeElapsed
import kotlin.time.Duration.Companion.milliseconds

/**
 * A service that manages the stopwatch and the rest timer during a workout session.
 *
 * This service is designed to run in the foreground and it's used by
 * [org.librefit.ui.screens.workout.WorkoutScreenViewModel] to start, pause, and modify
 * a stopwatch and a rest timer.
 *
 * ## State Flows
 * - [timeElapsed]: A [StateFlow] that emits the total time elapsed in seconds.
 * - [isStopwatchPaused]: A [StateFlow] that indicates whether the stopwatch is currently paused.
 * - [restTime]: A [StateFlow] that emits the remaining time for the rest timer in seconds.
 *
 * ## Intent Extras
 * - [EXTRA_INITIAL_REST_TIME]: An integer extra that specifies the initial time for the rest timer.
 * - [EXTRA_ADD_TEN_SECONDS]: A boolean extra that indicates how to modify the rest timer. When
 *   `true` it adds ten seconds otherwise it subtracts.
 * - [EXTRA_IS_FOCUSED]: A boolean extra that indicates whether the workout is currently focused. When
 *   `false` the rest timer notification is sent.
 *
 * ## Actions
 * The service can handle the following actions:
 * - [WorkoutServiceActions.START_STOPWATCH]: Starts and resumes the stopwatch with [startStopwatch]
 * - [WorkoutServiceActions.PAUSE_STOPWATCH]: Pauses the stopwatch with [pauseStopwatch]
 * - [WorkoutServiceActions.START_REST_TIMER]: Starts the rest timer with the specified initial time with [startRestTimer]
 * - [WorkoutServiceActions.ADD_TEN_SECONDS_TO_REST_TIMER]: Adds ten seconds to rest timer with [modifyRestTimer].
 * - [WorkoutServiceActions.SUBTRACT_TEN_SECONDS_TO_REST_TIMER]: Subtract ten seconds to rest timer with [modifyRestTimer].
 * - [WorkoutServiceActions.ADD_SECONDS_TO_REST_TIMER]: Adds [EXTRA_REST_TIME_DELTA] seconds, or removes them when negative, to the rest timer with [modifyRestTimer].
 * - [WorkoutServiceActions.WORKOUT_FOCUS]: Updates [isFocused] based on the focus state of [org.librefit.ui.screens.workout.WorkoutScreen].
 * - [WorkoutServiceActions.STOP_SERVICE]: It calls [stopService] and resets all timers.
 *
 * ## Lifecycle
 * - [onBind]: Returns null as this service is not bound to any component.
 * - [onStartCommand]: Handles incoming intents and performs the corresponding actions.
 *
 * ## Notifications
 * The service uses [NotificationHelper] to create and manage notifications.
 *
 * ## Rest timer alert
 * When the rest timer is over, the service plays the alert sound with [SoundPlayer], which ducks
 * any music playing and uses the media stream, so the sound is heard over music, in silent mode
 * and with the phone locked. A partial wake lock is held while the rest timer runs, so that the
 * CPU does not sleep and the timer ends on time with the screen off.
 */

class WorkoutService : Service() {

    // A lifecycle-aware scope for the entire service
    // SupervisorJob ensures that if one child coroutine fails, it doesn't cancel the others.
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    companion object {
        private val _timeElapsed = MutableStateFlow(0)
        val timeElapsed: StateFlow<Int> = _timeElapsed

        private val _isStopwatchPaused = MutableStateFlow(false)
        val isStopwatchPaused: StateFlow<Boolean> = _isStopwatchPaused

        private val _restTime = MutableStateFlow(0)
        val restTime: StateFlow<Int> = _restTime

        const val EXTRA_INITIAL_REST_TIME = "EXTRA_INITIAL_REST_TIME"
        const val EXTRA_ADD_TEN_SECONDS = "EXTRA_ADD_TEN_SECONDS"
        const val EXTRA_IS_FOCUSED = "EXTRA_IS_FOCUSED"
        const val EXTRA_SET_ELAPSED_TIME = "EXTRA_SET_ELAPSED_TIME"
        const val EXTRA_REST_TIME_DELTA = "EXTRA_REST_TIME_DELTA"
    }

    private var initialRestTime = 0
    private var isFocused = true

    private val notificationHelper: NotificationHelper by inject()
    private val soundPlayer: SoundPlayer by inject()
    private val userPreferences: UserPreferencesRepository by inject()

    private val restTimerWakeLock: PowerManager.WakeLock by lazy {
        (getSystemService(POWER_SERVICE) as PowerManager)
            .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "LibreFit:RestTimer")
            .apply { setReferenceCounted(false) }
    }

    /** [SystemClock.elapsedRealtime] at which the rest timer ends */
    private var restTimerEnd = 0L

    override fun onCreate() {
        super.onCreate()
        // SoundPool loads the sound asynchronously, so it is loaded before a rest timer ends
        soundPlayer
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        releaseRestTimerWakeLock()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = WorkoutServiceActions.entries.find { it.string == intent?.action }
            ?: WorkoutServiceActions.START_STOPWATCH

        // Every command but the stop one arrives through startForegroundService, and Android kills
        // the app when such a start is not followed by startForeground, whatever the command is
        if (action != WorkoutServiceActions.STOP_SERVICE) {
            startForeground(
                NotificationHelper.WORKOUT_NOTIFICATION_ID,
                notificationHelper.createWorkoutNotification()
            )
        }

        when (action) {
            WorkoutServiceActions.START_STOPWATCH -> startStopwatch()

            WorkoutServiceActions.PAUSE_STOPWATCH -> pauseStopwatch()
            WorkoutServiceActions.START_REST_TIMER -> {
                val initialRestTime = intent?.getIntExtra(EXTRA_INITIAL_REST_TIME, 0) ?: 0

                restTimerJob?.cancel()
                this.initialRestTime = initialRestTime
                restTimerEnd = SystemClock.elapsedRealtime() + initialRestTime * 1000L
                _restTime.update { initialRestTime }

                startRestTimer()
            }

            WorkoutServiceActions.ADD_TEN_SECONDS_TO_REST_TIMER -> {
                modifyRestTimer(10)
            }

            WorkoutServiceActions.SUBTRACT_TEN_SECONDS_TO_REST_TIMER -> {
                modifyRestTimer(-10)
            }

            WorkoutServiceActions.ADD_SECONDS_TO_REST_TIMER -> {
                modifyRestTimer(intent?.getIntExtra(EXTRA_REST_TIME_DELTA, 0) ?: 0)
            }

            WorkoutServiceActions.WORKOUT_FOCUS -> {
                isFocused = intent?.getBooleanExtra(EXTRA_IS_FOCUSED, true) != false

                // The workout screen reports its focus even when it closes after the workout has
                // ended, which starts the service again with no workout to show. It goes away
                // straight after, unless another command has reached it in the meantime
                if (stopwatchJob == null) {
                    stopForeground(STOP_FOREGROUND_REMOVE)
                    stopSelf(startId)
                }
            }

            WorkoutServiceActions.STOP_SERVICE -> stopService()
            WorkoutServiceActions.SET_ELAPSED_TIME -> {
                _timeElapsed.update {
                    it + (intent?.getIntExtra(EXTRA_SET_ELAPSED_TIME, 0) ?: 0)
                }
            }
        }

        return START_STICKY
    }

    fun stopService() {
        stopwatchJob?.cancel()
        restTimerJob?.cancel()
        _timeElapsed.update { 0 }
        _restTime.update { 0 }
        initialRestTime = 0
        releaseRestTimerWakeLock()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }


    private var stopwatchJob: Job? = null

    private fun startStopwatch() {

        _isStopwatchPaused.update { false }

        stopwatchJob?.cancel()

        stopwatchJob = serviceScope.launch {
            while (true) {
                delay(1000.milliseconds)

                if (!isStopwatchPaused.value) {
                    _timeElapsed.update {
                        // TODO: use a more accurate calculation
                        it + 1
                    }
                }

                if (isFocused) {
                    notificationHelper.cancelTimerIsOverNotification()
                }

                notificationHelper.notifyOngoingWorkout(
                    timeElapsed.value,
                    isStopwatchPaused.value,
                    restTime.value,
                    initialRestTime,
                    restTimerEnd
                )
            }
        }
    }

    private fun pauseStopwatch() {
        _isStopwatchPaused.update { true }
        // The rest timer keeps running while the stopwatch is paused, so its Live Update stays
        notificationHelper.notifyOngoingWorkout(
            timeElapsed.value,
            isStopwatchPaused.value,
            restTime.value,
            initialRestTime,
            restTimerEnd
        )
    }


    private var restTimerJob: Job? = null

    private fun startRestTimer() {
        acquireRestTimerWakeLock()

        restTimerJob = serviceScope.launch {
            // The remaining time is computed from the end time instead of counting the ticks,
            // so it does not drift and catches up if the process was delayed
            while (true) {
                val remainingMillis = restTimerEnd - SystemClock.elapsedRealtime()
                if (remainingMillis <= 0) break
                _restTime.update { ((remainingMillis + 999) / 1000).toInt() }
                // Wakes up on the next whole second of the remaining time
                delay((remainingMillis - 1) % 1000 + 1)
            }
            _restTime.update { 0 }

            onRestTimerOver()

            initialRestTime = 0

            // Keeps the CPU awake until the alert sound has played, then the lock times out.
            // It is not released in a finally block: a cancelled job would release it after the
            // job that replaced it has acquired it. stopService and onDestroy release it otherwise
            restTimerWakeLock.acquire(3_000L)
        }
    }

    private fun onRestTimerOver() {
        if (userPreferences.restTimerSoundOn.value) {
            soundPlayer.playAlert()
        }
        if (!isFocused) {
            notificationHelper.notifyTimerIsOver()
        }
    }

    private fun modifyRestTimer(deltaSeconds: Int) {
        if (restTimerJob?.isActive != true || deltaSeconds == 0) return

        val now = SystemClock.elapsedRealtime()
        restTimerEnd = (restTimerEnd + deltaSeconds * 1000L).coerceAtLeast(now)
        // Keeps the progress of the notification in proportion with the longer rest
        if (deltaSeconds > 0) initialRestTime += deltaSeconds
        _restTime.update { ((restTimerEnd - now + 999) / 1000).toInt().coerceAtLeast(0) }

        // Restarts the loop, so it wakes up on time for the new end, and extends the wake lock
        restTimerJob?.cancel()
        startRestTimer()
    }

    private fun acquireRestTimerWakeLock() {
        val remainingMillis = (restTimerEnd - SystemClock.elapsedRealtime()).coerceAtLeast(0)
        // The timeout is a safety net, the lock is released as soon as the timer ends
        restTimerWakeLock.acquire(remainingMillis + 10_000L)
    }

    private fun releaseRestTimerWakeLock() {
        if (restTimerWakeLock.isHeld) {
            restTimerWakeLock.release()
        }
    }
}