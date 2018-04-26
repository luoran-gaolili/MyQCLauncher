/*
* Copyright (C) 2014 MediaTek Inc.
* Modification based on code covered by the mentioned copyright
* and/or permission notice(s).
*/
/*
 * Copyright (C) 2008 The Android Open Source Project
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

import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;

import android.view.View;
import android.widget.ImageView;
import java.util.ArrayList;
import java.util.Map;
import com.qingcheng.home.R;

import com.qingcheng.home.ThemeInfo;
import com.qingcheng.home.compat.LauncherActivityInfoCompat;
import com.qingcheng.home.compat.LauncherAppsCompat;
import com.qingcheng.home.compat.UserHandleCompat;
import com.qingcheng.home.compat.UserManagerCompat;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map.Entry;

import com.mediatek.launcher3.ext.LauncherLog;
import com.qingcheng.home.config.QCConfig;
import com.qingcheng.home.database.QCPreference;
import com.qingcheng.home.util.QCLog;

/**
 * Cache of application icons.  Icons can be made from any thread.
 */
public class IconCache {

    private static final String TAG = "Launcher.IconCache";

    private static final int INITIAL_ICON_CACHE_CAPACITY = 50;
    private static final String RESOURCE_FILE_PREFIX = "icon_";

    // Empty class name is used for storing package default entry.
    private static final String EMPTY_CLASS_NAME = ".";

    private static final boolean DEBUG = false;

    private static class CacheEntry {
        public Bitmap icon;
        public CharSequence title;
        public CharSequence contentDescription;
    }

    private static class CacheKey {
        public ComponentName componentName;
        public UserHandleCompat user;

        CacheKey(ComponentName componentName, UserHandleCompat user) {
            this.componentName = componentName;
            this.user = user;
        }

        @Override
        public int hashCode() {
            return componentName.hashCode() + user.hashCode();
        }

        @Override
        public boolean equals(Object o) {
            CacheKey other = (CacheKey) o;
            return other.componentName.equals(componentName) && other.user.equals(user);
        }
    }

    private final HashMap<UserHandleCompat, Bitmap> mDefaultIcons =
            new HashMap<UserHandleCompat, Bitmap>();
    private final Context mContext;
    private final PackageManager mPackageManager;
    private final UserManagerCompat mUserManager;
    private final LauncherAppsCompat mLauncherApps;
    private final HashMap<CacheKey, CacheEntry> mCache =
            new HashMap<CacheKey, CacheEntry>(INITIAL_ICON_CACHE_CAPACITY);
    private int mIconDpi;
    private final ThemeInfo mThemeInfo;

    public IconCache(Context context) {
        ActivityManager activityManager =
                (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);

        mContext = context;
        mPackageManager = context.getPackageManager();
        mUserManager = UserManagerCompat.getInstance(mContext);
        mLauncherApps = LauncherAppsCompat.getInstance(mContext);
//        mIconDpi = activityManager.getLauncherLargeIconDensity();
        DisplayMetrics dm = context.getResources().getDisplayMetrics();
        mIconDpi = getLauncherIconDensity(DynamicGrid.pxFromDp(DynamicGrid.DEFAULT_ICON_SIZE_DP, dm));;
//        if (LauncherLog.DEBUG) {
//            LauncherLog.d(TAG, "IconCache, mIconDpi = " + mIconDpi);
//        }
 //       Log.d(TAG, "IconCache: mIconDpi = " + mIconDpi);

        // need to set mIconDpi before getting default icon
        UserHandleCompat myUser = UserHandleCompat.myUserHandle();
        mDefaultIcons.put(myUser, makeDefaultIcon(myUser));
        
        mThemeInfo = new ThemeInfo(context);
    }

