/*
 * Copyright (C) 2013 The CyanogenMod Project
 * Copyright (C) 2017 The LineageOS Project
 *
 * * Licensed under the GNU GPLv2 license
 *
 * The text of the license can be found in the LICENSE file
 * or at https://www.gnu.org/licenses/gpl-2.0.txt
 */

package com.cyanogenmod.updater.utils;

import android.app.AlarmManager;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.PowerManager;
import android.os.SystemProperties;
import android.preference.PreferenceManager;
import android.util.Log;

import com.cyanogenmod.updater.R;
import com.cyanogenmod.updater.misc.Constants;
import com.cyanogenmod.updater.misc.UpdateInfo;
import com.cyanogenmod.updater.service.ABOTAService;
import com.cyanogenmod.updater.service.UpdateCheckService;
import com.cyanogenmod.updater.UpdatePreference;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.text.DateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class Utils {

    private static final String TAG = "Utils";

    private Utils() {
        // this class is not supposed to be instantiated
    }

    public static File makeUpdateFolder(Context context) {
        return context.getDir(Constants.UPDATES_FOLDER, Context.MODE_PRIVATE);
    }

    public static void cancelNotification(Context context) {
        final NotificationManager nm =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        nm.cancel(R.string.not_new_updates_found_title);
        nm.cancel(R.string.not_download_success);
    }

    public static String getDeviceType() {
        return SystemProperties.get("ro.cm.device");
    }

    public static String getInstalledVersion() {
        return SystemProperties.get("ro.cm.version").toLowerCase();
    }

    public static String getInstalledVersionName() {
        return getInstalledVersion().split("-")[0];
    }

    public static int getInstalledApiLevel() {
        return SystemProperties.getInt("ro.build.version.sdk", 0);
    }

    public static long getInstalledBuildDate() {
        return SystemProperties.getLong("ro.build.date.utc", 0);
    }

    public static String getInstalledBuildType() {
        return SystemProperties.get(Constants.PROPERTY_CM_RELEASETYPE,
                Constants.CM_RELEASE_TYPE_DEFAULT).toLowerCase();
    }

    public static UpdateInfo getInstalledUpdateInfo() {
        return new UpdateInfo.Builder()
            .setFileName("lineage-" + getInstalledVersion() + ".zip")
            .setVersion(getInstalledVersionName())
            .setApiLevel(getInstalledApiLevel())
            .setBuildDate(getInstalledBuildDate())
            .setType(getInstalledBuildType())
            .build();
    }

    public static String getDateLocalized(Context context, long unixTimestamp) {
        DateFormat f = DateFormat.getDateInstance(DateFormat.LONG, getCurrentLocale(context));
        f.setTimeZone(TimeZone.getTimeZone("UTC"));
        Date date = new Date(unixTimestamp * 1000);
        return f.format(date);
    }

    public static String getDateLocalizedFromFileName(Context context, String fileName) {
        return getDateLocalized(context, getTimestampFromFileName(fileName));
    }

    public static long getTimestampFromFileName(String fileName) {
        String[] subStrings = fileName.split("-");
        if (subStrings.length < 3 || subStrings[2].length() < 8) {
            Log.e(TAG, "The given filename is not valid: " + fileName);
            return 0;
        }
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd");
        try {
            return (dateFormat.parse(subStrings[2]).getTime() / 1000);
        } catch (ParseException e) {
            Log.e(TAG, "The given filename is not valid: " + fileName);
            return 0;
        }
    }

    public static String getAndroidVersion(String versionName) {
        if (versionName != null) {
            switch (versionName) {
                case "13.0":
                    return "6.0";
                case "14.1":
                    return "7.1";
            }
        }
        return "???";
    }

    public static String getVersionFromFileName(String fileName) {
        String[] subStrings = fileName.split("-");
        if (subStrings.length < 2 || subStrings[1].length() < 4) {
            Log.e(TAG, "The given filename is not valid: " + fileName);
            return "????";
        }
        return subStrings[1];
    }

    public static String getTypeFromFileName(String fileName) {
        String[] subStrings = fileName.split("-");
        if (subStrings.length < 4 || subStrings[3].length() < 7) {
           Log.e(TAG, "The given filename is not valid: " + fileName);
           return "???????";
        }
        return subStrings[3];
    }

    public static String getUserAgentString(Context context) {
        try {
            PackageManager pm = context.getPackageManager();
            PackageInfo pi = pm.getPackageInfo(context.getPackageName(), 0);
            return pi.packageName + "/" + pi.versionName;
        } catch (PackageManager.NameNotFoundException nnfe) {
            return null;
        }
    }

    public static boolean isOnline(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = cm.getActiveNetworkInfo();
        if (netInfo != null && netInfo.isConnected()) {
            return true;
        }
        return false;
    }

    public static void scheduleUpdateService(Context context, int updateFrequency) {
        // Load the required settings from preferences
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        long lastCheck = prefs.getLong(Constants.LAST_UPDATE_CHECK_PREF, 0);

        // Get the intent ready
        Intent i = new Intent(context, UpdateCheckService.class);
        i.setAction(UpdateCheckService.ACTION_CHECK);
        PendingIntent pi = PendingIntent.getService(context, 0, i, PendingIntent.FLAG_UPDATE_CURRENT);

        // Clear any old alarms and schedule the new alarm
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        am.cancel(pi);

        if (updateFrequency != Constants.UPDATE_FREQ_NONE) {
            am.setRepeating(AlarmManager.RTC_WAKEUP, lastCheck + updateFrequency, updateFrequency, pi);
        }
    }

    public static void triggerUpdateAB(Context context, String updateFileName) {
        Intent otaIntent = new Intent(context, ABOTAService.class);
        otaIntent.putExtra(ABOTAService.EXTRA_ZIP_NAME, updateFileName);
        context.startService(otaIntent);
    }

    public static void triggerUpdate(Context context, String updateFileName) throws IOException {
        // Create the path for the update package
        String updatePackagePath = makeUpdateFolder(context).getPath() + "/" + updateFileName;

        // Reboot into recovery and trigger the update
        android.os.RecoverySystem.installPackage(context, new File(updatePackagePath));
    }

    public static String getUpdateType() {
        return getInstalledBuildType();
    }

    public static void triggerReboot(Context context) {
        PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        pm.reboot(null);
    }

    public static Locale getCurrentLocale(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            return context.getResources().getConfiguration().getLocales()
                    .getFirstMatch(context.getResources().getAssets().getLocales());
        } else {
            return context.getResources().getConfiguration().locale;
        }
    }

    public static boolean deleteDir(File dir) {
        if (dir.isDirectory()) {
            String[] children = dir.list();
            for (String aChildren : children) {
                boolean success = deleteDir(new File(dir, aChildren));
                if (!success) {
                    return false;
                }
            }
        }
        // The directory is now empty so delete it
        return dir.delete();
    }

    public static boolean isABUpdate(Context context, String filename) {
        String zipPath = Utils.makeUpdateFolder(context).getPath() + "/" + filename;
        List nonABFiles = Arrays.asList("file_contexts.bin",
                                        "install/bin/backuptool.functions",
                                        "install/bin/backuptool.sh",
                                        "install/bin/otasigcheck.sh",
                                        "system.patch.dat",
                                        "system/build.prop",
                                        "META-INF/org/lineageos/releasekey",
                                        "META-INF/com/google/android/updater-script",
                                        "META-INF/com/google/android/update-binary",
                                        "system.new.dat",
                                        "boot.img",
                                        "system.transfer.list");

        List ABOTAFiles = Arrays.asList("payload_properties.txt",
                                        "care_map.txt",
                                        "payload.bin");
        boolean ret = false;

        try {
            ZipInputStream zin = new ZipInputStream(new FileInputStream(zipPath));
            ZipEntry entry;

            while ((entry = zin.getNextEntry()) != null) {
                String file = entry.getName();
                if (nonABFiles.contains(file)) {
                    break;
                } else if (ABOTAFiles.contains(file)) {
                    ret = true;
                    break;
                }
            }
            zin.close();

        } catch (IOException e) {
            Log.e(TAG, "Failed to examine zip", e);
        }

        return ret;
    }

    public static void copy(String src, String dst) throws IOException {
        InputStream in = new FileInputStream(new File(src));
        OutputStream out = new FileOutputStream(new File(dst));

        // Transfer bytes from in to out
        byte[] buf = new byte[1024];
        int len;
        while ((len = in.read(buf)) > 0) {
            out.write(buf, 0, len);
        }
        in.close();
        out.close();
    }
}
