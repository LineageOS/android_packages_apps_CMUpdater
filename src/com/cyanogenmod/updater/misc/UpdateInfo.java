/*
 * Copyright (C) 2012 The CyanogenMod Project
 *
 * * Licensed under the GNU GPLv2 license
 *
 * The text of the license can be found in the LICENSE file
 * or at https://www.gnu.org/licenses/gpl-2.0.txt
 */

package com.cyanogenmod.updater.misc;

import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;

import com.cyanogenmod.updater.utils.Utils;
import com.cyanogenmod.updater.misc.Constants;

import java.io.File;
import java.io.Serializable;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class UpdateInfo implements Parcelable, Serializable {
    private static final long serialVersionUID = 5499890003569313403L;
    public static final String CHANGELOG_EXTENSION = ".changelog.html";

    public enum Type {
        SNAPSHOT,
        NIGHTLY,
        EXPERIMENTAL,
        UNOFFICIAL,
        STABLE,
        RC,
        UNKNOWN
    };
    private String mUiName;
    private String mFileName;
    private Type mType = Type.UNKNOWN;
    private int mApiLevel;
    private long mBuildDate;
    private String mDownloadUrl;
    private String mChangelogUrl;
    private String mVersion;

    private Boolean mIsNewerThanInstalled;

    private UpdateInfo() {
        // Use the builder
    }

    private UpdateInfo(Parcel in) {
        readFromParcel(in);
    }

    public File getChangeLogFile(Context context) {
        return new File(context.getCacheDir(), mFileName + CHANGELOG_EXTENSION);
    }

    /**
     * Get API level
     */
    public int getApiLevel() {
        return mApiLevel;
    }

    /**
     * Get name for UI display
     */
    public String getName() {
        return mUiName;
    }

    /**
     * Get file name
     */
    public String getFileName() {
        return mFileName;
    }

    /**
     * Set file name
     */
    public void setFileName(String fileName) {
        mFileName = fileName;
        UpdateInfo parsed = parseFileName(mFileName);
        if (mUiName == null) {
            mUiName = parsed.mUiName;
        }
        if (mVersion == null) {
            mVersion = parsed.mVersion;
        }
        if (mBuildDate == 0) {
            mBuildDate = parsed.mBuildDate;
        }
        if (mType == Type.UNKNOWN) {
            mType = parsed.mType;
        }
    }

    /**
     * Get build type
     */
    public Type getType() {
        return mType;
    }

    /**
     * Convert build type to string
     */
    public String getTypeString() {
        switch (mType) {
            case STABLE:
                return Constants.CM_RELEASETYPE_STABLE;
            case RC:
                return Constants.CM_RELEASETYPE_RC;
            case SNAPSHOT:
                return Constants.CM_RELEASETYPE_SNAPSHOT;
            case NIGHTLY:
                return Constants.CM_RELEASETYPE_NIGHTLY;
            case EXPERIMENTAL:
                return Constants.CM_RELEASETYPE_EXPERIMENTAL;
            case UNOFFICIAL:
                return Constants.CM_RELEASETYPE_UNOFFICIAL;
            case UNKNOWN:
            default:
                return Constants.CM_RELEASETYPE_UNKNOWN;
        }
    }

    /**
     * Get build timestamp
     */
    public long getDate() {
        return mBuildDate;
    }

    /**
     * Get download location
     */
    public String getDownloadUrl() {
        return mDownloadUrl;
    }

    /**
     * Get changelog location
     */
    public String getChangelogUrl() {
        return mChangelogUrl;
    }

    /**
     * Get version
     */
    public String getVersion() {
        return mVersion;
    }

    public boolean isNewerThanInstalled() {
        if (mIsNewerThanInstalled != null) {
            return mIsNewerThanInstalled;
        }

        int installedApiLevel = Utils.getInstalledApiLevel();
        if (installedApiLevel != mApiLevel && mApiLevel > 0) {
            mIsNewerThanInstalled = mApiLevel > installedApiLevel;
        } else {
            // API levels match, so compare build dates.
            mIsNewerThanInstalled = mBuildDate > Utils.getInstalledBuildDate();
        }

        return mIsNewerThanInstalled;
    }

    public boolean isSameVersion(String version) {
        if (TextUtils.equals(version, mVersion)) {
            return true;
        }

        return false;
    }

    public boolean isCompatible(UpdateInfo update) {
        if (this.equals(update)) {
            return true;
        }
        if (!isSameVersion(update.getVersion())) {
            return false;
        }
        if (!isNewerThanInstalled()) {
            return false;
        }
        // XXXX Add other checks here

        return true;
    }

    public static String extractUiName(String fileName) {
        String deviceType = Utils.getDeviceType();
        String uiName = fileName.replaceAll("\\.zip$", "");
        return uiName.replaceAll("-" + deviceType + "-?", "");
    }

    @Override
    public String toString() {
        return "UpdateInfo: " + mFileName;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }

        if (!(o instanceof UpdateInfo)) {
            return false;
        }

        UpdateInfo ui = (UpdateInfo) o;
        return TextUtils.equals(mFileName, ui.mFileName)
                && mType.equals(ui.mType)
                && mBuildDate == ui.mBuildDate
                && TextUtils.equals(mDownloadUrl, ui.mDownloadUrl);
    }

    public static final Parcelable.Creator<UpdateInfo> CREATOR = new Parcelable.Creator<UpdateInfo>() {
        public UpdateInfo createFromParcel(Parcel in) {
            return new UpdateInfo(in);
        }

        public UpdateInfo[] newArray(int size) {
            return new UpdateInfo[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        out.writeString(mUiName);
        out.writeString(mFileName);
        out.writeString(mType.toString());
        out.writeInt(mApiLevel);
        out.writeLong(mBuildDate);
        out.writeString(mDownloadUrl);
        out.writeString(mVersion);
    }

    private void readFromParcel(Parcel in) {
        mUiName = in.readString();
        mFileName = in.readString();
        mType = Enum.valueOf(Type.class, in.readString());
        mApiLevel = in.readInt();
        mBuildDate = in.readLong();
        mDownloadUrl = in.readString();
        mVersion = in.readString();
    }

    public static class Builder {
        private String mUiName;
        private String mFileName;
        private Type mType = Type.UNKNOWN;
        private int mApiLevel;
        private long mBuildDate;
        private String mDownloadUrl;
        private String mChangelogUrl;
        private String mVersion;

        public Builder setName(String uiName) {
            mUiName = uiName;
            return this;
        }

        public Builder setFileName(String fileName) {
            mFileName = fileName;
            mUiName = initializeName(fileName);
            return this;
        }

        public Builder setType(String typeString) {
            mType = stringToType(typeString);
            return this;
        }

        public Builder setType(Type type) {
            mType = type;
            return this;
        }

        public Builder setApiLevel(int apiLevel) {
            mApiLevel = apiLevel;
            return this;
        }

        public Builder setBuildDate(long buildDate) {
            mBuildDate = buildDate;
            return this;
        }

        public Builder setDownloadUrl(String downloadUrl) {
            mDownloadUrl = downloadUrl;
            return this;
        }

        public Builder setChangelogUrl(String changelogUrl) {
            mChangelogUrl = changelogUrl;
            return this;
        }

        public Builder setVersion(String version) {
            mVersion = version;
            return this;
        }

        public UpdateInfo build() {
            UpdateInfo info = new UpdateInfo();
            UpdateInfo parsed = parseFileName(mFileName);

            if (mVersion == null) {
                info.mVersion = parsed.mVersion;
            } else {
                info.mVersion = mVersion;
            }

            if (mBuildDate == 0) {
                info.mBuildDate = parsed.mBuildDate;
            } else {
                info.mBuildDate = mBuildDate;
            }

            if (mType == Type.UNKNOWN) {
                info.mType = parsed.mType;
            } else {
                info.mType = mType;
            }

            info.mUiName = mUiName;
            info.mFileName = mFileName;
            info.mApiLevel = mApiLevel;
            info.mDownloadUrl = mDownloadUrl;
            info.mChangelogUrl = mChangelogUrl;
            return info;
        }
    }

    private static String initializeName(String fileName) {
        if (!TextUtils.isEmpty(fileName)) {
             return extractUiName(fileName);
        } else {
             return null;
        }
    }

    private static Type stringToType(String typeString) {
        if (TextUtils.equals(typeString, "stable")) {
            return Type.STABLE;
        } else if (TextUtils.equals(typeString, "RC")) {
            return Type.RC;
        } else if (TextUtils.equals(typeString, "snapshot")) {
            return Type.SNAPSHOT;
        } else if (TextUtils.equals(typeString, "nightly")) {
            return Type.NIGHTLY;
        } else {
            return Type.UNKNOWN;
        }
    }

    // Parse filename and return a new UpdateInfo object
    public static UpdateInfo parseFileName(String fileName) {
        UpdateInfo info = new UpdateInfo();
        info.mType = Type.UNKNOWN;
        info.mVersion = "????";
        if (fileName == null) {
            // We're done, nothing to parse
            return info;
        }

        info.mFileName = fileName;
        info.mUiName = initializeName(fileName);

        String[] subStrings = fileName.split("-");

        if (subStrings.length >= 2 && subStrings[1] != null) {
            info.mVersion = subStrings[1];
        }

        if (subStrings.length >= 3 && subStrings[2] != null) {
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd");
            try {
                info.mBuildDate = dateFormat.parse(subStrings[2]).getTime();
            } catch (ParseException e) {
                info.mBuildDate = 0;
            }
        }

        if (subStrings.length >= 4 && subStrings[3] != null) {
            info.mType = stringToType(subStrings[3]);
        }

        return info;
    }
}
