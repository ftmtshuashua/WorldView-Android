package com.acap.app;

import android.app.Application;

/**
 * <pre>
 * Tip:
 *
 * @author A·Cap
 * @date 2021/12/15 14:26
 * </pre>
 */
public class App extends Application {
    public static App mInstance;


    @Override
    public void onCreate() {
        super.onCreate();
        mInstance = this;
    }
}