    private int getLauncherIconDensity(int requiredSize) {
        // Densities typically defined by an app.
//        int[] densityBuckets = new int[] {
//                DisplayMetrics.DENSITY_LOW,
//                DisplayMetrics.DENSITY_MEDIUM,
//                DisplayMetrics.DENSITY_TV,
//                DisplayMetrics.DENSITY_HIGH,
//                DisplayMetrics.DENSITY_XHIGH,
//                DisplayMetrics.DENSITY_XXHIGH,
//                DisplayMetrics.DENSITY_XXXHIGH
//        };
//
        int density = DisplayMetrics.DENSITY_XXXHIGH;
//        for (int i = densityBuckets.length - 1; i >= 0; i--) {
//            float expectedSize = DynamicGrid.DEFAULT_ICON_SIZE_DP * densityBuckets[i]
//                    / DisplayMetrics.DENSITY_DEFAULT;
//            if (expectedSize >= requiredSize) {
//                density = densityBuckets[i];
//            }
//        }

        if(DisplayMetrics.DENSITY_DEVICE_STABLE == DisplayMetrics.DENSITY_XHIGH){
            density = DisplayMetrics.DENSITY_XXHIGH;
        }else if(DisplayMetrics.DENSITY_DEVICE_STABLE == DisplayMetrics.DENSITY_HIGH){
            density = DisplayMetrics.DENSITY_XHIGH;
        }
        return density;
    }

    public Drawable getFullResDefaultActivityIcon() {
        return getFullResIcon(Resources.getSystem(),
                android.R.mipmap.sym_def_app_icon);
    }

    public Drawable getFullResIcon(Resources resources, int iconId) {
        Drawable d;
        try {
            d = resources.getDrawableForDensity(iconId, mIconDpi);
        } catch (Resources.NotFoundException e) {
            d = null;
        }

        return (d != null) ? d : getFullResDefaultActivityIcon();
    }

    public Drawable getFullResIcon(String packageName, int iconId) {
        Resources resources;
        try {
            resources = mPackageManager.getResourcesForApplication(packageName);
        } catch (PackageManager.NameNotFoundException e) {
            resources = null;
        }
        if (resources != null) {
            if (iconId != 0) {
                return getFullResIcon(resources, iconId);
            }
        }
        return getFullResDefaultActivityIcon();
    }

    public int getFullResIconDpi() {
        return mIconDpi;
    }

    public Drawable getFullResIcon(ResolveInfo info) {
        return getFullResIcon(info.activityInfo);
    }

    public Drawable getFullResIcon(ActivityInfo info) {

        Resources resources;
        try {
            resources = mPackageManager.getResourcesForApplication(
                    info.applicationInfo);
        } catch (PackageManager.NameNotFoundException e) {
            resources = null;
        }
        if (resources != null) {
            int iconId = info.getIconResource();
            if (iconId != 0) {
                return getFullResIcon(resources, iconId);
            }
        }

        return getFullResDefaultActivityIcon();
    }

