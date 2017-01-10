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
import android.os.UpdateEngine;
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

    public ABOTAService() {
        super("ABOTAService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Context mContext = getApplicationContext();
        String extractionSite = Utils.makeUpdateFolder(mContext).getPath() + "/unzipped/";

        try {
            Scanner sc = new Scanner(new File(extractionSite + "payload_properties.txt"));
            List<String> lines = new ArrayList<String>();
            while (sc.hasNextLine()) {
                lines.add(sc.nextLine());
            }

            String[] header = lines.toArray(new String[0]);
            long offset = 0;
            long size = 0;

            UpdateEngine mUpdateEngine = new UpdateEngine();
//            mUpdateEngine.applyPayload("file://" + extractionSite + "payload.bin", offset, size, header);

            for (int i = 0; i < 100; i+= 10) {
                ABOTANotifier.notifyOngoingABOTA(mContext, i);
                try{Thread.sleep(1000);}catch(InterruptedException e){}
            }

            ABOTANotifier.notifyOngoingABOTA(mContext, 100);

            Intent mIntent = new Intent(ACTION_UPDATE_INSTALL_FINISHED);
            sendBroadcast(mIntent);
        } catch (FileNotFoundException e) {
            Log.e(TAG, "Failed to read extracted zip", e);
            Intent mIntent = new Intent(ACTION_UPDATE_INSTALL_FAILED);
            sendBroadcast(mIntent);
        }
    }
}
