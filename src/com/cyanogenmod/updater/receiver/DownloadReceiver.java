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

import android.app.DownloadManager;
import android.app.StatusBarManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

import com.cyanogenmod.updater.R;
import com.cyanogenmod.updater.misc.Constants;
import com.cyanogenmod.updater.misc.UpdateInfo;
import com.cyanogenmod.updater.service.DownloadCompleteIntentService;
import com.cyanogenmod.updater.service.DownloadService;
import com.cyanogenmod.updater.utils.Utils;

import java.io.IOException;

public class DownloadReceiver extends BroadcastReceiver{
    private static final String TAG = "DownloadReceiver";

    public static final String ACTION_START_DOWNLOAD = "com.cyanogenmod.cmupdater.action.START_DOWNLOAD";
    public static final String EXTRA_UPDATE_INFO = "update_info";

    public static final String ACTION_DOWNLOAD_STARTED = "com.cyanogenmod.cmupdater.action.DOWNLOAD_STARTED";

    static final String ACTION_INSTALL_UPDATE = "com.cyanogenmod.cmupdater.action.INSTALL_UPDATE";
    public static final String ACTION_INSTALL_REBOOT = "com.cyanogenmod.cmupdater.action.INSTALL_REBOOT";
    static final String EXTRA_FILENAME = "filename";
    static final String EXTRA_IS_AB_UPDATE = "is_ab_update";

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();

        if (ACTION_START_DOWNLOAD.equals(action)) {
            UpdateInfo ui = (UpdateInfo) intent.getParcelableExtra(EXTRA_UPDATE_INFO);
            handleStartDownload(context, ui);
        } else if (DownloadManager.ACTION_DOWNLOAD_COMPLETE.equals(action)) {
            long id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);
            handleDownloadComplete(context, id);
        } else if (ACTION_INSTALL_UPDATE.equals(action)) {
            StatusBarManager sb = (StatusBarManager) context.getSystemService(Context.STATUS_BAR_SERVICE);
            sb.collapsePanels();
            String fileName = intent.getStringExtra(EXTRA_FILENAME);
            boolean isABUpdate = intent.getBooleanExtra(EXTRA_IS_AB_UPDATE, false);
            if (isABUpdate) {
                Utils.cancelNotification(context);
                Utils.triggerUpdateAB(context, fileName);
            } else {
                try {
                    Utils.triggerUpdate(context, fileName);
                } catch (IOException e) {
                    Log.e(TAG, "Unable to reboot into recovery mode", e);
                    Toast.makeText(context, R.string.apply_unable_to_reboot_toast,
                                Toast.LENGTH_SHORT).show();
                    Utils.cancelNotification(context);
                }
            }
        } else if (ACTION_INSTALL_REBOOT.equals(action)) {
            Utils.triggerReboot(context);
        }
    }

    private void handleStartDownload(Context context, UpdateInfo ui) {
        DownloadService.start(context, ui);
    }

    private void handleDownloadComplete(Context context, long id) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        long enqueued = prefs.getLong(Constants.DOWNLOAD_ID, -1);
        String fileName = prefs.getString(Constants.DOWNLOAD_NAME, null);
        if (enqueued < 0 || id < 0 || id != enqueued || fileName == null) {
            return;
        }

        // Send off to DownloadCompleteIntentService
        Intent intent = new Intent(context, DownloadCompleteIntentService.class);
        intent.putExtra(Constants.DOWNLOAD_ID, id);
        intent.putExtra(Constants.DOWNLOAD_NAME, fileName);
        context.startService(intent);

        // Clear the shared prefs
        prefs.edit()
                .remove(Constants.DOWNLOAD_ID)
                .remove(Constants.DOWNLOAD_NAME)
                .apply();
    }
}
