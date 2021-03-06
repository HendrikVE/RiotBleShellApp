package de.vanappsteer.riotbleshell.activities;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import android.text.Html;
import android.widget.TextView;

import butterknife.ButterKnife;
import butterknife.OnClick;
import de.vanappsteer.riotbleshell.R;
import de.vanappsteer.riotbleshell.util.LoggingUtil;

public class AboutAppActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about_app);
        ButterKnife.bind(this);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        String versionName = "";
        int versionCode = 0;
        try {
            versionName = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
            versionCode = getPackageManager().getPackageInfo(getPackageName(), 0).versionCode;
        }
        catch (PackageManager.NameNotFoundException e) {
            LoggingUtil.error(e.getMessage());
        }

        TextView textViewVersion = findViewById(R.id.textview_version);
        textViewVersion.setText(String.format("%s (%s)", versionName, versionCode));
    }

    @OnClick(R.id.linearlayout_license)
    protected void showLicenseDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(AboutAppActivity.this);
        builder.setPositiveButton(R.string.action_ok, null);
        builder.setTitle(R.string.license_title);

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            builder.setMessage(Html.fromHtml(getString(R.string.license_text), Html.FROM_HTML_MODE_LEGACY));
        }
        else {
            builder.setMessage(Html.fromHtml(getString(R.string.license_text)));
        }

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    @OnClick(R.id.textview_project_page)
    protected void openProjectPage() {
        String url = getString(R.string.app_github_project_link);
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(Uri.parse(url));
        startActivity(intent);
    }

}
