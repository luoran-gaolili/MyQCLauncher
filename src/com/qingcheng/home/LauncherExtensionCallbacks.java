package com.qingcheng.home;

import android.animation.ObjectAnimator;
import android.app.ActivityManager;
import android.app.IWallpaperManager;
import android.app.WallpaperManager;
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.os.ParcelFileDescriptor;
import android.os.RemoteException;
import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.RenderScript;
import android.renderscript.ScriptIntrinsicBlur;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.graphics.drawable.BitmapDrawable;

import com.mediatek.launcher3.ext.LauncherLog;
import com.qingcheng.home.custom.ViewItemType;
import com.qingcheng.home.jni.ImageUtils;
import com.qingcheng.home.util.FastBlur;
import com.qingcheng.home.util.QCLog;

import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Random;


public class LauncherExtensionCallbacks implements LauncherCallbacks {
    private static final String TAG = "Market";
    private Launcher mLauncher;
    private RGKWidgetsRecyclerView mCustomContent;

    /** 0 */
    public static final int STATUS_NEW = 0;
    /** 1 */
    public static final int STATUS_DOWLOADING = 1;
    /** 2 */
    public static final int STATUS_PAUSED = 2;
    /** 3 */
    public static final int STATUS_COMPLETED = 3;
    /** 4 */
    public static final int STATUS_FAILED = 4;
    /** 5 */
    public static final int STATUS_INSTALLED = 5;
    /** 6 */
    public static final int STATUS_NEED_UPDATE = 6;
    /** 7 */
    public static final int STATUS_NEED_UNINSTALL = 7;

    private Calendar mCalendar = Calendar.getInstance();

    public LauncherExtensionCallbacks(Launcher launcher){
        mLauncher = launcher;
    }

