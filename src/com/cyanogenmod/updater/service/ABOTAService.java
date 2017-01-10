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
import android.os.AsyncTask;
import android.os.UpdateEngine;
import android.os.UpdateEngineCallback;
import android.util.Log;

import com.cyanogenmod.updater.receiver.ABOTANotifier;
import com.cyanogenmod.updater.utils.Utils;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class ABOTAService extends IntentService {

    private static final String TAG = "ABOTAService";

    // broadcast actions
    public static final String ACTION_UPDATE_INSTALL_FINISHED = "com.cyanogenmod.cmupdater.action.ACTION_UPDATE_INSTALL_FINISHED";
    public static final String ACTION_UPDATE_INSTALL_FAILED = "com.cyanogenmod.cmupdater.action.ACTION_UPDATE_INSTALL_FAILED";

    private static Context mContext;

    public ABOTAService() {
        super("ABOTAService");
    }

    private class UpdateEngineCB extends UpdateEngineCallback {
        @Override
        public void onStatusUpdate(int status, float percent) {
            Log.e(TAG, "Percent: " + percent);
            ABOTANotifier.notifyOngoingABOTA(mContext, (int)(percent*100));
        }

        @Override
        public void onPayloadApplicationComplete(int errorCode) {
            // addon.d stuff here
            Intent mIntent = new Intent(ACTION_UPDATE_INSTALL_FINISHED);
            sendBroadcast(mIntent);
        }
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        mContext = getApplicationContext();
        String extractionSite = Utils.makeUpdateFolder(mContext).getPath() + "/unzipped/";

        try {
            Scanner sc = new Scanner(new File(extractionSite + "payload_properties.txt"));
            List<String> lines = new ArrayList<String>();
            while (sc.hasNextLine()) {
                lines.add(sc.nextLine());
            }

            String[] header = lines.toArray(new String[0]);
            UpdateEngine mUpdateEngine = new UpdateEngine();
            mUpdateEngine.bind(new UpdateEngineCB());
            mUpdateEngine.applyPayload("file://" + extractionSite + "payload.bin", (long) 0, (long) 0, header);

        } catch (FileNotFoundException e) {
            Log.e(TAG, "Failed to read extracted zip", e);
            Intent mIntent = new Intent(ACTION_UPDATE_INSTALL_FAILED);
            sendBroadcast(mIntent);
        }
    }
}
