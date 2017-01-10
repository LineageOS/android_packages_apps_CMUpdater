/*
 * Copyright (C) 2017 The LineageOS Project
 *
 * * Licensed under the GNU GPLv2 license
 *
 * The text of the license can be found in the LICENSE file
 * or at https://www.gnu.org/licenses/gpl-2.0.txt
 */

package com.cyanogenmod.updater.service;

import android.app.IntentService;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.ServiceSpecificException;
import android.os.UpdateEngine;
import android.os.UpdateEngine.ErrorCodeConstants;
import android.os.UpdateEngine.UpdateStatusConstants;
import android.os.UpdateEngineCallback;
import android.support.v4.app.NotificationCompat.Builder;
import android.util.Log;

import com.cyanogenmod.updater.R;
import com.cyanogenmod.updater.misc.Constants;
import com.cyanogenmod.updater.receiver.DownloadReceiver;
import com.cyanogenmod.updater.utils.Utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Scanner;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class ABOTAService extends IntentService {

    private static final String TAG = "ABOTAService";

    // broadcast actions
    public static final String ACTION_UPDATE_INSTALL_FINISHED = "com.cyanogenmod.cmupdater.action.ACTION_UPDATE_INSTALL_FINISHED";
    public static final String ACTION_UPDATE_INSTALL_ERRORED = "com.cyanogenmod.cmupdater.action.ACTION_UPDATE_INSTALL_ERRORED";

    public static final int STATUS_PREPARING_ZIP = 1000;

    public static final String EXTRA_ZIP_NAME = "zip_name";
    public static final String EXTRA_ERROR_CODE = "error_code";;

    private static boolean mABUpdateRunning = false;
    private static Context mContext;

    private boolean errorCaught = false;
    private String mFilename;
    private final String updatePackageDir = "/data/ota_package/" + Constants.UPDATES_FOLDER;

    public ABOTAService() {
        super("ABOTAService");
    }

    private class UpdateEngineCB extends UpdateEngineCallback {
        /*
         *  frameworks/base/core/java/android/os/UpdateEngine.java
         *
         *  public static final class UpdateStatusConstants {
         *    public static final int IDLE = 0;
         *    public static final int CHECKING_FOR_UPDATE = 1;
         *    public static final int UPDATE_AVAILABLE = 2;
         *    public static final int DOWNLOADING = 3;
         *    public static final int VERIFYING = 4;
         *    public static final int FINALIZING = 5;
         *    public static final int UPDATED_NEED_REBOOT = 6;
         *    public static final int REPORTING_ERROR_EVENT = 7;
         *    public static final int ATTEMPTING_ROLLBACK = 8;
         *    public static final int DISABLED = 9;
         *  }
         *
         */
        @Override
        public void onStatusUpdate(int status, float percent) {
            if (!errorCaught) {
                switch (status) {
                    case UpdateStatusConstants.DOWNLOADING:
                    case UpdateStatusConstants.FINALIZING:
                    case UpdateStatusConstants.UPDATED_NEED_REBOOT:
                        notifyOngoingABOTA(mContext, (int)(percent*100), status);
                        break;
                    case UpdateStatusConstants.REPORTING_ERROR_EVENT:
                        Intent errorIntent = new Intent(ACTION_UPDATE_INSTALL_ERRORED);
                        errorIntent.putExtra(EXTRA_ERROR_CODE, status);
                        sendBroadcast(errorIntent);
                        break;
                }
            }
        }

        /*
         *  frameworks/base/core/java/android/os/UpdateEngine.java
         *
         *  public static final class ErrorCodeConstants {
         *    public static final int SUCCESS = 0;
         *    public static final int ERROR = 1;
         *    public static final int FILESYSTEM_COPIER_ERROR = 4;
         *    public static final int POST_INSTALL_RUNNER_ERROR = 5;
         *    public static final int PAYLOAD_MISMATCHED_TYPE_ERROR = 6;
         *    public static final int INSTALL_DEVICE_OPEN_ERROR = 7;
         *    public static final int KERNEL_DEVICE_OPEN_ERROR = 8;
         *    public static final int DOWNLOAD_TRANSFER_ERROR = 9;
         *    public static final int PAYLOAD_HASH_MISMATCH_ERROR = 10;
         *    public static final int PAYLOAD_SIZE_MISMATCH_ERROR = 11;
         *    public static final int DOWNLOAD_PAYLOAD_VERIFICATION_ERROR = 12;
         *  }
         *
         */

        @Override
        public void onPayloadApplicationComplete(int errorCode) {
            switch (errorCode) {
                case ErrorCodeConstants.SUCCESS:
                    File path = new File(updatePackageDir);
                    if (path.exists()) {
                        Utils.deleteDir(path);
                    }

                    Intent successIntent = new Intent(ACTION_UPDATE_INSTALL_FINISHED);
                    successIntent.putExtra(EXTRA_ZIP_NAME, mFilename);
                    sendBroadcast(successIntent);
                    break;

                default:
                    Intent errorIntent = new Intent(ACTION_UPDATE_INSTALL_ERRORED);
                    errorIntent.putExtra(EXTRA_ERROR_CODE, errorCode);
                    sendBroadcast(errorIntent);
                    break;
            }
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        return START_STICKY;
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        errorCaught = false;
        mContext = getApplicationContext();
        mFilename = intent.getStringExtra(EXTRA_ZIP_NAME);

        File path = new File(updatePackageDir);
        if (path.exists()) {
            Utils.deleteDir(path);
        }

        path.mkdir();
        path.setReadable(true, false);
        path.setExecutable(true, false);

        String updatePackagePath = updatePackageDir + "/" + mFilename;
        String inputPackagePath = Utils.makeUpdateFolder(mContext).getPath() + "/" + mFilename;

        try {
            Utils.copy(inputPackagePath, updatePackagePath);

            ZipFile zipFile = new ZipFile(updatePackagePath);
            Enumeration<? extends ZipEntry> zipEntries = zipFile.entries();
            long payloadOffset = 0;
            long offset = 0;
            List<String> lines = new ArrayList<String>();

            while (zipEntries.hasMoreElements()) {
                ZipEntry entry = (ZipEntry) zipEntries.nextElement();
                long fileSize = 0;
                long extra = entry.getExtra() == null ? 0 : entry.getExtra().length;
                offset += 30 + entry.getName().length() + extra;

                if (!entry.isDirectory()) {
                    fileSize = entry.getCompressedSize();
                    if ("payload.bin".equals(entry.getName())) {
                        Log.e(TAG, "Found offset for payload.bin at: " + offset);
                        payloadOffset = offset;
                    } else if ("payload_properties.txt".equals(entry.getName())) {
                        BufferedReader reader = new BufferedReader(new InputStreamReader(zipFile.getInputStream(entry)));
                        String line;
                        while ((line = reader.readLine()) != null) {
                            lines.add(line);
                        }
                    }
                }
                offset += fileSize;
            }

            UpdateEngine mUpdateEngine = new UpdateEngine();
            mUpdateEngine.bind(new UpdateEngineCB());
            mUpdateEngine.applyPayload("file://" + updatePackagePath, payloadOffset, 0, lines.toArray(new String[lines.size()]));

        } catch (IOException e) {
            errorCaught = true;
            Log.e(TAG, "IOException caught!", e);
            Intent errorIntent = new Intent(ACTION_UPDATE_INSTALL_ERRORED);
            sendBroadcast(errorIntent);
        } catch (ServiceSpecificException e) {
            errorCaught = true;
            Log.e(TAG, String.format("Failed to apply payload, ErrorCode: %d", e.errorCode), e);
            Intent errorIntent = new Intent(ACTION_UPDATE_INSTALL_ERRORED);
            errorIntent.putExtra(EXTRA_ERROR_CODE, e.errorCode);
            sendBroadcast(errorIntent);
        }
    }

    public static void notifyOngoingABOTA(Context context, int progress, int status) {
        Builder builder = new Builder(context).setSmallIcon(R.drawable.ic_system_update);

        switch (status) {
            case STATUS_PREPARING_ZIP:
                builder.setProgress(0, 0, true)
                       .setOngoing(true)
                       .setContentTitle(context.getString(R.string.preparing_package));
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
                           .setOngoing(true)
                           .addAction(R.drawable.ic_tab_install,
                               context.getString(R.string.not_action_install_reboot),
                               createRebootPendingIntent(context));
                }
                break;

            case UpdateStatusConstants.UPDATED_NEED_REBOOT:
                ((NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE))
                    .cancel(R.string.installing_package);

                builder.setContentText(context.getString(R.string.installing_package_finished))
                       .setOngoing(true)
                       .addAction(R.drawable.ic_tab_install,
                           context.getString(R.string.not_action_install_reboot),
                           createRebootPendingIntent(context));
                break;

            default:
                builder.setContentTitle(context.getString(R.string.error_title))
                       .setContentText(String.format(context.getString(R.string.error_update_engine, status)));
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

    public static boolean isABUpdateRunning() {
        return mABUpdateRunning;
    }

    public static void setABUpdateRunning(boolean status) {
        mABUpdateRunning = status;
    }
}
