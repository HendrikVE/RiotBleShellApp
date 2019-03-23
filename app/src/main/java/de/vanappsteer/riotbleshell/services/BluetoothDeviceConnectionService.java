package de.vanappsteer.riotbleshell.services;

import android.app.Service;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import de.vanappsteer.riotbleshell.util.LoggingUtil;

import static de.vanappsteer.riotbleshell.services.BluetoothDeviceConnectionService.DeviceConnectionListener.DEVICE_CONNECTION_ERROR_GENERIC;
import static de.vanappsteer.riotbleshell.services.BluetoothDeviceConnectionService.DeviceConnectionListener.DEVICE_CONNECTION_ERROR_READ;
import static de.vanappsteer.riotbleshell.services.BluetoothDeviceConnectionService.DeviceConnectionListener.DEVICE_CONNECTION_ERROR_UNSUPPORTED;
import static de.vanappsteer.riotbleshell.services.BluetoothDeviceConnectionService.DeviceConnectionListener.DEVICE_CONNECTION_ERROR_WRITE;
import static de.vanappsteer.riotbleshell.services.BluetoothDeviceConnectionService.DeviceConnectionListener.DEVICE_DISCONNECTED;

public class BluetoothDeviceConnectionService extends Service {

    private final UUID BLE_SERVICE_UUID = UUID.fromString("e6d54866-0292-4779-b8f8-c52bbec91e71");

    private final IBinder mBinder = new LocalBinder();

    private BluetoothGatt mBluetoothGatt = null;
    private BluetoothGattService mBluetoothGattService = null;
    private boolean mDisconnectPending = false;

    private HashMap<UUID, String> mCharacteristicHashMap;
    private final Queue<UUID> mReadCharacteristicsOperationsQueue = new LinkedList<>();
    private final Queue<UUID> mWriteCharacteristicsOperationsQueue = new LinkedList<>();

    private Set<DeviceConnectionListener> mDeviceConnectionListenerSet = new HashSet<>();

    public BluetoothDeviceConnectionService() {

    }

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
        public void onAllCharacteristicsRead(Map<UUID, String> characteristicMap) {}
        public void onAllCharacteristicsWrote() {}
        public void onDeviceConnectionError(int errorCode) {}
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public void onDestroy() {
        disconnectDevice();
    }

    public void connectDevice(BluetoothDevice device) {

        LoggingUtil.debug("connectDevice()");

        mBluetoothGatt = device.connectGatt(BluetoothDeviceConnectionService.this, false, mGattCharacteristicCallback);
    }

    public void disconnectDevice() {

        if (mBluetoothGatt != null) {

            synchronized (mReadCharacteristicsOperationsQueue) {
                synchronized (mWriteCharacteristicsOperationsQueue) {
                    int operationsPending = mReadCharacteristicsOperationsQueue.size() + mWriteCharacteristicsOperationsQueue.size();

                    if (operationsPending == 0) {
                        mBluetoothGatt.disconnect();
                        mDisconnectPending = false;
                    }
                    else {
                        mDisconnectPending = true;
                    }
                }
            }
        }
    }

    private boolean isDisconnected() {
        return mBluetoothGatt == null || mBluetoothGattService == null;
    }

    public List<UUID> getReadableCharacteristicUuidList() {

        List<BluetoothGattCharacteristic> list = mBluetoothGattService.getCharacteristics();
        List<UUID> uuidList = list.stream()
                .filter(c -> (c.getProperties() & BluetoothGattCharacteristic.PROPERTY_READ) != 0)
                .map(BluetoothGattCharacteristic::getUuid)
                .collect(Collectors.toList());

        return uuidList;
    }

    public List<UUID> getWriteableCharacteristicUuidList() {

        List<BluetoothGattCharacteristic> list = mBluetoothGattService.getCharacteristics();
        List<UUID> uuidList = list.stream()
                .filter(c -> (c.getProperties() & BluetoothGattCharacteristic.PROPERTY_WRITE) != 0)
                .map(BluetoothGattCharacteristic::getUuid)
                .collect(Collectors.toList());

        return uuidList;
    }

