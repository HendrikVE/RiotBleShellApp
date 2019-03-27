package de.vanappsteer.riotbleshell.services;

import android.app.Service;
import android.content.Intent;
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
import io.reactivex.Observable;
import io.reactivex.disposables.Disposable;

public class BluetoothDeviceConnectionService extends Service {

    public final static int READY = 0;
    public final static int BLUETOOTH_NOT_AVAILABLE = 1;
    public final static int LOCATION_PERMISSION_NOT_GRANTED = 2;
    public final static int BLUETOOTH_NOT_ENABLED = 3;
    public final static int LOCATION_SERVICES_NOT_ENABLED = 4;

    private final UUID BLE_SERVICE_UUID = UUID.fromString("e6d54866-0292-4779-b8f8-c52bbec91e71");

    private final IBinder mBinder = new LocalBinder();

    private boolean mDisconnectPending = false;

    private HashMap<UUID, String> mCharacteristicHashMap;

    private List<ScanListener> mScanListenerList = new ArrayList<>();
    private List<BluetoothStateListener> mBluetoothStateListenerList = new ArrayList<>();

    private Set<DeviceConnectionListener> mDeviceConnectionListenerSet = new HashSet<>();

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

        mFlowDisposable = mRxBleClient.observeStateChanges()
            .subscribe(
                state -> {
                    switch (state) {
                        case READY:
                            mBluetoothStateListenerList.forEach(
                                l -> l.onStateChange(READY)
                            );
                            break;

                        case BLUETOOTH_NOT_AVAILABLE:
                            mBluetoothStateListenerList.forEach(
                                    l -> l.onStateChange(BLUETOOTH_NOT_AVAILABLE)
                            );
                            break;

                        case LOCATION_PERMISSION_NOT_GRANTED:
                            mBluetoothStateListenerList.forEach(
                                    l -> l.onStateChange(LOCATION_PERMISSION_NOT_GRANTED)
                            );
                            break;

                        case BLUETOOTH_NOT_ENABLED:
                            mBluetoothStateListenerList.forEach(
                                    l -> l.onStateChange(BLUETOOTH_NOT_ENABLED)
                            );
                            break;

                        case LOCATION_SERVICES_NOT_ENABLED:
                            mBluetoothStateListenerList.forEach(
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

    public void disconnectDevice() {

        mConnectionSubscription.dispose();
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

    public void addBluetoothStateListener(BluetoothStateListener listener) {

        mBluetoothStateListenerList.add(listener);
    }

    public void removeBluetoothStateListener(BluetoothStateListener listener) {

        mBluetoothStateListenerList.remove(listener);
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

    public static abstract class BluetoothStateListener {
        public abstract void onStateChange(int state);
    }
}