    @Override
    public void preOnCreate() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
    }

    @Override
    public void preOnResume() {
    }

    @Override
    public void onResume() {
        updateWidgetView();
    }

    @Override
    public void onStart() {
        LauncherAppState.initProjectWallpaperSize();
        LauncherAppState.setWallpaperChanged();//first check change
    }

    @Override
    public void onStop() {
    }

    @Override
    public void onPause() {
    }

    @Override
    public void onDestroy() {
        if(mCustomContent != null){
            mCustomContent.onDestroy();
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
    }

    @Override
    public void onPostCreate(Bundle savedInstanceState) {
    }

    @Override
    public void onNewIntent(Intent intent) {
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions,
                                           int[] grantResults) {
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        return false;
    }

    @Override
    public void dump(String prefix, FileDescriptor fd, PrintWriter w, String[] args) {
    }

    @Override
    public void onHomeIntent() {
    }

    @Override
    public boolean handleBackPressed() {
        return false;
    }

    @Override
    public void onTrimMemory(int level) {
    }

    @Override
    public void onLauncherProviderChange() {
    }

    @Override
    public void finishBindingItems(boolean upgradePath) {
    }

    @Override
    public void onClickAllAppsButton(View v) {
    }

    @Override
    public void onClickFolderIcon(View v) {
    }

    @Override
    public void onClickAppShortcut(View v) {
        Object tag = v.getTag();
        final Intent intent;
        if (tag instanceof ShortcutInfo) {
            intent = ((ShortcutInfo) tag).intent;
        } else if (tag instanceof AppInfo) {
            intent = ((AppInfo) tag).intent;
        } else {
            throw new IllegalArgumentException("Input must be a Shortcut or AppInfo");
        }

        insertRecord(intent);
    }

    private void insertRecord(Intent intent) {
        if(intent == null){
            return;
        }

        ComponentName componentName = intent.getComponent();
        if(componentName == null){
            return;
        }

        ContentValues values = new ContentValues();
        values.put(LauncherSettings.APPS.PACKAGENAME, componentName.getPackageName());
        values.put(LauncherSettings.APPS.CLASSNAME, componentName.getClassName());
        values.put(LauncherSettings.APPS.CLICKTIME, ""+System.currentTimeMillis());
        values.put(LauncherSettings.APPS.DURATION, mCalendar.get(Calendar.HOUR_OF_DAY) * 60 + mCalendar.get(Calendar.MINUTE));
        mLauncher.getContentResolver().insert(LauncherSettings.APPS.APPS_URI, values);
    }

    @Override
    public void onClickPagedViewIcon(View v) {
    }

    @Override
    public void onClickWallpaperPicker(View v) {
    }

    @Override
    public void onClickSettingsButton(View v) {
    }

    @Override
    public void onClickAddWidgetButton(View v) {
    }

    @Override
    public void onPageSwitch(View newPage, int newPageIndex) {
//        if(mCustomContent != null && mCustomContentCallbacks != null){
//            mCustomContent.dismissFullScreenView(false);
//        }
    }

    @Override
    public void onWorkspaceLockedChanged() {
    }

    @Override
    public void onDragStarted(View view) {
    }

    @Override
    public void onInteractionBegin() {
    }

    @Override
    public void onInteractionEnd() {
    }

    @Override
    public boolean providesSearch() {
        return false;
    }

    @Override
    public boolean startSearch(String initialQuery, boolean selectInitialQuery,
                               Bundle appSearchData, Rect sourceBounds) {
        return false;
    }

    @Override
    public boolean startSearchFromAllApps(String query) {
        return false;
    }

    Launcher.CustomContentCallbacks mCustomContentCallbacks = new Launcher.CustomContentCallbacks() {

        // Custom content is completely shown. {@code fromResume} indicates whether this was caused
        // by a onResume or by scrolling otherwise.
        public void onShow(boolean fromResume) {
//            Log.d(TAG, "onShow: ");
//            if(mLauncher.getFakeView() != null){
//                mLauncher.getFakeView().setVisibility(View.VISIBLE);
//            }
            if(mCustomContent != null ){
                mCustomContent.initData();
            }
        }

        // Custom content is completely hidden
        public void onHide() {
//            Log.d(TAG, "onHide: ");
//            if(mCustomContent != null){
//                mCustomContent.moveToPosition(0);
//            }
//            if(mLauncher.getFakeView() != null){
//                mLauncher.getFakeView().setVisibility(View.INVISIBLE);
//            }
        }

        // Custom content scroll progress changed. From 0 (not showing) to 1 (fully showing).
        public void onScrollProgressChanged(float progress) {
//            Log.d(TAG, "onScrollProgressChanged: progress = " + progress);

        }

        // Indicates whether the user is allowed to scroll away from the custom content.
        public boolean isScrollingAllowed() {
//            Log.d(TAG, "isScrollingAllowed: ");
            return true;
        }

    };

    @Override
    public boolean hasCustomContentToLeft() {
        return true;
    }

    @Override
    public void populateCustomContentContainer() {
        mCustomContent = (RGKWidgetsRecyclerView) LayoutInflater.from(mLauncher).inflate(R.layout.widgets_recycler_view, null, false);
//        updateCustomBg();

        mLauncher.addToCustomContentPage(mCustomContent, mCustomContentCallbacks, "");
    }

    private void updateCustomBg() {
        try {
            Bitmap bitmapSrc = null;
            WallpaperManager wallpaperManager = WallpaperManager.getInstance(mLauncher);
            BitmapDrawable drawable = null;
            if (wallpaperManager.getWallpaperInfo() == null) {
                drawable = (BitmapDrawable)wallpaperManager.getDrawable();
            }
            if(drawable != null){
                bitmapSrc = drawableToBitmap(drawable);
            }
            if (bitmapSrc != null) {
//              LauncherAppState.getInstance().mWallpaperBlurBg = bitmapSrc;
//                Bitmap bitmapOut = null;
//
//                try {
//                    if (QCLog.DEBUG) {
//                        QCLog.d(TAG, "GausscianBlur and nativeToBlur()");
//                    }
//                    bitmapOut = FastBlur.doBlur(bitmapSrc, 8, false);
//                    if(bitmapOut != null){
//                        mLauncher.getFakeView().setBackground(new BitmapDrawable(bitmapOut));
//                    }
//                } catch (Exception e) {
//                    e.printStackTrace();
//                    Log.e(TAG, "populateCustomContentContainer: bitmapOut " + e.toString());
//                }
             if(mCustomContent != null ){
               mCustomContent.updateNewsLayout();
             }

          } 
        } catch (Exception e) {
            Log.e(TAG, "populateCustomContentContainer: " + e.toString());
             e.printStackTrace();
        }
    }

    /**
     *
     * @param
     * @return
     */
    public static Bitmap drawableToBitmap(Drawable drawable) {
        Bitmap bitmap;

        if (drawable instanceof BitmapDrawable) {
            BitmapDrawable bitmapDrawable = (BitmapDrawable) drawable;
            if (bitmapDrawable.getBitmap() != null) {

                Bitmap bkgBitmap = bitmapDrawable.getBitmap();

                Bitmap positionBkgBitmap = null;

                int x = 0;
                int y =0;
                int width = Math.min(bkgBitmap.getWidth(), (int)LauncherApplication.getScreenWidthPixel());
                int height = bkgBitmap.getHeight();


                try {
                    positionBkgBitmap = Bitmap.createBitmap(bkgBitmap, x, y,width,height).copy(Bitmap.Config.ARGB_8888, true);
                } catch (Exception e) {
                    e.printStackTrace();
                    positionBkgBitmap = bkgBitmap;
                }


                int smallWidth = (int)(720*(0.2f));
                int smallHeight = (int)((smallWidth*positionBkgBitmap.getHeight())/positionBkgBitmap.getWidth());

                Bitmap smallBitmap = null;
                if (smallWidth < positionBkgBitmap.getWidth()) {
                    smallBitmap = Bitmap.createScaledBitmap(positionBkgBitmap, smallWidth, smallHeight, true);
                } else {
                    smallBitmap = positionBkgBitmap;
                }
                //int progress = 1;
                float contrast = 0.4f;//(float) ((progress + 30) / 128.0);
                ColorMatrix cMatrix = new ColorMatrix();
                cMatrix.set(new float[] { contrast, 0, 0, 0, 0, 0,
                        contrast, 0, 0, 0,
                        0, 0, contrast, 0, 0, 0, 0, 0, 1, 0 });

                Paint paint = new Paint();
                paint.setColorFilter(new ColorMatrixColorFilter(cMatrix));

                Canvas canvas = new Canvas(smallBitmap);
                canvas.drawBitmap(smallBitmap, 0, 0, paint);

                return smallBitmap;
            }
        }

        if (drawable.getIntrinsicWidth() <= 0 || drawable.getIntrinsicHeight() <= 0) {
            bitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888);
        } else {
            bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        }

        bitmap = Bitmap.createScaledBitmap(bitmap, bitmap.getWidth()/32, bitmap.getHeight()/32, false);
        return bitmap;
    }

    @Override
    public View getQsbBar() {
        return null;
    }

    @Override
    public Bundle getAdditionalSearchWidgetOptions() {
        return new Bundle();
    }

    @Override
    public void clickAppFromMarket(final View v) {
        AppInfoMarket appData = (AppInfoMarket) v.getTag();
        if (appData != null) {
            if (LauncherLog.DEBUG) {
                Log.i(TAG, "click app " + appData.toString());
            }

            if(!TextUtils.isEmpty(appData.packageName) && appData.intent != null){
                try{
                    appData.intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    mLauncher.startActivity(appData.intent);
                }catch (Exception e){
                    Log.e(TAG, "clickAppFromMarket: try start app failed " + e.toString() );
                }
            }
        }
    }

    @Override
    public Intent getFirstRunActivity() {
        return null;
    }

    @Override
    public boolean hasFirstRunActivity() {
        return false;
    }

    @Override
    public boolean hasDismissableIntroScreen() {
        return false;
    }

    @Override
    public View getIntroScreen() {
        return null;
    }

    @Override
    public boolean shouldMoveToDefaultScreenOnHomeIntent() {
        return true;
    }

    @Override
    public boolean hasSettings() {
        return false;
    }

    @Override
    public boolean overrideWallpaperDimensions() {
        return false;
    }

    @Override
    public int getSearchBarHeight() {
        return SEARCH_BAR_HEIGHT_NORMAL;
    }

    @Override
    public boolean isLauncherPreinstalled() {
        return false;
    }

    @Override
    public void setLauncherSearchCallback(Object callbacks) {
        // Do nothing
    }

    @Override
    public void bindComponentsRemoved(ArrayList<AppInfo> appInfos) {
        try {
            deleteAppsRecord(appInfos);
            mCustomContent.removeData(appInfos);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void deleteAppsRecord(ArrayList<AppInfo> appInfos) {
        if(appInfos == null || appInfos.size() <1){
            return;
        }
        for (int i = 0; i < appInfos.size(); i++) {
            AppInfo info = appInfos.get(i);
            if(info != null && info.intent != null && info.intent.getComponent() != null){
                mLauncher.getContentResolver().delete(LauncherSettings.APPS.APPS_URI, LauncherSettings.APPS.PACKAGENAME + " = ? AND " + LauncherSettings.APPS.CLASSNAME + " = ?"
                        , new String[]{info.intent.getComponent().getPackageName(),info.intent.getComponent().getClassName()});
            }
        }
    }

    @Override
    public void bindAppsUpdated(ArrayList<AppInfo> apps) {
        Log.d(TAG, "bindAppsUpdated: ");
        try{
        if (mCustomContent != null && mCustomContentCallbacks != null) {
            mCustomContent.updateData(apps);
        }
        }catch (Exception e){
            Log.e(TAG, "bindAppsUpdated: " + e.toString());
        }
    }

    @Override
    public boolean onBackPressed() {
        boolean result = false;
//        if(mCustomContent != null && mCustomContentCallbacks != null){
//            if(mCustomContent.isFullScreenViewVisible()){
//                mCustomContent.dismissFullScreenView(true);
//                result = true;
//            }
//        }
        return result;
    }

    @Override
    public void updateNewsLayout() {
        if(mCustomContent != null && mCustomContentCallbacks != null){
            mCustomContent.updateNewsLayout();
        }
    }

    @Override
    public void onUpdateCustomBlur() {
  //      if(mCustomContent != null){
   //         updateCustomBg();
  //      }
        checkChangeStyle();
    }

    private void checkChangeStyle() {
        if (mCustomContent != null) {
            mCustomContent.updateNewsLayout();
            Log.d(TAG, "update custom style");
        }
    }

    @Override
    public void bindAllApplications(ArrayList<AppInfo> apps) {
        if(mCustomContent != null && mCustomContentCallbacks != null){
           
            mCustomContent.initData();
        }
    }

    public boolean isFullScreen(){
        boolean result = false;
//        if(mCustomContent != null && mCustomContentCallbacks != null){
//            result =  mCustomContent.isFullScreenViewVisible();
//        }
        return result;
    }

    public boolean isLastNewsTab(){
        boolean result = false;
//        if(mCustomContent != null && mCustomContentCallbacks != null){
//            result =  mCustomContent.isLastNewsTab();
//        }
        return result;
    }

    @Override
    public void onAttachedToWindow() {
    }

    @Override
    public void onDetachedFromWindow() {
    }

    public boolean interceptTouchEvent(MotionEvent ev) {
//        if(mCustomContent != null && mCustomContentCallbacks != null){
//            return mCustomContent.interceptTouchEvent(ev);
//        }
        return false;
    }

    private void updateWidgetView() {
        Log.i("LauncherExtensionCallbacks","updateWidgetView");
        if(mCustomContent != null && mCustomContentCallbacks != null) {
            boolean foundIntelCards = false;
            ActivityManager am = (ActivityManager) mLauncher
                    .getSystemService(Context.ACTIVITY_SERVICE);
            List<ActivityManager.RunningServiceInfo> runningServices = am
                    .getRunningServices(Integer.MAX_VALUE);
            for (ActivityManager.RunningServiceInfo service : runningServices) {

                String packageName = service.service.getPackageName();
                String className = service.service.getClassName();
                if (ViewItemType.INTELCARDS_WIDGET_CLASS_NAME.equals(packageName)) {
                    foundIntelCards = true;
                }

            }
            if (foundIntelCards) {
                Log.i("LauncherExtensionCallbacks","updateWidget");
                mCustomContent.isIntelcardsShow();
            } else {
                mCustomContent.removeViewItem(ViewItemType.VIEW_ITEM_INTELCARDS_WIDGET);
            }
            mCustomContent.updateAllWidgets();
        }
    }
}

