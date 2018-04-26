package com.qingcheng.home;

import com.qingcheng.home.database.QCPreference;
import com.qingcheng.home.util.QCLog;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class StartupReceiver extends BroadcastReceiver {

    static final String SYSTEM_READY = "com.android.launcher3.SYSTEM_READY";

    @Override
    public void onReceive(Context context, Intent intent) {
//    	if (intent.getAction().equals(Intent.ACTION_MEDIA_MOUNTED)){
//    		String pathString = intent.getDataString();
//    		if (pathString!=null && !pathString.isEmpty()) {
//				String path = pathString.split("file://")[1];
//				if ((LauncherApplication.gotInternalStoragePath() &&
//						LauncherApplication.getInternalStoragePath().contains(path)) ||
//						!LauncherApplication.gotInternalStoragePath()) {
//					LauncherApplication mApplication = (LauncherApplication)context.getApplicationContext();
//					mApplication.getDeviceInternalStoragePath();
//					mApplication.checkDataReadable();
//
//
//					Intent mIntent = new Intent();
//					mIntent.setAction(QCPreference.INTENT_ACTION_UPDATE_THEME);
//					context.sendBroadcast(mIntent);
//				}
//				if (QCLog.DEBUG) {
//					QCLog.d("StartupReceiver", "onReceive() path = "+path
//							+" , and gotInternalStoragePath = "+LauncherApplication.gotInternalStoragePath()
//							+" , and getInternalStoragePath = "+LauncherApplication.getInternalStoragePath()
//							+" , flag = "+(LauncherApplication.getInternalStoragePath().contains(path)));
//				}
//			}
//		} else if (intent.getAction().equals(Intent.ACTION_BOOT_COMPLETED)) {
			context.sendStickyBroadcast(new Intent(SYSTEM_READY));
//		}
    }
}
