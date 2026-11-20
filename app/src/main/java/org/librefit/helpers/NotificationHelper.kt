/*
 * SPDX-License-Identifier: GPL-3.0-or-later
 * Copyright (c) 2024-2026. The LibreFit Contributors
 *
 * LibreFit is subject to additional terms covering author attribution and trademark usage;
 * see the ADDITIONAL_TERMS.md and TRADEMARK_POLICY.md files in the project root.
 */

package org.librefit.helpers

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import androidx.core.app.NotificationCompat
import androidx.core.net.toUri
import org.librefit.R
import org.librefit.activities.MainActivity
import org.librefit.enums.WorkoutServiceActions
import org.librefit.services.WorkoutService
import org.librefit.util.Formatter.formatTime


/**
 * A helper class for managing notifications during workout sessions.
 *
 * This class is responsible for creating and displaying notifications for ongoing workouts
 * and timer events. It sets up notification channels, builds notification content, and
 * handles user interactions through actions in the notifications.
 *
 */
class NotificationHelper(
    context: Context,
) {
    companion object {
        const val WORKOUT_CHANNEL_ID = "WORKOUT_CHANNEL_ID"
        const val WORKOUT_NOTIFICATION_ID = 1001

        /**
         * The alert sound is played by [WorkoutService] on the media stream, so that it is heard
         * over music and in silent mode, therefore this channel is silent. Channel sounds cannot be
         * changed once created, hence a new id replacing [LEGACY_TIMER_CHANNEL_ID].
         */
        const val TIMER_CHANNEL_ID = "REST_TIMER_OVER_CHANNEL_ID"
        private const val LEGACY_TIMER_CHANNEL_ID = "TIMER_CHANNEL_ID"
        const val TIMER_NOTIFICATION_ID = 1002
    }

    private val appContext = context.applicationContext


    private val workoutNotificationBuilder =
        NotificationCompat.Builder(appContext, WORKOUT_CHANNEL_ID)

    private val timerNotificationBuilder = NotificationCompat.Builder(appContext, TIMER_CHANNEL_ID)


    private val notificationManager =
        appContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    private lateinit var workoutNotification: Notification

    init {
        createNotificationChannels()
        initializeNotificationBuilders()
    }

    private fun initializeNotificationBuilders() {
        val contentIntent = PendingIntent.getActivity(
            appContext,
            0,
            Intent(appContext, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_IMMUTABLE
        )

        workoutNotificationBuilder
            .setSmallIcon(R.drawable.ic_notification_monochrome)
            .setColor(appContext.getColor(R.color.md_theme_primary))
            .setCategory(NotificationCompat.CATEGORY_WORKOUT)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(contentIntent)
            .setAutoCancel(false)
            .setOngoing(true)
            .setOnlyAlertOnce(true)


        timerNotificationBuilder
            .setSmallIcon(R.drawable.ic_notification_monochrome)
            .setColor(appContext.getColor(R.color.md_theme_primary))
            .setContentTitle(appContext.getString(R.string.rest_time_is_over))
            .setCategory(NotificationCompat.CATEGORY_WORKOUT)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentIntent(contentIntent)
            .setAutoCancel(true)

        timerNotification = timerNotificationBuilder.build()
    }


    private fun createNotificationChannels() {
        val workoutChannel = NotificationChannel(
            WORKOUT_CHANNEL_ID,
            appContext.getString(R.string.workout_info_channel_title),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = appContext.getString(R.string.workout_info_channel_desc)
        }

        workoutChannel.setSound(
            "android.resource://${appContext.packageName}/raw/system_notification".toUri(),
            AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                .build()
        )

        notificationManager.createNotificationChannel(workoutChannel)


        val timerChannel = NotificationChannel(
            TIMER_CHANNEL_ID,
            appContext.getString(R.string.rest_timer_channel_title),
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = appContext.getString(R.string.rest_timer_channel_desc)
        }

        timerChannel.setSound(null, null)
        timerChannel.enableVibration(true)
        timerChannel.lockscreenVisibility = Notification.VISIBILITY_PUBLIC

        notificationManager.deleteNotificationChannel(LEGACY_TIMER_CHANNEL_ID)
        notificationManager.createNotificationChannel(timerChannel)
    }

    fun createWorkoutNotification(): Notification {
        workoutNotification = workoutNotificationBuilder.build()
        return workoutNotification
    }

    fun notifyOngoingWorkout(
        timeInSeconds: Int,
        isStopwatchPaused: Boolean,
        restTime: Int = 0,
        initialRestTime: Int = 0,
    ) {
        val builder = workoutNotificationBuilder

        builder.clearActions()

        if (isStopwatchPaused) {
            builder
                .addAction(
                    R.drawable.ic_play_arrow,
                    appContext.getString(R.string.resume),
                    createWorkoutServiceIntent(action = WorkoutServiceActions.START_STOPWATCH.string)
                )
        } else {
            builder
                .addAction(
                    R.drawable.ic_pause,
                    appContext.getString(R.string.pause),
                    createWorkoutServiceIntent(action = WorkoutServiceActions.PAUSE_STOPWATCH.string)
                )
        }


        val isResting = restTime != 0 && initialRestTime != 0

        if (isResting) {
            applyRestTimerLiveUpdate(builder, restTime, initialRestTime)
        } else {
            applyWorkoutLiveUpdate(builder, timeInSeconds, isStopwatchPaused)
        }

        if (isResting) {
            builder
                .setContentTitle(
                    appContext.getString(R.string.rest) + ": " + formatTime(restTime - 1).substring(
                        startIndex = 3
                    )
                )
                .setContentText(
                    appContext.getString(R.string.time) + ": " + formatTime(
                        timeInSeconds
                    )
                )
                .addAction(
                    R.drawable.ic_replay_10,
                    appContext.getString(R.string.reduce_ten_seconds),
                    createWorkoutServiceIntent(
                        action = WorkoutServiceActions.SUBTRACT_TEN_SECONDS_TO_REST_TIMER.string
                    )
                )
                .addAction(
                    R.drawable.ic_forward_10,
                    appContext.getString(R.string.add_ten_seconds),
                    createWorkoutServiceIntent(
                        action = WorkoutServiceActions.ADD_TEN_SECONDS_TO_REST_TIMER.string
                    )
                )
        } else {
            builder
                .setContentTitle(
                    appContext.getString(R.string.elapsed_time) + ": " + formatTime(
                        timeInSeconds
                    )
                )
                .setContentText("")
        }




        workoutNotification = builder.build()

        notificationManager.notify(WORKOUT_NOTIFICATION_ID, workoutNotification)
    }


    /**
     * Turns the workout notification into a Live Update while the rest timer runs.
     *
     * On Android 16+ a promoted ongoing notification with [NotificationCompat.ProgressStyle] is
     * shown as a Live Update: a status bar chip and a card at the top of the shade. Samsung One UI
     * 8 and 9 show these Live Updates in the Now Bar (lock screen and AOD) as a live notification,
     * so no Samsung-specific SDK is needed. On older versions the style falls back to a regular
     * progress bar and the promotion request is ignored.
     *
     * The remaining time is shown as a count-down chronometer, so the chip and the Now Bar keep
     * ticking even when the system throttles the per-second updates.
     */
    private fun applyRestTimerLiveUpdate(
        builder: NotificationCompat.Builder,
        restTime: Int,
        initialRestTime: Int,
    ) {
        val remainingSeconds = (restTime - 1).coerceAtLeast(0)
        // Adding ten seconds can push the remaining time past the initial one
        val totalSeconds = maxOf(initialRestTime, remainingSeconds, 1)
        val primaryColor = appContext.getColor(R.color.md_theme_primary)

        builder
            .setStyle(
                NotificationCompat.ProgressStyle()
                    .setStyledByProgress(true)
                    .setProgressSegments(
                        listOf(
                            NotificationCompat.ProgressStyle.Segment(totalSeconds)
                                .setColor(primaryColor)
                        )
                    )
                    .setProgress(totalSeconds - remainingSeconds)
            )
            .setRequestPromotedOngoing(true)
            .setShowWhen(true)
            .setWhen(System.currentTimeMillis() + remainingSeconds * 1000L)
            .setUsesChronometer(true)
            .setChronometerCountDown(true)
    }

    /**
     * Keeps the workout notification a Live Update between rests, so the status bar chip and the
     * Samsung Now Bar show the workout for its whole length instead of only while resting.
     *
     * The elapsed time is shown as a counting-up chronometer started at the beginning of the
     * workout, so the chip keeps ticking on its own. While the stopwatch is paused there is no
     * chronometer and the chip shows only the icon.
     */
    private fun applyWorkoutLiveUpdate(
        builder: NotificationCompat.Builder,
        timeInSeconds: Int,
        isStopwatchPaused: Boolean,
    ) {
        builder
            .setStyle(null)
            .setProgress(0, 0, false)
            .setRequestPromotedOngoing(true)
            .setChronometerCountDown(false)

        if (isStopwatchPaused) {
            builder
                .setUsesChronometer(false)
                .setShowWhen(false)
        } else {
            builder
                .setShowWhen(true)
                .setWhen(System.currentTimeMillis() - timeInSeconds * 1000L)
                .setUsesChronometer(true)
        }
    }

    private lateinit var timerNotification: Notification

    fun notifyTimerIsOver() {
        notificationManager.notify(TIMER_NOTIFICATION_ID, timerNotification)
    }

    fun cancelTimerIsOverNotification() {
        notificationManager.cancel(TIMER_NOTIFICATION_ID)
    }


    private fun createWorkoutServiceIntent(
        action: String
    ): PendingIntent {
        val intent = Intent(appContext, WorkoutService::class.java).apply {
            this.action = action
        }
        return PendingIntent.getService(appContext, 0, intent, PendingIntent.FLAG_IMMUTABLE)
    }
}