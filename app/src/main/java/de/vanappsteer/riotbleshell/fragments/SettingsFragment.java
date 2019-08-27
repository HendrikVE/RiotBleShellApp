package de.vanappsteer.riotbleshell.fragments;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.widget.Toast;

import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;

import de.vanappsteer.riotbleshell.R;
import de.vanappsteer.riotbleshell.activities.AboutAppActivity;
import de.vanappsteer.riotbleshell.dialogs.HtmlDialogFragment;
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

        HtmlDialogFragment licenseDialog
                = HtmlDialogFragment.newInstance(getString(R.string.preference_license_title), getLicenseHtmlText(), false);

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

        Preference about = findPreference(getString(R.string.preference_key_about));
        about.setOnPreferenceClickListener(preference -> {

            Intent intent = new Intent(mContext, AboutAppActivity.class);
            startActivity(intent);

            return true;
        });

        Preference appRating = findPreference(getString(R.string.preference_key_app_rating));
        appRating.setOnPreferenceClickListener(preference -> {

            final String appPackageName = "de.vanappsteer.riotbleshell";//mContext.getPackageName();
            try {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + appPackageName)));
            }
            catch (android.content.ActivityNotFoundException anfe) {
                LoggingUtil.error(anfe.getMessage());
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=" + appPackageName)));
            }

            return true;
        });

        Preference licenses = findPreference(getString(R.string.preference_key_license));
        licenses.setOnPreferenceClickListener(preference -> {
            licenseDialog.showNow(getFragmentManager(), "HtmlDialogFragment");
            return true;
        });

        Preference bugReport = findPreference(getString(R.string.preference_key_contact_developer));
        bugReport.setOnPreferenceClickListener(preference -> {

            Intent intent = new Intent(Intent.ACTION_SENDTO, Uri.fromParts(
                    "mailto","support@vanAppsteer.de", null));
            intent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.app_name));
            //intent.putExtra(Intent.EXTRA_TEXT, "Body");
            intent.putExtra(Intent.EXTRA_EMAIL, new String[]{"support@vanAppsteer.de"}); // String[] addresses

            PackageManager packageManager = mContext.getPackageManager();
            List activities = packageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);
            boolean isIntentSafe = activities.size() > 0;

            if(isIntentSafe) {
                startActivity(Intent.createChooser(intent, getString(R.string.action_send_mail)));
            }
            else {
                Toast.makeText(mContext, getString(R.string.no_suitable_app_for_intent), Toast.LENGTH_LONG).show();
            }

            //disable animation for one time
            getActivity().overridePendingTransition(0, 0);
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

    private String getLicenseHtmlText() {

        StringBuilder sb = new StringBuilder();
        try {
            BufferedReader input = new BufferedReader(new InputStreamReader(
                    mContext.getAssets().open(getString(R.string.filename_licenses)), StandardCharsets.UTF_8));

            String line;
            while ((line = input.readLine()) != null) {
                sb.append(line);
                sb.append(System.getProperty("line.separator"));
            }
            input.close();
        }
        catch (IOException e) {
            LoggingUtil.error(e.getMessage());
        }

        return sb.toString();
    }
}
