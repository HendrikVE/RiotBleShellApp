package de.vanappsteer.riotbleshell.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import androidx.annotation.StringRes;

import java.util.Set;

public class PreferenceStorage {

    private static PreferenceStorage instance = null;

    private final SharedPreferences mSharedPreferences;

    private Context mContext;
    private SharedPreferences.Editor mEditor;
    private static final Object mEditorLock = new Object();

    protected PreferenceStorage(Context context) {

        mContext = context;
        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
    }

    public synchronized static PreferenceStorage getInstance(Context context) {

        if(instance == null) {
            instance = new PreferenceStorage(context);
        }
        return instance;
    }

    /*
    SETTER
     */
    public void putString(String key, String value) {

        synchronized(mEditorLock) {
            mEditor = mSharedPreferences.edit();
            mEditor.putString(key, value);
            mEditor.apply();
        }
    }

    public void putString(@StringRes int stringId, String value) {
        String key = mContext.getString(stringId);
        putString(key, value);
    }


    public void putInt(String key, int value) {

        synchronized(mEditorLock) {
            mEditor = mSharedPreferences.edit();
            mEditor.putInt(key, value);
            mEditor.apply();
        }
    }

    public void putInt(@StringRes int stringId, int value) {
        String key = mContext.getString(stringId);
        putInt(key, value);
    }

    public void putBoolean(String key, boolean value) {

        synchronized(mEditorLock) {
            mEditor = mSharedPreferences.edit();
            mEditor.putBoolean(key, value);
            mEditor.apply();
        }
    }

    public void putBoolean(@StringRes int stringId, boolean value) {
        String key = mContext.getString(stringId);
        putBoolean(key, value);
    }

    public void putFloat(String key, float value) {

        synchronized(mEditorLock) {
            mEditor = mSharedPreferences.edit();
            mEditor.putFloat(key, value);
            mEditor.apply();
        }
    }

    public void putFloat(@StringRes int stringId, float value) {
        String key = mContext.getString(stringId);
        putFloat(key, value);
    }

    public void putLong(String key, long value) {

        synchronized(mEditorLock) {
            mEditor = mSharedPreferences.edit();
            mEditor.putLong(key, value);
            mEditor.apply();
        }
    }

    public void putLong(@StringRes int stringId, long value) {
        String key = mContext.getString(stringId);
        putLong(key, value);
    }

    public void putStringSet(String key, Set<String> values) {

        synchronized(mEditorLock) {
            mEditor = mSharedPreferences.edit();
            mEditor.putStringSet(key, values);
            mEditor.apply();
        }
    }

    public void putStringSet(@StringRes int stringId, Set<String> value) {
        String key = mContext.getString(stringId);
        putStringSet(key, value);
    }

    /*
    GETTER
     */
    public String getString(String key, String defaultValue) {

        synchronized(mSharedPreferences) {
            return mSharedPreferences.getString(key, defaultValue);
        }
    }

    public String getString(@StringRes int stringId, String defaultValue) {
        String key = mContext.getString(stringId);
        return getString(key, defaultValue);
    }

    public int getInt(String key, int defaultValue) {

        synchronized(mSharedPreferences) {
            return mSharedPreferences.getInt(key, defaultValue);
        }
    }

    public int getInt(@StringRes int stringId, int defaultValue) {
        String key = mContext.getString(stringId);
        return getInt(key, defaultValue);
    }

    public boolean getBoolean(String key, boolean defaultValue) {

        synchronized(mSharedPreferences) {
            return mSharedPreferences.getBoolean(key, defaultValue);
        }
    }

    public boolean getBoolean(@StringRes int stringId, boolean defaultValue) {
        String key = mContext.getString(stringId);
        return getBoolean(key, defaultValue);
    }

    public float getFloat(String key, float defaultValue) {

        synchronized(mSharedPreferences) {
            return mSharedPreferences.getFloat(key, defaultValue);
        }
    }

    public float getFloat(@StringRes int stringId, float defaultValue) {
        String key = mContext.getString(stringId);
        return getFloat(key, defaultValue);
    }

    public long getLong(String key, long defaultValue) {

        synchronized(mSharedPreferences) {
            return mSharedPreferences.getLong(key, defaultValue);
        }
    }

    public long getLong(@StringRes int stringId, long defaultValue) {
        String key = mContext.getString(stringId);
        return getLong(key, defaultValue);
    }

    public Set<String> getStringSet(String key, Set<String> defaultValues) {

        synchronized(mSharedPreferences) {
            return mSharedPreferences.getStringSet(key, defaultValues);
        }
    }

    public Set<String> getStringSet(@StringRes int stringId, Set<String> defaultValues) {
        String key = mContext.getString(stringId);
        return getStringSet(key, defaultValues);
    }

    /*
    OTHER
     */
    public void remove(String key) {

        synchronized(mEditorLock) {
            mEditor = mSharedPreferences.edit();
            mEditor.remove(key);
            mEditor.apply();
        }
    }

    public void remove(@StringRes int stringId) {
        String key = mContext.getString(stringId);
        remove(key);
    }

    public void clear() {

        synchronized(mEditorLock) {
            mEditor = mSharedPreferences.edit();
            mEditor.clear();
            mEditor.apply();
        }
    }

    public boolean contains(String key) {

        synchronized(mSharedPreferences) {
            return mSharedPreferences.contains(key);
        }
    }

    public boolean contains(@StringRes int stringId) {
        String key = mContext.getString(stringId);
        return contains(key);
    }

}
