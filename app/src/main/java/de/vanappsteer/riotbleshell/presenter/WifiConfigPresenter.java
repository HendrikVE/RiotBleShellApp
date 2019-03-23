package de.vanappsteer.riotbleshell.presenter;

import de.vanappsteer.riotbleshell.interfaces.ConfigController;
import de.vanappsteer.riotbleshell.interfaces.WifiConfigView;
import de.vanappsteer.riotbleshell.models.WifiConfigModel;

public class WifiConfigPresenter extends ConfigController<WifiConfigModel> {

    private WifiConfigModel mModel;
    private WifiConfigView mView;

    public WifiConfigPresenter(WifiConfigModel model, WifiConfigView view) {
        mModel = model;
        mView = view;
    }

    @Override
    public void updateView() {
        mView.updateWifiSsid(mModel.getWifiSsid());
        mView.updateWifiPassword(mModel.getWifiPassword());
    }

    @Override
    public WifiConfigModel getModel() {
        return mModel;
    }


    /* BEGIN GETTER */
    public String getSsid() {
        return mModel.getWifiSsid();
    }

    public String getPassword() {
        return mModel.getWifiPassword();
    }
    /* END GETTER */


    /* BEGIN SETTER */
    public void setWifiSsid(String ssid) {
        mModel.setWifiSsid(ssid);
    }

    public void setWifiPassword(String password) {
        mModel.setWifiPassword(password);
    }
    /* END SETTER */
}
