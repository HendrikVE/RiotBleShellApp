package de.vanappsteer.riotbleshell.activities;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.method.ScrollingMovementMethod;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.util.UUID;

import de.vanappsteer.riotbleshell.R;
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

    private TextView mTextViewTerminal;
    private EditText mEditTextTerminalInput;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ble_terminal);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        mTextViewTerminal = findViewById(R.id.textview_terminal);
        mTextViewTerminal.setMovementMethod(new ScrollingMovementMethod());

        mEditTextTerminalInput = findViewById(R.id.edittext_terminal_input);

        Button buttonSend = findViewById(R.id.button_send);
        buttonSend.setOnClickListener(view -> {
            String cmd = mEditTextTerminalInput.getText().toString();
            if (! "".equals(cmd.trim())) {
                sendCommand(cmd);
            }
        });
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

    private void sendCommand(String cmd) {
        mDeviceService.writeCharacteristic(BLE_CHARACTERISTIC_UUID_STDIN, cmd.getBytes());
    }

    private void updateViews(String additionalText) {
        mTextViewTerminal.setText(String.format("%s%s", mTextViewTerminal.getText(), additionalText));
        mEditTextTerminalInput.setText("");
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