    public void readCharacteristic(UUID uuid) {

        List<UUID> list = new ArrayList<>();
        list.add(uuid);

        readCharacteristics(list);
    }

    public void writeCharacteristic(UUID uuid, String value) {

        Map<UUID, String> map = new HashMap<>();
        map.put(uuid, value);

        writeCharacteristics(map);
    }

    public void readCharacteristics(List<UUID> list) {

        if (isDisconnected()) {
            mDeviceConnectionListenerSet.forEach(l -> l.onDeviceConnectionError(DEVICE_DISCONNECTED));

            return;
        }

        mCharacteristicHashMap = new HashMap<>();

        mReadCharacteristicsOperationsQueue.addAll(list);

        BluetoothGattCharacteristic characteristic
                = mBluetoothGattService.getCharacteristic(mReadCharacteristicsOperationsQueue.poll());

        // initial call of readCharacteristic, further calls are done within onCharacteristicRead afterwards
        boolean success = mBluetoothGatt.readCharacteristic(characteristic);
        if (! success) {
            mDeviceConnectionListenerSet.forEach(l -> l.onDeviceConnectionError(DEVICE_CONNECTION_ERROR_READ));
        }
    }

    public void writeCharacteristics(Map<UUID, String> characteristicMap) {

        if (isDisconnected()) {
            mDeviceConnectionListenerSet.forEach(l -> l.onDeviceConnectionError(DEVICE_DISCONNECTED));

            return;
        }

        synchronized (mWriteCharacteristicsOperationsQueue) {

            boolean needInitialCall = mWriteCharacteristicsOperationsQueue.size() == 0;

            BluetoothGattService gattService = mBluetoothGatt.getService(BLE_SERVICE_UUID);

            for (Map.Entry<UUID, String> entry : characteristicMap.entrySet()) {

                BluetoothGattCharacteristic characteristic = gattService.getCharacteristic(entry.getKey());
                characteristic.setValue(entry.getValue());

                LoggingUtil.debug("entry.getValue() = " + entry.getValue());
                LoggingUtil.debug("characteristic.getStringValue(0) = " +  characteristic.getStringValue(0));

                mWriteCharacteristicsOperationsQueue.add(characteristic.getUuid());
            }

            if (needInitialCall) {

                BluetoothGattCharacteristic characteristic
                        = mBluetoothGattService.getCharacteristic(mWriteCharacteristicsOperationsQueue.poll());

                // initial call of writeCharacteristic, further calls are done within onCharacteristicWrite afterwards
                boolean success = mBluetoothGatt.writeCharacteristic(characteristic);
                if (! success) {
                    mDeviceConnectionListenerSet.forEach(l -> l.onDeviceConnectionError(DEVICE_CONNECTION_ERROR_WRITE));
                }
            }
        }
    }

    public void addDeviceConnectionListener(DeviceConnectionListener listener) {
        mDeviceConnectionListenerSet.add(listener);
    }

    public void removeDeviceConnectionListener(DeviceConnectionListener listener) {
        mDeviceConnectionListenerSet.remove(listener);
    }

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

            LoggingUtil.debug("onCharacteristicRead");
            LoggingUtil.debug("uuid: " + characteristicRead.getUuid());
            LoggingUtil.debug("value: " + characteristicRead.getStringValue(0));

            mCharacteristicHashMap.put(characteristicRead.getUuid(), characteristicRead.getStringValue(0));

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

            LoggingUtil.debug("onCharacteristicWrite");
            LoggingUtil.debug("uuid: " + characteristicWrote.getUuid());
            LoggingUtil.debug("value: " + characteristicWrote.getStringValue(0));

            LoggingUtil.debug("pending queue size: " + mWriteCharacteristicsOperationsQueue.size());

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
}
