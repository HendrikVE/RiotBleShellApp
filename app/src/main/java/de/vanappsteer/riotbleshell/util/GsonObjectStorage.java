package de.vanappsteer.riotbleshell.util;

import android.content.Context;

import com.google.gson.Gson;

public class GsonObjectStorage extends PreferenceStorage {

    private static GsonObjectStorage instance = null;

    private Gson gson = new Gson();
    private Context mContext;

    private GsonObjectStorage(Context context) {
        super(context);

        mContext = context;
    }

    public synchronized static GsonObjectStorage getInstance(Context context) {

        if(instance == null) {
            instance = new GsonObjectStorage(context);
        }
        return instance;
    }

    public void storeObject(String key, Object object) {
        String json = gson.toJson(object);
        putString(key, json);
    }

    public void storeObject(int stringId, Object object) {
        String key = mContext.getString(stringId);
        storeObject(key, object);
    }

    public <T> T loadObject(String key, Class<T> classOfT) {
        String json = getString(key, null);

        return gson.fromJson(json, classOfT);
    }

    public <T> T loadObject(int stringId, Class<T> classOfT) {
        String key = mContext.getString(stringId);
        return loadObject(key, classOfT);
    }

}
