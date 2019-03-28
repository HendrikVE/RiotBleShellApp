package de.vanappsteer.riotbleshell.services;

import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;

import java.util.UUID;

public class BleTerminalProtocolService extends GenericBleProtocolService {

    public static final UUID BLE_SERVICE_UUID = UUID.fromString("e6d54866-0292-4779-b8f8-c52bbec91e71");

    public static final UUID BLE_CHARACTERISTIC_UUID_STDOUT = UUID.fromString("35f28386-3070-4f3b-ba38-27507e991762");
    public static final UUID BLE_CHARACTERISTIC_UUID_STDIN = UUID.fromString("ccdd113f-40d5-4d68-86ac-a728dd82f4aa");

    private final IBinder mBinder = new LocalBinder();

    public BleTerminalProtocolService() { }

    @Override
    public void onCreate() {
        super.onCreate();

        addDeviceConnectionListener(mDeviceConnectionListener);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    public class LocalBinder extends Binder {

        public BleTerminalProtocolService getService() {
            return BleTerminalProtocolService.this;
        }
    }

    private DeviceConnectionListener mDeviceConnectionListener = new DeviceConnectionListener() {

        @Override
        public void onCharacteristicWrote(UUID uuid, String value) {

            if (BLE_CHARACTERISTIC_UUID_STDOUT.equals(uuid)) {
                readCharacteristic(BLE_CHARACTERISTIC_UUID_STDIN);
            }
        }
    };
}
