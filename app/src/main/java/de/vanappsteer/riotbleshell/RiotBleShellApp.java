package de.vanappsteer.riotbleshell;

import android.app.Application;

import de.vanappsteer.riotbleshell.util.LoggingUtil;

public class RiotBleShellApp extends Application {

    private Thread.UncaughtExceptionHandler defaultUncaughtExceptionHandler;

    @Override
    public void onCreate() {

        super.onCreate();

        defaultUncaughtExceptionHandler = Thread.getDefaultUncaughtExceptionHandler();

        Thread.setDefaultUncaughtExceptionHandler((thread, ex) -> {

            LoggingUtil.error(ex.getMessage());
            ex.printStackTrace();

            // re-throw critical exception further to the os (important)
            defaultUncaughtExceptionHandler.uncaughtException(thread, ex);

            System.exit(2);
        });
    }

}
