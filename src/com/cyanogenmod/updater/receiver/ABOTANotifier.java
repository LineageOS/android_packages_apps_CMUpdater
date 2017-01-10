/*
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
import android.os.UpdateEngine.ErrorCodeConstants;
import android.os.UpdateEngine.UpdateStatusConstants;
import android.support.v4.app.NotificationCompat.Builder;

import com.cyanogenmod.updater.R;
import com.cyanogenmod.updater.utils.Utils;
import com.cyanogenmod.updater.service.ZipExtractionService.ZipExtractionStatusConstants;

import java.io.File;

public class ABOTANotifier {

    private ABOTANotifier() {
        // Don't instantiate me bro
    }

    public static void notifyOngoingABOTA(Context context, int progress, int status) {
        Builder builder = new Builder(context).setSmallIcon(R.drawable.ic_system_update);

        if (progress < 0) {
            ((NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE))
                .cancel(R.string.installing_package);

            switch (status) {
                case ZipExtractionStatusConstants.ERRORED:
                    builder.setContentTitle(context.getString(R.string.error_title))
                           .setContentText(String.format(context.getString(R.string.error_extracting_zip, status)));
                    break;
                default:
                    builder.setContentTitle(context.getString(R.string.error_title))
                           .setContentText(String.format(context.getString(R.string.error_update_engine, status)));
                    break;
            }

        } else {
            switch (status) {
                case ZipExtractionStatusConstants.EXTRACTING:
                    builder.setProgress(0, 0, true)
                           .setOngoing(true)
                           .setContentTitle(context.getString(R.string.processing_zip_file));
                    break;
                case UpdateStatusConstants.DOWNLOADING:
                    builder.setProgress(100, progress, false)
                           .setOngoing(true)
                           .setContentText(String.format("%1$d%%", progress))
                           .setContentTitle(context.getString(R.string.installing_package));
                    break;

                case UpdateStatusConstants.FINALIZING:
                    if (progress == 0) {
                        builder.setProgress(0, 0, true)
                               .setOngoing(true)
                               .setContentTitle(context.getString(R.string.finalizing_package));
                    } else if (progress < 100) {
                        builder.setProgress(100, progress, false)
                               .setOngoing(true)
                               .setContentText(String.format("%1$d%%", progress))
                               .setContentTitle(context.getString(R.string.preparing_ota_first_boot));
                    } else {
                        ((NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE))
                            .cancel(R.string.installing_package);

                        builder.setContentText(context.getString(R.string.installing_package_finished))
                               .addAction(R.drawable.ic_tab_install,
                                   context.getString(R.string.not_action_install_reboot),
                                   createRebootPendingIntent(context));
                    }
                    break;

                case UpdateStatusConstants.UPDATED_NEED_REBOOT:
                    ((NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE))
                        .cancel(R.string.installing_package);

                    builder.setContentText(context.getString(R.string.installing_package_finished))
                           .addAction(R.drawable.ic_tab_install,
                               context.getString(R.string.not_action_install_reboot),
                               createRebootPendingIntent(context));
                    break;

                default:
                    builder.setContentTitle(context.getString(R.string.error_title))
                           .setContentText(String.format(context.getString(R.string.error_update_engine, status)));
                    break;
            }
        }

        ((NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE))
                .notify(R.string.installing_package, builder.build());
    }

    private static PendingIntent createRebootPendingIntent(Context context) {
        Intent rebootIntent = new Intent(context, DownloadReceiver.class);
        rebootIntent.setAction(DownloadReceiver.ACTION_INSTALL_REBOOT);

        return PendingIntent.getBroadcast(context, 0,
                rebootIntent, PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_UPDATE_CURRENT);
    }
}
