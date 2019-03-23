package de.vanappsteer.riotbleshell.interfaces;

import de.vanappsteer.riotbleshell.models.MqttConfigModel;

public interface MqttConfigView extends ConfigView<MqttConfigModel> {

    void updateMqttUsername(String username);
    void updateMqttPassword(String password);
    void updateMqttBrokerAddress(String address);
    void updateMqttBrokerPort(String port);
}
