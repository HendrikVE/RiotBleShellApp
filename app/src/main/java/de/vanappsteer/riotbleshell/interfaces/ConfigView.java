package de.vanappsteer.riotbleshell.interfaces;

import de.vanappsteer.riotbleshell.models.ConfigModel;

public interface ConfigView<T extends ConfigModel> {

    void setModel(T model);
    T getModel();

    void updateDisplayedErrors();
}
