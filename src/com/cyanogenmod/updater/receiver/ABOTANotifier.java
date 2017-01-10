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
import android.os.UpdateEngine.UpdateStatusConstants;
import android.support.v4.app.NotificationCompat.Builder;

import com.cyanogenmod.updater.R;
import com.cyanogenmod.updater.utils.Utils;

import java.io.File;

public class ABOTANotifier {

    private ABOTANotifier() {
        // Don't instantiate me bro
    }

    public static void notifyOngoingABOTA(Context context, int progress, int status) {
        Builder builder = new Builder(context).setSmallIcon(R.drawable.ic_system_update);

        switch (status) {
            case UpdateStatusConstants.DOWNLOADING:
                builder.setOngoing(true)
                       .setProgress(100, progress, false)
                       .setContentTitle(context.getString(R.string.installing_package))
                       .setContentText(String.format("%1$d%%", progress));
            break;

            case UpdateStatusConstants.FINALIZING:
                builder.setOngoing(true)
                       .setContentTitle(context.getString(R.string.finalizing_package));
                if (progress == 0)
                    builder.setProgress(0, 0, true);
                else
                    builder.setProgress(100, progress, false)
                           .setContentText(String.format("%1$d%%", progress));
            break;

            case UpdateStatusConstants.UPDATED_NEED_REBOOT:
                ((NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE))
                    .cancel(R.string.installing_package);

                builder.setContentText(context.getString(R.string.installing_package_finished))
                       .addAction(R.drawable.ic_tab_install,
                           context.getString(R.string.not_action_install_reboot),
                           createRebootPendingIntent(context));
            break;
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
