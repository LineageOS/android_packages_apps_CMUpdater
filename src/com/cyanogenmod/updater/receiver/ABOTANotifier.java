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
import android.content.Context;
import android.support.v4.app.NotificationCompat;

import com.cyanogenmod.updater.R;

import java.io.File;

public class ABOTANotifier {

    private ABOTANotifier() {
        // Don't instantiate me bro
    }

    public static void notifyOngoingABOTA(Context context) {
        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(context)
            .setSmallIcon(R.drawable.ic_system_update)
            .setContentTitle(context.getString(R.string.installing_package))
            .setContentText(context.getString(R.string.installing_package_progress));

        ((NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE))
                .notify(R.string.installing_package, mBuilder.build());
    }
}
