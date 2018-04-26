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

import android.app.SearchManager;

import com.qingcheng.home.R;
import com.qingcheng.home.custom.binder.NewsBinder;
import com.qingcheng.home.database.QCPreference;

import android.app.WallpaperManager;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Handler;
import android.os.ParcelFileDescriptor;
import android.os.PowerManager;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ListView;

import com.qingcheng.home.compat.LauncherAppsCompat;
import com.qingcheng.home.compat.PackageInstallerCompat.PackageInstallInfo;

import com.mediatek.launcher3.ext.LauncherLog;
import com.qingcheng.home.util.ConfigMonitor;
//import com.ragentek.infostream.engine.ToutiaoEngine;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.ref.WeakReference;
import java.net.URLEncoder;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;

public class LauncherAppState implements DeviceProfile.DeviceProfileCallbacks {
    private static final String TAG = "LauncherAppState";
    private static final String SHARED_PREFERENCES_KEY = "com.qingcheng.home.baseprefs";

    private static final boolean DEBUG = false;
    public static long mLastNewsUpdateTime;
    private static StringBuffer mUrlBuffer;
    private static StringBuffer mNewsParams;

    private final AppFilter mAppFilter;
    private final BuildInfo mBuildInfo;
    private LauncherModel mModel;
    private IconCache mIconCache;
    private WidgetPreviewLoader.CacheDb mWidgetPreviewCacheDb;
    private boolean mIsScreenLarge;
    private float mScreenDensity;
    private int mLongPressTimeout = 300;
    private boolean mWallpaperChangedSinceLastCheck;

    private static WeakReference<LauncherProvider> sLauncherProvider;
    private static Context sContext;
    private LauncherApplication mApp;

    private static LauncherAppState INSTANCE;

    private DynamicGrid mDynamicGrid;

    private PowerManager mPowerManager;
    public AppInfoMarket mMarketRecommed;
    public boolean mShowCustomIconAni;

    public static final String KEY_SIZE_PROJECTION_WALLPAPER = "size_projection_wallpaper";

    public static final String KEY_IS_PROJECTION_WALLPAPER = "is_projection_wallpaper";

    public static final String NAME_CUSTOM_SHARE = "custom_share";

    static String ACTION_HIDE_ICON = "com.videogamecenter.action.HIDE_ICON";
    static String HIDE_ICON_PACKAGENAME = "com.videogamecenter.extra.PACKAGE_NAME";

    public static LauncherAppState getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new LauncherAppState();
        }
        return INSTANCE;
    }

    private ArrayList<AppInfo> mApps = new ArrayList<>();
    private ArrayList<AppInfo> mItems  = new ArrayList<>();

    public static LauncherAppState getInstanceNoCreate() {
        return INSTANCE;
    }

    public Context getContext() {
        return sContext;
    }

    public static void setApplicationContext(Context context) {
        if (sContext != null) {
            Log.w(Launcher.TAG, "setApplicationContext called twice! old=" + sContext + " new=" + context);
        }
        sContext = context.getApplicationContext();
    }

    private LauncherAppState() {
        if (sContext == null) {
            throw new IllegalStateException("LauncherAppState inited before app context set");
        }

        Log.v(Launcher.TAG, "LauncherAppState inited");

        if (sContext.getResources().getBoolean(R.bool.debug_memory_enabled)) {
            MemoryTracker.startTrackingMe(sContext, "L");
        }

        // set sIsScreenXLarge and mScreenDensity *before* creating icon cache
        mIsScreenLarge = isScreenLarge(sContext.getResources());
        mScreenDensity = sContext.getResources().getDisplayMetrics().density;

        recreateWidgetPreviewDb();
        mIconCache = new IconCache(sContext);

        mAppFilter = AppFilter.loadByName(sContext.getString(R.string.app_filter_class));
        mBuildInfo = BuildInfo.loadByName(sContext.getString(R.string.build_info_class));
        mModel = new LauncherModel(this, mIconCache, mAppFilter);
        final LauncherAppsCompat launcherApps = LauncherAppsCompat.getInstance(sContext);
        launcherApps.addOnAppsChangedCallback(mModel);

        // Register intent receivers
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_LOCALE_CHANGED);
//        filter.addAction(Intent.ACTION_CONFIGURATION_CHANGED);
        sContext.registerReceiver(mModel, filter);
        filter = new IntentFilter();
        filter.addAction(SearchManager.INTENT_GLOBAL_SEARCH_ACTIVITY_CHANGED);
        sContext.registerReceiver(mModel, filter);
        filter = new IntentFilter();
        filter.addAction(SearchManager.INTENT_ACTION_SEARCHABLES_CHANGED);
        sContext.registerReceiver(mModel, filter);

        // Add for MyUI---20150722
        //filter = new IntentFilter();
        //filter.addAction(Intent.ACTION_PACKAGE_ADDED);
        //filter.addAction(Intent.ACTION_PACKAGE_REMOVED);
        //filter.addAction(Intent.ACTION_PACKAGE_CHANGED);
        //filter.addDataScheme("package");
        //sContext.registerReceiver(mModel, filter);
    	// Add for navigationbar hide Jing.Wu 20150915 start
        filter = new IntentFilter();
        filter.addAction(QCPreference.INTENT_ACTION_HIDE_NAVIGATIONBAR);
        sContext.registerReceiver(mModel, filter);
    	// Add for navigationbar hide Jing.Wu 20150915 end
        
        filter = new IntentFilter();
        filter.addAction(Intent.ACTION_EXTERNAL_APPLICATIONS_AVAILABLE);
        filter.addAction(Intent.ACTION_EXTERNAL_APPLICATIONS_UNAVAILABLE);
        sContext.registerReceiver(mModel, filter);
        filter = new IntentFilter();
        filter.addAction(QCPreference.INTENT_ACTION_REFLUSH_WORKSPACE);
        sContext.registerReceiver(mModel, filter);
        
        // Move to Launcher class because of WallpaperSettings Jing.Wu 20170118 start
        filter = new IntentFilter();
        filter.addAction(QCPreference.INTENT_ACTION_SWITCH_THEME);
