package de.vanappsteer.riotbleshell.activities;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SwitchCompat;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.polidea.rxandroidble2.RxBleDevice;
import com.polidea.rxandroidble2.scan.ScanResult;

import java.util.HashSet;
import java.util.Set;

import de.vanappsteer.riotbleshell.R;
import de.vanappsteer.riotbleshell.adapter.DeviceListAdapter;
import de.vanappsteer.riotbleshell.services.BleTerminalProtocolService;
import de.vanappsteer.riotbleshell.services.GenericBleProtocolService.BluetoothAdapterStateListener;
import de.vanappsteer.riotbleshell.services.GenericBleProtocolService.DeviceConnectionListener;
import de.vanappsteer.riotbleshell.services.GenericBleProtocolService.ScanListener;
import de.vanappsteer.riotbleshell.util.LoggingUtil;

import static de.vanappsteer.riotbleshell.services.GenericBleProtocolService.DEVICE_CONNECTION_ERROR_GENERIC;
import static de.vanappsteer.riotbleshell.services.GenericBleProtocolService.DEVICE_CONNECTION_ERROR_READ;
import static de.vanappsteer.riotbleshell.services.GenericBleProtocolService.DEVICE_CONNECTION_ERROR_UNSUPPORTED;
import static de.vanappsteer.riotbleshell.services.GenericBleProtocolService.DEVICE_CONNECTION_ERROR_WRITE;
import static de.vanappsteer.riotbleshell.services.GenericBleProtocolService.DEVICE_DISCONNECTED;
import static de.vanappsteer.riotbleshell.services.GenericBleProtocolService.STATE_OFF;
import static de.vanappsteer.riotbleshell.services.GenericBleProtocolService.STATE_ON;

public class DeviceScanActivity extends AppCompatActivity {

    private final int COMMAND_SHOW_DISCONNECTED = 0;
    private final int COMMAND_SHOW_CONNECTION_ERROR_DIALOG = 1;
    private final int COMMAND_SHOW_DEVICE_UNSUPPORTED_DIALOG = 2;
    private final int COMMAND_SHOW_DEVICE_READ_ERROR_DIALOG = 3;
    private final int COMMAND_SHOW_DEVICE_WRITE_ERROR_DIALOG = 4;

    private boolean mDialogDisconnectedShown = false;
    private boolean mDialogConnectionErrorShown = false;
    private boolean mDialogUnsupportedErrorShown = false;
    private boolean mDialogReadErrorShown = false;
    private boolean mDialogWriteErrorShown = false;

    private final int ACTIVITY_RESULT_ENABLE_BLUETOOTH = 1;
    private final int ACTIVITY_RESULT_ENABLE_LOCATION_PERMISSION = 2;
    private final int ACTIVITY_RESULT_CONFIGURE_DEVICE = 3;

    private final int REQUEST_PERMISSION_COARSE_LOCATION = 1;

    private final String KEY_SP_ASKED_FOR_LOCATION = "KEY_SP_ASKED_FOR_LOCATION";

    private BleTerminalProtocolService mDeviceService;
    private boolean mDeviceServiceBound = false;

    private Set<RxBleDevice> bleDeviceSet = new HashSet<>();

    private DeviceListAdapter mAdapter;
    private boolean mScanSwitchEnabled = true;
    private boolean mIsScanning = false;
    private boolean mScanPaused = false;

    private SwitchCompat mScanSwitch;
    private ProgressBar mScanProgressbar;
    private TextView mTextViewEnableBluetooth;

    private AlertDialog mDialogConnectDevice;

