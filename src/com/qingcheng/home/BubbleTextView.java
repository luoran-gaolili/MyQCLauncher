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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.content.res.Resources.Theme;
import android.database.ContentObserver;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Parcelable;
import android.os.Handler;
import android.text.TextPaint;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.SparseArray;
import android.util.TypedValue;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.ViewConfiguration;
import android.widget.TextView;

import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

import com.mediatek.launcher3.ext.LauncherExtPlugin;
import com.mediatek.launcher3.ext.LauncherLog;

import java.text.SimpleDateFormat;
import java.util.Calendar;

/**
 * TextView that draws a bubble behind the text. We cannot use a LineBackgroundSpan
 * because we want to make the bubble taller than the text and TextView's clip is
 * too aggressive.
 */
public class BubbleTextView extends TextView {
    private static final String TAG = "BubbleTextView";
    private static SparseArray<Theme> sPreloaderThemes = new SparseArray<>(2);

    private static final float SHADOW_LARGE_RADIUS = 4.0f;
    private static final float SHADOW_SMALL_RADIUS = 1.75f;
    private static final float SHADOW_Y_OFFSET = 2.0f;
    private static final int SHADOW_LARGE_COLOUR = 0xDD000000;
    private static final int SHADOW_SMALL_COLOUR = 0xCC000000;
    static final float PADDING_V = 3.0f;

    private HolographicOutlineHelper mOutlineHelper;
    private Bitmap mPressedBackground;

    //added by zhenglei
    private ItemInfo shortcutInfo;
    private boolean mAttached = false;
    private boolean misclock = false;
    //private boolean mIsweather = false;
    //private ContentObserver weatherSearchObserver;
    private TimeReceiver mTimeReceiver = null;
    public static int[] dateNumber = new int[]{R.drawable.date0, R.drawable.date1, R.drawable.date2, R.drawable.date3, R.drawable.date4, R.drawable.date5,
            R.drawable.date6, R.drawable.date7, R.drawable.date8, R.drawable.date9,};
    /*add for weather*/
    public static int[] wenduNumber = new int[]{R.drawable.wendu0, R.drawable.wendu1, R.drawable.wendu2, R.drawable.wendu3, R.drawable.wendu4, R.drawable.wendu5,
            R.drawable.wendu6, R.drawable.wendu7, R.drawable.wendu8, R.drawable.wendu9,};
    public static int[] tianqiDrawble = new int[]{R.drawable.tianqi0, R.drawable.tianqi1, R.drawable.tianqi2,
            R.drawable.tianqi3, R.drawable.tianqi4, R.drawable.tianqi5, R.drawable.tianqi6, R.drawable.tianqi7,
            R.drawable.tianqi8, R.drawable.tianqi9, R.drawable.tianqi10, R.drawable.tianqi11, R.drawable.tianqi12,
            R.drawable.tianqi13, R.drawable.tianqi14, R.drawable.tianqi15, R.drawable.tianqi15, R.drawable.tianqi17,
            R.drawable.tianqi18, R.drawable.tianqi19, R.drawable.tianqi20, R.drawable.tianqi21, R.drawable.tianqi22,
            R.drawable.tianqi23, R.drawable.tianqi24, R.drawable.tianqi25, R.drawable.tianqi26, R.drawable.tianqi27,
            R.drawable.tianqi28, R.drawable.tianqi29, R.drawable.tianqi30, R.drawable.tianqi31, R.drawable.tianqi32,
            R.drawable.tianqi33, R.drawable.tianqi34, R.drawable.tianqi35, R.drawable.tianqi36, R.drawable.tianqi37,
            R.drawable.tianqi38};
    public static final String Url = "content://com.greenorange.weather/weather_data";
    public static final String SHISHICODE = "shishicode";
    public static final String SHISHIWENDU = "shishiwendu";
    private String shishiwendu = "";
    private String shishicode = "";

    private float mSlop;

    private int mTextColor;
//    private final boolean mCustomShadowsEnabled;
    private boolean mIsTextVisible;

    // TODO: Remove custom background handling code, as no instance of BubbleTextView use any
    // background.
    private boolean mBackgroundSizeChanged;
    private final Drawable mBackground;

    private boolean mStayPressed;
    private boolean mIgnorePressedStateChange;
    private CheckLongPressHelper mLongPressHelper;

    /// M: [OP09] @{
    private boolean mSupportEditAndHideApps = false;
    private boolean mDeleteButtonVisiable = false;
    private Drawable mDeleteButtonDrawable = null;
    private int mDeleteMarginleft;
    //}@

    private boolean isProgress;
    private RectF mRectF = new RectF();
    private int mProgress;

    class TimeReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if ((Intent.ACTION_TIME_TICK).equals(intent.getAction()) || Intent.ACTION_TIME_CHANGED.equals(intent.getAction())||
                    Intent.ACTION_TIMEZONE_CHANGED.equals(intent.getAction())) {
                BubbleTextView.this.invalidate();
            }
        }
    }



    public BubbleTextView(Context context) {
        this(context, null, 0);
    }

    public BubbleTextView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public BubbleTextView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
//        TypedArray a = context.obtainStyledAttributes(attrs,
//                R.styleable.BubbleTextView, defStyle, 0);
//        mCustomShadowsEnabled = a.getBoolean(R.styleable.BubbleTextView_customShadows, true);
//        a.recycle();

        /// M: [OP09]Add for edit and hide apps for .
        mSupportEditAndHideApps = LauncherExtPlugin.getInstance().getWorkspaceExt(context)
                                .supportEditAndHideApps();
        if (mSupportEditAndHideApps) {
            mDeleteButtonDrawable = context.getResources().getDrawable(
                             R.drawable.ic_launcher_delete_holo);
            mDeleteMarginleft = (int) context.getResources().getDimension(
                             R.dimen.apps_customize_delete_margin_left);
        }


//        if (mCustomShadowsEnabled) {
//            // Draw the background itself as the parent is drawn twice.
//            mBackground = getBackground();
//            setBackground(null);
//        } else {
            mBackground = null;
