package de.vanappsteer.riotbleshell.activities;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.IBinder;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.preference.PreferenceManager;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.UUID;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import de.vanappsteer.riotbleshell.R;
import de.vanappsteer.riotbleshell.dialogs.FavouriteCommandListDialogFragment;
import de.vanappsteer.riotbleshell.services.BleTerminalProtocolService;
import de.vanappsteer.genericbleprotocolservice.GenericBleProtocolService.DeviceConnectionListener;
import de.vanappsteer.riotbleshell.util.LoggingUtil;

import static de.vanappsteer.genericbleprotocolservice.GenericBleProtocolService.DEVICE_DISCONNECTED;
import static de.vanappsteer.riotbleshell.services.BleTerminalProtocolService.BLE_CHARACTERISTIC_UUID_STDIN;
import static de.vanappsteer.riotbleshell.services.BleTerminalProtocolService.BLE_CHARACTERISTIC_UUID_STDOUT;

public class BleTerminalActivity extends AppCompatActivity {

    public enum Result {
        CANCELLED,
        FAILED,
        SUCCESS
    }

    public static final String ACTIVITY_RESULT_KEY_RESULT = "ACTIVITY_RESULT_KEY_RESULT";

    public static final String KEY_CHARACTERISTIC_HASH_MAP = "KEY_CHARACTERISTIC_HASH_MAP";

    private BleTerminalProtocolService mDeviceService;
    private boolean mDeviceServiceBound = false;
    private SharedPreferences mSharedPreferences;

    @BindView(R.id.textview_terminal)
    protected TextView mTextViewTerminal;

    @BindView(R.id.scrollview_terminal)
    protected ScrollView mScrollView;

    @BindView(R.id.edittext_terminal_input)
    protected EditText mEditTextTerminalInput;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ble_terminal);
        ButterKnife.bind(this);

        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setLogo(R.drawable.ic_logo_large);
        toolbar.setTitle("");
        setSupportActionBar(toolbar);

        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
    }

    @Override
    protected void onStart() {
        super.onStart();

        Intent intent = new Intent(this, BleTerminalProtocolService.class);
        bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        unbindService(mConnection);
        mDeviceServiceBound = false;
    }

    @Override
    protected void onResume() {
        super.onResume();

        int textSize = mSharedPreferences.getInt(getString(R.string.sp_int_terminal_text_size), 6);
        mTextViewTerminal.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSize);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        super.onCreateOptionsMenu(menu);

        getMenuInflater().inflate(R.menu.terminal_menu, menu);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {

            case R.id.menuitem_favourite_commands:
                FavouriteCommandListDialogFragment dialogFragment = FavouriteCommandListDialogFragment.newInstance();
                dialogFragment.setOnCommandSelectedListener(new FavouriteCommandListDialogFragment.OnCommandSelectedListener() {

                    @Override
                    public void onCommandSelected(String command) {
                        sendCommand(command);
                        dialogFragment.dismiss();
                    }
                });
                dialogFragment.showNow(getSupportFragmentManager(), "FavouriteCommandListDialogFragment");
                return true;

            case R.id.menuitem_setting:
                Intent intent = new Intent(this, SettingsActivity.class);
                startActivity(intent);
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @OnClick(R.id.button_send)
    protected void processCmd() {
        String cmd = mEditTextTerminalInput.getText().toString();
        if (! "".equals(cmd.trim())) {
            sendCommand(cmd);
        }
    }

    private void sendCommand(String cmd) {
        cmd += "\n";
        mDeviceService.writeCharacteristic(BLE_CHARACTERISTIC_UUID_STDIN, cmd.getBytes());
    }

    private void updateViews(String additionalText) {
        mTextViewTerminal.setText(String.format("%s%s", mTextViewTerminal.getText(), additionalText));
        scrollDownScrollView();
        mEditTextTerminalInput.setText("");
    }

    private void scrollDownScrollView() {

        mScrollView.post(() -> mScrollView.fullScroll(ScrollView.FOCUS_DOWN));
    }

    private ServiceConnection mConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            BleTerminalProtocolService.LocalBinder binder = (BleTerminalProtocolService.LocalBinder) service;
            mDeviceService = binder.getService();
            mDeviceService.addDeviceConnectionListener(mDeviceConnectionListener);
            mDeviceService.subscribeIndication(BLE_CHARACTERISTIC_UUID_STDOUT);

            mDeviceServiceBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mDeviceServiceBound = false;
        }
    };

    private DeviceConnectionListener mDeviceConnectionListener = new DeviceConnectionListener() {

        @Override
        public void onCharacteristicRead(UUID uuid, String value) {

            if (BLE_CHARACTERISTIC_UUID_STDOUT.equals(uuid)) {
                BleTerminalActivity.this.runOnUiThread(() -> updateViews(value));
            }
        }

        @Override
        public void onDeviceDisconnected() {
            finish();
        }

        @Override
        public void onDeviceConnectionError(int errorCode) {

            if (errorCode == DEVICE_DISCONNECTED) {
                finish();
            }

            LoggingUtil.debug("" + errorCode);
        }
    };
}
