package de.vanappsteer.riotbleshell.services;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Binder;
import android.os.IBinder;

import com.polidea.rxandroidble2.RxBleClient;
import com.polidea.rxandroidble2.RxBleConnection;
import com.polidea.rxandroidble2.RxBleDevice;
import com.polidea.rxandroidble2.scan.ScanResult;
import com.polidea.rxandroidble2.scan.ScanSettings;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import de.vanappsteer.riotbleshell.util.LoggingUtil;
import io.reactivex.disposables.Disposable;

public class BluetoothDeviceConnectionService extends Service {

    public final static int READY = 0;
    public final static int BLUETOOTH_NOT_AVAILABLE = 1;
    public final static int LOCATION_PERMISSION_NOT_GRANTED = 2;
    public final static int BLUETOOTH_NOT_ENABLED = 3;
    public final static int LOCATION_SERVICES_NOT_ENABLED = 4;

    public final static int STATE_OFF = 0;
    public final static int STATE_TURNING_ON = 1;
    public final static int STATE_ON = 2;
    public final static int STATE_TURNING_OFF = 3;

    private final UUID BLE_SERVICE_UUID = UUID.fromString("e6d54866-0292-4779-b8f8-c52bbec91e71");

    private final IBinder mBinder = new LocalBinder();

    private boolean mDisconnectPending = false;

    private HashMap<UUID, String> mCharacteristicHashMap;

    private List<ScanListener> mScanListenerList = new ArrayList<>();
    private List<BluetoothPreconditionStateListener> mBluetoothPreconditionStateListenerList = new ArrayList<>();
    private List<BluetoothAdapterStateListener> mBluetoothAdapterStateListenerList = new ArrayList<>();

    private Set<DeviceConnectionListener> mDeviceConnectionListenerSet = new HashSet<>();

    private BluetoothManager mBluetoothManager;
    private BluetoothAdapter mBluetoothAdapter;

    private RxBleClient mRxBleClient;
    private RxBleConnection mRxBleConnection;

    private Disposable mFlowDisposable;
    private Disposable mScanSubscription;
    private Disposable mConnectionSubscription;

    public BluetoothDeviceConnectionService() { }

    @Override
    public void onCreate() {

        super.onCreate();

        mRxBleClient = RxBleClient.create(this);

        mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);

        if (mBluetoothManager != null) {
            mBluetoothAdapter = mBluetoothManager.getAdapter();
        }

        IntentFilter filter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        registerReceiver(mBroadcastReceiver, filter);

        mFlowDisposable = mRxBleClient.observeStateChanges()
            .subscribe(
                state -> {
                    switch (state) {
                        case READY:
                            mBluetoothPreconditionStateListenerList.forEach(
                                l -> l.onStateChange(READY)
                            );
                            break;

                        case BLUETOOTH_NOT_AVAILABLE:
                            mBluetoothPreconditionStateListenerList.forEach(
                                    l -> l.onStateChange(BLUETOOTH_NOT_AVAILABLE)
                            );
                            break;

                        case LOCATION_PERMISSION_NOT_GRANTED:
                            mBluetoothPreconditionStateListenerList.forEach(
                                    l -> l.onStateChange(LOCATION_PERMISSION_NOT_GRANTED)
                            );
                            break;

                        case BLUETOOTH_NOT_ENABLED:
                            mBluetoothPreconditionStateListenerList.forEach(
                                    l -> l.onStateChange(BLUETOOTH_NOT_ENABLED)
                            );
                            break;

                        case LOCATION_SERVICES_NOT_ENABLED:
                            mBluetoothPreconditionStateListenerList.forEach(
                                    l -> l.onStateChange(LOCATION_SERVICES_NOT_ENABLED)
                            );
                            break;

                        default:
                            LoggingUtil.warning("unhandled state: " + state);
                    }
                },
                throwable -> {
                    LoggingUtil.error(throwable.getMessage());
                }
            );
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public void onDestroy() {

        unregisterReceiver(mBroadcastReceiver);

        disconnectDevice();
    }

    public void startDeviceScan() {
        mScanSubscription = mRxBleClient.scanBleDevices(
                new ScanSettings.Builder().build()
        ).subscribe(
                scanResult -> {
                    mScanListenerList.forEach(l -> l.onScanResult(scanResult));
                },
                throwable -> {
                    // Handle an error here.
                }
        );
    }

    public void stopDeviceScan() {
        mScanSubscription.dispose();
    }

    public void connectDevice(RxBleDevice device) {

        if (mConnectionSubscription != null && !mConnectionSubscription.isDisposed()) {
            //allow only one connection at the same time
            mConnectionSubscription.dispose();
        }

        mConnectionSubscription = device.establishConnection(false)
                .subscribe(
                        rxBleConnection -> {
                            mRxBleConnection = rxBleConnection;
                        },
                        throwable -> {
                            // TODO
                        }
                );
    }

