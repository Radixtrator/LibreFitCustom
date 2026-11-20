/*
 * SPDX-License-Identifier: GPL-3.0-or-later
 * Copyright (c) 2026. The LibreFit Contributors
 *
 * LibreFit is subject to additional terms covering author attribution and trademark usage;
 * see the ADDITIONAL_TERMS.md and TRADEMARK_POLICY.md files in the project root.
 */

package org.librefit.helpers

import android.app.NotificationManager
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings

/**
 * Whether the workout notification can be shown as a Live Update (status bar chip, Samsung Now
 * Bar), and a way to the system setting that turns them on for LibreFit.
 *
 * Live Updates exist from Android 16. The user can turn them off per app, in which case the
 * workout notification is shown as a regular one. Refer to [NotificationHelper].
 */
object LiveUpdates {

    private const val ACTION_MANAGE_APP_PROMOTED_NOTIFICATIONS =
        "android.settings.MANAGE_APP_PROMOTED_NOTIFICATIONS"

    val isSupported: Boolean
        get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA

    /**
     * Whether the user lets LibreFit post Live Updates. It is `false` where they are not supported,
     * and `true` when the system cannot tell, so the setting is not shown as off by mistake.
     */
    fun canPost(context: Context): Boolean {
        if (!isSupported) return false
        val manager = context.getSystemService(NotificationManager::class.java) ?: return true
        return runCatching { manager.canPostPromotedNotifications() }.getOrDefault(true)
    }

    /**
     * Opens the system setting for the Live Updates of LibreFit, falling back to its notification
     * settings on systems without a dedicated page.
     */
    fun openSettings(context: Context) {
        val promoted = Intent(ACTION_MANAGE_APP_PROMOTED_NOTIFICATIONS)
            .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try {
            context.startActivity(promoted)
        } catch (_: ActivityNotFoundException) {
            context.startActivity(
                Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                    .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }
    }
}
