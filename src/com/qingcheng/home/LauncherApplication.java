/*
* Copyright (C) 2014 MediaTek Inc.
* Modification based on code covered by the mentioned copyright
* and/or permission notice(s).
*/
/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.qingcheng.home;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import android.app.Application;

import com.qingcheng.home.R;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Environment;
import android.os.storage.StorageManager;
import android.provider.Settings;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.KeyCharacterMap;
import android.view.KeyEvent;
import android.view.ViewConfiguration;
import android.view.WindowManager;
import android.widget.TextView;
import android.provider.Settings;

import com.qingcheng.home.database.QCPreference;
import com.qingcheng.home.MTKUnreadLoader;
import com.qingcheng.home.util.QCLog;


public class LauncherApplication extends Application {
    private static final String TAG = "LauncherApplication";
    
    public static boolean getIsBootCompleted() {
    	String bootComplete = "";
    	try {
			Class SystemProperties = Class.forName("android.os.SystemProperties");
			if (SystemProperties!=null) {
				Method getBootCompletedMethod = SystemProperties.getMethod("get", String.class);
				if (getBootCompletedMethod!=null) {
					bootComplete = (String)getBootCompletedMethod.invoke(SystemProperties, "sys.boot_completed");
					if (bootComplete.equals("1")) {
						return true;
					}
				}
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    	if (QCLog.DEBUG) {
			QCLog.d(TAG, "getIsBootCompleted() bootComplete = "+bootComplete);
		}
    	return false;
    }
    
    private static String DEVICE_NAME;
    public static String getDeviceName() {
    	return DEVICE_NAME;
    }
    private static boolean hasDeviceLayoutFile = false;
    public static boolean getHasDeviceLayoutFile() {
    	return hasDeviceLayoutFile;
    }
    private static boolean hasDeviceLayoutParameter = false;
    public static boolean getHasDeviceLayoutParameter() {
		return hasDeviceLayoutParameter;
	}
	public static void setHasDeviceLayoutParameter(
			boolean hasDeviceNameScreenLayoutParameter) {
		LauncherApplication.hasDeviceLayoutParameter = hasDeviceNameScreenLayoutParameter;
	}
	private void getDeviceNameAndInfo() {
    	try {
			Class SystemProperties = Class.forName("android.os.SystemProperties");
			if (SystemProperties!=null) {
				Method getInfoMethod = SystemProperties.getMethod("get", String.class, String.class);
				DEVICE_NAME = (String)getInfoMethod.invoke(SystemProperties, "ro.cta.model", android.os.Build.MODEL);
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			DEVICE_NAME = android.os.Build.MODEL;
		}
    	DEVICE_NAME = DEVICE_NAME.toLowerCase();
    	DEVICE_NAME = DEVICE_NAME.replaceAll("[\\p{P}\\p{M}\\p{Z}\\p{S}\\p{C}]", "");
    	
    	if (getResources().getIdentifier(DEVICE_NAME, "xml", getPackageName())!=0) {
			hasDeviceLayoutFile = true;
		} else {
			hasDeviceLayoutFile = false;
		}
    	if (QCLog.DEBUG) {
			QCLog.d(TAG, "getDeviceNameAndInfo() and hasDeviceLayoutFile = "
					+hasDeviceLayoutFile+", DEVICE_NAME = "+DEVICE_NAME);
		}
    }

	private static boolean isDataReadeble = true;
	public static boolean getDataReadeble() {
		return isDataReadeble;
	}
    private static boolean gotInternalStoragePath = false;
    public static boolean gotInternalStoragePath() {
    	return gotInternalStoragePath;
    }
    private static String INTERNAL_STORAGE_PATH;

    public static String getInternalStoragePath() {
        return INTERNAL_STORAGE_PATH + "/";
    }

    public void getDeviceInternalStoragePath() {
        SharedPreferences mPreferences = getSharedPreferences(QCPreference.PREFERENCE_NAME, Context.MODE_PRIVATE);
        String path = "/storage/emulated/0";
        gotInternalStoragePath = true;
        INTERNAL_STORAGE_PATH = path;
        Editor mEditor = mPreferences.edit();
        mEditor.putString(QCPreference.KEY_INTERNAL_PATH, path);
        mEditor.commit();
    }

    public static void checkDataReadable() {
		// Check data file readable
		isDataReadeble = true;
		File mHomeTheme = new File(QCPreference.getInternalThemePath()+"test/test.zip");
		mHomeTheme.mkdirs();
		File mHomeThemeParentFile = mHomeTheme.getParentFile();
		try {
			mHomeTheme.createNewFile();
			if (mHomeThemeParentFile!=null && mHomeThemeParentFile.isDirectory() && 
					mHomeThemeParentFile.canRead() && mHomeThemeParentFile.canWrite() && 
					mHomeTheme!=null && mHomeTheme.canRead() && mHomeTheme.canWrite()) {
				isDataReadeble = true;
			} else {
				isDataReadeble = false;
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			if (QCLog.DEBUG) {
				QCLog.e(TAG, "checkDataReadable() error");
			}
			e.printStackTrace();
			isDataReadeble = false;
		}
		if (mHomeTheme.exists()) {
			mHomeTheme.delete();
		}
		if (mHomeThemeParentFile.exists()) {
			mHomeThemeParentFile.delete();
		}
	}

    /// M: flag for starting Launcher from application
    private boolean mTotallyStart = true;
    
    private Launcher mLauncher = null;
    public Launcher getLauncher() {
		return mLauncher;
	}
	public void setLauncher(Launcher mLauncher) {
		this.mLauncher = mLauncher;
	}

    //Add for MyUI---20150619
	private static boolean hasErrorWhenLauncherLayout = false;
    public static boolean isHasErrorWhenLauncherLayout() {
		return hasErrorWhenLauncherLayout;
	}
	public static void setHasErrorWhenLauncherLayout(
			boolean hasErrorWhenLauncherLayout) {
		LauncherApplication.hasErrorWhenLauncherLayout = hasErrorWhenLauncherLayout;
	}
	
    public static boolean hasNavBar;
    public static int navBarHeight;
    public static int statusBarHeight;
    private boolean checkDeviceHasNavigationBar(){
    	boolean hasMenuKey = ViewConfiguration.get(getApplicationContext()).hasPermanentMenuKey();
    	boolean hasBackKey = KeyCharacterMap.deviceHasKey(KeyEvent.KEYCODE_BACK);
    	boolean hasHomeKey = KeyCharacterMap.deviceHasKey(KeyEvent.KEYCODE_HOME);
        boolean resHasNav =  getResources().getBoolean(
        		getResources().getIdentifier("config_showNavigationBar", "bool", "android"));
        if (QCLog.DEBUG) {
			QCLog.d(TAG, "checkDeviceHasNavigationBar, resHasNav:"+resHasNav+", hasBackKey:"+hasBackKey+
	        		", hasMenuKey:"+hasMenuKey+", hasHomeKey:"+hasHomeKey+", and (resHasNav || (hasBackKey && (hasMenuKey || hasHomeKey))) is:"+
	        		(resHasNav || (hasBackKey && (hasMenuKey || hasHomeKey))));
		}
        //if (resHasNav || (hasBackKey && (hasMenuKey || hasHomeKey))) {
        if (resHasNav) {
			return true;
		} else {
			return false;
		}
    }
    
    private static float screenWidthPixel;
	private static float screenHeightPixel;
    private static float screenWidthDimen;
	private static float screenHeightDimen;
    public static float getScreenWidthPixel() {
		return screenWidthPixel;
	}
	public static float getScreenHeightPixel() {
		return screenHeightPixel + navBarHeight;
	}
    public static float getScreenWidthDimen() {
    	return screenWidthDimen;
    }
    public static float getScreenHeightDimen() {
    	return screenHeightDimen;
    }
	public static boolean isFHDScreen() {
		return ((getScreenWidthPixel() == 1080)||(getScreenHeightPixel()==1920));
	}
	public static boolean isHDScreen() {
		return ((getScreenWidthPixel() == 720)||(getScreenHeightPixel()==1280));
	}
	public static boolean isFWVGAScreen() {
		return ((getScreenWidthPixel() == 480)||(getScreenHeightPixel()==854));
	}
	
    private static float screenDensity;
    public static float getScreenDensity() {
		return screenDensity;
	}
    private static boolean isDensityChanged = false;
    public static boolean getIsDensityChanged() {
    	return isDensityChanged;
    }
    public static void setIsDensityChanged(boolean change) {
    	isDensityChanged = change;
    }
    
    private static int normalScreenWidthPixel;
    private static int normalScreenHeightPixel;
    private static boolean isNormalScreenResolutionAndDensity;
    public static boolean getIsNormalScreenResolutionAndDensity() {
    	return isNormalScreenResolutionAndDensity;
    }
    public int getPxforXLayout(boolean useRes, int resid, int pixel) {
    	int newPixel = 0;
    	if (useRes) {
    		String type = "";
    		try {
				type = getResources().getResourceTypeName(resid);
			} catch (Exception e) {
				// TODO: handle exception
				if (QCLog.DEBUG) {
					QCLog.e(TAG, "getPxforXLayout() error");
				}
				e.printStackTrace();
				return 0;
			}
			if (type.equals("dimen")) {
				newPixel = getResources().getDimensionPixelSize(resid);
			} else if (type.equals("integer")) {
				newPixel = getResources().getInteger(resid);
			}
		} else {
			newPixel = pixel;
		}
		if (isNormalScreenResolutionAndDensity) {
			return newPixel;
		} else {
			return (int)(newPixel*(getScreenWidthPixel()/normalScreenWidthPixel));
		}
    }
    public int getPxforYLayout(boolean useRes, int resid, int pixel) {
    	int newPixel = 0;
    	if (useRes) {
    		String type = "";
    		try {
				type = getResources().getResourceTypeName(resid);
			} catch (Exception e) {
				// TODO: handle exception
				if (QCLog.DEBUG) {
					QCLog.e(TAG, "getPxforYLayout() error");
				}
				e.printStackTrace();
				return 0;
			}
			if (type.equals("dimen")) {
				newPixel = getResources().getDimensionPixelSize(resid);
			} else if (type.equals("integer")) {
				newPixel = getResources().getInteger(resid);
			}
		} else {
			newPixel = pixel;
		}
    	if (QCLog.DEBUG) {
			QCLog.d(TAG, "getPxforYLayout() and newPixel = "+newPixel
					+", isNormalScreenResolutionAndDensity = "+isNormalScreenResolutionAndDensity
					+", getScreenHeightPixel() = "+getScreenHeightPixel()
					+", normalScreenHeightPixel = "+normalScreenHeightPixel
					+", if false return "+((int)(newPixel*(getScreenHeightPixel()/normalScreenHeightPixel))));
		}
		if (isNormalScreenResolutionAndDensity) {
			return newPixel;
		} else {
			return (int)(newPixel*(getScreenHeightPixel()/normalScreenHeightPixel));
		}
    }
    
	private void getDeviceScreenInfo(){
    	WindowManager wm = (WindowManager)getSystemService(Context.WINDOW_SERVICE);
    	Display display = wm.getDefaultDisplay();
    	screenWidthPixel = display.getWidth();
    	screenHeightPixel = display.getHeight();
    	DisplayMetrics metrics = new DisplayMetrics();
    	display.getMetrics(metrics);
    	screenDensity = metrics.density;
    	SharedPreferences mPreferences = getSharedPreferences(QCPreference.PREFERENCE_NAME, Context.MODE_PRIVATE);
    	Editor mEditor = mPreferences.edit();
    	float oldDensity = mPreferences.getFloat(QCPreference.KEY_SCREEN_DENSITY, -1);
    	if (oldDensity==-1 || oldDensity==screenDensity) {
			isDensityChanged = false;
		} else if (oldDensity != screenDensity) {
			isDensityChanged = true;
		}
		mEditor.putFloat(QCPreference.KEY_SCREEN_DENSITY, screenDensity);
		mEditor.commit();
    	
        hasNavBar =  checkDeviceHasNavigationBar();
        navBarHeight = hasNavBar ? getResources().getDimensionPixelSize(
        		getResources().getIdentifier("navigation_bar_height", "dimen", "android")) : 0;
        statusBarHeight = getResources().getDimensionPixelSize(getResources().getIdentifier("status_bar_height", "dimen", "android"));
        
        mEditor.putBoolean(QCPreference.KEY_HAS_NAV_BAR, hasNavBar);
        mEditor.putInt(QCPreference.KEY_NAV_BAR_HEIGHT, navBarHeight);
        mEditor.commit();
        
    	double x = Math.pow(metrics.widthPixels/metrics.xdpi, 2);
    	double y = Math.pow(metrics.heightPixels/metrics.ydpi, 2);
    	double screenInches = Math.sqrt(x + y);
    	if (screenInches <= 6.0 || (screenWidthPixel < screenHeightPixel)) {
    		isPhone = true;
		} else {
			isPhone = false;
		}
    	
    	screenWidthDimen = DynamicGrid.dpiFromPx((int)getScreenWidthPixel(), metrics);
    	screenHeightDimen = DynamicGrid.dpiFromPx((int)getScreenHeightPixel(), metrics);
    	isNormalScreenResolutionAndDensity = screenWidthDimen==360 && screenHeightDimen==640;
    	normalScreenWidthPixel = DynamicGrid.pxFromDp(360, metrics);
    	normalScreenHeightPixel = DynamicGrid.pxFromDp(640, metrics);
    }

    private boolean isPhone = false;
    public boolean getIsPhone() {
    	return isPhone;
    }
    
    /// M: added for unread feature.
    private MTKUnreadLoader mUnreadLoader;
    /// M: flag for multi window support    
    //public static final boolean FLOAT_WINDOW_SUPPORT = FeatureOption.MTK_MULTI_WINDOW_SUPPORT;

	private static LauncherApplication mAppApplication;

	public static LauncherApplication getApp() {
		return mAppApplication;
	}

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "LauncherApplication onCreate");
		mAppApplication = this;

        Settings.Global.putInt(getContentResolver(), "com.qingcheng.home_has_custom_page", 1);
        getDeviceNameAndInfo();
        getDeviceInternalStoragePath();
        checkDataReadable();
        getDeviceScreenInfo();
        
        Intent themeIntent = new Intent();
        themeIntent.setClassName("com.qingcheng.theme", "com.qingcheng.theme.ui.SyncCoverThemeReceiver");
        themeIntent.setAction("com.qingcheng.home.checklocalthemes");
        if (isFHDScreen()) {
        	themeIntent.putExtra("resolution", "FHD");
		} else if (isFWVGAScreen()) {
			themeIntent.putExtra("resolution", "FWVGA");
		} else {
			themeIntent.putExtra("resolution", "HD");
		}
        sendBroadcast(themeIntent);
        
        LauncherAppState.setApplicationContext(this);
        LauncherAppState.getInstance().setLauncehrApplication(this);
        
        /**M: register unread broadcast.@{**/
        if (getResources().getBoolean(R.bool.config_unreadSupport)) {
            mUnreadLoader = new MTKUnreadLoader(getApplicationContext());
            // Register unread change broadcast.
            IntentFilter filter = new IntentFilter();
            filter.addAction("com.mediatek.action.UNREAD_CHANGED");
            registerReceiver(mUnreadLoader, filter);
        }
        /**@}**/


//		CrashHandler crashHandler = CrashHandler.getInstance();
//		crashHandler.init(this.getApplicationContext());

	}

    @Override
    public void onTerminate() {
        super.onTerminate();

        /**M: added for unread feature, unregister unread receiver.@{**/
        if (getResources().getBoolean(R.bool.config_unreadSupport)) {
            unregisterReceiver(mUnreadLoader);
        }
        /**@}**/
        
        LauncherAppState.getInstance().onTerminate();
    }

    /// M: LauncherApplication start flag @{
    public void setTotalStartFlag() {
        mTotallyStart = true;
    }

    public void resetTotalStartFlag() {
        mTotallyStart = false;
    }

    public boolean isTotalStart() {
        return mTotallyStart;
    }
    /// M: }@
    
    /**M: Added for unread message feature.@{**/
    /**
     * M: Get unread loader, added for unread feature.
     */
    public MTKUnreadLoader getUnreadLoader() {
        return mUnreadLoader;
    }
    /**@}**/
}