    public int getBluetoothState() {

        RxBleClient.State state = mRxBleClient.getState();

        switch (state) {

            case READY:
                return READY;

            case BLUETOOTH_NOT_AVAILABLE:
                return BLUETOOTH_NOT_AVAILABLE;

            case LOCATION_PERMISSION_NOT_GRANTED:
                return LOCATION_PERMISSION_NOT_GRANTED;

            case BLUETOOTH_NOT_ENABLED:
                return BLUETOOTH_NOT_ENABLED;

            case LOCATION_SERVICES_NOT_ENABLED:
                return LOCATION_SERVICES_NOT_ENABLED;

            default:
                LoggingUtil.warning("unhandled state: " + state);
                return -1;
        }
    }

    public int getBluetoothAdapterState() {

        if (mBluetoothAdapter == null) {
            return BluetoothAdapter.STATE_OFF;
        }

        int state = mBluetoothAdapter.getState();

        switch (state) {

            case BluetoothAdapter.STATE_OFF:
                return STATE_OFF;

            case BluetoothAdapter.STATE_TURNING_ON:
                return STATE_TURNING_ON;

            case BluetoothAdapter.STATE_ON:
                return STATE_ON;

            case BluetoothAdapter.STATE_TURNING_OFF:
                return STATE_TURNING_OFF;

            default:
                LoggingUtil.warning("unhandled state: " + state);
                return STATE_OFF;
        }
    }

    public void disconnectDevice() {

        if (mConnectionSubscription != null) {
            mConnectionSubscription.dispose();
        }
    }

    private boolean isDisconnected() {

        return mConnectionSubscription.isDisposed();
    }

    public void readCharacteristic(UUID uuid) {

        mRxBleConnection.readCharacteristic(uuid);
    }

    public void writeCharacteristic(UUID uuid, byte[] value) {

        mRxBleConnection.writeCharacteristic(uuid, value);
    }

    public void addDeviceConnectionListener(DeviceConnectionListener listener) {

        mDeviceConnectionListenerSet.add(listener);
    }

    public void removeDeviceConnectionListener(DeviceConnectionListener listener) {

        mDeviceConnectionListenerSet.remove(listener);
    }

    public void addScanListener(ScanListener listener) {

        mScanListenerList.add(listener);
    }

    public void removeScanListener(ScanListener listener) {

        mScanListenerList.remove(listener);
    }

    public void addBluetoothStateListener(BluetoothPreconditionStateListener listener) {

        mBluetoothPreconditionStateListenerList.add(listener);
    }

    public void removeBluetoothStateListener(BluetoothPreconditionStateListener listener) {

        mBluetoothPreconditionStateListenerList.remove(listener);
    }

    public void addBluetoothAdapterStateListener(BluetoothAdapterStateListener listener) {

        mBluetoothAdapterStateListenerList.add(listener);
    }

    public void removeBluetoothAdapterStateListener(BluetoothAdapterStateListener listener) {

        mBluetoothAdapterStateListenerList.remove(listener);
    }

