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
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.cyanogenmod.updater.receiver.ABOTANotifier;
import com.cyanogenmod.updater.utils.Utils;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class ZipExtractionService extends IntentService {

    private static final String TAG = "ZipExtractionService";

    // broadcast actions
    public static final String ACTION_EXTRACT_FINISHED = "com.cyanogenmod.cmupdater.action.ACTION_EXTRACT_FINISHED";
    public static final String ACTION_EXTRACT_ERRORED = "com.cyanogenmod.cmupdater.action.ACTION_EXTRACT_ERRORED";
    public static final String EXTRA_ZIP_NAME = "zip_name";

    public class ZipExtractionStatusConstants {
        public static final int EXTRACTING = 1001;
        public static final int ERRORED = 1002;
    }

    public ZipExtractionService() {
        super("ZipExtractionService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Context context = getApplicationContext();

        ABOTANotifier.notifyOngoingABOTA(context, 0, ZipExtractionStatusConstants.EXTRACTING);

        String filename = intent.getStringExtra(EXTRA_ZIP_NAME);
        String outputdir = Utils.makeUpdateFolder(context).getPath() + "/unzipped/";

        try {
            ZipInputStream zin = new ZipInputStream(new FileInputStream(filename));
            ZipEntry entry;
            File d;

            d = new File(outputdir);
            if (d.exists()) {
                Utils.deleteDir(d);
            }

            d.mkdirs();

            while ((entry = zin.getNextEntry()) != null) {
                String name = entry.getName();

                if (entry.isDirectory()) {
                    d = new File(outputdir, name);
                    if (!d.exists()) {
                        d.mkdirs();
                    }
                    continue;
                }

                int s = name.lastIndexOf(File.separatorChar);
                String dir = s == -1 ? null : name.substring(0, s);
                if (dir != null) {
                    d = new File(outputdir, dir);
                    if (!d.exists()) {
                        d.mkdirs();
                    }
                }

                byte[] buffer = new byte[4096];
                BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(new File(outputdir, name)));
                int count = -1;
                while ((count = zin.read(buffer)) != -1) {
                    out.write(buffer, 0, count);
                }
                out.close();
            }

            zin.close();

            Intent otaIntent = new Intent(context, ABOTAService.class);
            otaIntent.putExtra(ABOTAService.EXTRA_ZIP_NAME, filename);
            context.startService(otaIntent);
        } catch (IOException e) {
            Log.e(TAG, "Failed to extract zip", e);
            ABOTANotifier.notifyOngoingABOTA(context, -1, ZipExtractionStatusConstants.ERRORED);
            Intent mIntent = new Intent(ACTION_EXTRACT_ERRORED);
            sendBroadcast(mIntent);
        }
    }
}
