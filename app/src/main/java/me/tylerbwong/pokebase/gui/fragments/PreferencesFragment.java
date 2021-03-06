/*
 * Copyright 2016 Tyler Wong
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package me.tylerbwong.pokebase.gui.fragments;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.support.v4.content.ContextCompat;
import android.view.View;
import android.widget.ListView;

import com.yarolegovich.lovelydialog.LovelyTextInputDialog;

import me.tylerbwong.pokebase.R;
import me.tylerbwong.pokebase.gui.activities.SignUpActivity;
import me.tylerbwong.pokebase.gui.activities.SplashActivity;

/**
 * @author Tyler Wong
 */
public class PreferencesFragment extends PreferenceFragment implements
        Preference.OnPreferenceClickListener {
    private Preference namePreference;
    private Preference updatePreference;
    private Preference versionPreference;

    private SharedPreferences pref;
    private boolean listStyled = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        pref = getActivity().getSharedPreferences(SplashActivity.ACTIVITY_PREF,
                Context.MODE_PRIVATE);
        View rootView = getView();
        ListView list = null;
        if (rootView != null) {
            list = rootView.findViewById(android.R.id.list);
        }
        if (list != null) {
            list.setDivider(null);
        }

        PackageInfo packageInfo = null;

        try {
            packageInfo = getActivity().getPackageManager().getPackageInfo(
                    getActivity().getPackageName(), 0);
        }
        catch (PackageManager.NameNotFoundException e) {

        }

        addPreferencesFromResource(R.xml.preferences);

        namePreference = getPreferenceScreen().findPreference(getString(R.string.trainer_name_key));
        updatePreference = getPreferenceScreen().findPreference(getString(R.string.update_key));
        versionPreference = getPreferenceScreen().findPreference(getString(R.string.version_key));

        namePreference.setOnPreferenceClickListener(this);
        updatePreference.setOnPreferenceClickListener(this);

        namePreference.setSummary(pref.getString(SignUpActivity.USERNAME, "Error"));

        if (packageInfo != null) {
            versionPreference.setSummary(packageInfo.versionName);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        updatePreference.setSummary(R.string.check_updates);

        if (!listStyled) {
            View rootView = getView();
            if (rootView != null) {
                ListView list = (ListView) rootView.findViewById(android.R.id.list);
                list.setPadding(0, 0, 0, 0);
                list.setDivider(null);
                //any other styling call
                listStyled = true;
            }
        }
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        if (preference.getKey().equals(getString(R.string.trainer_name_key))) {
            new LovelyTextInputDialog(getActivity())
                    .setTopColor(ContextCompat.getColor(getActivity(), R.color.colorPrimary))
                    .setTitle(getString(R.string.change_trainer_name))
                    .setIcon(R.drawable.ic_info_white_24dp)
                    .setInitialInput(pref.getString(SignUpActivity.USERNAME, "Error"))
                    .setConfirmButton(android.R.string.ok, text -> {
                        if (!text.isEmpty()) {
                            pref.edit().putString(SignUpActivity.USERNAME, text).apply();
                            namePreference.setSummary(pref.getString(SignUpActivity.USERNAME, "Error"));
                        }
                    }).show();
        }
        else if (preference.getKey().equals(getString(R.string.update_key))) {
            preference.setSummary(getString(R.string.checking));

            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.update_url)));
            startActivity(browserIntent);
        }
        return true;
    }
}