//        }
        init();

        bgColor =default_bg_color;
        progressColor = default_progress_color;

        initPainters();
    }

    public void onFinishInflate() {
        super.onFinishInflate();

        // Ensure we are using the right text size
        LauncherAppState app = LauncherAppState.getInstance();
        DeviceProfile grid = app.getDynamicGrid().getDeviceProfile();
        setTextSize(TypedValue.COMPLEX_UNIT_PX, grid.iconTextSizePx);

        /// M: Customize workSpace icon text for CT project.
        LauncherExtPlugin.getInstance().getWorkspaceExt(getContext())
                .customizeWorkSpaceIconText(this, grid.iconTextSizePx);
    }

    private void init() {
        mLongPressHelper = new CheckLongPressHelper(this);

        mOutlineHelper = HolographicOutlineHelper.obtain(getContext());
//        if (mCustomShadowsEnabled) {
//            setShadowLayer(SHADOW_LARGE_RADIUS, 0.0f, SHADOW_Y_OFFSET, SHADOW_LARGE_COLOUR);
//        }
    }

    public void applyFromShortcutInfo(String title, Bitmap b) {
        LauncherAppState app = LauncherAppState.getInstance();
        DeviceProfile grid = app.getDynamicGrid().getDeviceProfile();

        setCompoundDrawables(null,
                Utilities.createIconDrawable(b), null, null);
        setCompoundDrawablePadding((int) ((grid.folderIconSizePx - grid.iconSizePx) / 2f));
        setText(title);
    }

    public void applyFromShortcutInfo(ShortcutInfo info, IconCache iconCache,
                                      boolean setDefaultPadding) {
        shortcutInfo = info;
        applyFromShortcutInfo(info, iconCache, setDefaultPadding, false);
    }

    public void applyFromShortcutInfo(ShortcutInfo info, IconCache iconCache,
            boolean setDefaultPadding, boolean promiseStateChanged) {
        Bitmap b = info.getIcon(iconCache);
        LauncherAppState app = LauncherAppState.getInstance();

        FastBitmapDrawable iconDrawable = Utilities.createIconDrawable(b);
        iconDrawable.setGhostModeEnabled(info.isDisabled);

        /*if (LauncherAppState.getInstance().mShowCustomIconAni) {
            *//*if (info.intent != null && info.intent.getComponent() != null && "com.greenorange.weather".equals(info.intent.getComponent().getPackageName())) {
                mIsweather = true;
            }*//*

            if (info.intent != null && info.intent.getComponent() != null && "com.android.calendar".equals(info.intent.getComponent().getPackageName())) {
                Bitmap bm = BitmapFactory.decodeResource(getResources(), R.drawable.calendar_blank);
                FastBitmapDrawable iconDrawablecalendar = Utilities.createIconDrawable(bm);
                setCompoundDrawables(null, iconDrawablecalendar, null, null);
            } else if (info.intent != null && info.intent.getComponent() != null && "com.android.deskclock".equals(info.intent.getComponent().getPackageName())) {
                misclock = true;
                Bitmap bm = BitmapFactory.decodeResource(getResources(), R.drawable.clock_blank);
                FastBitmapDrawable iconDrawableclock = Utilities.createIconDrawable(bm);
                setCompoundDrawables(null, iconDrawableclock, null, null);
            } else {
                setCompoundDrawables(null, iconDrawable, null, null);
            }
        } else {
            setCompoundDrawables(null, iconDrawable, null, null);
        }
*/
        setCompoundDrawables(null, iconDrawable, null, null);

        if (setDefaultPadding) {
            DeviceProfile grid = app.getDynamicGrid().getDeviceProfile();
            setCompoundDrawablePadding(grid.iconDrawablePaddingPx);
        }

        /// M: Customize compound padding of workspace icon text for CT project.
        LauncherExtPlugin.getInstance().getWorkspaceExt(getContext())
                .customizeCompoundPaddingForBubbleText(this, getCompoundDrawablePadding());

        if (info.contentDescription != null) {
            setContentDescription(info.contentDescription);
        }
        setText(info.title);
        setTag(info);

        if (promiseStateChanged || info.isPromise()) {
            applyState(promiseStateChanged);
        }
    }

    private String mAppTitleFromMarket;

    public Target mTarget = new Target() {
        @Override
        public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {
            LauncherAppState app = LauncherAppState.getInstance();
            DeviceProfile grid = app.getDynamicGrid().getDeviceProfile();

            Drawable topDrawable = Utilities.createIconDrawable(bitmap);
            topDrawable.setBounds(0, 0, grid.allAppsIconSizePx, grid.allAppsIconSizePx);
            setCompoundDrawables(null, topDrawable, null, null);
            setCompoundDrawablePadding(grid.iconDrawablePaddingPx);
            setText(mAppTitleFromMarket);
        }

        @Override
        public void onBitmapFailed(Drawable errorDrawable) {
// TODO: 17-2-28  
        }

        @Override
        public void onPrepareLoad(Drawable placeHolderDrawable) {
// TODO: 17-2-28  
        }
    };

    private void setupMarketAppView(String logoUrl, String title) {
        if (!TextUtils.isEmpty(logoUrl)) {
            mAppTitleFromMarket = title;
            Picasso.with(getContext())
                    .load(logoUrl)
                    .placeholder(getResources().getDrawable(android.R.drawable.sym_def_app_icon))
                    .error(getResources().getDrawable(android.R.drawable.sym_def_app_icon))
                    .into(mTarget);
        }
    }

    public void applyFromApplicationInfo(AppInfo info) {
        LauncherAppState app = LauncherAppState.getInstance();
        DeviceProfile grid = app.getDynamicGrid().getDeviceProfile();
        shortcutInfo = info;

        if (info instanceof AppInfoMarket) {
            setupMarketAppView(((AppInfoMarket)info).logoUrl, ((AppInfoMarket)info).title.toString());
        } else {
            /*if (LauncherAppState.getInstance().mShowCustomIconAni) {
                if (info.intent != null && info.intent.getComponent() != null && "com.android.calendar".equals(info.intent.getComponent().getPackageName())) {
                    Bitmap bm = BitmapFactory.decodeResource(getResources(), R.drawable.calendar_blank);
                    FastBitmapDrawable iconDrawablecalendar = Utilities.createIconDrawable(bm);
                    setCompoundDrawables(null, iconDrawablecalendar, null, null);
                } else if (info.intent != null && info.intent.getComponent() != null && "com.android.deskclock".equals(info.intent.getComponent().getPackageName())) {
                    misclock = true;
                    Bitmap bm = BitmapFactory.decodeResource(getResources(), R.drawable.calendar_blank);
                    FastBitmapDrawable iconDrawableclock = Utilities.createIconDrawable(bm);
                    setCompoundDrawables(null, iconDrawableclock, null, null);
                } else {
                    Drawable topDrawable = Utilities.createIconDrawable(info.iconBitmap);
                    topDrawable.setBounds(0, 0, grid.allAppsIconSizePx, grid.allAppsIconSizePx);
                    setCompoundDrawables(null, topDrawable, null, null);
                }
            } else {*/
            Drawable topDrawable = Utilities.createIconDrawable(info.iconBitmap);
            topDrawable.setBounds(0, 0, grid.allAppsIconSizePx, grid.allAppsIconSizePx);
            setCompoundDrawables(null, topDrawable, null, null);
            //}
            setCompoundDrawablePadding(grid.iconDrawablePaddingPx);
            setText(info.title);
            if (info.contentDescription != null) {
                setContentDescription(info.contentDescription);
            }
        }
        setTag(info);
    }


    @Override
    protected boolean setFrame(int left, int top, int right, int bottom) {
        if (getLeft() != left || getRight() != right || getTop() != top || getBottom() != bottom) {
            mBackgroundSizeChanged = true;
        }
        return super.setFrame(left, top, right, bottom);
    }

    @Override
    protected boolean verifyDrawable(Drawable who) {
        return who == mBackground || super.verifyDrawable(who);
    }

    @Override
    public void setTag(Object tag) {
        if (tag != null) {
            //M: [OP09] Needn't check item info, because by default, app.id=-1
//            if (LauncherLog.DEBUG_EDIT) {
//                LauncherLog.d(TAG, "setTag, itemType = " +  ((ItemInfo) tag).itemType);
//            }

            LauncherModel.checkItemInfo((ItemInfo) tag);
        }
        super.setTag(tag);
    }

    @Override
    public void setPressed(boolean pressed) {
        super.setPressed(pressed);

        if (!mIgnorePressedStateChange) {
            updateIconState();
        }
    }

    private void updateIconState() {
        Drawable top = getCompoundDrawables()[1];
        if (top instanceof FastBitmapDrawable) {
            ((FastBitmapDrawable) top).setPressed(isPressed() || mStayPressed);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        // Call the superclass onTouchEvent first, because sometimes it changes the state to
        // isPressed() on an ACTION_UP
        boolean result = super.onTouchEvent(event);

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                // So that the pressed outline is visible immediately on setStayPressed(),
                // we pre-create it on ACTION_DOWN (it takes a small but perceptible amount of time
                // to create it)
                if (mPressedBackground == null) {
                    mPressedBackground = mOutlineHelper.createMediumDropShadow(this);
                }

                mLongPressHelper.postCheckForLongPress();
                break;
            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP:
                // If we've touched down and up on an item, and it's still not "pressed", then
                // destroy the pressed outline
                if (!isPressed()) {
                    mPressedBackground = null;
                }

                mLongPressHelper.cancelLongPress();
                break;
            case MotionEvent.ACTION_MOVE:
                if (!Utilities.pointInView(this, event.getX(), event.getY(), mSlop)) {
                    mLongPressHelper.cancelLongPress();
                }
                break;
        }
        return result;
    }

    void setStayPressed(boolean stayPressed) {
        mStayPressed = stayPressed;
        if (!stayPressed) {
            mPressedBackground = null;
        }

        // Only show the shadow effect when persistent pressed state is set.
        if (getParent() instanceof ShortcutAndWidgetContainer) {
            CellLayout layout = (CellLayout) getParent().getParent();
            layout.setPressedIcon(this, mPressedBackground, mOutlineHelper.shadowBitmapPadding);
        }

        updateIconState();
    }

    void clearPressedBackground() {
        setPressed(false);
        setStayPressed(false);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (super.onKeyDown(keyCode, event)) {
            // Pre-create shadow so show immediately on click.
            if (mPressedBackground == null) {
                mPressedBackground = mOutlineHelper.createMediumDropShadow(this);
            }
            return true;
        }
        return false;
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        // Unlike touch events, keypress event propagate pressed state change immediately,
        // without waiting for onClickHandler to execute. Disable pressed state changes here
        // to avoid flickering.
        mIgnorePressedStateChange = true;
        boolean result = super.onKeyUp(keyCode, event);

        mPressedBackground = null;
        mIgnorePressedStateChange = false;
        updateIconState();
        return result;
    }

    @Override
    public void draw(Canvas canvas) {
        super.draw(canvas);
        if (LauncherAppState.getInstance().mShowCustomIconAni) {
            try {
//        drawProgress(canvas);
                drawCalendar(canvas);
                drawClock(canvas);
                //drawWeather(canvas);
            } catch (Exception e) {
                Log.e(TAG, "draw: " + e.toString());
            }
        }

        if(shortcutInfo != null && (shortcutInfo instanceof ShortcutInfo) ){
            drawUnreadEvent(canvas);
        }

        drawDeleteButton(canvas);

    }

    public void setProgress(int progress){
        if(progress >= 100) {
            isProgress = false;
        }else{
            isProgress = true;
        }

        mProgress = progress;
        invalidate();
    }
    public int getProgress(){
       return mProgress;
    }

    private Paint paint = new Paint();

    private int bgColor;
    private int progressColor;
    private final int default_bg_color = Color.argb(0, 0, 0, 0);
    private final int default_progress_color = Color.argb(51,0, 0, 0);
    private Paint textPaint;

    protected void initPainters() {
        textPaint = new TextPaint();
        textPaint.setColor(Color.RED);
        textPaint.setTextSize(Utilities.sp2px(getResources(), 13));
        textPaint.setAntiAlias(true);

        paint.setAntiAlias(true);
    }
    public int getBgColor() {
        return bgColor;
    }

    public int getProgressColor() {
        return progressColor;
    }


    private void drawProgress(Canvas canvas) {
        if(getTag() != null && getTag() instanceof AppInfoMarket){
            if(isProgress){ //is downloading
                Drawable[] drawables = getCompoundDrawables();
                Drawable topDrawable = drawables[1];
                if (topDrawable != null) {
                    Log.d("Market", "drawProgress: progress = " + getProgress());
                    Log.d("Market", "drawProgress: app name  = " + getText().toString());
                    Log.d("Market", "drawProgress: getTop()  = " + getTop());
                    Log.d("Market", "drawProgress: (topDrawable.getBounds().bottom - topDrawable.getBounds().top)  = " + ((topDrawable.getBounds().bottom - topDrawable.getBounds().top)));
                    int top = (topDrawable.getBounds().bottom - topDrawable.getBounds().top) + 10;
                    int left = 0;
                    int right = getRight();
                    int bottom = getBottom();
                    mRectF.set(left, top, right, bottom);
                    Log.d("Market", "drawProgress: rectF = " + mRectF);


                    float xWidth = getProgress() / (float) 100 * getWidth();

                    paint.setColor(getBgColor());
                    canvas.drawRect(mRectF, paint);

                    paint.setColor(getProgressColor());
                    canvas.drawRect(mRectF.left + xWidth, mRectF.top, mRectF.right, mRectF.bottom ,paint);
                }
            }
        }
    }

    private String getDrawText() {
        return getResources().getString(R.string.recommend_market_app);
    }

    // 判断天气的图片
    public static int judge(String code) {
        for (int i = 0; i < 39; i++) {
            if (Integer.parseInt(code.trim()) == i) {
                return tianqiDrawble[i];
            }
        }
        return -1;
    }

    private void drawWeather(Canvas canvas) {
        if(shortcutInfo == null){
            return;
        }

        if (((shortcutInfo instanceof ShortcutInfo)  && ((ShortcutInfo)shortcutInfo).intent != null && ((ShortcutInfo)shortcutInfo).intent.getComponent() != null
                && ((ShortcutInfo)shortcutInfo).intent.getComponent().getPackageName() != null
                && "com.greenorange.weather".equals(((ShortcutInfo)shortcutInfo).intent.getComponent().getPackageName()))
                ||((shortcutInfo instanceof AppInfo)  && ((AppInfo)shortcutInfo).intent != null && ((AppInfo)shortcutInfo).intent.getComponent() != null
                && ((AppInfo)shortcutInfo).intent.getComponent().getPackageName() != null
                && "com.greenorange.weather".equals(((AppInfo)shortcutInfo).intent.getComponent().getPackageName()))
                ) {
            Resources res = getContext().getResources();
            if ("".equals(shishicode) || "".equals(shishiwendu)) {
                getSearchAppDatas(getContext());
                if ("".equals(shishicode) || "".equals(shishiwendu)) {
                    return;
                }
            }

            /*画背景图*/
            /*Drawable back = res.getDrawable(judge(shishicode));
            //Drawable back = res.getDrawable(R.drawable.tianqi3);
            int back_width = back.getIntrinsicWidth();
            int back_height = back.getIntrinsicHeight();
            Rect backRect = new Rect(0, 0, back_width, back_height);
            back.setBounds(backRect);
            int back_x = this.getScrollX() + this.getWidth() / 2 - back_width / 2;
            int back_y = this.getScrollY() + this.getHeight() / 2 - back_height / 2 - 20;
            if(shortcutInfo.container == LauncherSettings.Favorites.CONTAINER_HOTSEAT){ 
                back_y = this.getScrollY() + this.getHeight() / 2 - back_height / 2;
            }
            canvas.save();
            canvas.translate(back_x, back_y);
            if (back != null) {
                back.draw(canvas);
            }
            canvas.restore();*/

            Bitmap bm = BitmapFactory.decodeResource(getResources(), judge(shishicode));
            FastBitmapDrawable iconDrawableclock = Utilities.createIconDrawable(bm);
            setCompoundDrawables(null, iconDrawableclock, null, null);

            int wendubefore = Integer.parseInt(shishiwendu);
            int shortcut_margintop = 23;
            int app_margintop = 60;
            boolean isfushu = wendubefore > 0 ? false : true;
            int wendu = Math.abs(wendubefore);
            if (wendu > 0 && wendu < 10) {
                Drawable date_left = res.getDrawable(wenduNumber[wendu]);
                int left_drawble_width = date_left.getIntrinsicWidth();
                int left_drawble_height = date_left.getIntrinsicHeight();
                Rect leftRect = new Rect(0, 0, left_drawble_width, left_drawble_height);
                date_left.setBounds(leftRect);
                int date_left_x = this.getScrollX() + this.getWidth() / 2 - left_drawble_width / 2;
                int date_left_y = this.getScrollY() + app_margintop;
                if(shortcutInfo.container == LauncherSettings.Favorites.CONTAINER_HOTSEAT || shortcutInfo instanceof AppInfo){ /*-101代表是docker上面的图标*/
                    date_left_y = this.getScrollY() +shortcut_margintop;
                }

                Drawable yuanquan = res.getDrawable(R.drawable.wendu_yuanquan);
                int yuanquan_width = yuanquan.getIntrinsicWidth();
                int yuanquan_height = yuanquan.getIntrinsicHeight();
                Rect yuanquanRect = new Rect(0, 0, yuanquan_width, yuanquan_height);
                yuanquan.setBounds(yuanquanRect);
                int yuanquan_x = this.getScrollX() + this.getWidth() / 2 + left_drawble_width / 2;
                int yuanquan_y = this.getScrollY() + app_margintop;
                if(shortcutInfo.container == LauncherSettings.Favorites.CONTAINER_HOTSEAT || shortcutInfo instanceof AppInfo){ /*-101代表是docker上面的图标*/
                    yuanquan_y = this.getScrollY() +shortcut_margintop;
                }
                
                if (isfushu) {
                    Drawable fushu = res.getDrawable(R.drawable.wendu_fushu);
                    int fushu_width = fushu.getIntrinsicWidth();
                    int fushu_height = fushu.getIntrinsicHeight();
                    Rect fushuRect = new Rect(0, 0, fushu_width, fushu_height);
                    fushu.setBounds(fushuRect);
                    int fushu_x = this.getScrollX() + this.getWidth() / 2 - left_drawble_width / 2 - fushu_width;
                    int fushu_y = this.getScrollY() + app_margintop;
                    if(shortcutInfo.container == LauncherSettings.Favorites.CONTAINER_HOTSEAT || shortcutInfo instanceof AppInfo){ /*-101代表是docker上面的图标*/
                        fushu_y = this.getScrollY() +shortcut_margintop;
                    }
                    
                    
                    canvas.save();
                    canvas.translate(fushu_x, fushu_y);
                    if (fushu != null) {
                        fushu.draw(canvas);
                    }
                    canvas.restore();
                }

                canvas.save();
                canvas.translate(date_left_x, date_left_y);
                if (date_left != null) {
                    date_left.draw(canvas);
                }
                canvas.restore();

                canvas.save();
                canvas.translate(yuanquan_x, yuanquan_y);
                if (yuanquan != null) {
                    yuanquan.draw(canvas);
                }
                canvas.restore();

            } else if (wendu >= 10) {
                int mdateleft = Integer.parseInt(String.valueOf(shishiwendu.charAt(0)));
                int mdateright = Integer.parseInt(String.valueOf(shishiwendu.charAt(1)));
                Drawable date_left = res.getDrawable(wenduNumber[mdateleft]);
                int left_drawble_width = date_left.getIntrinsicWidth();
                int left_drawble_height = date_left.getIntrinsicHeight();
                Rect leftRect = new Rect(0, 0, left_drawble_width, left_drawble_height);
                date_left.setBounds(leftRect);
                int date_left_x = this.getScrollX() + this.getWidth() / 2 - left_drawble_width;
                int date_left_y = this.getScrollY() + app_margintop;
                if(shortcutInfo.container == LauncherSettings.Favorites.CONTAINER_HOTSEAT || shortcutInfo instanceof AppInfo){ /*-101代表是docker上面的图标*/
                    date_left_y = this.getScrollY() +shortcut_margintop;
                }

                Drawable date_right = res.getDrawable(wenduNumber[mdateright]);
                int right_drawble_width = date_right.getIntrinsicWidth();
                int right_drawble_height = date_right.getIntrinsicHeight();
                Rect rightRect = new Rect(0, 0, right_drawble_width, right_drawble_height);
                date_right.setBounds(rightRect);
                int date_right_x = this.getScrollX() + this.getWidth() / 2;
                int date_right_y = this.getScrollY() + app_margintop;
                if(shortcutInfo.container == LauncherSettings.Favorites.CONTAINER_HOTSEAT || shortcutInfo instanceof AppInfo){ /*-101代表是docker上面的图标*/
                    date_right_y = this.getScrollY() +shortcut_margintop;
                }

                Drawable yuanquan = res.getDrawable(R.drawable.wendu_yuanquan);
                int yuanquan_width = yuanquan.getIntrinsicWidth();
                int yuanquan_height = yuanquan.getIntrinsicHeight();
                Rect yuanquanRect = new Rect(0, 0, yuanquan_width, yuanquan_height);
                yuanquan.setBounds(yuanquanRect);
                int yuanquan_x = this.getScrollX() + this.getWidth() / 2 + left_drawble_width;
                int yuanquan_y = this.getScrollY() + app_margintop;
                if(shortcutInfo.container == LauncherSettings.Favorites.CONTAINER_HOTSEAT || shortcutInfo instanceof AppInfo){ /*-101代表是docker上面的图标*/
                    yuanquan_y = this.getScrollY() +shortcut_margintop;
                }

                if (isfushu) {
                    Drawable fushu = res.getDrawable(R.drawable.wendu_fushu);
                    int fushu_width = fushu.getIntrinsicWidth();
                    int fushu_height = fushu.getIntrinsicHeight();
                    Rect fushuRect = new Rect(0, 0, fushu_width, fushu_height);
                    fushu.setBounds(fushuRect);
                    int fushu_x = this.getScrollX() + this.getWidth() / 2 - left_drawble_width - fushu_width;
                    int fushu_y = this.getScrollY() + app_margintop;
                    if(shortcutInfo.container == LauncherSettings.Favorites.CONTAINER_HOTSEAT || shortcutInfo instanceof AppInfo){ /*-101代表是docker上面的图标*/
                        fushu_y = this.getScrollY() +shortcut_margintop;
                    }

                    canvas.save();
                    canvas.translate(fushu_x, fushu_y);
                    if (fushu != null) {
                        fushu.draw(canvas);
                    }
                    canvas.restore();
                }

                canvas.save();
                canvas.translate(date_left_x, date_left_y);
                if (date_left != null) {
                    date_left.draw(canvas);
                }
                canvas.restore();

                canvas.save();
                canvas.translate(date_right_x, date_right_y);
                if (date_right != null) {
                    date_right.draw(canvas);
                }
                canvas.restore();

                canvas.save();
                canvas.translate(yuanquan_x, yuanquan_y);
                if (yuanquan != null) {
                    yuanquan.draw(canvas);
                }
                canvas.restore();
            }
        }
    }

    /*获取天气缓存的数据*/
    public void getSearchAppDatas(Context context) {
        Uri lsappdatas = Uri.parse(Url);
        Cursor c = context.getContentResolver().query(lsappdatas, null, null, null, null);
        try {
            if (c == null || !c.moveToFirst()) {
                if (c != null) {
                    c.close();
                }
            } else {
                shishiwendu = c.getString(c.getColumnIndex(SHISHIWENDU));
                shishicode = c.getString(c.getColumnIndex(SHISHICODE));
                if(TextUtils.isEmpty(shishicode)){
                    shishicode = "";
                }
                if(TextUtils.isEmpty(shishiwendu)){
                    shishiwendu = "";
                }
                Log.e(TAG, "getSearchAppDatas: folder icon shishicode = " + shishicode + " shishiwendu = " + shishiwendu);
                c.close();
            }
        } catch (Exception e) {
            if (c != null) {
                c.close();
            }
            e.printStackTrace();
        }
    }

    private void drawClock(Canvas canvas) {
        if(shortcutInfo == null){
            return;
        }

        if (((shortcutInfo instanceof ShortcutInfo)  && ((ShortcutInfo)shortcutInfo).intent != null && ((ShortcutInfo)shortcutInfo).intent.getComponent() != null
                && ((ShortcutInfo)shortcutInfo).intent.getComponent().getPackageName() != null
                && "com.android.deskclock".equals(((ShortcutInfo)shortcutInfo).intent.getComponent().getPackageName()))
                ||((shortcutInfo instanceof AppInfo)  && ((AppInfo)shortcutInfo).intent != null && ((AppInfo)shortcutInfo).intent.getComponent() != null
                && ((AppInfo)shortcutInfo).intent.getComponent().getPackageName() != null
                && "com.android.deskclock".equals(((AppInfo)shortcutInfo).intent.getComponent().getPackageName()))
                ) {

            /*Resources res = getContext().getResources();
            Drawable clockhour = res.getDrawable(R.drawable.clock_hour);
            Bitmap bmp = BitmapFactory.decodeResource(res, R.drawable.clock_hour);
            int BgWidth = clockhour.getIntrinsicWidth();
            int BgHeight = clockhour.getIntrinsicHeight();

            Calendar c = Calendar.getInstance();
            float milliSecond = c.get(Calendar.MILLISECOND);
            float second = c.get(Calendar.SECOND) + milliSecond / 1000;
            float minute = c.get(Calendar.MINUTE) + second / 60;
            float hour = c.get(Calendar.HOUR) + minute / 60;
            //计算分针旋转角度
            int hourdegree = (int)(360 * (hour / 12));
            int minutedegree = (int)(360 * (minute / 60));

            Rect BgBounds = new Rect(0, 0, BgWidth, BgHeight);
            clockhour.setBounds(BgBounds);
            int BgPosX = this.getScrollX() + this.getWidth() / 2 - BgWidth / 2;
            int BgPosY;
            if(shortcutInfo.container == -101 || shortcutInfo instanceof AppInfo){
                BgPosY = this.getScrollY() + this.getHeight() / 2 ;
            }else{
                BgPosY = this.getScrollY() + this.getHeight() / 2 - 30 ;
            }
            canvas.save();
            canvas.translate(BgPosX, BgPosY);
            //canvas.rotate(hourdegree);
            //canvas.translate(BgPosX, BgPosY);
            if (clockhour != null) {
                clockhour.draw(canvas);
            }
            canvas.translate(canvas.getWidth()/2, canvas.getHeight()/2);
            canvas.rotate(hourdegree);
            canvas.translate(canvas.getWidth()/2, canvas.getHeight()/2);
            clockhour.draw(canvas);
            //canvas.drawBitmap(bmp,BgPosX,BgPosX,new Paint());
            canvas.restore();*/

            Resources res = getContext().getResources();
            Drawable yuandianDrawable = res.getDrawable(R.drawable.clock_point);
            int BgWidth = yuandianDrawable.getIntrinsicWidth();
            int BgHeight = yuandianDrawable.getIntrinsicHeight();

            Rect BgBounds = new Rect(0, 0, BgWidth, BgHeight);
            yuandianDrawable.setBounds(BgBounds);
            int BgPosX = this.getScrollX() + this.getWidth() / 2 - BgWidth / 2;
            int BgPosY;
            if(shortcutInfo.container == -101 || shortcutInfo instanceof AppInfo){
                BgPosY = this.getScrollY() + this.getHeight() / 2 -3;
            }else{
                BgPosY = this.getScrollY() + this.getHeight() / 2 - 33;
            }

            canvas.save();
            canvas.translate(BgPosX, BgPosY);
            if (yuandianDrawable != null) {
                yuandianDrawable.draw(canvas);
            }
            canvas.restore();

            int minuteBgPosX = this.getScrollX() + this.getWidth() / 2;
            int minuteBgPosY;
            if(shortcutInfo.container == -101){
                minuteBgPosY = this.getScrollY() + this.getHeight() / 2 + BgHeight / 2 -3;
            }else{
                minuteBgPosY = this.getScrollY() + this.getHeight() / 2 - 33 + BgHeight / 2;
            }

            Calendar c = Calendar.getInstance();
            float milliSecond = c.get(Calendar.MILLISECOND);
            float second = c.get(Calendar.SECOND) + milliSecond / 1000;
            float minute = c.get(Calendar.MINUTE) + second / 60;
            float hour = c.get(Calendar.HOUR) + minute / 60;
            //*计算分针旋转角度*//*
            int minutedegree = (int)(360 * (minute / 60));
            float minutemovex = 0;
            float minutemovey = 0;
            int minutelength = 40;
            if (minutedegree <= 90) {
                minutemovex = (float) (minutelength * Math.cos(Math.toRadians(90 - minutedegree)));
                minutemovey = (float) (minutelength * Math.sin(Math.toRadians(90 - minutedegree)));
            } else if (minutedegree > 90 && minutedegree <= 180) {
                minutemovex = (float) (minutelength * Math.cos(Math.toRadians(90 - (180 - minutedegree))));
                minutemovey = -(float) (minutelength * Math.sin(Math.toRadians(90 - (180 - minutedegree))));
            }else if(minutedegree > 180 && minutedegree <= 270){
                minutemovex = -(float) (minutelength * Math.cos(Math.toRadians(270 - minutedegree)));
                minutemovey = -(float) (minutelength * Math.sin(Math.toRadians(270 - minutedegree)));
            }else{
                minutemovex = -(float) (minutelength * Math.cos(Math.toRadians(90 - (360 - minutedegree))));
                minutemovey = (float) (minutelength * Math.sin(Math.toRadians(90 - (360 - minutedegree))));
            }

             //*计算时针旋转角度*//*
            int hourdegree = (int)(360 * (hour / 12));
            float hourmovex = 0;
            float hourmovey = 0;
            int hourlength = 27;
            if (hourdegree <= 90) {
                hourmovex = (float) (hourlength * Math.cos(Math.toRadians(90 - hourdegree)));
                hourmovey = (float) (hourlength * Math.sin(Math.toRadians(90 - hourdegree)));
            } else if (hourdegree > 90 && hourdegree <= 180) {
                hourmovex = (float) (hourlength * Math.cos(Math.toRadians(90 - (180 - hourdegree))));
                hourmovey = -(float) (hourlength * Math.sin(Math.toRadians(90 - (180 - hourdegree))));
            }else if(hourdegree > 180 && hourdegree <= 270){
                hourmovex = -(float) (hourlength * Math.cos(Math.toRadians((270 - hourdegree))));
                hourmovey = -(float) (hourlength * Math.sin(Math.toRadians((270 - hourdegree))));
            }else{
                hourmovex = -(float) (hourlength * Math.cos(Math.toRadians(90 - (360 - hourdegree))));
                hourmovey = (float) (hourlength * Math.sin(Math.toRadians(90 - (360 - hourdegree))));
            }

            canvas.save();
            Paint p = new Paint();
            p.setStrokeWidth(5);
            p.setColor(res.getColor(R.color.clock_minute));
            p.setAntiAlias(true);
            canvas.drawLine(minuteBgPosX, minuteBgPosY, minuteBgPosX + minutemovex, minuteBgPosY - minutemovey, p);
            canvas.restore();

            canvas.save();
            p.setColor(res.getColor(R.color.clock_hour));
            canvas.drawLine(minuteBgPosX, minuteBgPosY, minuteBgPosX + hourmovex, minuteBgPosY - hourmovey, p);
            canvas.restore();
        }
    }

    /*画日历的图标*/
    private void drawCalendar(Canvas canvas) {
        if(shortcutInfo == null){
            return;
        }

        if (((shortcutInfo instanceof ShortcutInfo)  && ((ShortcutInfo)shortcutInfo).intent != null && ((ShortcutInfo)shortcutInfo).intent.getComponent() != null
                && ((ShortcutInfo)shortcutInfo).intent.getComponent().getPackageName() != null
                && "com.android.calendar".equals(((ShortcutInfo)shortcutInfo).intent.getComponent().getPackageName()))
                ||((shortcutInfo instanceof AppInfo)  && ((AppInfo)shortcutInfo).intent != null && ((AppInfo)shortcutInfo).intent.getComponent() != null
                && ((AppInfo)shortcutInfo).intent.getComponent().getPackageName() != null
                && "com.android.calendar".equals(((AppInfo)shortcutInfo).intent.getComponent().getPackageName()))
                ) {
//        if (shortcutInfo != null &&  shortcutInfo.intent != null && shortcutInfo.intent.getComponent() != null
//                && "com.android.calendar".equals(shortcutInfo.intent.getComponent().getPackageName())) {
            Resources res = getContext().getResources();

            Drawable unreadBgNinePatchDrawable = res.getDrawableForDensity(R.drawable.week_monday, DisplayMetrics.DENSITY_XHIGH);
            Calendar c = Calendar.getInstance();
            String way = String.valueOf(c.get(Calendar.DAY_OF_WEEK));
            if ("1".equals(way)) {
                unreadBgNinePatchDrawable = res.getDrawableForDensity(R.drawable.week_sunday, DisplayMetrics.DENSITY_XHIGH);
            } else if ("2".equals(way)) {
                unreadBgNinePatchDrawable = res.getDrawableForDensity(R.drawable.week_monday, DisplayMetrics.DENSITY_XHIGH);
            } else if ("3".equals(way)) {
                unreadBgNinePatchDrawable = res.getDrawableForDensity(R.drawable.week_tuesday, DisplayMetrics.DENSITY_XHIGH);
            } else if ("4".equals(way)) {
                unreadBgNinePatchDrawable = res.getDrawableForDensity(R.drawable.week_wednesday, DisplayMetrics.DENSITY_XHIGH);
            } else if ("5".equals(way)) {
                unreadBgNinePatchDrawable = res.getDrawableForDensity(R.drawable.week_thursday, DisplayMetrics.DENSITY_XHIGH);
            } else if ("6".equals(way)) {
                unreadBgNinePatchDrawable = res.getDrawableForDensity(R.drawable.week_friday, DisplayMetrics.DENSITY_XHIGH);
            } else if ("7".equals(way)) {
                unreadBgNinePatchDrawable = res.getDrawableForDensity(R.drawable.week_saturday, DisplayMetrics.DENSITY_XHIGH);
            }

            int unreadBgWidth = unreadBgNinePatchDrawable.getIntrinsicWidth();
            int unreadBgHeight = unreadBgNinePatchDrawable.getIntrinsicHeight();

            Rect unreadBgBounds = new Rect(0, 0, unreadBgWidth, unreadBgHeight);
            unreadBgNinePatchDrawable.setBounds(unreadBgBounds);

            /*这边获取星期几图标具体顶部的高度*/
            int week_margin_top = 30;

            int unreadBgPosX = this.getScrollX() + this.getWidth() / 2 - unreadBgWidth / 2;
            int unreadBgPosY = this.getScrollY() + week_margin_top + 40;
            if(shortcutInfo.container == LauncherSettings.Favorites.CONTAINER_HOTSEAT || shortcutInfo instanceof AppInfo){ //*-101代表是docker上面的图标*//*
                unreadBgPosY = this.getScrollY() + week_margin_top;
            }

            /*这边画下面的日期*/
            SimpleDateFormat sDateFormat = new SimpleDateFormat("dd");
            String date = sDateFormat.format(new java.util.Date());
            int mdateleft = Integer.parseInt(String.valueOf(date.charAt(0)));
            int mdateright = Integer.parseInt(String.valueOf(date.charAt(1)));

            Drawable date_left = res.getDrawableForDensity(dateNumber[mdateleft], DisplayMetrics.DENSITY_XHIGH);
            int left_drawble_width = date_left.getIntrinsicWidth();
            int left_drawble_height = date_left.getIntrinsicHeight();
            Rect leftRect = new Rect(0, 0, left_drawble_width, left_drawble_height);
            date_left.setBounds(leftRect);
            int date_left_x = this.getScrollX() + this.getWidth() / 2 - left_drawble_width;
            int date_left_y = this.getScrollY() + unreadBgHeight * 2 + 40;
            if(shortcutInfo.container == LauncherSettings.Favorites.CONTAINER_HOTSEAT || shortcutInfo instanceof AppInfo){ /*-101代表是docker上面的图标*/
                date_left_y = this.getScrollY() + unreadBgHeight  + week_margin_top+2;
            }

            Drawable date_right = res.getDrawableForDensity(dateNumber[mdateright], DisplayMetrics.DENSITY_XHIGH);
            int right_drawble_width = date_right.getIntrinsicWidth();
            int right_drawble_height = date_right.getIntrinsicHeight();
            Rect rightRect = new Rect(0, 0, right_drawble_width, right_drawble_height);
            date_right.setBounds(rightRect);
            int date_right_x = this.getScrollX() + this.getWidth() / 2;
            int date_right_y = this.getScrollY() + unreadBgHeight * 2 + 40;
            if(shortcutInfo.container == LauncherSettings.Favorites.CONTAINER_HOTSEAT || shortcutInfo instanceof AppInfo){ /*-101代表是docker上面的图标*/
                date_right_y = this.getScrollY() + unreadBgHeight  + week_margin_top+2;
            }
            canvas.save();
            canvas.translate(unreadBgPosX, unreadBgPosY);

            if (unreadBgNinePatchDrawable != null) {
                unreadBgNinePatchDrawable.draw(canvas);
            }
            canvas.restore();

            canvas.save();
            canvas.translate(date_left_x, date_left_y);
            if (date_left != null) {
                date_left.draw(canvas);
            }
            canvas.restore();

            canvas.save();
            canvas.translate(date_right_x, date_right_y);
            if (date_right != null) {
                date_right.draw(canvas);
            }
            canvas.restore();
        }
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();

        if (!mAttached && misclock) {
            mAttached = true;
            mTimeReceiver = new TimeReceiver();
            IntentFilter filter = new IntentFilter();
            filter.addAction(Intent.ACTION_TIME_TICK);
            filter.addAction(Intent.ACTION_TIMEZONE_CHANGED);
            filter.addAction(Intent.ACTION_TIME_CHANGED);
            getContext().registerReceiver(mTimeReceiver, filter, null, getHandler());
        }
        /*注册天气的数据库observer*/
        /*if (!mAttached && mIsweather) {
            mAttached = true;
            Uri lsappdatas = Uri.parse(Url);
            weatherSearchObserver = new ContentObserver(new Handler()) {
                @Override
                public void onChange(boolean selfChange) {
                    super.onChange(selfChange);
                    getSearchAppDatas(getContext());
                }
            };
            getContext().getContentResolver().registerContentObserver(lsappdatas, true, weatherSearchObserver);
        }*/

        if (mBackground != null) mBackground.setCallback(this);
        Drawable top = getCompoundDrawables()[1];

        if (top instanceof PreloadIconDrawable) {
            ((PreloadIconDrawable) top).applyTheme(getPreloaderTheme());
        }
        mSlop = ViewConfiguration.get(getContext()).getScaledTouchSlop();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();

        if (mAttached && mTimeReceiver != null && misclock) {
            getContext().unregisterReceiver(mTimeReceiver);
            mAttached = false;
        }
         /*取消注册天气的数据库observer*/
        /*if (mAttached && mIsweather && weatherSearchObserver != null) {
            mAttached = false;
            getContext().getContentResolver().unregisterContentObserver(weatherSearchObserver);
        }*/

        if (mBackground != null) mBackground.setCallback(null);
    }

    @Override
    public void setTextColor(int color) {
        mTextColor = color;
        super.setTextColor(color);
    }

    @Override
    public void setTextColor(ColorStateList colors) {
        mTextColor = colors.getDefaultColor();
        super.setTextColor(colors);
    }

	//sunfeng modfiy @20150802 for folder start
    public void setTextVisibility(boolean visible) {
        Resources res = getResources();
        if (visible) {
            super.setTextColor(res.getColor(R.color.workspace_icon_text_color));
//            setShadowLayer(4, 0, 0, res.getColor(android.R.color.black));
        } else {
            super.setTextColor(res.getColor(android.R.color.transparent));
//            setShadowLayer(4, 0, 0, res.getColor(android.R.color.transparent));
        }
        
       /* if (visible) {
            super.setTextColor(mTextColor);
        } else {
            super.setTextColor(res.getColor(android.R.color.transparent));
        }*/
        mIsTextVisible = visible;
    }

	//sunfeng modfiy @20150802 for folder end
    public boolean isTextVisible() {
        return mIsTextVisible;
    }

    @Override
    protected boolean onSetAlpha(int alpha) {
        return true;
    }

    @Override
    public void cancelLongPress() {
        super.cancelLongPress();

        mLongPressHelper.cancelLongPress();
    }

    public void applyState(boolean promiseStateChanged) {
        if (getTag() instanceof ShortcutInfo) {
            ShortcutInfo info = (ShortcutInfo) getTag();
            final boolean isPromise = info.isPromise();
            final int progressLevel = isPromise ?
                    ((info.hasStatusFlag(ShortcutInfo.FLAG_INSTALL_SESSION_ACTIVE) ?
                            info.getInstallProgress() : 0)) : 100;

            Drawable[] drawables = getCompoundDrawables();
            Drawable top = drawables[1];
            if (top != null) {
                final PreloadIconDrawable preloadDrawable;
                if (top instanceof PreloadIconDrawable) {
                    preloadDrawable = (PreloadIconDrawable) top;
                } else {
                    preloadDrawable = new PreloadIconDrawable(top, getPreloaderTheme());
                    setCompoundDrawables(drawables[0], preloadDrawable, drawables[2], drawables[3]);
                }

                preloadDrawable.setLevel(progressLevel);
                if (promiseStateChanged) {
                    preloadDrawable.maybePerformFinishedAnimation();
                }
            }
        }
    }

    private Theme getPreloaderTheme() {
        Object tag = getTag();
        int style = ((tag != null) && (tag instanceof ShortcutInfo) &&
                (((ShortcutInfo) tag).container >= 0)) ? R.style.PreloadIcon_Folder
                        : R.style.PreloadIcon;
        Theme theme = sPreloaderThemes.get(style);
        if (theme == null) {
            theme = getResources().newTheme();
            theme.applyStyle(style, true);
            sPreloaderThemes.put(style, theme);
        }
        return theme;
    }

    
    ///: Added for MTK unread message feature.@{
    private void drawUnreadEvent(Canvas canvas){
//        LauncherLog.d(TAG, "drawUnreadEvent() this = " + this);
        MTKUnreadLoader.drawUnreadEventIfNeed(canvas, this);
    }
    ///: @}


    /// M: for OP09 DeleteButton.@{
    private void  drawDeleteButton(Canvas canvas) {
//        LauncherLog.d(TAG, "drawDeleteButton() mDeleteButtonVisiable = "
//                         + mDeleteButtonVisiable + ", this = " + this);
        if (mSupportEditAndHideApps && mDeleteButtonVisiable) {
            int deleteButtonWidth = mDeleteButtonDrawable.getIntrinsicWidth();
            int deleteButtonHeight = mDeleteButtonDrawable.getIntrinsicHeight();
            int deleteButtonPosX = getScrollX() + mDeleteMarginleft;
            int deleteButtonPosY = getScrollY();

            Rect deleteButtonBounds = new Rect(0, 0, deleteButtonWidth, deleteButtonHeight);
            mDeleteButtonDrawable.setBounds(deleteButtonBounds);

            canvas.save();
            canvas.translate(deleteButtonPosX, deleteButtonPosY);

            mDeleteButtonDrawable.draw(canvas);

            canvas.restore();
        }
    }

    public void setDeleteButtonVisibility(boolean visiable) {
        mDeleteButtonVisiable = visiable;
    }

    public boolean getDeleteButtonVisibility() {
        return mDeleteButtonVisiable;
    }
    /// M: for OP09 DeleteButton.}@
}
