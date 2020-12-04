package com.wearable.sound.ui.activity;

import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;
import androidx.preference.SwitchPreferenceCompat;

import com.wearable.sound.R;

import java.util.Objects;

public class SettingsFragment extends PreferenceFragmentCompat {
    public Preference pref;
    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.root_preferences, rootKey);
        final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(requireActivity());
        final SwitchPreferenceCompat switchPreference1 = (SwitchPreferenceCompat) findPreference("foreground_service");
        final SwitchPreferenceCompat switchPreference2 = (SwitchPreferenceCompat) findPreference("listening_status");
        // enable this for Listening MODE: 2 out of 3
//        if (preferences.getBoolean("foreground_service", false)) {
//            switchPreference2.setChecked(false);
//            switchPreference2.setEnabled(false);
//        } else {
//            switchPreference2.setEnabled(true);
//        }
//        assert switchPreference1 != null;
//        switchPreference1.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
//            @Override
//            public boolean onPreferenceChange(Preference preference, Object newValue) {
//                boolean turned = (Boolean) newValue;
//                if (turned) {
//                    switchPreference2.setChecked(false);
//                    switchPreference2.setEnabled(false);
//                } else {
//                    switchPreference2.setEnabled(true);
//                }
//                preferences.edit().putBoolean("foreground_service", turned).apply();
//                return true;
//            }
//        });
    }

    @Nullable
    @Override
    public <T extends Preference> T findPreference(@NonNull CharSequence key) {
        return super.findPreference(key);
    }

    public Preference getMyPref(){
        return pref;
    }
}