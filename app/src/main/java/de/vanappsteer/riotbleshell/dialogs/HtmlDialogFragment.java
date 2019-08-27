package de.vanappsteer.riotbleshell.dialogs;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.webkit.WebSettings;
import android.webkit.WebView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import de.vanappsteer.riotbleshell.R;

public class HtmlDialogFragment extends DialogFragment {

    private final static String BUNDLE_KEY_TITLE = "BUNDLE_KEY_TITLE";
    private final static String BUNDLE_KEY_HTML = "BUNDLE_KEY_HTML";
    private final static String BUNDLE_KEY_CENTER_HTML = "BUNDLE_KEY_CENTER_HTML";

    private Context mContext;

    public static HtmlDialogFragment newInstance(String html, boolean centerHtml) {

        HtmlDialogFragment fragment = new HtmlDialogFragment();

        Bundle args = new Bundle();
        args.putString(BUNDLE_KEY_HTML, html);
        args.putBoolean(BUNDLE_KEY_CENTER_HTML, centerHtml);
        fragment.setArguments(args);

        return fragment;
    }

    public static HtmlDialogFragment newInstance(String title, String html, boolean centerHtml) {

        HtmlDialogFragment fragment = new HtmlDialogFragment();

        Bundle args = new Bundle();
        args.putString(BUNDLE_KEY_TITLE, title);
        args.putString(BUNDLE_KEY_HTML, html);
        args.putBoolean(BUNDLE_KEY_CENTER_HTML, centerHtml);
        fragment.setArguments(args);

        return fragment;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        mContext = context;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        Bundle bundle = getArguments();

        String title = null;
        String html = null;
        boolean centerHtml = true;

        if (bundle != null) {
            title = bundle.getString(BUNDLE_KEY_TITLE);
            html = bundle.getString(BUNDLE_KEY_HTML);
            centerHtml = bundle.getBoolean(BUNDLE_KEY_CENTER_HTML, true);
        }

        if (centerHtml) {
            html = "<center>" + html + "</center>";
        }

        WebView webView = new WebView(mContext);

        WebSettings ws = webView.getSettings();
        ws.setTextZoom(50);

        webView.loadData(html, "text/html", "UTF-8");

        AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
        builder.setView(webView);
        builder.setPositiveButton(R.string.action_close, null);
        builder.setTitle(title);

        return builder.create();
    }

}
