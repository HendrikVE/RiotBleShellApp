package de.vanappsteer.riotbleshell.interfaces;

import de.vanappsteer.riotbleshell.models.SensorConfigModel;

public interface SensorConfigView extends ConfigView<SensorConfigModel> {

    void updateSensorPollInterval(String pollInterval);
}
