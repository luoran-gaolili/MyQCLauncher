package com.qingcheng.home.util;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.util.Log;

import com.qingcheng.home.Utilities;


public class ConfigMonitor extends BroadcastReceiver {

    private final Context mContext;
    private final float mFontScale;
    private final int mDensity;

    public ConfigMonitor(Context context) {
        mContext = context;

        Configuration config = context.getResources().getConfiguration();
        mFontScale = config.fontScale;
        mDensity = getDensity(config);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        Configuration config = context.getResources().getConfiguration();
        if (mFontScale != config.fontScale || mDensity != getDensity(config)) {
            Log.d("ConfigMonitor", "Configuration changed, restarting launcher");
            mContext.unregisterReceiver(this);
            android.os.Process.killProcess(android.os.Process.myPid());
        }
    }

    public void register() {
        mContext.registerReceiver(this, new IntentFilter(Intent.ACTION_CONFIGURATION_CHANGED));
    }

    private static int getDensity(Configuration config) {
        return Utilities.ATLEAST_JB_MR1 ? config.densityDpi : 0;
    }
}
