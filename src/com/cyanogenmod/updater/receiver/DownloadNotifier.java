/*
 * Copyright (C) 2014 The CyanogenMod Project
 * Copyright (C) 2017 The LineageOS Project
 *
 * * Licensed under the GNU GPLv2 license
 *
 * The text of the license can be found in the LICENSE file
 * or at https://www.gnu.org/licenses/gpl-2.0.txt
 */

package com.cyanogenmod.updater.receiver;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.NotificationCompat;

import com.cyanogenmod.updater.R;
import com.cyanogenmod.updater.misc.UpdateInfo;
import com.cyanogenmod.updater.utils.Utils;

import java.io.File;

public class DownloadNotifier {

    private DownloadNotifier() {
        // Don't instantiate me bro
    }

    public static void notifyDownloadComplete(Context context,
            Intent updateIntent, File updateFile) {
        String updateUiName = UpdateInfo.extractUiName(updateFile.getName());
        boolean isABUpdate = Utils.isABUpdate(context, updateFile.getName());

        int styleBigText = isABUpdate ? R.string.not_download_install_notice_ab : R.string.not_download_install_notice;
        int actionButtonText = isABUpdate ? R.string.not_action_install : R.string.not_action_install_update;

        NotificationCompat.BigTextStyle style = new NotificationCompat.BigTextStyle()
                .setBigContentTitle(context.getString(R.string.not_download_success))
                .bigText(context.getString(styleBigText, updateUiName));

        NotificationCompat.Builder builder = createBaseContentBuilder(context, updateIntent)
                .setSmallIcon(R.drawable.ic_system_update)
                .setContentTitle(context.getString(R.string.not_download_success))
                .setContentText(updateUiName)
                .setTicker(context.getString(R.string.not_download_success))
                .setStyle(style)
                .addAction(R.drawable.ic_tab_install,
                        context.getString(actionButtonText),
                        createInstallPendingIntent(context, updateFile, isABUpdate));

        ((NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE))
                .notify(R.string.not_download_success, builder.build());
    }

    public static void notifyDownloadError(Context context,
            Intent updateIntent, int failureMessageResId) {
        NotificationCompat.Builder builder = createBaseContentBuilder(context, updateIntent)
                .setSmallIcon(android.R.drawable.stat_notify_error)
                .setContentTitle(context.getString(R.string.not_download_failure))
                .setContentText(context.getString(failureMessageResId))
                .setTicker(context.getString(R.string.not_download_failure));

        ((NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE))
                .notify(R.string.not_download_success, builder.build());
    }

    private static NotificationCompat.Builder createBaseContentBuilder(Context context,
            Intent updateIntent) {
        PendingIntent contentIntent = PendingIntent.getActivity(context, 1,
                updateIntent, PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_UPDATE_CURRENT);

        return new NotificationCompat.Builder(context)
                .setWhen(System.currentTimeMillis())
                .setContentIntent(contentIntent)
                .setLocalOnly(true)
                .setAutoCancel(true);
    }

    private static PendingIntent createInstallPendingIntent(Context context, File updateFile,
             boolean isABUpdate) {
        Intent installIntent = new Intent(context, DownloadReceiver.class);
        installIntent.setAction(DownloadReceiver.ACTION_INSTALL_UPDATE);
        installIntent.putExtra(DownloadReceiver.EXTRA_FILENAME, updateFile.getName());
        installIntent.putExtra(DownloadReceiver.EXTRA_IS_AB_UPDATE, isABUpdate);

        return PendingIntent.getBroadcast(context, 0,
                installIntent, PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_UPDATE_CURRENT);
    }
}
