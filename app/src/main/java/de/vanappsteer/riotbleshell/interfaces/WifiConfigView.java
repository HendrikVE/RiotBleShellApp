package de.vanappsteer.riotbleshell.interfaces;

import de.vanappsteer.riotbleshell.models.WifiConfigModel;

public interface WifiConfigView extends ConfigView<WifiConfigModel> {

    void updateWifiSsid(String ssid);
    void updateWifiPassword(String password);
}
