package com.duoshine.opengltest;

import android.app.Application;
import android.content.Context;

/**
 * Created by duo_shine on 2022/3/8
 */
public class MyApplication extends Application {

    private static Context mContext;

    @Override
    public void onCreate() {
        super.onCreate();
        mContext = this;
    }

    public static Context getContext() {
        return mContext;
    }
}
