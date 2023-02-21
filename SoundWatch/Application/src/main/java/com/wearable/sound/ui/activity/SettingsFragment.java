package com.wearable.sound.ui.activity;

import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.preference.EditTextPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;
import androidx.preference.SwitchPreferenceCompat;

import com.wearable.sound.R;

public class SettingsFragment extends PreferenceFragmentCompat {

    private final static String PREFIX = "SWFS22";
    private final static int idLen = 11;
    public Preference pref;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.root_preferences, rootKey);
        final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(requireActivity());
        final SwitchPreferenceCompat switchPreference1 = findPreference("foreground_service");
        final SwitchPreferenceCompat switchPreference2 = findPreference("listening_status");

        // For participant id settings
        final EditTextPreference participantIdPref = findPreference("participant_id");
        participantIdPref.setOnPreferenceChangeListener(
                new Preference.OnPreferenceChangeListener() {
                    @Override
                    public boolean onPreferenceChange(Preference preference, Object newValue) {
                        boolean isValid = SettingsFragment.isValidParticipantId((String) newValue);
                        if (!isValid) {
                            // Invalid participant id, let the user know
                            final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                            builder.setTitle("Invalid Participant Id");
                            builder.setMessage("Please make sure you enter the correct id given to you." +
                                    " If it still doesn't work, please contact the development team.");
                            builder.setPositiveButton(android.R.string.ok, null);
                            builder.show();
                        }

                        return isValid;
                    }
                }
        );
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

    // Make sure that the participant id matches the pattern
    // "SWFS22-<string of 4 characters such as their sum ascii value % 127 == 79>
    // eg: SWFS22-Xcxg
    public static boolean isValidParticipantId(String participantId) {
//        if (participantId.length() != idLen) {
//            return false;
//        } else if (!participantId.startsWith(PREFIX)) {
//            return false;
//        }
//
//        String randomString = participantId.substring(PREFIX.length() + 1);
//        int sum = 0;
//        for (char ch : randomString.toCharArray()) {
//            if (!Character.isLetterOrDigit(ch)) {
//                return false;
//            }
//            sum += (int) ch;
//        }
//
//        return sum % 127 == 29;

        return true;
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