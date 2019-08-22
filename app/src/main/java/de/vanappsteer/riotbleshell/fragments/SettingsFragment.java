package de.vanappsteer.riotbleshell.fragments;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;

import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import de.vanappsteer.riotbleshell.R;
import de.vanappsteer.riotbleshell.dialogs.SeekBarDialogFragment;
import de.vanappsteer.riotbleshell.util.LoggingUtil;

public class SettingsFragment extends PreferenceFragmentCompat implements SharedPreferences.OnSharedPreferenceChangeListener {

    private Context mContext;
    private SharedPreferences mSharedPreferences;
    private SharedPreferences.Editor mSharedPreferencesEditor;

    @SuppressLint("CommitPrefEdits")
    @Override
    public void onAttach(Context context) {

        super.onAttach(context);

        mContext = context;
        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        mSharedPreferencesEditor = mSharedPreferences.edit();
    }

    @Override
    public void onCreatePreferences(Bundle bundle, String s) {

        addPreferencesFromResource(R.xml.preferences);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        Preference terminalTextSizePreference = findPreference(getString(R.string.sp_int_terminal_text_size));
        terminalTextSizePreference.setOnPreferenceClickListener(preference -> {

            int selectedValue = mSharedPreferences.getInt(getString(R.string.sp_int_terminal_text_size), 6);

            LoggingUtil.error("" + selectedValue);

            SeekBarDialogFragment dialog = SeekBarDialogFragment.newInstance(
                    getString(R.string.dialog_preference_text_size), null,
                    selectedValue, 20, 6);

            dialog.setOnValueSelectedListener(
                    value -> {
                        mSharedPreferencesEditor.putInt(getString(R.string.sp_int_terminal_text_size), value);
                        mSharedPreferencesEditor.apply();
                    });

            dialog.showNow(getFragmentManager(), "SeekBarDialogFragment");

            return true;
        });
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {

    }

    @Override
    public void onResume() {

        super.onResume();
        getPreferenceScreen().getSharedPreferences()
                .registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onPause() {

        super.onPause();
        getPreferenceScreen().getSharedPreferences()
                .unregisterOnSharedPreferenceChangeListener(this);
    }
}
