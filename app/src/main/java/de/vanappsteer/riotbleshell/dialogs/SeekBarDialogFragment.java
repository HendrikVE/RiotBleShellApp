package de.vanappsteer.riotbleshell.dialogs;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import de.vanappsteer.riotbleshell.R;
import de.vanappsteer.riotbleshell.util.LoggingUtil;

public class SeekBarDialogFragment extends DialogFragment {

    private final static String BUNDLE_KEY_TITLE = "BUNDLE_KEY_TITLE";
    private final static String BUNDLE_KEY_MESSAGE = "BUNDLE_KEY_MESSAGE";
    private final static String BUNDLE_KEY_VALUE = "BUNDLE_KEY_VALUE";
    private final static String BUNDLE_KEY_MAX_VALUE = "BUNDLE_KEY_MAX_VALUE";
    private final static String BUNDLE_KEY_MIN_VALUE = "BUNDLE_KEY_MIN_VALUE";

    private Context mContext;

    private View mDialogView;

    private OnValueSelectedListener mOnValueSelectedListener = null;

    public static SeekBarDialogFragment newInstance(String title, String message, int value, int maxValue, int minValue) {

        SeekBarDialogFragment fragment = new SeekBarDialogFragment();

        Bundle args = new Bundle();
        args.putString(BUNDLE_KEY_TITLE, title);
        args.putString(BUNDLE_KEY_MESSAGE, message);
        args.putInt(BUNDLE_KEY_VALUE, value);
        args.putInt(BUNDLE_KEY_MAX_VALUE, maxValue);
        args.putInt(BUNDLE_KEY_MIN_VALUE, minValue);
        fragment.setArguments(args);

        return fragment;
    }

    private SeekBarDialogFragment() { }

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
        String message = null;
        int maxValue = 100;
        int minValue = 0;
        int value = maxValue / 2;

        if (bundle != null) {
            title = bundle.getString(BUNDLE_KEY_TITLE);
            message = bundle.getString(BUNDLE_KEY_MESSAGE);
            value = bundle.getInt(BUNDLE_KEY_VALUE, maxValue / 2);
            maxValue = bundle.getInt(BUNDLE_KEY_MAX_VALUE, 100);
            minValue = bundle.getInt(BUNDLE_KEY_MIN_VALUE, 0);
        }

        final int seekBarBase = minValue;

        mDialogView = LayoutInflater.from(mContext).inflate(R.layout.dialog_seekbar, null);

        SeekBar seekBar = mDialogView.findViewById(R.id.seekbar);
        seekBar.setMax(maxValue - seekBarBase);

        seekBar.setProgress(value - seekBarBase);
        showValueInTextView(value);

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                int value = seekBarBase + progress;

                showValueInTextView(value);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) { }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) { }
        });

        AlertDialog.Builder builder = new AlertDialog.Builder(mContext)
                .setTitle(title)
                .setMessage(message)
                .setView(mDialogView)
                .setPositiveButton(R.string.action_ok, (dialogInterface, i) -> {
                    if (mOnValueSelectedListener != null) {

                        int result = seekBar.getProgress() + seekBarBase;

                        mOnValueSelectedListener.onValueSelected(result);
                    }
                })
                .setNegativeButton(R.string.action_cancel, null);

        return builder.create();
    }

    private void showValueInTextView(int value) {
        TextView textView = mDialogView.findViewById(R.id.textview);
        textView.setText(String.format("Text size: %d", value));
    }

    public void setOnValueSelectedListener(OnValueSelectedListener listener) {
        mOnValueSelectedListener = listener;
    }

    public interface OnValueSelectedListener {
        void onValueSelected(int value);
    }

}
