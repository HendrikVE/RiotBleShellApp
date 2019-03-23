package de.vanappsteer.riotbleshell.interfaces;

import de.vanappsteer.riotbleshell.models.DeviceConfigModel;

public interface DeviceConfigView extends ConfigView<DeviceConfigModel> {

    void updateDeviceRoom(String room);
    void updateDeviceId(String id);
}