    /*
    private BluetoothGattCallback mGattCharacteristicCallback = new BluetoothGattCallback() {

        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {

            LoggingUtil.debug("onConnectionStateChange");
            LoggingUtil.debug("status: " + status);
            LoggingUtil.debug("newState: " + newState);

            if (status == BluetoothGatt.GATT_SUCCESS) {

                switch (newState) {

                    case BluetoothProfile.STATE_CONNECTED:
                        LoggingUtil.debug("discoverServices");
                        gatt.discoverServices();
                        break;

                    case BluetoothProfile.STATE_DISCONNECTED:
                        gatt.close();
                        mBluetoothGatt = null;
                        mBluetoothGattService = null;
                        mDeviceConnectionListenerSet.forEach(l -> l.onDeviceDisconnected());
                        break;

                    default:
                        LoggingUtil.debug("unhandled state: " + newState);
                }
            }
            else {
                mDeviceConnectionListenerSet.forEach(l -> l.onDeviceConnectionError(DEVICE_CONNECTION_ERROR_GENERIC));
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {

            LoggingUtil.debug("onServicesDiscovered");

            if (status != BluetoothGatt.GATT_SUCCESS) {
                LoggingUtil.error("status != BluetoothGatt.GATT_SUCCESS");

                mDeviceConnectionListenerSet.forEach(l -> l.onDeviceConnectionError(DEVICE_CONNECTION_ERROR_GENERIC));

                return;
            }

            mBluetoothGattService = gatt.getService(BLE_SERVICE_UUID);
            if (mBluetoothGattService == null) {

                gatt.disconnect();

                mDeviceConnectionListenerSet.forEach(l -> l.onDeviceConnectionError(DEVICE_CONNECTION_ERROR_UNSUPPORTED));

                return;
            }

            mDeviceConnectionListenerSet.forEach(l -> l.onDeviceConnected());
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristicRead, int status) {

            UUID uuid = characteristicRead.getUuid();
            String value = characteristicRead.getStringValue(0);

            LoggingUtil.debug("onCharacteristicRead");
            LoggingUtil.debug("uuid: " + uuid);
            LoggingUtil.debug("value: " + value);

            LoggingUtil.debug("pending queue size: " + mReadCharacteristicsOperationsQueue.size());

            mCharacteristicHashMap.put(characteristicRead.getUuid(), characteristicRead.getStringValue(0));
            mDeviceConnectionListenerSet.forEach(l -> l.onCharacteristicRead(uuid, value));

            synchronized (mReadCharacteristicsOperationsQueue) {

                if (mReadCharacteristicsOperationsQueue.size() > 0) {

                    BluetoothGattCharacteristic characteristic
                            = mBluetoothGattService.getCharacteristic(mReadCharacteristicsOperationsQueue.poll());

                    boolean success = gatt.readCharacteristic(characteristic);
                    if (! success) {
                        mDeviceConnectionListenerSet.forEach(l -> l.onDeviceConnectionError(DEVICE_CONNECTION_ERROR_READ));
                    }
                }
                else {
                    mDeviceConnectionListenerSet.forEach(l -> l.onAllCharacteristicsRead(mCharacteristicHashMap));

                    if (mDisconnectPending) {
                        disconnectDevice();
                    }
                }
            }
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristicWrote, int status) {

            UUID uuid = characteristicWrote.getUuid();
            String value = characteristicWrote.getStringValue(0);

            LoggingUtil.debug("onCharacteristicWrite");
            LoggingUtil.debug("uuid: " + uuid);
            LoggingUtil.debug("value: " + value);

            LoggingUtil.debug("pending queue size: " + mWriteCharacteristicsOperationsQueue.size());

            mDeviceConnectionListenerSet.forEach(l -> l.onCharacteristicWrote(uuid, value));

            synchronized (mWriteCharacteristicsOperationsQueue) {

                if (mWriteCharacteristicsOperationsQueue.size() > 0) {

                    BluetoothGattCharacteristic characteristic
                            = mBluetoothGattService.getCharacteristic(mWriteCharacteristicsOperationsQueue.poll());

                    boolean success = gatt.writeCharacteristic(characteristic);
                    if (! success) {
                        mDeviceConnectionListenerSet.forEach(l -> l.onDeviceConnectionError(DEVICE_CONNECTION_ERROR_WRITE));
                    }
                }
                else {
                    mDeviceConnectionListenerSet.forEach(l -> l.onAllCharacteristicsWrote());

                    if (mDisconnectPending) {
                        disconnectDevice();
                    }
                }
            }
        }
    };
    */

    private final BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {

            final String action = intent.getAction();

            if (action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {

                final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
                switch (state) {

                    case BluetoothAdapter.STATE_OFF:
                        mBluetoothAdapterStateListenerList.forEach(
                                l -> l.onStateChange(STATE_OFF)
                        );
                        break;

                    case BluetoothAdapter.STATE_TURNING_OFF:
                        mBluetoothAdapterStateListenerList.forEach(
                                l -> l.onStateChange(STATE_TURNING_OFF)
                        );
                        break;

                    case BluetoothAdapter.STATE_ON:
                        mBluetoothAdapterStateListenerList.forEach(
                                l -> l.onStateChange(STATE_ON)
                        );
                        break;

                    case BluetoothAdapter.STATE_TURNING_ON:
                        mBluetoothAdapterStateListenerList.forEach(
                                l -> l.onStateChange(STATE_TURNING_ON)
                        );
                        break;

                    default:
                        LoggingUtil.warning("unhandled state: " + state);
                }
            }
        }
    };

    public class LocalBinder extends Binder {

        public BluetoothDeviceConnectionService getService() {
            return BluetoothDeviceConnectionService.this;
        }
    }

    public static class DeviceConnectionListener {

        protected static final int DEVICE_DISCONNECTED = 0;
        protected static final int DEVICE_CONNECTION_ERROR_GENERIC = 1;
        protected static final int DEVICE_CONNECTION_ERROR_UNSUPPORTED = 2;
        protected static final int DEVICE_CONNECTION_ERROR_READ = 3;
        protected static final int DEVICE_CONNECTION_ERROR_WRITE = 4;

        public void onDeviceConnected() {}
        public void onDeviceDisconnected() {}

        public void onCharacteristicRead(UUID uuid, String value) {}
        public void onCharacteristicWrote(UUID uuid, String value) {}
        public void onAllCharacteristicsRead(Map<UUID, String> characteristicMap) {}
        public void onAllCharacteristicsWrote() {}

        public void onDeviceConnectionError(int errorCode) {}
    }

    public static abstract class ScanListener {
        public abstract void onScanResult(ScanResult scanResult);
    }

    public static abstract class BluetoothPreconditionStateListener {
        public abstract void onStateChange(int state);
    }

    public static abstract class BluetoothAdapterStateListener {
        public abstract void onStateChange(int state);
    }
}
