/*
 * Copyright (C) 2017 The LineageOS Project
 *
 * * Licensed under the GNU GPLv2 license
 *
 * The text of the license can be found in the LICENSE file
 * or at https://www.gnu.org/licenses/gpl-2.0.txt
 */
package com.cyanogenmod.updater;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.widget.TextView;

import com.cyanogenmod.updater.utils.Utils;

public class UpdatesActivity extends AppCompatActivity {

    private UpdatesSettings mSettingsFragment;

    @Override
    protected void onCreate(Bundle savedInstance) {
        super.onCreate(savedInstance);
        setContentView(R.layout.activity_updater);

        Toolbar mToolbar = (Toolbar) findViewById(R.id.toolbar);
        TextView mHeaderCm = (TextView) findViewById(R.id.header_version);
        TextView mHeaderInfo = (TextView) findViewById(R.id.header_info);

        mSettingsFragment = new UpdatesSettings();

        String[] mInstalled = Utils.getInstalledVersion().split("-");

        mHeaderCm.setText(String.format(getString(R.string.header_lineage), mInstalled[0]));
        String mApi;
        switch (Utils.getInstalledApiLevel()) {
            case 25:
                mApi = "7.1.1";
                break;
            default:
                mApi = "API " + String.valueOf(Utils.getInstalledApiLevel());
        }
        mHeaderInfo.setText(String.format(getString(R.string.update_api), mApi,
                mInstalled[0], Utils.getInstalledBuildDate()));

        mToolbar.setTitle("");
        //mToolbar.setNavigationIcon(R.drawable.ic_cid_head);
        setSupportActionBar(mToolbar);

        getFragmentManager().beginTransaction()
                .replace(R.id.content_frame, mSettingsFragment).commit();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        // Check if we need to refresh the screen to show new updates
        if (intent.getBooleanExtra(UpdatesSettings.EXTRA_UPDATE_LIST_UPDATED, false)) {
            mSettingsFragment.updateLayout();
        }

        mSettingsFragment.checkForDownloadCompleted(intent);
    }

}
