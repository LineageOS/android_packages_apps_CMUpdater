/*
 * Copyright (C) 2012 The CyanogenMod Project
 * Copyright (C) 2017 The LineageOS Project
 *
 * * Licensed under the GNU GPLv2 license
 *
 * The text of the license can be found in the LICENSE file
 * or at https://www.gnu.org/licenses/gpl-2.0.txt
 */

package com.cyanogenmod.updater.misc;

public class Constants {
    // Download related
    public static final String UPDATES_FOLDER = "updates";
    public static final String DOWNLOAD_ID = "download_id";
    public static final String DOWNLOAD_NAME = "download_name";
    public static final String DOWNLOAD_TMP_EXT = ".tmp";

    // Preferences
    public static final String ENABLE_PREF = "pref_enable_updates";
    public static final String UPDATE_CHECK_PREF = "pref_update_check_interval";
    public static final String UPDATE_TYPE_PREF = "pref_update_type";
    public static final String LAST_UPDATE_CHECK_PREF = "pref_last_update_check";
    public static final String IGNORE_MOBILE_DATA_WARNING_PREF = "pref_ignore_mobile_data_warning";

    // Update Check items
    public static final String BOOT_CHECK_COMPLETED = "boot_check_completed";
    public static final int UPDATE_FREQ_NONE = -2;
    public static final int UPDATE_FREQ_DAILY = 86400;
    public static final int UPDATE_FREQ_WEEKLY = 604800;
    public static final int UPDATE_FREQ_BI_WEEKLY = 1209600;
    public static final int UPDATE_FREQ_MONTHLY = 2419200;

    // Properties
    public static final String PROPERTY_CM_RELEASETYPE = "ro.cm.releasetype";

    // Defaults
    public static final String CM_UPDATE_TYPE_DEFAULT = "NIGHTLY";
    public static final String CM_RELEASE_TYPE_DEFAULT = "UNOFFICIAL";
}