    private SharedPreferences mSP;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device_scan);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        initViews();

        mSP = PreferenceManager.getDefaultSharedPreferences(this);
    }

    @Override
    protected void onStart() {
        super.onStart();

        Intent intent = new Intent(this, BleTerminalProtocolService.class);
        bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onResume() {

        super.onResume();

        if (mScanSwitch != null && (mScanSwitch.isChecked()) || mScanPaused) {
            checkPermissions();
        }
    }

    @Override
    protected void onPause() {

        super.onPause();

        if (mIsScanning) {
            mScanPaused = true;
        }
        stopScan();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (mDeviceServiceBound) {
            mDeviceService.removeDeviceConnectionListener(mDeviceConnectionListener);
            mDeviceService.removeBluetoothAdapterStateListener(mBluetoothAdapterStateListener);
            mDeviceService.disconnectDevice();

            unbindService(mConnection);
            mDeviceServiceBound = false;
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent dataIntent) {

        switch (requestCode) {

            case ACTIVITY_RESULT_ENABLE_BLUETOOTH:
                if (resultCode == RESULT_OK) {
                    checkPermissions();
                }
                else {
                    mScanSwitch.setChecked(false);
                }
                break;

            case ACTIVITY_RESULT_ENABLE_LOCATION_PERMISSION:
                checkPermissions();
                break;

            case ACTIVITY_RESULT_CONFIGURE_DEVICE:
                if (resultCode != RESULT_OK) {

                    if (dataIntent != null) {
                        BleTerminalActivity.Result returnValue = (BleTerminalActivity.Result) dataIntent.getSerializableExtra(BleTerminalActivity.ACTIVITY_RESULT_KEY_RESULT);

                        if (returnValue != BleTerminalActivity.Result.CANCELLED) {
                            Message message = mUiHandler.obtainMessage(COMMAND_SHOW_DEVICE_WRITE_ERROR_DIALOG, null);
                            message.sendToTarget();
                        }
                    }
                }

                if (mDeviceServiceBound) {
                    mDeviceService.removeDeviceConnectionListener(mDeviceConnectionListener);
                    mDeviceService.disconnectDevice();
                }
                break;

            default:
                LoggingUtil.warning("unhandled request code: " + requestCode);

        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {

        switch (requestCode) {

            case REQUEST_PERMISSION_COARSE_LOCATION:

                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    checkBluetooth();
                }
                else {
                    checkPermissions();
                }

                break;

            default:
                LoggingUtil.warning("unhandled request code: " + requestCode);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        super.onCreateOptionsMenu(menu);

        getMenuInflater().inflate(R.menu.scan_menu, menu);

        mScanSwitch = menu.findItem(R.id.menuItemBluetoothSwitch).getActionView().findViewById(R.id.bluetoothScanSwitch);
        mScanSwitch.setOnCheckedChangeListener((compoundButton, isChecked) -> {

            if (! mScanSwitchEnabled) {
                return;
            }

            if (isChecked) {
                checkPermissions();
            }
            else {
                stopScan();
            }
        });

        checkPermissions();

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {

            case R.id.menuItemBluetoothSwitch:
                return true;

            case R.id.menuItemAbout:
                Intent intent = new Intent(DeviceScanActivity.this, AboutApp.class);
                startActivity(intent);
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void startScan() {

        mScanPaused = false;

        mScanProgressbar.setVisibility(View.VISIBLE);

        mScanSwitchEnabled = false;
        mScanSwitch.setChecked(true);
        mScanSwitchEnabled = true;

        mTextViewEnableBluetooth.setVisibility(View.GONE);

        mDeviceService.addScanListener(mScanListener);

        mIsScanning = true;
        mDeviceService.startDeviceScan();
    }

    private void stopScan() {

        mScanProgressbar.setVisibility(View.INVISIBLE);

        if (mScanSwitch != null) {
            mScanSwitchEnabled = false;
            mScanSwitch.setChecked(false);
            mScanSwitchEnabled = true;
        }

        int adapterState = mDeviceService.getBluetoothAdapterState();

        if (adapterState == STATE_ON) {
            mDeviceService.removeScanListener(mScanListener);

            mIsScanning = false;
            mDeviceService.stopDeviceScan();
        }

        bleDeviceSet.clear();
        mAdapter.setDevices(bleDeviceSet);
        mAdapter.notifyDataSetChanged();

        mTextViewEnableBluetooth.setVisibility(View.VISIBLE);
    }

    private void initViews() {
        initRecyclerView();
        mScanProgressbar = findViewById(R.id.scanProgressbar);
        mTextViewEnableBluetooth = findViewById(R.id.textViewEnableBluetooth);
    }

    private void initRecyclerView() {

        RecyclerView recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setHasFixedSize(true);

        RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(mLayoutManager);

        mAdapter = new DeviceListAdapter();
        mAdapter.setOnDeviceSelectionListener(new DeviceListAdapter.OnDeviceSelectionListener() {

            @Override
            public void onDeviceSelected(RxBleDevice device) {

                if (mDeviceServiceBound) {
                    mDeviceService.addDeviceConnectionListener(mDeviceConnectionListener);
                    mDeviceService.connectDevice(device);
                }
                else {
                    Message message = mUiHandler.obtainMessage(COMMAND_SHOW_CONNECTION_ERROR_DIALOG, null);
                    message.sendToTarget();
                }

                AlertDialog.Builder builder = new AlertDialog.Builder(DeviceScanActivity.this);
                builder.setTitle(R.string.dialog_bluetooth_device_connecting_title);
                builder.setView(R.layout.progress_infinite);
                builder.setNegativeButton(R.string.action_cancel, (dialogInterface, i) -> {
                    if (mDeviceServiceBound) {
                        mDeviceService.removeDeviceConnectionListener(mDeviceConnectionListener);
                        mDeviceService.disconnectDevice();
                    }
                });
                builder.setCancelable(false);

                mDialogConnectDevice = builder.create();
                mDialogConnectDevice.show();
            }
        });
        recyclerView.setAdapter(mAdapter);
    }

    private void checkBluetooth() {

        int adapterState = mDeviceService.getBluetoothAdapterState();

        if (adapterState != STATE_ON) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, ACTIVITY_RESULT_ENABLE_BLUETOOTH);
        }
        else {
            startScan();
        }
    }

    private void checkPermissions() {

        boolean coarseLocationGranted = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        if (! coarseLocationGranted) {

            boolean showRationale = ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_COARSE_LOCATION);
            boolean alreadyAskedBefore = mSP.getBoolean(KEY_SP_ASKED_FOR_LOCATION, false);

            if ( !showRationale && alreadyAskedBefore) {// user CHECKED "never ask again"

                showLocationRequestDialog(true);
            }
            else {
                showLocationRequestDialog(false);

                SharedPreferences.Editor editor = mSP.edit();
                editor.putBoolean(KEY_SP_ASKED_FOR_LOCATION, true);
                editor.apply();
            }
        }
        else {
            checkBluetooth();
        }
    }

    private void showLocationRequestDialog(boolean neverAskAgain) {

        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        if (neverAskAgain) {
            builder.setMessage(R.string.dialog_coarse_location_permitted_message).setTitle(R.string.dialog_coarse_location_permitted_title);
            builder.setPositiveButton(R.string.action_ok, (dialogInterface, i) -> {
                Intent intent = new Intent();
                intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                Uri uri = Uri.fromParts("package", getPackageName(), null);
                intent.setData(uri);
                startActivityForResult(intent, ACTIVITY_RESULT_ENABLE_LOCATION_PERMISSION);
            });
        }
        else {
            builder.setMessage(R.string.dialog_request_coarse_location_message).setTitle(R.string.dialog_request_coarse_location_title);
            builder.setPositiveButton(
                    R.string.action_ok,
                    (dialogInterface, i) -> ActivityCompat.requestPermissions(
                            DeviceScanActivity.this,
                            new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, REQUEST_PERMISSION_COARSE_LOCATION)
            );
        }

        builder.setCancelable(false);

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private RxBleDevice getDeviceByBleAddress(Set<RxBleDevice> deviceSet, String address) {

        for (RxBleDevice device : deviceSet) {
            if (device.getMacAddress().equals(address)) {
                return device;
            }
        }

        return null;
    }

    private void openBleTerminalActivity() {

        Intent intent = new Intent(DeviceScanActivity.this, BleTerminalActivity.class);
        startActivityForResult(intent, ACTIVITY_RESULT_CONFIGURE_DEVICE);
    }

    private Handler mUiHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message message) {

            AlertDialog.Builder builder;
            AlertDialog dialog;

            builder = new AlertDialog.Builder(DeviceScanActivity.this);

            switch (message.what) {

                case COMMAND_SHOW_DISCONNECTED:
                    if (mDialogDisconnectedShown) {
                        return;
                    }
                    builder.setTitle(R.string.dialog_bluetooth_device_disconnected_title);
                    builder.setMessage(R.string.dialog_bluetooth_device_disconnected_message);
                    builder.setPositiveButton(R.string.action_ok, null);
                    builder.setOnDismissListener(dialogInterface -> mDialogDisconnectedShown = false);
                    break;

                case COMMAND_SHOW_CONNECTION_ERROR_DIALOG:
                    if (mDialogConnectionErrorShown) {
                        return;
                    }
                    builder.setTitle(R.string.dialog_bluetooth_device_connection_error_title);
                    builder.setMessage(R.string.dialog_bluetooth_device_connection_error_message);
                    builder.setPositiveButton(R.string.action_ok, null);
                    builder.setOnDismissListener(dialogInterface -> mDialogConnectionErrorShown = false);
                    break;

                case COMMAND_SHOW_DEVICE_UNSUPPORTED_DIALOG:
                    if (mDialogUnsupportedErrorShown) {
                        return;
                    }
                    builder.setTitle(R.string.dialog_bluetooth_device_not_supported_title);
                    builder.setMessage(R.string.dialog_bluetooth_device_not_supported_message);
                    builder.setPositiveButton(R.string.action_ok, null);
                    builder.setOnDismissListener(dialogInterface -> mDialogUnsupportedErrorShown = false);
                    break;

                case COMMAND_SHOW_DEVICE_READ_ERROR_DIALOG:
                    if (mDialogReadErrorShown) {
                        return;
                    }
                    builder.setTitle(R.string.dialog_bluetooth_device_read_error_title);
                    builder.setMessage(R.string.dialog_bluetooth_device_read_error_message);
                    builder.setPositiveButton(R.string.action_ok, null);
                    builder.setOnDismissListener(dialogInterface -> mDialogReadErrorShown = false);
                    break;

                case COMMAND_SHOW_DEVICE_WRITE_ERROR_DIALOG:
                    if (mDialogWriteErrorShown) {
                        return;
                    }
                    builder.setTitle(R.string.dialog_bluetooth_device_write_error_title);
                    builder.setMessage(R.string.dialog_bluetooth_device_write_error_message);
                    builder.setPositiveButton(R.string.action_ok, null);
                    builder.setOnDismissListener(dialogInterface -> mDialogWriteErrorShown = false);
                    break;

                default:
                    LoggingUtil.warning("unhandled command: " + message.what);
                    return;
            }

            dialog = builder.create();
            dialog.show();
        }
    };

    private ServiceConnection mConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            BleTerminalProtocolService.LocalBinder binder = (BleTerminalProtocolService.LocalBinder) service;
            mDeviceService = binder.getService();
            mDeviceServiceBound = true;

            mDeviceService.addBluetoothAdapterStateListener(mBluetoothAdapterStateListener);
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            mDeviceServiceBound = false;
        }
    };

    private ScanListener mScanListener = new ScanListener() {

        @Override
        public void onScanResult(ScanResult scanResult) {

            RxBleDevice device = scanResult.getBleDevice();

            if (! mIsScanning) {
                return;
            }

            boolean added = false;

            // update device in set if a name was found after address was already discovered
            RxBleDevice deviceFound = getDeviceByBleAddress(bleDeviceSet, device.getMacAddress());

            if (device.getName() == null) {
                // ignore devices without a name
                return;
            }

            if (deviceFound != null && deviceFound.getName() == null && device.getName() != null) {
                bleDeviceSet.remove(deviceFound);
                added = bleDeviceSet.add(device);
            }
            else if(deviceFound == null) {
                added = bleDeviceSet.add(device);
            }

            if (added) {
                mAdapter.setDevices(bleDeviceSet);
                mAdapter.notifyDataSetChanged();
            }
        }
    };

    private BluetoothAdapterStateListener mBluetoothAdapterStateListener = new BluetoothAdapterStateListener() {

        @Override
        public void onStateChange(int state) {

            switch (state) {
                case STATE_ON:
                    startScan();
                    break;

                case STATE_OFF:
                    stopScan();
                    break;

                default:
                    LoggingUtil.warning("unhandled state: " + state);
            }
        }
    };

    private DeviceConnectionListener mDeviceConnectionListener = new DeviceConnectionListener() {

        @Override
        public void onDeviceConnected() {

            mDialogConnectDevice.dismiss();
            openBleTerminalActivity();
            mDeviceService.removeDeviceConnectionListener(this);
        }

        @Override
        public void onDeviceConnectionError(int errorCode) {

            mDialogConnectDevice.dismiss();

            Message message = null;
            switch (errorCode) {

                case DEVICE_DISCONNECTED:
                    message = mUiHandler.obtainMessage(COMMAND_SHOW_DISCONNECTED, null);
                    break;

                case DEVICE_CONNECTION_ERROR_GENERIC:
                    message = mUiHandler.obtainMessage(COMMAND_SHOW_CONNECTION_ERROR_DIALOG, null);
                    break;

                case DEVICE_CONNECTION_ERROR_UNSUPPORTED:
                    message = mUiHandler.obtainMessage(COMMAND_SHOW_DEVICE_UNSUPPORTED_DIALOG, null);
                    break;

                case DEVICE_CONNECTION_ERROR_READ:
                    message = mUiHandler.obtainMessage(COMMAND_SHOW_DEVICE_READ_ERROR_DIALOG, null);
                    break;

                case DEVICE_CONNECTION_ERROR_WRITE:
                    message = mUiHandler.obtainMessage(COMMAND_SHOW_DEVICE_WRITE_ERROR_DIALOG, null);
                    break;

                default:
                    LoggingUtil.warning("unhandled error code: " + errorCode);
            }

            if (message != null) {
                message.sendToTarget();
            }
        }
    };
}