    private Bitmap makeDefaultIcon(UserHandleCompat user) {
        Drawable unbadged = getFullResDefaultActivityIcon();
        Drawable d = mUserManager.getBadgedDrawableForUser(unbadged, user);
        Bitmap b = Bitmap.createBitmap(Math.max(d.getIntrinsicWidth(), 1),
                Math.max(d.getIntrinsicHeight(), 1),
                Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(b);
        d.setBounds(0, 0, b.getWidth(), b.getHeight());
        d.draw(c);
        c.setBitmap(null);
        return b;
    }

    /**
     * Remove any records for the supplied ComponentName.
     */
    public void remove(ComponentName componentName, UserHandleCompat user) {
        synchronized (mCache) {
            mCache.remove(new CacheKey(componentName, user));
        }
    }

    /**
     * Remove any records for the supplied package name.
     */
    public void remove(String packageName, UserHandleCompat user) {
        HashSet<CacheKey> forDeletion = new HashSet<CacheKey>();
        for (CacheKey key: mCache.keySet()) {
            if (key.componentName.getPackageName().equals(packageName)
                    && key.user.equals(user)) {
                forDeletion.add(key);
            }
        }
        for (CacheKey condemned: forDeletion) {
            mCache.remove(condemned);
        }
    }

    /**
     * Empty out the cache.
     */
    public void flush() {
        synchronized (mCache) {
            mCache.clear();
            mThemeInfo.clear();
        }

        if (LauncherLog.DEBUG) {
            LauncherLog.d(TAG, "Flush icon cache here.");
        }
    }

    /**
     * Empty out the cache that aren't of the correct grid size
     */
    public void flushInvalidIcons(DeviceProfile grid) {
        synchronized (mCache) {
            Iterator<Entry<CacheKey, CacheEntry>> it = mCache.entrySet().iterator();
            while (it.hasNext()) {
                final CacheEntry e = it.next().getValue();
                if ((e.icon != null) && (e.icon.getWidth() < grid.iconSizePx
                        || e.icon.getHeight() < grid.iconSizePx)) {
                    it.remove();
                }
            }
        }
    }

    /**
     * Fill in "application" with the icon and label for "info."
     */
    public void getTitleAndIcon(AppInfo application, LauncherActivityInfoCompat info,
            HashMap<Object, CharSequence> labelCache) {
        synchronized (mCache) {
            CacheEntry entry = cacheLocked(application.componentName, info, labelCache,
                    info.getUser(), false);

            application.title = entry.title;
            application.iconBitmap = entry.icon;
            application.contentDescription = entry.contentDescription;
        }
    }

    public Bitmap getIcon(Intent intent, UserHandleCompat user) {
        return getIcon(intent, null, user, true);
    }

    private Bitmap getIcon(Intent intent, String title, UserHandleCompat user, boolean usePkgIcon) {
        synchronized (mCache) {
            ComponentName component = intent.getComponent();
            // null info means not installed, but if we have a component from the intent then
            // we should still look in the cache for restored app icons.
            if (component == null) {
                return getDefaultIcon(user);
            }

            LauncherActivityInfoCompat launcherActInfo = mLauncherApps.resolveActivity(intent, user);
            CacheEntry entry = cacheLocked(component, launcherActInfo, null, user, usePkgIcon);
            if (title != null) {
                entry.title = title;
                entry.contentDescription = mUserManager.getBadgedLabelForUser(title, user);
            }
            return entry.icon;
        }
    }

    /**
     * Fill in "shortcutInfo" with the icon and label for "info."
     */
    public void getTitleAndIcon(ShortcutInfo shortcutInfo, Intent intent, UserHandleCompat user,
            boolean usePkgIcon) {
        synchronized (mCache) {
            ComponentName component = intent.getComponent();
            // null info means not installed, but if we have a component from the intent then
            // we should still look in the cache for restored app icons.
            if (component == null) {
                shortcutInfo.setIcon(getDefaultIcon(user));
                shortcutInfo.title = "";
                shortcutInfo.usingFallbackIcon = true;
            } else {
                LauncherActivityInfoCompat launcherActInfo =
                        mLauncherApps.resolveActivity(intent, user);
                CacheEntry entry = cacheLocked(component, launcherActInfo, null, user, usePkgIcon);

                shortcutInfo.setIcon(entry.icon);
                shortcutInfo.title = entry.title;
                shortcutInfo.usingFallbackIcon = isDefaultIcon(entry.icon, user);
            }
        }
    }


    public Bitmap getDefaultIcon(UserHandleCompat user) {
        if (!mDefaultIcons.containsKey(user)) {
            mDefaultIcons.put(user, makeDefaultIcon(user));
        }
        return mDefaultIcons.get(user);
    }

    public Bitmap getIcon(ComponentName component, LauncherActivityInfoCompat info,
            HashMap<Object, CharSequence> labelCache) {
        synchronized (mCache) {
            if (info == null || component == null) {
                return null;
            }

            CacheEntry entry = cacheLocked(component, info, labelCache, info.getUser(), false);
            return entry.icon;
        }
    }

    public boolean isDefaultIcon(Bitmap icon, UserHandleCompat user) {
        return mDefaultIcons.get(user) == icon;
    }

    private CacheEntry cacheLocked(ComponentName componentName, LauncherActivityInfoCompat info,
            HashMap<Object, CharSequence> labelCache, UserHandleCompat user, boolean usePackageIcon) {
        if (LauncherLog.DEBUG_LAYOUT) {
            LauncherLog.d(TAG, "cacheLocked: componentName = " + componentName
                    + ", info = " + info + ", HashMap<Object, CharSequence>:size = "
                    +  ((labelCache == null) ? "null" : labelCache.size()));
        }

        CacheKey cacheKey = new CacheKey(componentName, user);
        CacheEntry entry = mCache.get(cacheKey);
        if (entry == null) {
            entry = new CacheEntry();

            mCache.put(cacheKey, entry);

            if (info != null) {
                ComponentName labelKey = info.getComponentName();
                if (labelCache != null && labelCache.containsKey(labelKey)) {
                    entry.title = labelCache.get(labelKey).toString();
                    if (LauncherLog.DEBUG_LOADERS) {
                        LauncherLog.d(TAG, "CacheLocked get title from cache: title = " + entry.title);
                    }
                } else {
                    entry.title = info.getLabel().toString();
                    if (LauncherLog.DEBUG_LOADERS) {
                        LauncherLog.d(TAG, "CacheLocked get title from pms: title = " + entry.title);
                    }
                    if (labelCache != null) {
                        labelCache.put(labelKey, entry.title);
                    }
                }

                entry.contentDescription = mUserManager.getBadgedLabelForUser(entry.title, user);
                if (!QCConfig.useThemeIcons) {
                    entry.icon = Utilities.createIconBitmap(
                            info.getBadgedIcon(mIconDpi), mContext);
				} else {
					getIconFromTheme(componentName, entry, info);
				}
            } else {
                entry.title = "";
                Bitmap preloaded = getPreloadedIcon(componentName, user);
                if (preloaded != null) {
                    if (DEBUG) Log.d(TAG, "using preloaded icon for " +
                            componentName.toShortString());
                    if (!QCConfig.useThemeIcons) {
                    	entry.icon = preloaded;
					} else {
						getIconFromTheme(componentName, entry, info);
					}
                } else {
                    if (usePackageIcon) {
                        CacheEntry packageEntry = getEntryForPackage(
                                componentName.getPackageName(), user, info);
                        if (packageEntry != null) {
                            if (DEBUG) Log.d(TAG, "using package default icon for " +
                                    componentName.toShortString());
                            if (!QCConfig.useThemeIcons) {
                            	entry.icon = packageEntry.icon;
							} else {
								getIconFromTheme(componentName, entry, info);
							}
                            entry.title = packageEntry.title;
                        }
                    }
                    if (entry.icon == null) {
                        if (DEBUG) Log.d(TAG, "using default icon for " +
                                componentName.toShortString());
                        if (!QCConfig.useThemeIcons) {
                        	entry.icon = getDefaultIcon(user);
						} else {
							getIconFromTheme(componentName, entry, info);
						}
                    }
                }
            }
        }
        return entry;
    }

    private void getIconFromTheme(ComponentName componentName, CacheEntry entry, LauncherActivityInfoCompat launcherActivityInfoCompat) {
        String iconClassName = QCPreference.THEME_ICON_PATH + componentName.getClassName() + ".png";

        // Only change core applications' icon after app's name changed
        if (componentName.getPackageName() != null && componentName.getClassName().equals("com.android.dialer.DialtactsActivity")) {
            iconClassName = QCPreference.THEME_ICON_PATH + "com.android.contacts.activities.DialtactsActivity" + ".png";
        }
//        if (componentName.getPackageName() != null && componentName.getClassName().equals("com.android.gallery3d.app.GalleryActivity")) {
//            iconClassName = QCPreference.THEME_ICON_PATH + "com.android.gallery3d.app.Gallery" + ".png";
//        }
//        if (componentName.getPackageName() != null && componentName.getPackageName().equals("com.qingcheng.theme")) {
//            iconClassName = QCPreference.THEME_ICON_PATH + "com.ragentek.home.ui.RGKThemeStoreMainActivity" + ".png";
//        }
        if (componentName.getClassName() != null && componentName.getClassName().equals(LauncherProvider.getDefaultBrowserName())) {
            iconClassName = QCPreference.THEME_ICON_PATH + "com.android.browser.BrowserActivity" + ".png";
        }
        // RGKMyUI Build Contacts App in Dialer
        if(componentName.getPackageName()!=null&&componentName.getClassName().equals("com.android.contacts.PeopleActivity")){
            iconClassName=QCPreference.THEME_ICON_PATH+"com.android.contacts.activities.PeopleActivity"+".png";
        }
        // End
        if(componentName.getPackageName() != null && componentName.getClassName().equals("com.ting.main.WelcomeActivity")){
            iconClassName=QCPreference.THEME_ICON_PATH+"com.android.music.MusicBrowserActivity"+".png";
        }

        mThemeInfo.initializeIfNeed();

        Drawable drawable = null;
        try {
            InputStream stream = null;
            if(LauncherAppState.isCustomTheme()){
                StringBuilder sb = new StringBuilder();
                sb.append(mContext.getFilesDir().getPath());
                sb.append(QCPreference.THEME_DIR);
                sb.append("/");
                sb.append(iconClassName);
                Log.e(TAG, "getIconFromTheme: custom theme icon path = " + sb.toString());
                stream = new FileInputStream(sb.toString());
            }else{
                stream = mContext.getAssets().open(iconClassName);
            }
            if (stream != null) {
                drawable = Drawable.createFromResourceStream(mContext.getResources(), null, stream, null);
                stream.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
            Log.e(TAG, "getIconFromTheme: " + e);
        }
        if (drawable != null) {
            if (LauncherAppState.getInstance().mShowCustomIconAni && componentName.getPackageName() != null && componentName.getPackageName().equals("com.android.calendar")) {
                entry.icon = BitmapFactory.decodeResource(mContext.getResources(), R.drawable.calendar_blank);
            } else if (LauncherAppState.getInstance().mShowCustomIconAni && componentName.getPackageName() != null && componentName.getPackageName().equals("com.android.deskclock")) {
                entry.icon = BitmapFactory.decodeResource(mContext.getResources(), R.drawable.clock_blank);
            } else {
                entry.icon = Utilities.createIconBitmap(drawable, mContext);
            }
        } else {
            Intent intent = new Intent();
            intent.setComponent(componentName);
            ResolveInfo info = mPackageManager.resolveActivity(intent, 0);
            try {
                if (QCLog.DEBUG) {
                    QCLog.d(TAG, "getIconFromTheme() and drawable = null, (info!=null)? " + (info != null)
                            + " (info.activityInfo!=null)? " + (info.activityInfo != null));
                }
                if (info != null && info.activityInfo != null) {
                    if (LauncherAppState.getInstance().mShowCustomIconAni && componentName.getPackageName() != null && componentName.getPackageName().equals("com.android.calendar")) {
                        entry.icon = BitmapFactory.decodeResource(mContext.getResources(), R.drawable.calendar_blank);
                    } else if (LauncherAppState.getInstance().mShowCustomIconAni && componentName.getPackageName() != null && componentName.getPackageName().equals("com.android.deskclock")) {
                        entry.icon = BitmapFactory.decodeResource(mContext.getResources(), R.drawable.clock_blank);
                    }else if (launcherActivityInfoCompat != null) {
                        entry.icon = Utilities.createIconBitmap2(launcherActivityInfoCompat.getBadgedIcon(mIconDpi), mContext, mThemeInfo);
                    } else {
                        entry.icon = Utilities.createIconBitmap(getFullResIcon(info), mContext, mThemeInfo);
                    }
                } else {
                    UserHandleCompat myUser = UserHandleCompat.myUserHandle();
                    entry.icon = getDefaultIcon(myUser);
                    if (QCLog.DEBUG) {
                        QCLog.d(TAG, "getIconFromTheme() and getDefaultIcon(), componentName = " + componentName);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                UserHandleCompat myUser = UserHandleCompat.myUserHandle();
                entry.icon = getDefaultIcon(myUser);
                if (QCLog.DEBUG) {
                    QCLog.d(TAG, "getIconFromTheme() and Exception occurred, getDefaultIcon() componentName = " + componentName);
                }
            }
        }
    }

    public Bitmap decodeFile(InputStream inputStream, int resWidth, int resHeight) {
        try {
            BitmapFactory.Options options = new BitmapFactory.Options();

            options.inJustDecodeBounds = true;

            BitmapFactory.decodeStream(inputStream, null, options);

            options.inSampleSize = calculateInSampleSize(options, resWidth, resHeight);

            options.inJustDecodeBounds = false;
            return BitmapFactory.decodeStream(inputStream, null, options);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public int calculateInSampleSize(
            BitmapFactory.Options options, int reqWidth, int reqHeight) {
        // Raw height and width of image
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {

            final int halfHeight = height / 2;
            final int halfWidth = width / 2;

            // Calculate the largest inSampleSize value that is a power of 2 and keeps both
            // height and width larger than the requested height and width.
            while ((halfHeight / inSampleSize) > reqHeight
                    && (halfWidth / inSampleSize) > reqWidth) {
                inSampleSize *= 2;
            }
        }

        return inSampleSize;
    }

    public Drawable getDrawableFromTheme(Context context, String packPm,
                                            int mPreRes) {
        Resources res = context.getResources();
        Drawable drawable = null;

        InputStream stream = null;
        try {
            String filePathInFolder = QCPreference.THEME_ICON_PATH + packPm
                    + ".png";
            if(LauncherAppState.isCustomTheme()){
                StringBuilder sb = new StringBuilder();
                sb.append(mContext.getFilesDir().getPath());
                sb.append(QCPreference.THEME_DIR);
                sb.append("/");
                sb.append(filePathInFolder);
                Log.e(TAG, "getDrawableFromTheme: custom theme icon path = " + sb.toString());
                stream = new FileInputStream(sb.toString());
            }else {
                stream = context.getAssets().open(filePathInFolder);
            }
            if (stream != null) {
//                BitmapFactory.Options opts = new BitmapFactory.Options();
//                opts.inDensity = DisplayMetrics.DENSITY_DEVICE_STABLE;
//                drawable = new BitmapDrawable(decodeFile(stream, res.getDimensionPixelSize(R.dimen.overview_panel_icon_width), res.getDimensionPixelSize(R.dimen.overview_panel_icon_width)));
//                if (drawable != null) {
//                    return drawable;
//                }
                if (stream != null) {
                    drawable = Drawable.createFromResourceStream(mContext.getResources(), null, stream, null);
//                    stream.close();
                }
                if(drawable != null){
                    return drawable;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            Log.e(TAG, "getIconFromTheme: " + e);
        } finally {
            try {
                if (stream != null) {
                    stream.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return res.getDrawable(mPreRes);
    }

//    /**
//     * Adds a default package entry in the cache. This entry is not persisted and will be removed
//     * when the cache is flushed.
//     */
//    public void cachePackageInstallInfo(String packageName, UserHandleCompat user,
//            Bitmap icon, CharSequence title) {
//        remove(packageName, user);
//
//        CacheEntry entry = getEntryForPackage(packageName, user);
//        if (!TextUtils.isEmpty(title)) {
//            entry.title = title;
//        }
//        if (icon != null && !QCConfig.useThemeIcons) {
//            entry.icon = Utilities.createIconBitmap(
//                    new BitmapDrawable(mContext.getResources(), icon), mContext);
//        } else {
//        	ApplicationInfo info;
//			try {
//				info = mPackageManager.getApplicationInfo(packageName, 0);
//	        	ComponentName componentName = new ComponentName(packageName, info.className);
//				getIconFromTheme(componentName, entry);
//			} catch (NameNotFoundException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}
//		}
//    }

    /**
     * Gets an entry for the package, which can be used as a fallback entry for various components.
     */
    private CacheEntry getEntryForPackage(String packageName, UserHandleCompat user, LauncherActivityInfoCompat launcherActivityInfoCompat) {
        ComponentName cn = getPackageComponent(packageName);
        CacheKey cacheKey = new CacheKey(cn, user);
        CacheEntry entry = mCache.get(cacheKey);
        if (entry == null) {
            entry = new CacheEntry();
            entry.title = "";
            mCache.put(cacheKey, entry);

            try {
                ApplicationInfo info = mPackageManager.getApplicationInfo(packageName, 0);
                entry.title = info.loadLabel(mPackageManager);
                if (!QCConfig.useThemeIcons) {
                	entry.icon = Utilities.createIconBitmap(info.loadIcon(mPackageManager), mContext);
				} else {
					ComponentName componentName = new ComponentName(packageName, info.className);
					getIconFromTheme(componentName, entry, launcherActivityInfoCompat);
				}
                //JL628 JLLEB-116 modify sunfeng @20150926 start:
            } catch ( Exception e) {
                //JL628 JLLEB-116 modify sunfeng @20150926 end:
                if (DEBUG) Log.d(TAG, "Application not installed " + packageName);
            }

            if (entry.icon == null && !QCConfig.useThemeIcons) {
                entry.icon = getPreloadedIcon(cn, user);
            }
        }
        return entry;
    }

    public HashMap<ComponentName,Bitmap> getAllIcons() {
        synchronized (mCache) {
            HashMap<ComponentName,Bitmap> set = new HashMap<ComponentName,Bitmap>();
            for (CacheKey ck : mCache.keySet()) {
                final CacheEntry e = mCache.get(ck);
                set.put(ck.componentName, e.icon);
            }
            return set;
        }
    }

    /**
     * Pre-load an icon into the persistent cache.
     *
     * <P>Queries for a component that does not exist in the package manager
     * will be answered by the persistent cache.
     *
     * @param context application context
     * @param componentName the icon should be returned for this component
     * @param icon the icon to be persisted
     * @param dpi the native density of the icon
     */
    public static void preloadIcon(Context context, ComponentName componentName, Bitmap icon,
            int dpi) {
        // TODO rescale to the correct native DPI
        try {
            PackageManager packageManager = context.getPackageManager();
            packageManager.getActivityIcon(componentName);
            // component is present on the system already, do nothing
            return;
        } catch (PackageManager.NameNotFoundException e) {
            // pass
        }

        final String key = componentName.flattenToString();
        FileOutputStream resourceFile = null;
        try {
            resourceFile = context.openFileOutput(getResourceFilename(componentName),
                    Context.MODE_PRIVATE);
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            if (icon.compress(android.graphics.Bitmap.CompressFormat.PNG, 75, os)) {
                byte[] buffer = os.toByteArray();
                resourceFile.write(buffer, 0, buffer.length);
            } else {
                Log.w(TAG, "failed to encode cache for " + key);
                return;
            }
        } catch (FileNotFoundException e) {
            Log.w(TAG, "failed to pre-load cache for " + key, e);
        } catch (IOException e) {
            Log.w(TAG, "failed to pre-load cache for " + key, e);
        } finally {
            if (resourceFile != null) {
                try {
                    resourceFile.close();
                } catch (IOException e) {
                    Log.d(TAG, "failed to save restored icon for: " + key, e);
                }
            }
        }
    }

    /**
     * Read a pre-loaded icon from the persistent icon cache.
     *
     * @param componentName the component that should own the icon
     * @returns a bitmap if one is cached, or null.
     */
    private Bitmap getPreloadedIcon(ComponentName componentName, UserHandleCompat user) {
        final String key = componentName.flattenToShortString();

        // We don't keep icons for other profiles in persistent cache.
        if (!user.equals(UserHandleCompat.myUserHandle())) {
            return null;
        }

        if (DEBUG) Log.v(TAG, "looking for pre-load icon for " + key);
        Bitmap icon = null;
        FileInputStream resourceFile = null;
        try {
            resourceFile = mContext.openFileInput(getResourceFilename(componentName));
            byte[] buffer = new byte[1024];
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            int bytesRead = 0;
            while(bytesRead >= 0) {
                bytes.write(buffer, 0, bytesRead);
                bytesRead = resourceFile.read(buffer, 0, buffer.length);
            }
            if (DEBUG) Log.d(TAG, "read " + bytes.size());
            icon = BitmapFactory.decodeByteArray(bytes.toByteArray(), 0, bytes.size());
            if (icon == null) {
                Log.w(TAG, "failed to decode pre-load icon for " + key);
            }
        } catch (FileNotFoundException e) {
            if (DEBUG) Log.d(TAG, "there is no restored icon for: " + key);
        } catch (IOException e) {
            Log.w(TAG, "failed to read pre-load icon for: " + key, e);
        } finally {
            if(resourceFile != null) {
                try {
                    resourceFile.close();
                } catch (IOException e) {
                    Log.d(TAG, "failed to manage pre-load icon file: " + key, e);
                }
            }
        }

        if (icon != null) {
            // TODO: handle alpha mask in the view layer
            Bitmap b = Bitmap.createBitmap(Math.max(icon.getWidth(), 1),
                    Math.max(icon.getHeight(), 1),
                    Bitmap.Config.ARGB_8888);
            Canvas c = new Canvas(b);
            Paint paint = new Paint();
            paint.setAlpha(127);
            c.drawBitmap(icon, 0, 0, paint);
            c.setBitmap(null);
            icon.recycle();
            icon = b;
        }

        return icon;
    }

    /**
     * Remove a pre-loaded icon from the persistent icon cache.
     *
     * @param componentName the component that should own the icon
     * @returns true on success
     */
    public boolean deletePreloadedIcon(ComponentName componentName, UserHandleCompat user) {
        // We don't keep icons for other profiles in persistent cache.
        if (!user.equals(UserHandleCompat.myUserHandle())) {
            return false;
        }
        if (componentName == null) {
            return false;
        }
        if (mCache.remove(componentName) != null) {
            if (DEBUG) Log.d(TAG, "removed pre-loaded icon from the in-memory cache");
        }
        boolean success = mContext.deleteFile(getResourceFilename(componentName));
        if (DEBUG && success) Log.d(TAG, "removed pre-loaded icon from persistent cache");

        return success;
    }

    private static String getResourceFilename(ComponentName component) {
        String resourceName = component.flattenToShortString();
        String filename = resourceName.replace(File.separatorChar, '_');
        return RESOURCE_FILE_PREFIX + filename;
    }

    static ComponentName getPackageComponent(String packageName) {
        return new ComponentName(packageName, EMPTY_CLASS_NAME);
    }

    public ThemeInfo getTheme(){
        synchronized (mCache) {
            mThemeInfo.initializeIfNeed();
            return mThemeInfo;
        }
    }
    
    ArrayList<View> mSystemIcon =new ArrayList<View>();
    HashMap<String, View> hasMap =new HashMap<String, View>();
    public static final String THEME_PKGNAME = "com.qingcheng.theme";
    public static final String THEME_CLASSNAME ="com.qingcheng.theme.ui.ThemeStoreMainActivity";
    public static final String FILEMANAGER_PKGNAME ="com.mediatek2.filemanager";
    public static final String FILEMANAGER_CLASSNAME ="com.mediatek2.filemanager.FileManagerOperationActivity";
    public static final String GALLERY_PKGNAME ="com.android.gallery3d";
    public static final String GALLERY_CLASSNAME ="com.android.gallery3d.app.GalleryActivity";
    
    private int getDefaultIconRes(String packPm){
		if (packPm.equals(THEME_CLASSNAME) || packPm == THEME_CLASSNAME) {
			return R.drawable.online_wallpaper;
		} else if (packPm.equals(FILEMANAGER_CLASSNAME) || packPm == FILEMANAGER_CLASSNAME) {
			return R.drawable.filemanger_wallpaper;
		} else if (packPm.equals(GALLERY_CLASSNAME) || packPm == GALLERY_CLASSNAME) {
			return R.drawable.gallery_wallpaper;
		}
		return 0;
    }
    
    
}