//        filter.addAction(QCPreference.INTENT_ACTION_UPDATE_THEME);
        sContext.registerReceiver(mModel, filter);
        // Move to Launcher class because of WallpaperSettings Jing.Wu 20170118 end
        
        
        filter = new IntentFilter();
        filter.addAction(QCPreference.INTENT_ACTION_SWITCH_SLIDE_TYPE);
        sContext.registerReceiver(mModel, filter);

        filter = new IntentFilter();
        filter.addAction(Intent.ACTION_WALLPAPER_CHANGED);
        sContext.registerReceiver(mModel, filter);

        filter = new IntentFilter();
        filter.addAction(ACTION_HIDE_ICON);
        sContext.registerReceiver(mModel, filter);

        if (LauncherLog.DEBUG) {
            LauncherLog.d(TAG, "LauncherAppState: mIconCache = " + mIconCache + ", mModel = "
                    + mModel + ", this = " + this);
        }

        // Register for changes to the favorites
        ContentResolver resolver = sContext.getContentResolver();
        resolver.registerContentObserver(LauncherSettings.Favorites.CONTENT_URI, true,
                mFavoritesObserver);

        new ConfigMonitor(sContext).register();
        mPowerManager = (PowerManager)sContext.getSystemService(Context.POWER_SERVICE);
    }

    public void recreateWidgetPreviewDb() {
        if (mWidgetPreviewCacheDb != null) {
            mWidgetPreviewCacheDb.close();
        }
        mWidgetPreviewCacheDb = new WidgetPreviewLoader.CacheDb(sContext);
    }

    /**
     * Call from Application.onTerminate(), which is not guaranteed to ever be called.
     */
    public void onTerminate() {
        sContext.unregisterReceiver(mModel);
        final LauncherAppsCompat launcherApps = LauncherAppsCompat.getInstance(sContext);
        launcherApps.removeOnAppsChangedCallback(mModel);

        ContentResolver resolver = sContext.getContentResolver();
        resolver.unregisterContentObserver(mFavoritesObserver);
    }

    /**
     * Receives notifications whenever the user favorites have changed.
     */
    private final ContentObserver mFavoritesObserver = new ContentObserver(new Handler()) {
        @Override
        public void onChange(boolean selfChange) {
            if (LauncherLog.DEBUG) {
                LauncherLog.d(TAG, "mFavoritesObserver onChange: selfChange = " + selfChange);
            }

            // If the database has ever changed, then we really need to force a reload of the
            // workspace on the next load
            mModel.resetLoadedState(false, true);
            mModel.startLoaderFromBackground();
        }
    };

    LauncherModel setLauncher(Launcher launcher) {
        if (mModel == null) {
            throw new IllegalStateException("setLauncher() called before init()");
        }
        mModel.initialize(launcher);
        return mModel;
    }

    public IconCache getIconCache() {
        return mIconCache;
    }

    LauncherModel getModel() {
        return mModel;
    }

    boolean shouldShowAppOrWidgetProvider(ComponentName componentName) {
        return mAppFilter == null || mAppFilter.shouldShowApp(componentName);
    }

    WidgetPreviewLoader.CacheDb getWidgetPreviewCacheDb() {
        return mWidgetPreviewCacheDb;
    }

    static void setLauncherProvider(LauncherProvider provider) {
        sLauncherProvider = new WeakReference<LauncherProvider>(provider);
    }

    static LauncherProvider getLauncherProvider() {
        return sLauncherProvider.get();
    }

    public static String getSharedPreferencesKey() {
        return SHARED_PREFERENCES_KEY;
    }

    /// M: Add for smart book feature. Force re-init dynamic grid if database ID is changed.
    DeviceProfile initDynamicGrid(Context context, int minWidth, int minHeight,
                                  int width, int height,
                                  int availableWidth, int availableHeight, boolean forceReinit) {
        /// M: Add for smart book feature. Force re-init dynamic grid if database ID is changed.
        if (forceReinit) {
            mDynamicGrid = null;
        }

        if (mDynamicGrid == null) {
            mDynamicGrid = new DynamicGrid(context,
                    context.getResources(),
                    minWidth, minHeight, width, height,
                    availableWidth, availableHeight);
            mDynamicGrid.getDeviceProfile().addCallback(this);
        }

        // Update the icon size
        DeviceProfile grid = mDynamicGrid.getDeviceProfile();
        grid.updateFromConfiguration(context, context.getResources(), width, height,
                availableWidth, availableHeight);
        return grid;
    }
    public DynamicGrid getDynamicGrid() {
        return mDynamicGrid;
    }

    public boolean isScreenLarge() {
        return mIsScreenLarge;
    }

    // Need a version that doesn't require an instance of LauncherAppState for the wallpaper picker
    public static boolean isScreenLarge(Resources res) {
        return res.getBoolean(R.bool.is_large_tablet);
    }

    public static boolean isScreenLandscape(Context context) {
        return context.getResources().getConfiguration().orientation ==
            Configuration.ORIENTATION_LANDSCAPE;
    }

    public float getScreenDensity() {
        return mScreenDensity;
    }

    public int getLongPressTimeout() {
        return mLongPressTimeout;
    }

    public void onWallpaperChanged() {
        mWallpaperChangedSinceLastCheck = true;
    }

    public boolean hasWallpaperChangedSinceLastCheck() {
        boolean result = mWallpaperChangedSinceLastCheck;
        mWallpaperChangedSinceLastCheck = false;
        return result;
    }

    @Override
    public void onAvailableSizeChanged(DeviceProfile grid) {
        Utilities.setIconSize(grid.iconSizePx);
    }

    public static boolean isDisableAllApps() {
        // Returns false on non-dogfood builds.
    	//Change for MyUI---20150703
        //return getInstance().mBuildInfo.isDogfoodBuild() &&
        //        Launcher.isPropertyEnabled(Launcher.DISABLE_ALL_APPS_PROPERTY);
    	return true;
    }

    public static boolean isDogfoodBuild() {
        return getInstance().mBuildInfo.isDogfoodBuild();
    }

    public void setPackageState(ArrayList<PackageInstallInfo> installInfo) {
        mModel.setPackageState(installInfo);
    }

    /**
     * Updates the icons and label of all icons for the provided package name.
     */
    public void updatePackageBadge(String packageName) {
        mModel.updatePackageBadge(packageName);
    }

    /// M: Add for smart book feature. Re-init static variables. @{
    public void updateScreenInfo() {
        if (LauncherLog.DEBUG) {
            LauncherLog.d(TAG, "LauncherAppState: updateScreenInfo");
        }

        // set sIsScreenXLarge and mScreenDensity *before* creating icon cache
        mIsScreenLarge = isScreenLarge(sContext.getResources());
        mScreenDensity = sContext.getResources().getDisplayMetrics().density;
    }
    /// M: }@
    
    public void setLauncehrApplication(LauncherApplication app){
        mApp = app;
    }
    
    public LauncherApplication getLauncehrApplication(){
        return mApp;
    }

    public PowerManager getPowerManager() {
        return mPowerManager;
    }

    public ArrayList<AppInfo> getApps() {
        return mApps;
    }

    public void setApps(ArrayList<AppInfo> mApps) {
        this.mApps = mApps;
    }

    public ArrayList<AppInfo> getItems() {
        return mItems;
    }

    public void setItems(ArrayList<AppInfo> mItems) {
        this.mItems = mItems;
    }

    public static StringBuffer getNewsParams(){
        if(mNewsParams == null) {
            mNewsParams = new StringBuffer();
            try {
                TelephonyManager tm = (TelephonyManager) sContext.getSystemService(Context.TELEPHONY_SERVICE);
                getNewsUrlSb().append("?").append(getSecureKey());
                mNewsParams.append("os=").append("Android");
                mNewsParams.append("&os_version=").append(Build.VERSION.RELEASE);
                mNewsParams.append("&os_api=").append(Build.VERSION.SDK_INT);
                mNewsParams.append("&udid=").append(tm.getDeviceId());
                mNewsParams.append("&openudid=").append(Settings.Secure.getString(sContext.getContentResolver(), Settings.Secure.ANDROID_ID));
                mNewsParams.append("&device_model=").append(URLEncoder.encode(Build.MODEL, "utf-8"));
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }
        return mNewsParams;
    }

    public static StringBuffer getNewsUrlSb(){
        if(mUrlBuffer == null){
            mUrlBuffer = new StringBuffer(NewsBinder.TOKEN_URL);
        }

        return mUrlBuffer;
    }


    final private static char[] hexArray = "0123456789abcdef".toCharArray();

    private static String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }


    public static String getSecureKey() throws NoSuchAlgorithmException, UnsupportedEncodingException {
        String timestamp = String.valueOf(System.currentTimeMillis());
        String nonce = String.valueOf(Math.random());
        String key = NewsBinder.KEYSTRING;
        String[] list = new String[3];
        list[0] = timestamp;
        list[1] = nonce;
        list[2] = key;
        Arrays.sort(list);

        String text = TextUtils.join("", list);
        MessageDigest md = MessageDigest.getInstance("SHA-1");
        byte[] textBytes = text.getBytes("iso-8859-1");
        md.update(textBytes, 0, textBytes.length);
        byte[] sha1hash = md.digest();
        String signature = bytesToHex(sha1hash);

        StringBuffer sb = new StringBuffer();
        sb.append("timestamp=").append(timestamp).append("&nonce=").append(nonce).append("&signature=").append(signature).append("&partner=").append(NewsBinder.APISTRING);
        return sb.toString();
    }

    public static boolean isCustomTheme(){
        SharedPreferences pref = sContext.getSharedPreferences(QCPreference.PREFERENCE_NAME, Context.MODE_PRIVATE);
        return pref.getBoolean(QCPreference.KEY_CUSTOM_THEME, false);
    }

    public static void setWallpaperChanged() {
        WallpaperManager wallpaperManager = WallpaperManager.getInstance(sContext);
        long size = 0;
        ParcelFileDescriptor parcelFileDescriptor = null;
        try {
            parcelFileDescriptor = wallpaperManager.getWallpaperFile(WallpaperManager.FLAG_SYSTEM);
            if (parcelFileDescriptor != null) {
                size = parcelFileDescriptor.getStatSize();
                parcelFileDescriptor.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        SharedPreferences sharedPreferences = sContext.getSharedPreferences(NAME_CUSTOM_SHARE, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        long defaultSize = sharedPreferences.getLong(KEY_SIZE_PROJECTION_WALLPAPER, size);
        Log.d(TAG, "wall paper default size =" + defaultSize + "  current size=" + size);
        if (size != defaultSize) {
            editor.putBoolean(KEY_IS_PROJECTION_WALLPAPER, false).commit();
        } else {
            editor.putBoolean(KEY_IS_PROJECTION_WALLPAPER, true).commit();
        }
    }

    public static void initProjectWallpaperSize() {
        SharedPreferences sharedPreferences = sContext.getSharedPreferences(NAME_CUSTOM_SHARE, Context.MODE_PRIVATE);
        if (sharedPreferences.getLong(KEY_SIZE_PROJECTION_WALLPAPER, 0) == 0) {
            SharedPreferences.Editor editor2 = sharedPreferences.edit();
            WallpaperManager wallpaperManager = WallpaperManager.getInstance(sContext);
            ParcelFileDescriptor parcelFileDescriptor = null;
            long size = 0;
            try {
                parcelFileDescriptor = wallpaperManager.getWallpaperFile(WallpaperManager.FLAG_SYSTEM);
                if (parcelFileDescriptor != null) {
                    size = parcelFileDescriptor.getStatSize();
                    parcelFileDescriptor.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            editor2.putLong(KEY_SIZE_PROJECTION_WALLPAPER, size).commit();
        }
    }

}
