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

import android.animation.Animator;

import com.qingcheng.home.R;
import android.animation.Animator.AnimatorListener;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.LayoutTransition;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.animation.TimeInterpolator;
import android.animation.ValueAnimator;
import android.animation.ValueAnimator.AnimatorUpdateListener;
import android.app.WallpaperManager;
import android.appwidget.AppWidgetHostView;
import android.appwidget.AppWidgetProviderInfo;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Camera;
import android.graphics.Canvas;
import android.graphics.Color;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.Region.Op;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Parcelable;
import android.os.Trace;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.Pair;
import android.util.SparseArray;
import android.view.Choreographer;
import android.view.Display;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityManager;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Interpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;

import android.view.WindowManager;

import java.lang.reflect.Field;
import java.util.Arrays;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup.LayoutParams;
import android.view.LayoutInflater;
import android.widget.TextView;

import com.qingcheng.home.BubbleTextView;
import com.qingcheng.home.Folder;
import com.qingcheng.home.FolderIcon;
import com.qingcheng.home.FolderInfo;
import com.qingcheng.home.MTKUnreadLoader;
import com.qingcheng.home.ShortcutAndWidgetContainer;
import com.qingcheng.home.ShortcutInfo;
import com.qingcheng.home.FolderIcon.FolderRingAnimator;
import com.qingcheng.home.Launcher.CustomContentCallbacks;
import com.qingcheng.home.LauncherSettings.Favorites;
import com.qingcheng.home.compat.PackageInstallerCompat;
import com.qingcheng.home.compat.PackageInstallerCompat.PackageInstallInfo;
import com.qingcheng.home.compat.UserHandleCompat;

import com.mediatek.launcher3.ext.LauncherExtPlugin;
import com.mediatek.launcher3.ext.LauncherLog;
import com.qingcheng.home.config.QCConfig;
import com.qingcheng.home.database.QCPreference;
import com.qingcheng.home.util.QCLog;
import com.qingcheng.home.wallpaper.WallpaperCropActivity;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * The workspace is a wide area with a wallpaper and a finite number of pages.
 * Each page contains a number of icons, folders or widgets the user can
 * interact with. A workspace is meant to be used with a fixed width only.
 */
public class Workspace extends SmoothPagedView
        implements DropTarget, DragSource, DragScroller, View.OnTouchListener,
        DragController.DragListener, LauncherTransitionable, ViewGroup.OnHierarchyChangeListener,
        Insettable {
    private static final String TAG = "Launcher.Workspace";

    // Y rotation to apply to the workspace screens
    private static final float WORKSPACE_OVERSCROLL_ROTATION = 24f;

    private static final int CHILDREN_OUTLINE_FADE_OUT_DELAY = 0;
    private static final int CHILDREN_OUTLINE_FADE_OUT_DURATION = 375;
    private static final int CHILDREN_OUTLINE_FADE_IN_DURATION = 100;

    protected static final int SNAP_OFF_EMPTY_SCREEN_DURATION = 400;
    protected static final int FADE_EMPTY_SCREEN_DURATION = 150;

    protected static final int DELETE_EMPTY_SCREEN_DURATION = 260;


    private static final int BACKGROUND_FADE_OUT_DURATION = 350;
    private static final int ADJACENT_SCREEN_DROP_DURATION = 300;
    private static final int FLING_THRESHOLD_VELOCITY = 500;

    private static final float ALPHA_CUTOFF_THRESHOLD = 0.01f;

    static final boolean MAP_NO_RECURSE = false;
    static final boolean MAP_RECURSE = true;

    // These animators are used to fade the children's outlines
    private ObjectAnimator mChildrenOutlineFadeInAnimation;
    private ObjectAnimator mChildrenOutlineFadeOutAnimation;
    private float mChildrenOutlineAlpha = 0;

    // These properties refer to the background protection gradient used for AllApps and Customize
    private ValueAnimator mBackgroundFadeInAnimation;
    private ValueAnimator mBackgroundFadeOutAnimation;

    private static final long CUSTOM_CONTENT_GESTURE_DELAY = 200;
    private long mTouchDownTime = -1;
    private long mCustomContentShowTime = -1;

    private LayoutTransition mLayoutTransition;
    private final WallpaperManager mWallpaperManager;
    private IBinder mWindowToken;

    private int mOriginalDefaultPage;
    private int mDefaultPage;

    private ShortcutAndWidgetContainer mDragSourceInternal;
    private static boolean sAccessibilityEnabled;

    // The screen id used for the empty screen always present to the right.
    final static long EXTRA_EMPTY_SCREEN_ID = -201;
    public final static long CUSTOM_CONTENT_SCREEN_ID = -301; // RGK public
    public final static long PROJECTOR_CONTENT_SCREEN_ID = -401; // RGK public

    private HashMap<Long, CellLayout> mWorkspaceScreens = new HashMap<Long, CellLayout>();
    public ArrayList<Long> mScreenOrder = new ArrayList<Long>();
    
    public int getNormalChildCount() {
    	return getChildCount()-(hasCustomContent()?1:0)-(hasExtraEmptyScreen()?1:0);
    }

    private Runnable mRemoveEmptyScreenRunnable;
    private boolean mDeferRemoveExtraEmptyScreen = false;

    /**
     * CellInfo for the cell that is currently being dragged
     */
    private CellLayout.CellInfo mDragInfo;

    /**
     * Target drop area calculated during last acceptDrop call.
     */
    private int[] mTargetCell = new int[2];
    private int mDragOverX = -1;
    private int mDragOverY = -1;

    static Rect mLandscapeCellLayoutMetrics = null;
    static Rect mPortraitCellLayoutMetrics = null;

    CustomContentCallbacks mCustomContentCallbacks;
    boolean mCustomContentShowing;
    private float mLastCustomContentScrollProgress = -1f;
    private String mCustomContentDescription = "";

    /**
     * The CellLayout that is currently being dragged over
     */
    private CellLayout mDragTargetLayout = null;
    /**
     * The CellLayout that we will show as glowing
     */
    private CellLayout mDragOverlappingLayout = null;

    /**
     * The CellLayout which will be dropped to
     */
    private CellLayout mDropToLayout = null;

    private Launcher mLauncher;
    private IconCache mIconCache;
    private DragController mDragController;

    // These are temporary variables to prevent having to allocate a new object just to
    // return an (x, y) value from helper functions. Do NOT use them to maintain other state.
    private int[] mTempCell = new int[2];
    private int[] mTempPt = new int[2];
    private int[] mTempEstimate = new int[2];
    private float[] mDragViewVisualCenter = new float[2];
    private float[] mTempCellLayoutCenterCoordinates = new float[2];
    private Matrix mTempInverseMatrix = new Matrix();

    private SpringLoadedDragController mSpringLoadedDragController;

    public float mOverviewModeShrinkFactor;
    private float mNormalScaleShrinkFactor;

    // State variable that indicates whether the pages are small (ie when you're
    // in all apps or customize mode)

    enum State { NORMAL, NORMAL_HIDDEN, SPRING_LOADED, OVERVIEW, OVERVIEW_HIDDEN};
    private State mState = State.NORMAL;
    private boolean mIsSwitchingState = false;
    private boolean enableOverviewMode = false;
    

    private static boolean isNormalScaling = false;
	public static boolean isNormalScaling() {
		return isNormalScaling;
	}


	boolean mAnimatingViewIntoPlace = false;
    boolean mIsDragOccuring = false;
    boolean mChildrenLayersEnabled = true;

    private boolean mStripScreensOnPageStopMoving = false;

    /** Is the user is dragging an item near the edge of a page? */
    private boolean mInScrollArea = false;

    private HolographicOutlineHelper mOutlineHelper;
    private Bitmap mDragOutline = null;
    private static final Rect sTempRect = new Rect();
    private final int[] mTempXY = new int[2];
    private int[] mTempVisiblePagesRange = new int[2];
    private boolean mOverscrollEffectSet;
    public static final int DRAG_BITMAP_PADDING = 2;
    private boolean mWorkspaceFadeInAdjacentScreens;

    WallpaperOffsetInterpolator mWallpaperOffset;
    private boolean mWallpaperIsLiveWallpaper;
    private int mNumPagesForWallpaperParallax;
    private float mLastSetWallpaperOffsetSteps = 0;

    private Runnable mDelayedResizeRunnable;
    private Runnable mDelayedSnapToPageRunnable;
    private Point mDisplaySize = new Point();
    private int mCameraDistance;

    // Variables relating to the creation of user folders by hovering shortcuts over shortcuts
    protected static final int FOLDER_CREATION_TIMEOUT = 0;
    public static final int REORDER_TIMEOUT = 350;
    private final Alarm mFolderCreationAlarm = new Alarm();
    private final Alarm mReorderAlarm = new Alarm();
    private FolderRingAnimator mDragFolderRingAnimator = null;
    private FolderIcon mDragOverFolderIcon = null;
    private boolean mCreateUserFolderOnDrop = false;
    private boolean mAddToExistingFolderOnDrop = false;
    private DropTarget.DragEnforcer mDragEnforcer;
    private float mMaxDistanceForFolderCreation;

    private final Canvas mCanvas = new Canvas();

    // Variables relating to touch disambiguation (scrolling workspace vs. scrolling a widget)
    private float mXDown;
    private float mYDown;
    final static float START_DAMPING_TOUCH_SLOP_ANGLE = (float) Math.PI / 6;
    final static float MAX_SWIPE_ANGLE = (float) Math.PI / 3;
    final static float TOUCH_SLOP_DAMPING_FACTOR = 4;

    // Relating to the animation of items being dropped externally
    public static final int ANIMATE_INTO_POSITION_AND_DISAPPEAR = 0;
    public static final int ANIMATE_INTO_POSITION_AND_REMAIN = 1;
    public static final int ANIMATE_INTO_POSITION_AND_RESIZE = 2;
    public static final int COMPLETE_TWO_STAGE_WIDGET_DROP_ANIMATION = 3;
    public static final int CANCEL_TWO_STAGE_WIDGET_DROP_ANIMATION = 4;

    // Related to dragging, folder creation and reordering
    protected static final int DRAG_MODE_NONE = 0;
    protected static final int DRAG_MODE_CREATE_FOLDER = 1;
    protected static final int DRAG_MODE_ADD_TO_FOLDER = 2;
    protected static final int DRAG_MODE_REORDER = 3;
    private int mDragMode = DRAG_MODE_NONE;
    
    private static boolean isFolderDragMode = false;
    public static boolean isFolderDragMode() {
		return isFolderDragMode;
	}

	private int mLastReorderX = -1;
    private int mLastReorderY = -1;

    private SparseArray<Parcelable> mSavedStates;
    private final ArrayList<Integer> mRestoredPages = new ArrayList<Integer>();

    // These variables are used for storing the initial and final values during workspace animations
    private int mSavedScrollX;
    private float mSavedRotationY;
    private float mSavedTranslationX = 0f;

    private float mCurrentScale;
    private float mNewScale;
    private static float mNewScaleForWidgetResize = -1;
    public static float getmNewScaleForWidgetResize() {
		return mNewScaleForWidgetResize;
	}

	private float[] mOldBackgroundAlphas;
    private float[] mOldAlphas;
    private float[] mNewBackgroundAlphas;
    private float[] mNewAlphas;
    private int mLastChildCount = -1;
    private float mTransitionProgress;

    float mOverScrollEffect = 0f;

    private Runnable mDeferredAction;
    private boolean mDeferDropAfterUninstall;
    private boolean mUninstallSuccessful;

    private final Runnable mBindPages = new Runnable() {
        @Override
        public void run() {
            mLauncher.getModel().bindRemainingSynchronousPages();
        }
    };

    /// M: [ALPS01394977] [ALPS01422169] Delay disable free scroll to avoid getting
    /// landscape layout in snapToPage().
    private int mSnapPage;

    /**
     * Used to inflate the Workspace from XML.
     *
     * @param context The application's context.
     * @param attrs The attributes set containing the Workspace's customization values.
     */
    public Workspace(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    /**
     * Used to inflate the Workspace from XML.
     *
     * @param context The application's context.
     * @param attrs The attributes set containing the Workspace's customization values.
     * @param defStyle Unused.
     */
    public Workspace(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        mContentIsRefreshable = false;


        mOutlineHelper = HolographicOutlineHelper.obtain(context);

        mDragEnforcer = new DropTarget.DragEnforcer(context);
        // With workspace, data is available straight from the get-go
        setDataIsReady();

        mLauncher = (Launcher) context;
        final Resources res = getResources();
        //anyway , don't change the alpha
        mWorkspaceFadeInAdjacentScreens = false;/*LauncherAppState.getInstance().getDynamicGrid().
                getDeviceProfile().shouldFadeAdjacentWorkspaceScreens()*/;
        mFadeInAdjacentScreens = false;
        mWallpaperManager = WallpaperManager.getInstance(context);

        LauncherAppState app = LauncherAppState.getInstance();
        DeviceProfile grid = app.getDynamicGrid().getDeviceProfile();
        TypedArray a = context.obtainStyledAttributes(attrs,
                R.styleable.Workspace, defStyle, 0);

        mOverviewModeShrinkFactor = grid.getOverviewModeScale();
        mNormalScaleShrinkFactor = grid.getNormalScale();
        mCameraDistance = res.getInteger(R.integer.config_cameraDistance);
        mDefaultPage = a.getInt(R.styleable.Workspace_defaultScreen, 0);
        a.recycle();

        preferences = mLauncher.getSharedPreferences(QCPreference.PREFERENCE_NAME, Context.MODE_PRIVATE);  
        editor = preferences.edit();

        int mDefaultP = preferences.getInt(QCPreference.KEY_DEFAULT_SCREEN, -1);
        if(mDefaultP == -1){
			mDefaultHomeFlag = true;
        	editor.putInt(QCPreference.KEY_DEFAULT_SCREEN, mDefaultPage);
        	editor.commit();
        }else{
        	 mDefaultPage = mDefaultP;
        }

        setOnHierarchyChangeListener(this);
        setHapticFeedbackEnabled(false);

        initWorkspace();

        // Disable multitouch across the workspace/all apps/customize tray
        setMotionEventSplittingEnabled(true);
        setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_YES);
        
        SharedPreferences mPreferences = mLauncher.getSharedPreferences(QCPreference.PREFERENCE_NAME, Context.MODE_PRIVATE);
        mSlideEffect = mPreferences.getInt(QCPreference.KEY_SLIDE_ANIMATION, UI_SLIDE_EFFECT_DEFAULT);
    }

    @Override
    public void setInsets(Rect insets) {
        mInsets.set(insets);

        CellLayout customScreen = getScreenWithId(CUSTOM_CONTENT_SCREEN_ID);
        if (customScreen != null) {
            View customContent = customScreen.getShortcutsAndWidgets().getChildAt(0);
            if (customContent instanceof Insettable) {
                ((Insettable) customContent).setInsets(mInsets);
            }
        }
    }

    // estimate the size of a widget with spans hSpan, vSpan. return MAX_VALUE for each
    // dimension if unsuccessful
    public int[] estimateItemSize(int hSpan, int vSpan,
            ItemInfo itemInfo, boolean springLoaded) {
        int[] size = new int[2];
        if (getChildCount() > 0) {
            // Use the first non-custom page to estimate the child position
            CellLayout cl = (CellLayout) getChildAt(numCustomPages());
            Rect r = estimateItemPosition(cl, itemInfo, 0, 0, hSpan, vSpan);
            size[0] = r.width();
            size[1] = r.height();
            if (springLoaded) {
                size[0] *= mOverviewModeShrinkFactor;
                size[1] *= mOverviewModeShrinkFactor;
            }
            return size;
        } else {
            size[0] = Integer.MAX_VALUE;
            size[1] = Integer.MAX_VALUE;
            return size;
        }
    }

    public Rect estimateItemPosition(CellLayout cl, ItemInfo pendingInfo,
            int hCell, int vCell, int hSpan, int vSpan) {
        Rect r = new Rect();
        cl.cellToRect(hCell, vCell, hSpan, vSpan, r);
        return r;
    }

    public void onDragStart(final DragSource source, Object info, int dragAction) {
        if (LauncherLog.DEBUG_DRAG) {
            LauncherLog.d(TAG, "onDragStart: source = " + source + ", info = " + info + ", dragAction = " + dragAction);
        }

        mIsDragOccuring = true;
        updateChildrenLayersEnabled(false);
        mLauncher.lockScreenOrientation();
        mLauncher.onInteractionBegin();
        setChildrenBackgroundAlphaMultipliers(1f);
        // Prevent any Un/InstallShortcutReceivers from updating the db while we are dragging
        InstallShortcutReceiver.enableInstallQueue();
        UninstallShortcutReceiver.enableUninstallQueue();
        
        if (mState == State.NORMAL) {
        	setMinScale(mOverviewModeShrinkFactor);
        	enableNormalScaleMode(true);
		}

        if (QCConfig.autoDeleteAndAddEmptyScreen) {
            post(new Runnable() {
                @Override
                public void run() {
                    if (mIsDragOccuring) {
                        mDeferRemoveExtraEmptyScreen = false;
                        addExtraEmptyScreenOnDrag();
                    }
                }
            });
		}
        if((mState == State.NORMAL) && (qingcheng_page_indicator.getVisibility() == View.VISIBLE)){
        	showPiflow(false);
        }
        
		enterWidgetPageIndicatorTime = 0;
    }

    public void deferRemoveExtraEmptyScreen() {
        mDeferRemoveExtraEmptyScreen = true;
    }

    public void onDragEnd() {
        if (isNormalScaling) {
        	cancelMinScale();
			enableNormalScaleMode(false);
		}
        
        if (!mDeferRemoveExtraEmptyScreen) {
        	if (QCConfig.autoDeleteAndAddEmptyScreen) {
        		removeExtraEmptyScreen(true, mDragSourceInternal != null);
			} else {
				cellsShowAddOrDeleteButton(true, true);
			}
        }
        
    	if(mLauncher.hasCustomContentToLeft()){
    		if( mState != State.NORMAL){
	        	int childcount = getChildCount();
	        	if(childcount > mPageViewLongList.size()+1+ (mWorkspaceScreens.containsKey(EXTRA_EMPTY_SCREEN_ID)?1:0)){
	        		showPageIndex(childcount-1);
	        		showWsLongPageView(getCurrentPage());
	        	}
	    	}
    		
    	}
        
        mIsDragOccuring = false;
        updateChildrenLayersEnabled(false);
        mLauncher.unlockScreenOrientation(false);

        // Re-enable any Un/InstallShortcutReceiver and now process any queued items
        InstallShortcutReceiver.disableAndFlushInstallQueue(getContext());
        UninstallShortcutReceiver.disableAndFlushUninstallQueue(getContext());

        mDragSourceInternal = null;
        mLauncher.onInteractionEnd();
        if(mState == State.NORMAL){
        	hideWsLongPageView();
        }
        if((mState == State.NORMAL) && (qingcheng_page_indicator.getVisibility() != View.VISIBLE)){
        	showPiflow(true);
        }
        
		enterWidgetPageIndicatorTime = 0;
    }


    public State getmState(){
    	return mState;
    }
    
    private ViewGroup piflow =null;
    
    private void showPiflow(boolean flag){
    	if(piflow !=null && flag){
    		piflow.setVisibility(View.VISIBLE);
    		 if (hasCustomContent()) {
    	            mWorkspaceScreens.get(CUSTOM_CONTENT_SCREEN_ID).setVisibility(VISIBLE);
    	        }
     		//sunfeng modify @20150908 for JL620 JLLEL-564 end:
    	}else if(piflow !=null && !flag){
    		piflow.setVisibility(View.INVISIBLE);
    	}
    	
    }

    boolean mDefaultHomeFlag = false;

    /**
     * Initializes various states for this workspace.
     */
    protected void initWorkspace() {
        setCurrentPageReal(mDefaultPage);
        Launcher.setScreen(mCurrentPage);
        LauncherAppState app = LauncherAppState.getInstance();
        DeviceProfile grid = app.getDynamicGrid().getDeviceProfile();
        mIconCache = app.getIconCache();
        setWillNotDraw(false);
        setClipChildren(false);
        setClipToPadding(false);
        setChildrenDrawnWithCacheEnabled(true);

        //setMinScale(mOverviewModeShrinkFactor);
        setupLayoutTransition();

        mWallpaperOffset = new WallpaperOffsetInterpolator();
        Display display = mLauncher.getWindowManager().getDefaultDisplay();
        display.getSize(mDisplaySize);

        wm =mLauncher.getWindowManager();
        
        mMaxDistanceForFolderCreation = (0.55f * grid.iconSizePx);
        mFlingThresholdVelocity = (int) (FLING_THRESHOLD_VELOCITY * mDensity);

        // Set the wallpaper dimensions when Launcher starts up
        setWallpaperDimension();
        
        if (LauncherApplication.getIsNormalScreenResolutionAndDensity()) {
        	mCellPageDistance  = (int) mLauncher.getResources().getDimension(R.dimen.workspace_page_add_page_item);
		} else {
			mCellPageDistance = mLauncher.mApplication.getPxforXLayout(true, R.dimen.workspace_page_add_page_item, 0);
		}
        
        // Add for workspace overview effect Jing.Wu 20151230 start
        mReorderingChildScale = 1.09f;
        // Add for workspace overview effect Jing.Wu 20151230 end
    }

    private void setupLayoutTransition() {
        // We want to show layout transitions when pages are deleted, to close the gap.
        mLayoutTransition = new LayoutTransition();
        mLayoutTransition.enableTransitionType(LayoutTransition.DISAPPEARING);
        mLayoutTransition.enableTransitionType(LayoutTransition.CHANGE_DISAPPEARING);
        mLayoutTransition.disableTransitionType(LayoutTransition.APPEARING);
        mLayoutTransition.disableTransitionType(LayoutTransition.CHANGE_APPEARING);
        setLayoutTransition(mLayoutTransition);
    }

    void enableLayoutTransitions() {
        setLayoutTransition(mLayoutTransition);
    }
    void disableLayoutTransitions() {
        setLayoutTransition(null);
    }

    @Override
    protected int getScrollMode() {
        return SmoothPagedView.X_LARGE_MODE;
    }

    @Override
    public void onChildViewAdded(View parent, View child) {
        if (!(child instanceof CellLayout)) {
            throw new IllegalArgumentException("A Workspace can only have CellLayout children.");
        }
        CellLayout cl = ((CellLayout) child);
        cl.setOnInterceptTouchListener(this);
        cl.setClickable(true);

        super.onChildViewAdded(parent, child);
        if (!QCConfig.autoDeleteAndAddEmptyScreen) {
        	cellsShowAddOrDeleteButton(true, true);
		}
    }

    @Override
	public void onChildViewRemoved(View parent, View child) {
		super.onChildViewRemoved(parent, child);
		if (!QCConfig.autoDeleteAndAddEmptyScreen) {
			cellsShowAddOrDeleteButton(true, true);
		}
	}
    
	protected boolean shouldDrawChild(View child) {
        final CellLayout cl = (CellLayout) child;
        return super.shouldDrawChild(child) &&
            (mIsSwitchingState ||
             cl.getShortcutsAndWidgets().getAlpha() > 0 ||
             cl.getBackgroundAlpha() > 0);
    }

    /**
     * @return The open folder on the current screen, or null if there is none
     */
    Folder getOpenFolder() {
        DragLayer dragLayer = mLauncher.getDragLayer();
        int count = dragLayer.getChildCount();
        for (int i = 0; i < count; i++) {
            View child = dragLayer.getChildAt(i);
            if (child instanceof Folder) {
                Folder folder = (Folder) child;
                if (folder.getInfo().opened)
                    return folder;
            }
        }
        return null;
    }

    boolean isTouchActive() {
        return mTouchState != TOUCH_STATE_REST;
    }

    public void removeAllWorkspaceScreens() {
        // Disable all layout transitions before removing all pages to ensure that we don't get the
        // transition animations competing with us changing the scroll when we add pages or the
        // custom content screen
        disableLayoutTransitions();

        // Since we increment the current page when we call addCustomContentPage via bindScreens
        // (and other places), we need to adjust the current page back when we clear the pages
        if (hasCustomContent()) {
            removeCustomContentPage();
        }

        // Remove the pages and clear the screen models
        removeAllViews();
        mScreenOrder.clear();
        /**M: remove all the animator listener.@{**/
        Iterator<CellLayout> cells = mWorkspaceScreens.values().iterator();
        while(cells.hasNext()){
            cells.next().clear();
        }
        /**@}**/
        mWorkspaceScreens.clear();

        // Re-enable the layout transitions
        enableLayoutTransitions();
    }

    public long insertNewWorkspaceScreenBeforeEmptyScreen(long screenId) {
        // Find the index to insert this view into.  If the empty screen exists, then
        // insert it before that.
        int insertIndex = mScreenOrder.indexOf(EXTRA_EMPTY_SCREEN_ID);
        if (insertIndex < 0) {
            insertIndex = mScreenOrder.size();
        }
        return insertNewWorkspaceScreen(screenId, insertIndex);
    }

    public long insertNewWorkspaceScreen(long screenId) {
        return insertNewWorkspaceScreen(screenId, getChildCount());
    }

    public long insertNewWorkspaceScreen(long screenId, int insertIndex) {
        if (mWorkspaceScreens.containsKey(screenId)) {
            throw new RuntimeException("Screen id " + screenId + " already exists!");
        }

        CellLayout newScreen = (CellLayout)
                mLauncher.getLayoutInflater().inflate(R.layout.workspace_screen, null);

        newScreen.setOnLongClickListener(mLongClickListener);
        newScreen.setOnClickListener(mLauncher);
        newScreen.setSoundEffectsEnabled(false);
        mWorkspaceScreens.put(screenId, newScreen);
        mScreenOrder.add(insertIndex, screenId);
        addView(newScreen, insertIndex);

        if(mState != State.NORMAL && qingcheng_widget_page_indicator.getVisibility() == View.VISIBLE){
            newScreen.setBackgroundAlpha(1.0f);
        }

        showPageIndex(-1);

        return screenId;
    }
    
    private void updateCanvasRect() {
    	int left,top,right,bottom;
    	left = top = right = bottom = -1;
    	for(int i = numCustomPages(); i < getChildCount(); i++){
    		CellLayout newScreen = (CellLayout)getChildAt(i);
            left = ((left == -1)||(newScreen.getLeft()<left))? newScreen.getLeft():left;
            top = newScreen.getTop();
            right = newScreen.getRight()>right? newScreen.getRight():right;
            bottom = newScreen.getBottom();
    	}
    	left = left - getPaddingLeft();
    	top = top - getPaddingTop();
    	right = right + getPaddingRight();
    	bottom = bottom + getPaddingBottom();

        mChildrenCellRect.set(left, top, right, bottom);
    }

    public void createCustomContentContainer() {
        CellLayout customScreen = (CellLayout)
                mLauncher.getLayoutInflater().inflate(R.layout.workspace_screen, null);
        customScreen.disableBackground();
        customScreen.disableDragTarget();

        mWorkspaceScreens.put(CUSTOM_CONTENT_SCREEN_ID, customScreen);
        mScreenOrder.add(0, CUSTOM_CONTENT_SCREEN_ID);

        // We want no padding on the custom content
        customScreen.setPadding(0, 0, 0, 0);

        if (getPageIndicator() != null){
        	getPageIndicator().isNewsPage = true;
        }

        addFullScreenPage(customScreen);

        showPageIndex(-1);

 
        // Update the custom content hint
        if (mRestorePage != INVALID_RESTORE_PAGE) {
            mRestorePage = mRestorePage + 1;
        } else {
            setCurrentPage(getCurrentPage() /*+ 1*/);
        }
    }

    public void createProjectorContentContainer() {
        CellLayout projectorScreen = (CellLayout)
                mLauncher.getLayoutInflater().inflate(R.layout.workspace_screen, null);
        projectorScreen.disableBackground();
        projectorScreen.disableDragTarget();

        mWorkspaceScreens.put(PROJECTOR_CONTENT_SCREEN_ID, projectorScreen);
        mScreenOrder.add(1, PROJECTOR_CONTENT_SCREEN_ID);

        projectorScreen.setPadding(0, 0, 0, 0);


        LayoutParams lp = generateDefaultLayoutParams();
        lp.isFullScreenPage = true;
        super.addView(projectorScreen, 1, lp);
    }

    public void removeCustomContentPage() {
        if (getPageIndicator() != null){
        	getPageIndicator().isNewsPage = false;
        }
        CellLayout customScreen = getScreenWithId(CUSTOM_CONTENT_SCREEN_ID);
        if (customScreen == null) {
            throw new RuntimeException("Expected custom content screen to exist");
        }

        mWorkspaceScreens.remove(CUSTOM_CONTENT_SCREEN_ID);
        mScreenOrder.remove(CUSTOM_CONTENT_SCREEN_ID);
        /**M: remove all the animator listener.@{**/
        customScreen.clear();
        /**@}**/
        removeView(customScreen);

        if (mCustomContentCallbacks != null) {
            mCustomContentCallbacks.onScrollProgressChanged(0);
            mCustomContentCallbacks.onHide();
        }

        mCustomContentCallbacks = null;
		mDefaultHomeFlag = true;

        if (mRestorePage != INVALID_RESTORE_PAGE) {
            mRestorePage = mRestorePage - 1;
        } else {
            setCurrentPage(getCurrentPage());
        }
    }

    public void addToCustomContentPage(View customContent, CustomContentCallbacks callbacks,
            String description) {
        if (getPageIndexForScreenId(CUSTOM_CONTENT_SCREEN_ID) < 0) {
            throw new RuntimeException("Expected custom content screen to exist");
        }

        // Add the custom content to the full screen custom page
        CellLayout customScreen = getScreenWithId(CUSTOM_CONTENT_SCREEN_ID);
        int spanX = customScreen.getCountX();
        int spanY = customScreen.getCountY();
        CellLayout.LayoutParams lp = new CellLayout.LayoutParams(0, 0, spanX, spanY);
        lp.canReorder  = false;
        lp.isFullscreen = true;
        if (customContent instanceof Insettable) {
            ((Insettable)customContent).setInsets(mInsets);
        }

        // Verify that the child is removed from any existing parent.
        if (customContent.getParent() instanceof ViewGroup) {
            ViewGroup parent = (ViewGroup) customContent.getParent();
            parent.removeView(customContent);
        }
        customScreen.removeAllViews();
        customScreen.addViewToCellLayout(customContent, 0, 0, lp, true);
        mCustomContentDescription = description;

        mCustomContentCallbacks = callbacks;
    }

    public void addToProjectorContentPage(View content) {
        if (getPageIndexForScreenId(PROJECTOR_CONTENT_SCREEN_ID) < 0) {
            throw new RuntimeException("Expected projector content screen to exist");
        }

        // Add the custom content to the full screen custom page
        CellLayout customScreen = getScreenWithId(PROJECTOR_CONTENT_SCREEN_ID);
        int spanX = customScreen.getCountX();
        int spanY = customScreen.getCountY();
        CellLayout.LayoutParams lp = new CellLayout.LayoutParams(0, 0, spanX, spanY);
        lp.canReorder  = false;
        lp.isFullscreen = true;
        if (content instanceof Insettable) {
            ((Insettable)content).setInsets(mInsets);
        }

        // Verify that the child is removed from any existing parent.
        if (content.getParent() instanceof ViewGroup) {
            ViewGroup parent = (ViewGroup) content.getParent();
            parent.removeView(content);
        }
        customScreen.removeAllViews();
        customScreen.addViewToCellLayout(content, 0, 0, lp, true);
    }

    public void addExtraEmptyScreenOnDrag() {
        boolean lastChildOnScreen = false;
        boolean childOnFinalScreen = false;

        // Cancel any pending removal of empty screen
        mRemoveEmptyScreenRunnable = null;

        if (mDragSourceInternal != null) {
            if (mDragSourceInternal.getChildCount() == 1) {
                lastChildOnScreen = true;
            }
            CellLayout cl = (CellLayout) mDragSourceInternal.getParent();
            if (indexOfChild(cl) == getChildCount() - 1) {
                childOnFinalScreen = true;
            }
        }

        // If this is the last item on the final screen
        if (lastChildOnScreen && childOnFinalScreen) {
            return;
        }

        if (LauncherExtPlugin.getInstance().getWorkspaceExt(getContext())
            .exceedLimitedScreen(mWorkspaceScreens.size())) {
            return ;
        }
        int childCount = getChildCount() - numCustomPages();
        boolean countFlag = true;
        if (QCConfig.autoDeleteAndAddEmptyScreen) {
            countFlag = childCount < QCConfig.maxScreenCountInWorkspace;
        }
        if (!mWorkspaceScreens.containsKey(EXTRA_EMPTY_SCREEN_ID) && countFlag) {
            insertNewWorkspaceScreen(EXTRA_EMPTY_SCREEN_ID);
        }
    }

    public boolean addExtraEmptyScreen() {

        int childCount = getChildCount() - numCustomPages();
        boolean countFlag = true;
        if (!QCConfig.autoDeleteAndAddEmptyScreen) {
        	countFlag = childCount < QCConfig.maxScreenCountInWorkspace;
		}

        if (!mWorkspaceScreens.containsKey(EXTRA_EMPTY_SCREEN_ID) && countFlag) {
            insertNewWorkspaceScreen(EXTRA_EMPTY_SCREEN_ID);
            return true;
        }
        return false;
    }

    public boolean addExtraEmptyScreenProjector() {
        long screenID = LauncherModel.getMaxScreenId(getContext());

        ArrayList<Long> tempScreenOrder = new ArrayList<>();
        int size = mScreenOrder.size();
        tempScreenOrder.add(screenID);
        for (int i = 0; i < size -1; i++) {
            tempScreenOrder.add(mScreenOrder.get(i));
        }
        mLauncher.getModel().updateWorkspaceScreenOrder2(mLauncher, tempScreenOrder);

        LauncherModel.addProjectorToDatabase(getContext(), screenID, mLauncher.getAppWidgetHost());
        return true;

    }

    private void convertFinalScreenToEmptyScreenIfNecessary() {

        if (mLauncher.isWorkspaceLoading()) {
            return;
        }

        if (hasExtraEmptyScreen() || mScreenOrder.size() == 0) return;
        long finalScreenId = mScreenOrder.get(mScreenOrder.size() - 1);

        if (finalScreenId == CUSTOM_CONTENT_SCREEN_ID) return;
        CellLayout finalScreen = mWorkspaceScreens.get(finalScreenId);

        // If the final screen is empty, convert it to the extra empty screen
        if (finalScreen.getShortcutsAndWidgets().getChildCount() == 0 &&
                !finalScreen.isDropPending()) {
            mWorkspaceScreens.remove(finalScreenId);
            mScreenOrder.remove(finalScreenId);


            // if this is the last non-custom content screen, convert it to the empty screen
            mWorkspaceScreens.put(EXTRA_EMPTY_SCREEN_ID, finalScreen);
            mScreenOrder.add(EXTRA_EMPTY_SCREEN_ID);

            // Update the model if we have changed any screens

            mLauncher.getModel().updateWorkspaceScreenOrder(mLauncher, mScreenOrder);

        }
    }

    public void removeExtraEmptyScreen(final boolean animate, boolean stripEmptyScreens) {
        removeExtraEmptyScreenDelayed(animate, null, 0, stripEmptyScreens);
    }

    public void removeExtraEmptyScreenDelayed(final boolean animate, final Runnable onComplete,
            final int delay, final boolean stripEmptyScreens) {

        if (mLauncher.isWorkspaceLoading()) {
            return;
        }

        if (delay > 0) {
            postDelayed(new Runnable() {
                @Override
                public void run() {
                    removeExtraEmptyScreenDelayed(animate, onComplete, 0, stripEmptyScreens);
                }
            }, delay);
            return;
        }

        if (QCConfig.autoDeleteAndAddEmptyScreen) {
        	convertFinalScreenToEmptyScreenIfNecessary();
		}
        
        if (hasExtraEmptyScreen()) {
            int emptyIndex = mScreenOrder.indexOf(EXTRA_EMPTY_SCREEN_ID);
            if (getNextPage() == emptyIndex) {
                snapToPage(getNextPage() - 1, SNAP_OFF_EMPTY_SCREEN_DURATION);
                fadeAndRemoveEmptyScreen(SNAP_OFF_EMPTY_SCREEN_DURATION, FADE_EMPTY_SCREEN_DURATION,
                        onComplete, stripEmptyScreens);
            } else {
                fadeAndRemoveEmptyScreen(0, FADE_EMPTY_SCREEN_DURATION,
                        onComplete, stripEmptyScreens);
            }
            return;
        } else if (stripEmptyScreens) {
            // If we're not going to strip the empty screens after removing
            // the extra empty screen, do it right away.
            stripEmptyScreens();
        }

        if (onComplete != null) {
            onComplete.run();
        }
    }

    private void fadeAndRemoveEmptyScreen(int delay, int duration, final Runnable onComplete,
            final boolean stripEmptyScreens) {
        PropertyValuesHolder alpha = PropertyValuesHolder.ofFloat("alpha", 0f);
        PropertyValuesHolder bgAlpha = PropertyValuesHolder.ofFloat("backgroundAlpha", 0f);

        final CellLayout cl = mWorkspaceScreens.get(EXTRA_EMPTY_SCREEN_ID);

        mRemoveEmptyScreenRunnable = new Runnable() {
            @Override
            public void run() {
                if (hasExtraEmptyScreen()) {
                    mWorkspaceScreens.remove(EXTRA_EMPTY_SCREEN_ID);
                    mScreenOrder.remove(EXTRA_EMPTY_SCREEN_ID);
                    removeView(cl);


                    if (stripEmptyScreens) {
                        stripEmptyScreens();
                    }

                    if(getChildCount() == mCurrentPage){
                    	setCurrentPage(getChildCount() -1);
                    }

                    if(mLauncher.hasCustomContentToLeft()){
                    	int childcount = getChildCount();
        	        	if(childcount <= mPageViewLongList.size()+1+ (mWorkspaceScreens.containsKey(EXTRA_EMPTY_SCREEN_ID)?1:0)){
        	        		showPageIndex(childcount-1);
                        	if(qingcheng_widget_page_indicator.getVisibility() == View.VISIBLE|| mState != State.NORMAL){
                        		showWsLongPageView(getCurrentPage());
        	        		}
        	        	}
                    }else{
                    	showPageIndex(getChildCount());
                    	if(qingcheng_widget_page_indicator.getVisibility() == View.VISIBLE|| mState != State.NORMAL){
                    		showWsLongPageView(getCurrentPage());
                    	}
                    }
                }
            }
        };

        ObjectAnimator oa = ObjectAnimator.ofPropertyValuesHolder(cl, alpha, bgAlpha);
        oa.setDuration(duration);
        oa.setStartDelay(delay);
        oa.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                if (mRemoveEmptyScreenRunnable != null) {
                    mRemoveEmptyScreenRunnable.run();
                }
                if (onComplete != null) {
                    onComplete.run();
                }
            }
        });
        oa.start();
    }
    
    // Add for add and delete empty screen Jing.Wu 20151214 start
    public void deleteEmptyScreen(CellLayout cellLayout) {
        if (cellLayout == null) {
			return;
		}
        final CellLayout cl = cellLayout;
        final long clID = getIdForScreen(cellLayout);
        if (clID == -1) {
			return;
		}
        
        if (mWorkspaceScreens.containsValue(cellLayout) && (mScreenOrder.indexOf(clID) == mCurrentPage)) {
        	mLauncher.setIsSwitchAnimationing(true);
        	
			AnimatorSet mAnimatorSet = new AnimatorSet();
			Collection<Animator> mAnimators = new ArrayList<Animator>();
			
	        mRemoveEmptyScreenRunnable = new Runnable() {
	            @Override
	            public void run() {
	                mWorkspaceScreens.remove(clID);
	                mScreenOrder.remove(clID);
	                cl.clear();
	                removeView(cl);

	                if (!hasExtraEmptyScreen() || mScreenOrder.size() == mCurrentPage+1) {
//						mCurrentPage--;
                        setCurrentPageReal(mCurrentPage -1);
		                addExtraEmptyScreen();
		                needSnapToExtraEmptyScreen = true;
		                setCurrentPage(mCurrentPage);
					}

	                mLauncher.getModel().updateWorkspaceScreenOrder(mLauncher, mScreenOrder);
	                
	                if(mLauncher.hasCustomContentToLeft()){
	                	int childcount = getChildCount();
	    	        	if(childcount <= mPageViewLongList.size()+1+ (mWorkspaceScreens.containsKey(EXTRA_EMPTY_SCREEN_ID)?1:0)){
	    	        		showPageIndex(childcount-1);
	                    	if(qingcheng_widget_page_indicator.getVisibility() == View.VISIBLE|| mState != State.NORMAL){
	                    		showWsLongPageView(getCurrentPage());
	    	        		}
	    	        	}
	                }else{
	                	showPageIndex(getChildCount());
	                	if(qingcheng_widget_page_indicator.getVisibility() == View.VISIBLE|| mState != State.NORMAL){
	                		showWsLongPageView(getCurrentPage());
	                	}
	                }
	                mLauncher.setIsSwitchAnimationing(false);
	            }
	        };
	        
	        PropertyValuesHolder alpha = PropertyValuesHolder.ofFloat("alpha", 0f);
	        PropertyValuesHolder bgAlpha = PropertyValuesHolder.ofFloat("backgroundAlpha", 0f);
	        PropertyValuesHolder scaleX = PropertyValuesHolder.ofFloat("scaleX", 0.4f);
	        PropertyValuesHolder scaleY = PropertyValuesHolder.ofFloat("scaleY", 0.4f);

	        ObjectAnimator currentPageObjectAnimator = ObjectAnimator.ofPropertyValuesHolder(cl, alpha, bgAlpha, scaleX, scaleY);
	        currentPageObjectAnimator.setDuration(DELETE_EMPTY_SCREEN_DURATION);
	        currentPageObjectAnimator.addListener(new AnimatorListenerAdapter() {
	        	@Override
				public void onAnimationStart(Animator animation) {
					super.onAnimationStart(animation);
				}

				@Override
	        	public void onAnimationEnd(Animator animation) {
	        		if (mRemoveEmptyScreenRunnable != null) {
	        			postDelayed(mRemoveEmptyScreenRunnable, 25);
	        			//mRemoveEmptyScreenRunnable.run();
	        		}
	        	}
	        });
	        
			mAnimators.add(currentPageObjectAnimator);
			
			for (int i = mCurrentPage+1; i < mScreenOrder.size(); i++) {
				int leftX = getScreenWithId(mScreenOrder.get(i-1)).getLeft();
				int rightX = getScreenWithId(mScreenOrder.get(i-1)).getRight();
				PropertyValuesHolder leftHolder = PropertyValuesHolder.ofInt("left", leftX);
				PropertyValuesHolder rightHolder = PropertyValuesHolder.ofInt("right", rightX);
				ObjectAnimator pageAnimator = ObjectAnimator.ofPropertyValuesHolder(getScreenWithId(mScreenOrder.get(i)), leftHolder, rightHolder);
				pageAnimator.setDuration(DELETE_EMPTY_SCREEN_DURATION);
				
				mAnimators.add(pageAnimator);
			}

			mAnimatorSet.playTogether(mAnimators);
			mAnimatorSet.start();
		}
    }
    // Add for add and delete empty screen Jing.Wu 20151214 end
    
    public boolean hasExtraEmptyScreen() {
        int nScreens = getChildCount();
        nScreens = nScreens - numCustomPages();
        // Modify for autoDeleteAndAddEmptyScreen flag Jing.Wu 20150925 start
        if (QCConfig.autoDeleteAndAddEmptyScreen) {
        	return mWorkspaceScreens.containsKey(EXTRA_EMPTY_SCREEN_ID) && nScreens > 1;
		} else {
			return mWorkspaceScreens.containsKey(EXTRA_EMPTY_SCREEN_ID);
		}
        // Modify for autoDeleteAndAddEmptyScreen flag Jing.Wu 20150925 end
    }

    public long commitExtraEmptyScreen() {
        if (mLauncher.isWorkspaceLoading()) {
            return -1;
        }

        int index = getPageIndexForScreenId(EXTRA_EMPTY_SCREEN_ID);
        CellLayout cl = mWorkspaceScreens.get(EXTRA_EMPTY_SCREEN_ID);
        mWorkspaceScreens.remove(EXTRA_EMPTY_SCREEN_ID);
        mScreenOrder.remove(EXTRA_EMPTY_SCREEN_ID);

        long newId = LauncherAppState.getLauncherProvider().generateNewScreenId();
        mWorkspaceScreens.put(newId, cl);
        mScreenOrder.add(newId);

    	// Add for AddExtraEmptyScreen Jing.Wu 20151219 start
        if (!QCConfig.autoDeleteAndAddEmptyScreen) {
        	addExtraEmptyScreen();
        	cl.showAddButton(true);
		}
        
        // Update the page indicator marker
        if (getPageIndicator() != null) {
            getPageIndicator().updateMarker(index, getPageIndicatorMarker(index));
            getPageIndicator().changeMarkerVisibility(index, true);
        }
        boolean flagShow = qingcheng_page_indicator.getVisibility() == View.VISIBLE;
        int child  = getChildCount();
        
        if(mLauncher.hasCustomContentToLeft() /* && !mWorkspaceScreens.containsKey(EXTRA_EMPTY_SCREEN_ID)*/){
        	if(child > mPageViewLongList.size()+1+ (mWorkspaceScreens.containsKey(EXTRA_EMPTY_SCREEN_ID)?1:0)){
        		showPageIndex(child-1);
        		if(!flagShow){
        			showWsLongPageView(getCurrentPage());
        		}
        	}
		}else{
			showPageIndex(child);
		}

        if(mState != State.NORMAL && !flagShow){
        	showWsLongPageView(getCurrentPage());
        }
        
        if(flagShow){
        	qingcheng_page_indicator.setVisibility(View.VISIBLE);
        }
       	RefreshPageView(getCurrentPage());
        
        // Update the model for the new screen
        mLauncher.getModel().updateWorkspaceScreenOrder(mLauncher, mScreenOrder);

        return newId;
    }

    public CellLayout getScreenWithId(long screenId) {
        CellLayout layout = mWorkspaceScreens.get(screenId);
        return layout;
    }

    public long getIdForScreen(CellLayout layout) {
        Iterator<Long> iter = mWorkspaceScreens.keySet().iterator();
        while (iter.hasNext()) {
            long id = iter.next();
            if (mWorkspaceScreens.get(id) == layout) {
                return id;
            }
        }
        return -1;
    }
    
    public boolean isCustomPage(CellLayout layout) {
        Iterator<Long> iter = mWorkspaceScreens.keySet().iterator();
        while (iter.hasNext()) {
            long id = iter.next();
            if (mWorkspaceScreens.get(id) == layout) {
                return id == CUSTOM_CONTENT_SCREEN_ID;
            }
        }
        return false;
    }

    public int getPageIndexForScreenId(long screenId) {
        return indexOfChild(mWorkspaceScreens.get(screenId));
    }

    public long getScreenIdForPageIndex(int index) {
    	try {
    		if (0 <= index && index < mScreenOrder.size()) {
                return mScreenOrder.get(index);
            }
		} catch (Exception e) {
			e.printStackTrace();
		}
        return -1;
    }

    ArrayList<Long> getScreenOrder() {
        return mScreenOrder;
    }

    public void stripEmptyScreens() {
        if (mLauncher.isWorkspaceLoading()) {
            return;
        }

        if (isPageMoving()) {
            mStripScreensOnPageStopMoving = true;
            return;
        }

        int currentPage = getNextPage();
        ArrayList<Long> removeScreens = new ArrayList<Long>();
        for (Long id: mWorkspaceScreens.keySet()) {
            CellLayout cl = mWorkspaceScreens.get(id);
            if (id >= 0 && cl.getShortcutsAndWidgets().getChildCount() == 0) {
                removeScreens.add(id);
            }
        }

        // We enforce at least one page to add new items to. In the case that we remove the last
        // such screen, we convert the last screen to the empty screen
        int minScreens = 1 + numCustomPages();

        int pageShift = 0;
        for (Long id: removeScreens) {
            CellLayout cl = mWorkspaceScreens.get(id);
            mWorkspaceScreens.remove(id);
            mScreenOrder.remove(id);

            if (getChildCount() > minScreens) {
                if (indexOfChild(cl) < currentPage) {
                    pageShift++;
                }
                /**M: clear all the animator listeners.@{**/
                if (cl != null) {
                    cl.clear();
                }
                /**@}**/
                removeView(cl);

            } else {
                // if this is the last non-custom content screen, convert it to the empty screen
                mRemoveEmptyScreenRunnable = null;
                // Changed for MyUI---20150714
                if (QCConfig.autoDeleteAndAddEmptyScreen) {
                    mWorkspaceScreens.put(EXTRA_EMPTY_SCREEN_ID, cl);
                    mScreenOrder.add(EXTRA_EMPTY_SCREEN_ID);
				} else {
	                long newId = LauncherAppState.getLauncherProvider().generateNewScreenId();
	                mWorkspaceScreens.put(newId, cl);
	                mScreenOrder.add(newId);
				}
            }
        }

        if (!removeScreens.isEmpty()) {
            // Update the model if we have changed any screens

            mLauncher.getModel().updateWorkspaceScreenOrder(mLauncher, mScreenOrder);
        }

        if (pageShift >= 0) {
            setCurrentPage(currentPage - pageShift);
        }

        preferences = mLauncher.getSharedPreferences(QCPreference.PREFERENCE_NAME, Context.MODE_PRIVATE);
        //Log.e(TAG,"stripEmptyScreens  "+mWorkspaceScreens.size() +"  "+mScreenOrder.size() +  "homepage = "+preferences.getInt(QCPreference.KEY_DEFAULT_SCREEN,10));
        if(preferences.getInt(QCPreference.KEY_DEFAULT_SCREEN,0) > (mWorkspaceScreens.size() -1)){
            setHomePage(null,null,0);
        }

    }

    // See implementation for parameter definition.
    void addInScreen(View child, long container, long screenId,
            int x, int y, int spanX, int spanY) {
        addInScreen(child, container, screenId, x, y, spanX, spanY, false, false);
    }

    // At bind time, we use the rank (screenId) to compute x and y for hotseat items.
    // See implementation for parameter definition.
    void addInScreenFromBind(View child, long container, long screenId, int x, int y,
            int spanX, int spanY) {
        addInScreen(child, container, screenId, x, y, spanX, spanY, false, true);
    }

    // See implementation for parameter definition.
    void addInScreen(View child, long container, long screenId, int x, int y, int spanX, int spanY,
            boolean insert) {
        addInScreen(child, container, screenId, x, y, spanX, spanY, insert, false);
    }

    /**
     * Adds the specified child in the specified screen. The position and dimension of
     * the child are defined by x, y, spanX and spanY.
     *
     * @param child The child to add in one of the workspace's screens.
     * @param screenId The screen in which to add the child.
     * @param x The X position of the child in the screen's grid.
     * @param y The Y position of the child in the screen's grid.
     * @param spanX The number of cells spanned horizontally by the child.
     * @param spanY The number of cells spanned vertically by the child.
     * @param insert When true, the child is inserted at the beginning of the children list.
     * @param computeXYFromRank When true, we use the rank (stored in screenId) to compute
     *                          the x and y position in which to place hotseat items. Otherwise
     *                          we use the x and y position to compute the rank.
     */
    void addInScreen(View child, long container, long screenId, int x, int y, int spanX, int spanY,
            boolean insert, boolean computeXYFromRank) {
        if (container == LauncherSettings.Favorites.CONTAINER_DESKTOP) {
            if (getScreenWithId(screenId) == null) {
                Log.e(TAG, "Skipping child, screenId " + screenId + " not found");
                // DEBUGGING - Print out the stack trace to see where we are adding from
                new Throwable().printStackTrace();

				/// M: ALPS01669171, When the screen didn't exist, insert it.
				insertNewWorkspaceScreen(screenId);
				long maxScreenId = -1;
				for(int i = 0; i < mScreenOrder.size(); i++){
					if (maxScreenId < mScreenOrder.get(i)) {
						maxScreenId = mScreenOrder.get(i);
					}
				}
				LauncherAppState.getLauncherProvider().updateMaxScreenId(maxScreenId);
				
                mLauncher.getModel().updateWorkspaceScreenOrder(mLauncher, mScreenOrder);
                //return;
                /// M.
            }
        }
        if (screenId == EXTRA_EMPTY_SCREEN_ID) {
            // This should never happen
            throw new RuntimeException("Screen id should not be EXTRA_EMPTY_SCREEN_ID");
        }

        final CellLayout layout;
        if (container == LauncherSettings.Favorites.CONTAINER_HOTSEAT) {
            layout = mLauncher.getHotseat().getLayout();
            child.setOnKeyListener(new HotseatIconKeyEventListener());

            // Hide folder title in the hotseat
            if (child instanceof FolderIcon) {
                ((FolderIcon) child).setTextVisible(false);
            }

            if (computeXYFromRank) {
                x = mLauncher.getHotseat().getCellXFromOrder((int) screenId);
                y = mLauncher.getHotseat().getCellYFromOrder((int) screenId);
            } else {
                screenId = mLauncher.getHotseat().getOrderInHotseat(x, y);
            }
        } else {
            // Show folder title if not in the hotseat
            if (child instanceof FolderIcon) {
                ((FolderIcon) child).setTextVisible(true);
            }
            layout = getScreenWithId(screenId);
            child.setOnKeyListener(new IconKeyEventListener());
        }

        ViewGroup.LayoutParams genericLp = child.getLayoutParams();
        CellLayout.LayoutParams lp;
        if (genericLp == null || !(genericLp instanceof CellLayout.LayoutParams)) {
            lp = new CellLayout.LayoutParams(x, y, spanX, spanY);
        } else {
            lp = (CellLayout.LayoutParams) genericLp;
            lp.cellX = x;
            lp.cellY = y;
            lp.cellHSpan = spanX;
            lp.cellVSpan = spanY;
        }

        if (spanX < 0 && spanY < 0) {
            lp.isLockedToGrid = false;
        }

        // Get the canonical child id to uniquely represent this view in this screen
        ItemInfo info = (ItemInfo) child.getTag();
        int childId = mLauncher.getViewIdForItem(info);

        boolean markCellsAsOccupied = !(child instanceof Folder);
        if (!layout.addViewToCellLayout(child, insert ? 0 : -1, childId, lp, markCellsAsOccupied)) {
            // TODO: This branch occurs when the workspace is adding views
            // outside of the defined grid
            // maybe we should be deleting these items from the LauncherModel?
            Launcher.addDumpLog(TAG, "Failed to add to item at (" + lp.cellX + "," + lp.cellY + ") to CellLayout", true);
        }

        if (!(child instanceof Folder)) {
            child.setHapticFeedbackEnabled(false);
            child.setOnLongClickListener(mLongClickListener);
        }
        if (child instanceof DropTarget) {
            mDragController.addDropTarget((DropTarget) child);
        }
    }

    /**
     * Called directly from a CellLayout (not by the framework), after we've been added as a
     * listener via setOnInterceptTouchEventListener(). This allows us to tell the CellLayout
     * that it should intercept touch events, which is not something that is normally supported.
     */
    @Override
    public boolean onTouch(View v, MotionEvent event) {
        if (LauncherLog.DEBUG_MOTION) {
            LauncherLog.d(TAG, "onTouch: v = " + v + ", event = " + event + ", isFinishedSwitchingState() = "
                    + isFinishedSwitchingState() + ", mState = " + mState + ", mScrollX = " + getScrollX());
        }
        return (mLauncher.getIsSwitchAnimationing() || workspaceInModalState() || !isFinishedSwitchingState())
                || (!workspaceInModalState() && indexOfChild(v) != mCurrentPage);
    }

    public boolean isSwitchingState() {
        return mIsSwitchingState;
    }

    /** This differs from isSwitchingState in that we take into account how far the transition
     *  has completed. */
    public boolean isFinishedSwitchingState() {
        return !mIsSwitchingState || (mTransitionProgress > 0.5f);
    }

    protected void onWindowVisibilityChanged (int visibility) {
        mLauncher.onWindowVisibilityChanged(visibility);
    }

    @Override
    public boolean dispatchUnhandledMove(View focused, int direction) {
        if (workspaceInModalState() || !isFinishedSwitchingState()) {
            // when the home screens are shrunken, shouldn't allow side-scrolling
            return false;
        }
        return super.dispatchUnhandledMove(focused, direction);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        if (LauncherLog.DEBUG_MOTION) {
            LauncherLog.d(TAG, "onInterceptTouchEvent: ev = " + ev + ", mScrollX = " + getScrollX());
        }
        if (mLauncher.getIsSwitchAnimationing()) {
			return true;
		}
        switch (ev.getAction() & MotionEvent.ACTION_MASK) {
        case MotionEvent.ACTION_DOWN:
            mXDown = ev.getX();
            mYDown = ev.getY();
            mTouchDownTime = System.currentTimeMillis();
            break;
        case MotionEvent.ACTION_POINTER_UP:
        case MotionEvent.ACTION_UP:
            if (mTouchState == TOUCH_STATE_REST) {
                final CellLayout currentPage = (CellLayout) getChildAt(mCurrentPage);
                if (currentPage != null && !currentPage.lastDownOnOccupiedCell()) {
                    onWallpaperTap(ev);
                }
            }
        }
        return super.onInterceptTouchEvent(ev);
    }

    @Override
    public boolean onGenericMotionEvent(MotionEvent event) {
        // Ignore pointer scroll events if the custom content doesn't allow scrolling.
        if ((getScreenIdForPageIndex(getCurrentPage()) == CUSTOM_CONTENT_SCREEN_ID)
                && (mCustomContentCallbacks != null)
                && !mCustomContentCallbacks.isScrollingAllowed()) {
            return false;
        }
        return super.onGenericMotionEvent(event);
    }

    protected void reinflateWidgetsIfNecessary() {
        final int clCount = getChildCount();
        for (int i = 0; i < clCount; i++) {
            CellLayout cl = (CellLayout) getChildAt(i);
            ShortcutAndWidgetContainer swc = cl.getShortcutsAndWidgets();
            final int itemCount = swc.getChildCount();
            for (int j = 0; j < itemCount; j++) {
                View v = swc.getChildAt(j);

                if (v != null  && v.getTag() instanceof LauncherAppWidgetInfo) {
                    LauncherAppWidgetInfo info = (LauncherAppWidgetInfo) v.getTag();
                    LauncherAppWidgetHostView lahv = (LauncherAppWidgetHostView) info.hostView;
                    if (lahv != null && lahv.isReinflateRequired()) {
                        mLauncher.removeAppWidget(info);
                        // Remove the current widget which is inflated with the wrong orientation
                        cl.removeView(lahv);
                        mLauncher.bindAppWidget(info);
                    }
                }
            }
        }
    }

    @Override
    protected void determineScrollingStart(MotionEvent ev) {
        if (!isFinishedSwitchingState()) return;

        float deltaX = ev.getX() - mXDown;
        float absDeltaX = Math.abs(deltaX);
        float absDeltaY = Math.abs(ev.getY() - mYDown);

        if (Float.compare(absDeltaX, 0f) == 0) return;

        float slope = absDeltaY / absDeltaX;
        float theta = (float) Math.atan(slope);

        if (absDeltaX > mTouchSlop || absDeltaY > mTouchSlop) {
            cancelCurrentPageLongPress();
        }

        boolean passRightSwipesToCustomContent =
                (mTouchDownTime - mCustomContentShowTime) > CUSTOM_CONTENT_GESTURE_DELAY;

        boolean swipeInIgnoreDirection = isLayoutRtl() ? deltaX < 0 : deltaX > 0;
        boolean onCustomContentScreen =
                getScreenIdForPageIndex(getCurrentPage()) == CUSTOM_CONTENT_SCREEN_ID;
        if (swipeInIgnoreDirection && onCustomContentScreen && passRightSwipesToCustomContent) {
            // Pass swipes to the right to the custom content page.
            return;
        }

        if (onCustomContentScreen && (mCustomContentCallbacks != null)
                && !mCustomContentCallbacks.isScrollingAllowed()) {
            // Don't allow workspace scrolling if the current custom content screen doesn't allow
            // scrolling.
            return;
        }

        if (theta > MAX_SWIPE_ANGLE) {
            // Above MAX_SWIPE_ANGLE, we don't want to ever start scrolling the workspace
            return;
        } else if (theta > START_DAMPING_TOUCH_SLOP_ANGLE) {
            // Above START_DAMPING_TOUCH_SLOP_ANGLE and below MAX_SWIPE_ANGLE, we want to
            // increase the touch slop to make it harder to begin scrolling the workspace. This
            // results in vertically scrolling widgets to more easily. The higher the angle, the
            // more we increase touch slop.
            theta -= START_DAMPING_TOUCH_SLOP_ANGLE;
            float extraRatio = (float)
                    Math.sqrt((theta / (MAX_SWIPE_ANGLE - START_DAMPING_TOUCH_SLOP_ANGLE)));
            super.determineScrollingStart(ev, 1 + TOUCH_SLOP_DAMPING_FACTOR * extraRatio);
        } else {
            // Below START_DAMPING_TOUCH_SLOP_ANGLE, we don't do anything special
            /// M: [Performance] Reduce page moving threshold to improve response time.
            super.determineScrollingStart(ev, 0.5f);
        }
    }

    protected void onPageBeginMoving() {
        super.onPageBeginMoving();

        if (isHardwareAccelerated()) {
            updateChildrenLayersEnabled(false);
        } else {

            if (mNextPage != INVALID_PAGE) {
                // we're snapping to a particular screen
                enableChildrenCache(mCurrentPage, mNextPage);
            } else {
                // this is when user is actively dragging a particular screen, they might
                // swipe it either left or right (but we won't advance by more than one screen)
                enableChildrenCache(mCurrentPage - 1, mCurrentPage + 1);
            }
        }
    }

    protected void onPageEndMoving() {
        super.onPageEndMoving();

        if (isHardwareAccelerated()) {
            updateChildrenLayersEnabled(false);
        } else {
            clearChildrenCache();
        }

        if (mDragController.isDragging()) {
            if (workspaceInModalState()) {
                // If we are in springloaded mode, then force an event to check if the current touch
                // is under a new page (to scroll to)
                mDragController.forceTouchMove();
            }
        }

        if (mDelayedResizeRunnable != null) {
        	postDelayed(new Runnable() {
				@Override
				public void run() {
					mDelayedResizeRunnable.run();
		            mDelayedResizeRunnable = null;
				}
			}, getResources().getInteger(R.integer.config_appsCustomizeWorkspaceShrinkTime));
            
        }

        if (mDelayedSnapToPageRunnable != null) {
            mDelayedSnapToPageRunnable.run();
            mDelayedSnapToPageRunnable = null;
        }
        if (mStripScreensOnPageStopMoving) {
            stripEmptyScreens();
            mStripScreensOnPageStopMoving = false;
        }
    }

    @Override
    protected void notifyPageSwitchListener() {
        super.notifyPageSwitchListener();
        Launcher.setScreen(getNextPage());

        if (hasCustomContent() && getNextPage() == 0 && !mCustomContentShowing) {
            mCustomContentShowing = true;
            if (mCustomContentCallbacks != null) {
                mCustomContentCallbacks.onShow(false);
                mCustomContentShowTime = System.currentTimeMillis();
                mLauncher.updateVoiceButtonProxyVisible(false);
            }
        } else if (hasCustomContent() && getNextPage() != 0 && mCustomContentShowing) {
            mCustomContentShowing = false;
            if (mCustomContentCallbacks != null) {
                mCustomContentCallbacks.onHide();
                mLauncher.resetQSBScroll();
                mLauncher.updateVoiceButtonProxyVisible(false);
            }
        }
    }

    protected CustomContentCallbacks getCustomContentCallbacks() {
        return mCustomContentCallbacks;
    }

    protected void setWallpaperDimension() {
        new AsyncTask<Void, Void, Void>() {
            public Void doInBackground(Void ... args) {

                String spKey = WallpaperCropActivity.getSharedPreferencesKey();
                SharedPreferences sp =
                        mLauncher.getSharedPreferences(spKey, Context.MODE_MULTI_PROCESS);
                LauncherWallpaperPickerActivity.suggestWallpaperDimension(mLauncher.getResources(),
                        sp, mLauncher.getWindowManager(), mWallpaperManager,
                        mLauncher.overrideWallpaperDimensions());
                return null;
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, (Void) null);
    }

    protected void snapToPage(int whichPage, Runnable r) {
        snapToPage(whichPage, SLOW_PAGE_SNAP_ANIMATION_DURATION, r);
    }

    protected void snapToPage(int whichPage, int duration, Runnable r) {
        if (mDelayedSnapToPageRunnable != null) {
            mDelayedSnapToPageRunnable.run();
        }
        mDelayedSnapToPageRunnable = r;
        snapToPage(whichPage, duration);
    }

    public void snapToScreenId(long screenId) {
        snapToScreenId(screenId, null);
    }

    protected void snapToScreenId(long screenId, Runnable r) {
        snapToPage(getPageIndexForScreenId(screenId), r);
    }

    @Override
    protected void snapToPage(int whichPage, int delta, int duration,
    		boolean immediate, TimeInterpolator interpolator) {
    	super.snapToPage(whichPage, delta, duration, immediate, interpolator);
    	
    	if(currentIndex != getNextPage()){
    		RefreshPageView( getNextPage());
    	}
    	if(!(mState == State.NORMAL) ){
    		showWsLongPageView(getNextPage());
    	}
    }

    @Override
    protected void snapToPage(int whichPage, int delta, int duration) {
    	super.snapToPage(whichPage, delta, duration);
    	
       if(mIsDragOccuring && mState == State.SPRING_LOADED){
    	   return ;
       }
    	
       changeCurrentPageBottomBar(whichPage);
    }
    
    private void changeCurrentPageBottomBar(int whichPage) {
    	CellLayout cell = (CellLayout) getChildAt(whichPage);
    	if(cell.isPiflow()){
    		showBottomBar(false);
    		mLauncher.showStatusBar(false);
    	} else {
    		showBottomBar(true);
    		mLauncher.showStatusBar(true);
    	}
	}
    private void showBottomBar(boolean flag) {}

    
    class WallpaperOffsetInterpolator implements Choreographer.FrameCallback {
        float mFinalOffset = 0.0f;
        float mCurrentOffset = 0.5f; // to force an initial update
        boolean mWaitingForUpdate;
        Choreographer mChoreographer;
        Interpolator mInterpolator;
        boolean mAnimating;
        boolean mIsMovingFast;
        long mLastWallpaperOffsetUpdateTime;
        long mAnimationStartTime;
        float mAnimationStartOffset;
        private final int ANIMATION_DURATION = 250;
        // Don't use all the wallpaper for parallax until you have at least this many pages
        private final int MIN_PARALLAX_PAGE_SPAN = 2;
        int mNumScreens;

        public WallpaperOffsetInterpolator() {
            mChoreographer = Choreographer.getInstance();
            mInterpolator = new DecelerateInterpolator(1.5f);
        }

        @Override
        public void doFrame(long frameTimeNanos) {
            updateOffset(false);
        }

        private void updateOffset(boolean force) {
            if (mWaitingForUpdate || force) {
                mWaitingForUpdate = false;
                if (computeScrollOffset() && mWindowToken != null) {
                    try {
                        mWallpaperManager.setWallpaperOffsets(mWindowToken,
                                mWallpaperOffset.getCurrX(), 0.5f);
                        setWallpaperOffsetSteps();
                    } catch (IllegalArgumentException e) {
                        Log.e(TAG, "Error updating wallpaper offset: " + e);
                    }
                }
            }
        }

        public boolean computeScrollOffset() {
        	if (QCConfig.supportSlideEffect) {
                //if (getScrollX() <0 || getScrollX() > mMaxScrollX || Float.compare(getCurrX(), getFinalX()) == 0) {
                if (Float.compare(getCurrX(), getFinalX()) == 0) {
                    mIsMovingFast = false;
                    return false;
                }
                boolean isLandscape = mDisplaySize.x > mDisplaySize.y;

                long currentTime = System.currentTimeMillis();
                long timeSinceLastUpdate = currentTime - mLastWallpaperOffsetUpdateTime;
                timeSinceLastUpdate = Math.min((long) (1000/30f), timeSinceLastUpdate);
                timeSinceLastUpdate = Math.max(1L, timeSinceLastUpdate);

                float xdiff = Math.abs(getFinalX() - getCurrX());
                if (!mIsMovingFast && xdiff > 0.07) {
                    mIsMovingFast = true;
                }

                float fractionToCatchUpIn1MsHorizontal;
                if (mIsMovingFast) {
                    fractionToCatchUpIn1MsHorizontal = isLandscape ? 0.5f : 0.75f;
                } else {
                    // slow
                    fractionToCatchUpIn1MsHorizontal = isLandscape ? 0.27f : 0.5f;
                }

                fractionToCatchUpIn1MsHorizontal /= 33f;

                final float UPDATE_THRESHOLD = 0.00001f;
                float hOffsetDelta = getFinalX() - getCurrX();
                boolean jumpToFinalValue = Math.abs(hOffsetDelta) < UPDATE_THRESHOLD;

                // Don't have any lag between workspace and wallpaper on non-large devices
                if (jumpToFinalValue) {
                    mCurrentOffset = mFinalOffset;
                } else {
                    float percentToCatchUpHorizontal =
                            Math.min(1.0f, timeSinceLastUpdate * fractionToCatchUpIn1MsHorizontal);
                    mCurrentOffset += percentToCatchUpHorizontal * hOffsetDelta;
                }

                mLastWallpaperOffsetUpdateTime = System.currentTimeMillis();
                return true;
				
			} else {
	            final float oldOffset = mCurrentOffset;
	            if (mAnimating) {
	                long durationSinceAnimation = System.currentTimeMillis() - mAnimationStartTime;
	                float t0 = durationSinceAnimation / (float) ANIMATION_DURATION;
	                float t1 = mInterpolator.getInterpolation(t0);
	                mCurrentOffset = mAnimationStartOffset +
	                        (mFinalOffset - mAnimationStartOffset) * t1;
	                mAnimating = durationSinceAnimation < ANIMATION_DURATION;
	            } else {
	                mCurrentOffset = mFinalOffset;
	            }

	            if (Math.abs(mCurrentOffset - mFinalOffset) > 0.0000001f) {
	                scheduleUpdate();
	            }
	            if (Math.abs(oldOffset - mCurrentOffset) > 0.0000001f) {
	                return true;
	            }
	            return false;
				
			}
        
        }

        private float wallpaperOffsetForCurrentScroll() {
            if (getChildCount() <= 1) {
                return 0;
            }

            // Exclude the leftmost page
            int emptyExtraPages = numEmptyScreensToIgnore();
            int firstIndex = numCustomPages();
            // Exclude the last extra empty screen (if we have > MIN_PARALLAX_PAGE_SPAN pages)
            int lastIndex = getChildCount() - 1 - emptyExtraPages;
            if (isLayoutRtl()) {
                int temp = firstIndex;
                firstIndex = lastIndex;
                lastIndex = temp;
            }

            int firstPageScrollX = getScrollForPage(firstIndex);
            int lastPageScrollX = getScrollForPage(lastIndex);
            int scrollRange = getScrollForPage(lastIndex) - firstPageScrollX;
            if (scrollRange == 0) {
                return 0;
            } else {
                // TODO: do different behavior if it's  a live wallpaper?
                // Sometimes the left parameter of the pages is animated during a layout transition;
                // this parameter offsets it to keep the wallpaper from animating as well
                // Again, we adjust the wallpaper offset to be consistent between values of mLayoutScale
                int scrollX = getScrollX();

                if (isSupportCycleSlidingScreen()) {
                    if (scrollX > lastPageScrollX) {
                        int offset = scrollX - lastPageScrollX;
                        scrollX = (int) ((lastIndex) * getWidth() * (1 - ((float) offset)
                                / getWidth()));
                    } else if (scrollX < firstPageScrollX) {
                        scrollX = (lastIndex) * (-scrollX);
                    }
                }

                //int adjustedScroll = Math.max(0, Math.min(scrollX, mMaxScrollX)) - firstPageScrollX - getLayoutTransitionOffsetForPage(0);
                int adjustedScroll = Math.max(0, Math.min(scrollX-firstPageScrollX, scrollRange));

                float offset = Math.min(1, adjustedScroll / (float) scrollRange);

                offset = Math.max(0, offset);
                // Don't use up all the wallpaper parallax until you have at least
                // MIN_PARALLAX_PAGE_SPAN pages
                int numScrollingPages = getNumScreensExcludingEmptyAndCustom();
                int parallaxPageSpan;
                if (mWallpaperIsLiveWallpaper) {
                    parallaxPageSpan = numScrollingPages - 1;
                } else {
                    parallaxPageSpan = Math.max(MIN_PARALLAX_PAGE_SPAN-1, numScrollingPages - 1);
                }
                mNumPagesForWallpaperParallax = parallaxPageSpan;

                int padding = isLayoutRtl() ? parallaxPageSpan - numScrollingPages + 1 : 0;
                
                return offset * (padding + numScrollingPages - 1) / parallaxPageSpan;
            }
        }

        private int numEmptyScreensToIgnore() {
            int numScrollingPages = getChildCount() - numCustomPages();
            if (hasExtraEmptyScreen()) {
                return 1;
            } else {
                return 0;
            }
        }

        private int getNumScreensExcludingEmptyAndCustom() {
            int numScrollingPages = getChildCount() - numEmptyScreensToIgnore() - numCustomPages();
            return numScrollingPages;
        }

        public void syncWithScroll() {
            float offset = wallpaperOffsetForCurrentScroll();
            mWallpaperOffset.setFinalX(offset);
            updateOffset(true);
        }

        public float getCurrX() {
            return mCurrentOffset;
        }

        public float getFinalX() {
            return mFinalOffset;
        }

        private void animateToFinal() {
            mAnimating = true;
            mAnimationStartOffset = mCurrentOffset;
            mAnimationStartTime = System.currentTimeMillis();
        }

        private void setWallpaperOffsetSteps() {
            // Set wallpaper offset steps (1 / (number of screens - 1))
        	float xOffset = 1.0f / mNumPagesForWallpaperParallax;
            if (xOffset != mLastSetWallpaperOffsetSteps) {
                mWallpaperManager.setWallpaperOffsetSteps(xOffset, 1.0f);
                mLastSetWallpaperOffsetSteps = xOffset;
            }
        }

        public void setFinalX(float x) {
            scheduleUpdate();
            mFinalOffset = Math.max(0f, Math.min(x, 1.0f));
            if (getNumScreensExcludingEmptyAndCustom() != mNumScreens) {
                if (mNumScreens > 0) {
                    // Don't animate if we're going from 0 screens
                    animateToFinal();
                }
                mNumScreens = getNumScreensExcludingEmptyAndCustom();
            }
        }

        private void scheduleUpdate() {
            if (!mWaitingForUpdate) {
                mChoreographer.postFrameCallback(this);
                mWaitingForUpdate = true;
            }
        }

        public void jumpToFinal() {
            mCurrentOffset = mFinalOffset;
        }
    }

    @Override
    public void computeScroll() {
        super.computeScroll();
        mWallpaperOffset.syncWithScroll();
    }

    @Override
    public void announceForAccessibility(CharSequence text) {
        // Don't announce if apps is on top of us.
        if (!mLauncher.isAllAppsVisible()) {
            super.announceForAccessibility(text);
        }
    }

    void showOutlines() {
        if (!workspaceInModalState() && !mIsSwitchingState) {
            if (mChildrenOutlineFadeOutAnimation != null) mChildrenOutlineFadeOutAnimation.cancel();
            if (mChildrenOutlineFadeInAnimation != null) mChildrenOutlineFadeInAnimation.cancel();
            mChildrenOutlineFadeInAnimation = LauncherAnimUtils.ofFloat(this, "childrenOutlineAlpha", 1.0f);
            mChildrenOutlineFadeInAnimation.setDuration(CHILDREN_OUTLINE_FADE_IN_DURATION);
            mChildrenOutlineFadeInAnimation.start();
        }
    }

    void hideOutlines() {
        if (!workspaceInModalState() && !mIsSwitchingState) {
            if (mChildrenOutlineFadeInAnimation != null) mChildrenOutlineFadeInAnimation.cancel();
            if (mChildrenOutlineFadeOutAnimation != null) mChildrenOutlineFadeOutAnimation.cancel();
            mChildrenOutlineFadeOutAnimation = LauncherAnimUtils.ofFloat(this, "childrenOutlineAlpha", 0.0f);
            mChildrenOutlineFadeOutAnimation.setDuration(CHILDREN_OUTLINE_FADE_OUT_DURATION);
            mChildrenOutlineFadeOutAnimation.setStartDelay(CHILDREN_OUTLINE_FADE_OUT_DELAY);
            mChildrenOutlineFadeOutAnimation.start();
        }
    }

    public void showOutlinesTemporarily() {
        if (isSupportCycleSlidingScreen()) {
            return;
        }
        if (!mIsPageMoving && !isTouchActive()) {
            snapToPage(mCurrentPage);
        }
    }

    public void setChildrenOutlineAlpha(float alpha) {
        mChildrenOutlineAlpha = alpha;
    }

    public float getChildrenOutlineAlpha() {
        return mChildrenOutlineAlpha;
    }

    private void animateBackgroundGradient(float finalAlpha, boolean animated) {
        final DragLayer dragLayer = mLauncher.getDragLayer();

        if (mBackgroundFadeInAnimation != null) {
            mBackgroundFadeInAnimation.cancel();
            mBackgroundFadeInAnimation = null;
        }
        if (mBackgroundFadeOutAnimation != null) {
            mBackgroundFadeOutAnimation.cancel();
            mBackgroundFadeOutAnimation = null;
        }
        float startAlpha = dragLayer.getBackgroundAlpha();
        if (finalAlpha != startAlpha) {
            if (animated) {
                mBackgroundFadeOutAnimation =
                        LauncherAnimUtils.ofFloat(this, startAlpha, finalAlpha);
                mBackgroundFadeOutAnimation.addUpdateListener(new AnimatorUpdateListener() {
                    public void onAnimationUpdate(ValueAnimator animation) {
                        dragLayer.setBackgroundAlpha(
                                ((Float)animation.getAnimatedValue()).floatValue());
                    }
                });
                mBackgroundFadeOutAnimation.setInterpolator(new DecelerateInterpolator(1.5f));
                mBackgroundFadeOutAnimation.setDuration(BACKGROUND_FADE_OUT_DURATION);
                mBackgroundFadeOutAnimation.start();
            } else {
                dragLayer.setBackgroundAlpha(finalAlpha);
            }
        }
    }

    float backgroundAlphaInterpolator(float r) {
        float pivotA = 0.1f;
        float pivotB = 0.4f;
        if (r < pivotA) {
            return 0;
        } else if (r > pivotB) {
            return 1.0f;
        } else {
            return (r - pivotA)/(pivotB - pivotA);
        }
    }

    private void updatePageAlphaValues(int screenCenter) {
        boolean isInOverscroll = mOverScrollX < mShouldStartScrollX || mOverScrollX > mShouldEndScrollX;
        if (mWorkspaceFadeInAdjacentScreens &&
                !workspaceInModalState() &&
                !mIsSwitchingState &&
                !isInOverscroll) {
            for (int i = numCustomPages(); i < getChildCount(); i++) {
                CellLayout child = (CellLayout) getChildAt(i);
                if (child != null) {
                    float scrollProgress = getScrollProgress(screenCenter, child, i);
                    float alpha = QCConfig.defaultAlpha - Math.abs(scrollProgress);
                    child.getShortcutsAndWidgets().setAlpha(alpha);
                }
            }
        }
    }

    private void setChildrenBackgroundAlphaMultipliers(float a) {
        for (int i = 0; i < getChildCount(); i++) {
            CellLayout child = (CellLayout) getChildAt(i);
            child.setBackgroundAlphaMultiplier(a);
        }
    }

    public boolean hasCustomContent() {
        return (mScreenOrder.size() > 0 && mScreenOrder.get(0) == CUSTOM_CONTENT_SCREEN_ID);
    }

    public int numCustomPages() {
        return hasCustomContent() ? 1 : 0;
    }

    public boolean isOnOrMovingToCustomContent() {
        return hasCustomContent() && getNextPage() == 0;
    }

    private void updateStateForCustomContent(int screenCenter) {
        float translationX = 0;
        float progress = 0;
        if (hasCustomContent()) {
            int index = mScreenOrder.indexOf(CUSTOM_CONTENT_SCREEN_ID);

            int scrollDelta = getScrollX() - getScrollForPage(index) -
                    getLayoutTransitionOffsetForPage(index);
            float scrollRange = getScrollForPage(index + 1) - getScrollForPage(index);
            translationX = scrollRange - scrollDelta;
            progress = (scrollRange - scrollDelta) / scrollRange;

            if (isLayoutRtl()) {
                translationX = Math.min(0, translationX);
            } else {
                translationX = Math.max(0, translationX);
            }
            progress = Math.max(0, progress);
        }

        if (Float.compare(progress, mLastCustomContentScrollProgress) == 0) return;

        CellLayout cc = mWorkspaceScreens.get(CUSTOM_CONTENT_SCREEN_ID);
        if (progress > 0 && cc.getVisibility() != VISIBLE && !workspaceInModalState()) {
            cc.setVisibility(VISIBLE);
        }

        mLastCustomContentScrollProgress = progress;

        if(mLauncher.getFakeView() != null){
            mLauncher.getFakeView().setTranslationX(translationX);
        }

        if (mLauncher.getHotseat() != null) {
            mLauncher.getHotseat().setTranslationX(translationX);
        }

        if (getPageIndicator() != null) {
            getPageIndicator().setTranslationX(translationX);
        }

        if (mCustomContentCallbacks != null) {
            mCustomContentCallbacks.onScrollProgressChanged(progress);
        }
    }

    @Override
    protected OnClickListener getPageIndicatorClickListener() {
        AccessibilityManager am = (AccessibilityManager)
                getContext().getSystemService(Context.ACCESSIBILITY_SERVICE);
        if (!am.isTouchExplorationEnabled()) {
            return null;
        }
        OnClickListener listener = new OnClickListener() {
            @Override
            public void onClick(View arg0) {
                enterOverviewMode();
            }
        };
        return listener;
    }

    @Override
    protected void screenScrolled(int screenCenter) {
        final boolean isRtl = isLayoutRtl();
        super.screenScrolled(screenCenter);

        updatePageAlphaValues(screenCenter);
        updateStateForCustomContent(screenCenter);
        enableHwLayersOnVisiblePages();

        boolean shouldOverScroll = mOverScrollX < mShouldStartScrollX || mOverScrollX > mShouldEndScrollX;

        if (getChildCount() == 0) {
            return;
        }

        final int lowerIndex = ((mState!=State.NORMAL||isNormalScaling) ? numCustomPages() : 0);
        final int upperIndex = (mState != State.NORMAL ?
                getChildCount() - 1 :
                (getChildCount() - 1 - (hasExtraEmptyScreen() ? 1 : 0)));
        if (shouldOverScroll) {
            int index;

            final boolean isLeftPage = mOverScrollX < mShouldStartScrollX;
            index = (!isRtl && isLeftPage) || (isRtl && !isLeftPage) ? lowerIndex : upperIndex;

            CellLayout cl = (CellLayout) getChildAt(index);
            float effect = Math.abs(mOverScrollEffect);
            cl.setOverScrollAmount(Math.abs(effect), isLeftPage);

            mOverscrollEffectSet = true;
        } else {
            if (mOverscrollEffectSet && getChildCount() > 0) {
                mOverscrollEffectSet = false;
                CellLayout lowerLayout = (CellLayout) getChildAt(lowerIndex);
                if (lowerLayout != null) {
                    lowerLayout.setOverScrollAmount(0, false);
                }
                CellLayout upperLayout = (CellLayout) getChildAt(upperIndex);
                if (upperLayout != null) {
                    upperLayout.setOverScrollAmount(0, false);
                }
            }
        }
    }

    @Override
    protected void overScroll(float amount) {
        boolean shouldOverScroll = true;

        if (shouldOverScroll) {
            dampedOverScroll(amount);
            mOverScrollEffect = acceleratedOverFactor(amount);
        } else {
            mOverScrollEffect = 0;
        }
    }

    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        mWindowToken = getWindowToken();
        computeScroll();
        mDragController.setWindowToken(mWindowToken);
    }

    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        mWindowToken = null;
    }

    protected void onResume() {
        if (getPageIndicator() != null) {
            // In case accessibility state has changed, we need to perform this on every
            // attach to window
            OnClickListener listener = getPageIndicatorClickListener();
            if (listener != null) {
                getPageIndicator().setOnClickListener(listener);
            }
        }
        AccessibilityManager am = (AccessibilityManager)
                getContext().getSystemService(Context.ACCESSIBILITY_SERVICE);
        sAccessibilityEnabled = am.isEnabled();

        // Update wallpaper dimensions if they were changed since last onResume
        // (we also always set the wallpaper dimensions in the constructor)
        if (LauncherAppState.getInstance().hasWallpaperChangedSinceLastCheck()) {
            setWallpaperDimension();
        }
        mWallpaperIsLiveWallpaper = mWallpaperManager.getWallpaperInfo() != null;
        // Force the wallpaper offset steps to be set again, because another app might have changed
        // them
        mLastSetWallpaperOffsetSteps = 0f;


        if (mFreeScroll == true) {
            disableFreeScroll();
        }

    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        if (mFirstLayout && mCurrentPage >= 0 && mCurrentPage < getChildCount()) {
            mWallpaperOffset.syncWithScroll();
            mWallpaperOffset.jumpToFinal();
        }
        if(getChildCount() == 0){
            return;
        }
        super.onLayout(changed, left, top, right, bottom);
        int firstNormalChildPageNumb = numCustomPages();
        int lastNormalChildPageNumb = getChildCount()-1-(hasExtraEmptyScreen()?1:0);
        int firstNormalChildScrollX = getScrollForPage(firstNormalChildPageNumb);
        int lastNormalChildScrollX = getScrollForPage(lastNormalChildPageNumb);
        if (mState != State.NORMAL) {
        	mShouldStartPageNumb = firstNormalChildPageNumb;
        	mShouldEndPageNumb = getChildCount()-1;
			mShouldStartScrollX = firstNormalChildScrollX;
			mShouldEndScrollX = mMaxScrollX;
		} else if (mState == State.NORMAL && isNormalScaling) {
        	mShouldStartPageNumb = firstNormalChildPageNumb;
        	mShouldEndPageNumb = lastNormalChildPageNumb;
			mShouldStartScrollX = firstNormalChildScrollX;
			mShouldEndScrollX = lastNormalChildScrollX;
		} else if (mState == State.NORMAL && !isNormalScaling) {
        	mShouldStartPageNumb = 0;
        	mShouldEndPageNumb = lastNormalChildPageNumb;
			mShouldStartScrollX = 0;
			mShouldEndScrollX = lastNormalChildScrollX;
		}

        if (LauncherLog.DEBUG_LAYOUT) {
            LauncherLog.d(TAG, "onLayout: changed = " + changed + ", left = " + left
                    + ", top = " + top + ", right = " + right + ", bottom = " + bottom
                    + ", mState==State.NORMAL?" + (mState==State.NORMAL));

            LauncherLog.d(TAG, "onLayout: mShouldStartPageNumb = " + mShouldStartPageNumb + ", mShouldEndPageNumb = " + mShouldEndPageNumb
                    + ", mShouldStartScrollX = " + mShouldStartScrollX + ", mShouldEndScrollX = " + mShouldEndScrollX + ", isNormalScaling = " + isNormalScaling
                    + ", mState==State.NORMAL?" + (mState==State.NORMAL));
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        post(mBindPages);
    }

    @Override
    protected boolean onRequestFocusInDescendants(int direction, Rect previouslyFocusedRect) {
        if (!mLauncher.isAllAppsVisible()) {
            final Folder openFolder = getOpenFolder();
            if (openFolder != null) {
                return openFolder.requestFocus(direction, previouslyFocusedRect);
            } else {
                return super.onRequestFocusInDescendants(direction, previouslyFocusedRect);
            }
        }
        return false;
    }

    @Override
    public int getDescendantFocusability() {
        if (workspaceInModalState()) {
            return ViewGroup.FOCUS_BLOCK_DESCENDANTS;
        }
        return super.getDescendantFocusability();
    }

    @Override
    public void addFocusables(ArrayList<View> views, int direction, int focusableMode) {
        if (!mLauncher.isAllAppsVisible()) {
            final Folder openFolder = getOpenFolder();
            if (openFolder != null) {
                openFolder.addFocusables(views, direction);
            } else {
                super.addFocusables(views, direction, focusableMode);
            }
        }
    }

    public boolean workspaceInModalState() {
        return false;
    }

    void enableChildrenCache(int fromPage, int toPage) {
        if (fromPage > toPage) {
            final int temp = fromPage;
            fromPage = toPage;
            toPage = temp;
        }

        final int screenCount = getChildCount();

        fromPage = Math.max(fromPage, 0);
        toPage = Math.min(toPage, screenCount - 1);

        for (int i = fromPage; i <= toPage; i++) {
            final CellLayout layout = (CellLayout) getChildAt(i);
            layout.setChildrenDrawnWithCacheEnabled(true);
            layout.setChildrenDrawingCacheEnabled(true);
        }
    }

    void clearChildrenCache() {
        final int screenCount = getChildCount();
        for (int i = 0; i < screenCount; i++) {
            final CellLayout layout = (CellLayout) getChildAt(i);
            layout.setChildrenDrawnWithCacheEnabled(false);
            // In software mode, we don't want the items to continue to be drawn into bitmaps
            if (!isHardwareAccelerated()) {
                layout.setChildrenDrawingCacheEnabled(false);
            }
        }
    }

    /// M: [Performance] Set HW layer on down event to improve response time.
    public void updateChildrenLayersEnabled(boolean force) {
        boolean small = mState == State.OVERVIEW || mIsSwitchingState;
        boolean enableChildrenLayers = force || small || mAnimatingViewIntoPlace || isPageMoving();

        if (enableChildrenLayers != mChildrenLayersEnabled) {
            mChildrenLayersEnabled = enableChildrenLayers;
            if (mChildrenLayersEnabled) {
                enableHwLayersOnVisiblePages();
            } else {
                for (int i = 0; i < getPageCount(); i++) {
                    final CellLayout cl = (CellLayout) getChildAt(i);
                    cl.enableHardwareLayer(false);
                }
            }
        }
    }

    private void enableHwLayersOnVisiblePages() {
        if (mChildrenLayersEnabled) {
            final int screenCount = getChildCount();
            getVisiblePages(mTempVisiblePagesRange);
            int leftScreen = mTempVisiblePagesRange[0];
            int rightScreen = mTempVisiblePagesRange[1];
            if (leftScreen == rightScreen) {
                // make sure we're caching at least two pages always
                if (rightScreen < screenCount - 1) {
                    rightScreen++;
                } else if (leftScreen > 0) {
                    leftScreen--;
                }
            }

            final CellLayout customScreen = mWorkspaceScreens.get(CUSTOM_CONTENT_SCREEN_ID);
            for (int i = 0; i < screenCount; i++) {
                final CellLayout layout = (CellLayout) getPageAt(i);

                // enable layers between left and right screen inclusive, except for the
                // customScreen, which may animate its content during transitions.
                
                //boolean enableLayer = layout != customScreen &&
                //        leftScreen <= i && i <= rightScreen && shouldDrawChild(layout);
                /// M: enable drawing cache when in cycle sliding bounds.
                boolean enableLayer = layout != customScreen
                        && shouldEnableDrawingCache(i, leftScreen, rightScreen)
                        && shouldDrawChild(layout);
                if (LauncherLog.DEBUG_DRAW) {
                    LauncherLog.d(TAG, "enableHwLayersOnVisiblePages 1: i = " + i
                            + ",enableLayer = " + enableLayer + ",leftScreen = " + leftScreen
                            + ", rightScreen = " + rightScreen + ", screenCount = " + screenCount
                            + ", customScreen = " + customScreen + ",shouldDrawChild(layout) = "
                            + shouldDrawChild(layout));
                }
                layout.enableHardwareLayer(enableLayer);
            }
        }
    }

    public void buildPageHardwareLayers() {
        // force layers to be enabled just for the call to buildLayer
        updateChildrenLayersEnabled(true);
        if (getWindowToken() != null) {
            final int childCount = getChildCount();
            for (int i = 0; i < childCount; i++) {
                CellLayout cl = (CellLayout) getChildAt(i);
                cl.buildHardwareLayer();
            }
        }
        updateChildrenLayersEnabled(false);
    }

    protected void onWallpaperTap(MotionEvent ev) {
        final int[] position = mTempCell;
        getLocationOnScreen(position);

        int pointerIndex = ev.getActionIndex();
        position[0] += (int) ev.getX(pointerIndex);
        position[1] += (int) ev.getY(pointerIndex);

        mWallpaperManager.sendWallpaperCommand(getWindowToken(),
                ev.getAction() == MotionEvent.ACTION_UP
                        ? WallpaperManager.COMMAND_TAP : WallpaperManager.COMMAND_SECONDARY_TAP,
                position[0], position[1], 0, null);
    }

    /*
     * This interpolator emulates the rate at which the perceived scale of an object changes
     * as its distance from a camera increases. When this interpolator is applied to a scale
     * animation on a view, it evokes the sense that the object is shrinking due to moving away
     * from the camera.
     */
    static class ZInterpolator implements TimeInterpolator {
        private float focalLength;

        public ZInterpolator(float foc) {
            focalLength = foc;
        }

        public float getInterpolation(float input) {
            return (1.0f - focalLength / (focalLength + input)) /
                (1.0f - focalLength / (focalLength + 1.0f));
        }
    }

    /*
     * The exact reverse of ZInterpolator.
     */
    static class InverseZInterpolator implements TimeInterpolator {
        private ZInterpolator zInterpolator;
        public InverseZInterpolator(float foc) {
            zInterpolator = new ZInterpolator(foc);
        }
        public float getInterpolation(float input) {
            return 1 - zInterpolator.getInterpolation(1 - input);
        }
    }

    /*
     * ZInterpolator compounded with an ease-out.
     */
    static class ZoomOutInterpolator implements TimeInterpolator {
        private final DecelerateInterpolator decelerate = new DecelerateInterpolator(0.75f);
        private final ZInterpolator zInterpolator = new ZInterpolator(0.13f);

        public float getInterpolation(float input) {
            return decelerate.getInterpolation(zInterpolator.getInterpolation(input));
        }
    }

    /*
     * InvereZInterpolator compounded with an ease-out.
     */
    static class ZoomInInterpolator implements TimeInterpolator {
        private final InverseZInterpolator inverseZInterpolator = new InverseZInterpolator(0.35f);
        private final DecelerateInterpolator decelerate = new DecelerateInterpolator(3.0f);

        public float getInterpolation(float input) {
            return decelerate.getInterpolation(inverseZInterpolator.getInterpolation(input));
        }
    }

    private final ZoomInInterpolator mZoomInInterpolator = new ZoomInInterpolator();

    /*
    *
    * We call these methods (onDragStartedWithItemSpans/onDragStartedWithSize) whenever we
    * start a drag in Launcher, regardless of whether the drag has ever entered the Workspace
    *
    * These methods mark the appropriate pages as accepting drops (which alters their visual
    * appearance).
    *
    */
    private static Rect getDrawableBounds(Drawable d) {
        Rect bounds = new Rect();
        d.copyBounds(bounds);
        if (bounds.width() == 0 || bounds.height() == 0) {
            bounds.set(0, 0, d.getIntrinsicWidth(), d.getIntrinsicHeight());
        } else {
            bounds.offsetTo(0, 0);
        }
        if (d instanceof PreloadIconDrawable) {
            int inset = -((PreloadIconDrawable) d).getOutset();
            bounds.inset(inset, inset);
        }
        return bounds;
    }

    public void onExternalDragStartedWithItem(View v) {
        if (LauncherLog.DEBUG_DRAG) {
            LauncherLog.d(TAG, "onExternalDragStartedWithItem: v = " + v);
        }
        // Compose a drag bitmap with the view scaled to the icon size
        LauncherAppState app = LauncherAppState.getInstance();
        DeviceProfile grid = app.getDynamicGrid().getDeviceProfile();
        int iconSize = grid.iconSizePx;
        int bmpWidth = v.getMeasuredWidth();
        int bmpHeight = v.getMeasuredHeight();

        // If this is a text view, use its drawable instead
        if (v instanceof TextView) {
            TextView tv = (TextView) v;
            Drawable d = tv.getCompoundDrawables()[1];
            Rect bounds = getDrawableBounds(d);
            bmpWidth = bounds.width();
            bmpHeight = bounds.height();
        }

        // Compose the bitmap to create the icon from
        Bitmap b = Bitmap.createBitmap(bmpWidth, bmpHeight,
                Bitmap.Config.ARGB_8888);
        mCanvas.setBitmap(b);
        drawDragView(v, mCanvas, 0);
        mCanvas.setBitmap(null);

        // The outline is used to visualize where the item will land if dropped
        mDragOutline = createDragOutline(b, DRAG_BITMAP_PADDING, iconSize, iconSize, true);
    }

    public void onDragStartedWithItem(PendingAddItemInfo info, Bitmap b, boolean clipAlpha) {
        int[] size = estimateItemSize(info.spanX, info.spanY, info, false);

        // The outline is used to visualize where the item will land if dropped
        mDragOutline = createDragOutline(b, DRAG_BITMAP_PADDING, size[0], size[1], clipAlpha);
    }

    public void exitWidgetResizeMode() {
        DragLayer dragLayer = mLauncher.getDragLayer();
        dragLayer.clearAllResizeFrames();
    }

    private void initAnimationArrays() {
        final int childCount = getChildCount();
        if (mLastChildCount == childCount) return;

        mOldBackgroundAlphas = new float[childCount];
        mOldAlphas = new float[childCount];
        mNewBackgroundAlphas = new float[childCount];
        mNewAlphas = new float[childCount];
    }

    Animator getChangeStateAnimation(final State state, boolean animated,
            ArrayList<View> layerViews) {
        return getChangeStateAnimation(state, animated, 0, -1, layerViews);
    }

    @Override
    protected void getFreeScrollPageRange(int[] range) {
        getOverviewModePages(range);
    }

    private void getOverviewModePages(int[] range) {
        int start = numCustomPages();
        int end = getChildCount()-1;

        if (mReorderingStarted && hasExtraEmptyScreen()) {
			end--;
		}

        range[0] = Math.max(0, Math.min(start, getChildCount() - 1));
        range[1] = Math.max(0,  end);
    }

    protected void onStartReordering() {
    	if (hasExtraEmptyScreen()) {
        	getPageIndicator().changeMarkerVisibility(mScreenOrder.indexOf(EXTRA_EMPTY_SCREEN_ID), false);
		}

        super.onStartReordering();
        showOutlines();
        // Reordering handles its own animations, disable the automatic ones.
        disableLayoutTransitions();
    }

    protected void onEndReordering() {
    	if (hasExtraEmptyScreen()) {
        	getPageIndicator().changeMarkerVisibility(mScreenOrder.indexOf(EXTRA_EMPTY_SCREEN_ID), true);
		}

        super.onEndReordering();

        if (mLauncher.isWorkspaceLoading()) {
            // Invalid and dangerous operation if workspace is loading
            return;
        }

        hideOutlines();
        mScreenOrder.clear();
        int count = getChildCount();
        for (int i = 0; i < count; i++) {
            CellLayout cl = ((CellLayout) getChildAt(i));
            mScreenOrder.add(getIdForScreen(cl));
        }


        mLauncher.getModel().updateWorkspaceScreenOrder(mLauncher, mScreenOrder);

        // Re-enable auto layout transitions for page deletion.
        enableLayoutTransitions();
        
//        // Add to debug double icon problem Jing.Wu 20151017 start
//        mDragView = null;
//        // Add to debug double icon problem Jing.Wu 20151017 end
    }

    public boolean isInOverviewMode() {
        return mState == State.OVERVIEW;
    }

    public boolean enterOverviewMode() {
        if (mTouchState != TOUCH_STATE_REST) {
            return false;
        }
        enableOverviewMode(true, -1, true);

        setWorkspaceInOverviewMode(true);
        return true;
    }

    public void exitOverviewMode(boolean animated) {
        exitOverviewMode(-1, animated);

        setWorkspaceInOverviewMode(false);
    }

    public void exitOverviewMode(int snapPage, boolean animated) {
        enableOverviewMode(false, snapPage, animated);

        setWorkspaceInOverviewMode(false);
    }

    public void cellsShowAddOrDeleteButton(boolean show, boolean testState) {
    	if (testState && mState == State.NORMAL) {
			return;
		}
    	int doNotShowButton = -1;
    	if (getNormalChildCount() < 2) {
    		doNotShowButton = hasCustomContent()?1:0;
		}
    	for (int i = 0; i < getChildCount(); i++) {
    		View childView = getChildAt(i);
			if (childView instanceof CellLayout) {
				if (show && i>doNotShowButton) {
					((CellLayout)childView).showAddButton(testState);
				} else {
					((CellLayout)childView).hideAddButton();
				}
			}
		}
    }


    private void enableOverviewMode(boolean enable, int snapPage, boolean animated) {
        State state = Workspace.State.OVERVIEW;
        if (!enable) {
        	state = Workspace.State.NORMAL;
        } else {
        	enableOverviewMode = true;
		}
        final State finalState = state;

        Animator workspaceAnim = getChangeStateAnimation(state, animated, 0, snapPage);
        if (workspaceAnim != null) {
            onTransitionPrepare();
            workspaceAnim.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator arg0) {
                    onTransitionEnd();
                    if (finalState == State.NORMAL) {
						cancelMinScale();
						enableOverviewMode = false;
					}
					for (int i = 0; i < getChildCount(); i++) {
						CellLayout cl = (CellLayout)getChildAt(i);
						cl.setShortcutAndWidgetAlpha(1f);
						//cl.setBackgroundAlpha(enable?1f:0f);
						cl.setAlpha(QCConfig.defaultAlpha);
					}
                }
            });
            workspaceAnim.start();
        } else if (!animated) {
        	if( mState == State.NORMAL) {
        		enableOverviewMode = false;
        	}
		}
    }

    public void enableNormalScaleMode(boolean enable) {
    	if (enable) {
    		isNormalScaling = enable;
		}
        Animator workspaceAnim = getNormalScaleAnimation(enable, 0, -1, null);
        if (workspaceAnim != null) {
        	if (!enable) {
        		final boolean isEnable = enable;
        		workspaceAnim.addListener(new AnimatorListenerAdapter() {
    				@Override
    				public void onAnimationCancel(Animator animation) {
    					super.onAnimationCancel(animation);
    					isNormalScaling = isEnable;
                        mShouldStartScrollX = 0;
                        mShouldStartPageNumb = 0;
    				}
    				@Override
    				public void onAnimationEnd(Animator animation) {
    					super.onAnimationEnd(animation);
    					isNormalScaling = isEnable;
                        mShouldStartScrollX = 0;
                        mShouldStartPageNumb = 0;
    				}
            		
    			});
			}
            workspaceAnim.start();
        } else {
        	isNormalScaling = enable;
		}
    }

    int getOverviewModeTranslationY() {
        LauncherAppState app = LauncherAppState.getInstance();
        DeviceProfile grid = app.getDynamicGrid().getDeviceProfile();

    	int halfScreenSize = (int)(LauncherApplication.getScreenHeightPixel()/2);
    	int workspaceInOverviewHalfScreenSize = (int)((halfScreenSize-LauncherApplication.statusBarHeight)*mOverviewModeShrinkFactor);
    	int translationY = -(halfScreenSize-workspaceInOverviewHalfScreenSize-grid.searchBarSpaceHeightPx);
        return translationY;
    }

    boolean shouldVoiceButtonProxyBeVisible() {
        if (isOnOrMovingToCustomContent()) {
            return false;
        }
        if (mState != State.NORMAL) {
            return false;
        }
        return true;
    }

    public void updateInteractionForState() {
        if (mState != State.NORMAL) {
            mLauncher.onInteractionBegin();
        } else {
            mLauncher.onInteractionEnd();
        }
    }

    private void setState(State state) {
        mState = state;
        updateInteractionForState();
        updateAccessibilityFlags();
    }

    State getState() {
        return mState;
    }

    private void updateAccessibilityFlags() {
    	int accessYes = 1;
    	int accessNoHideDescendants = 4;
    	try {
			Class ViewCompat = Class.forName("android.support.v4.view.ViewCompat");
			if (ViewCompat!=null) {
				Field accessYesField = ViewCompat.getField("IMPORTANT_FOR_ACCESSIBILITY_YES");
				if (accessYesField.getType().equals(int.class)) {
					accessYes = accessYesField.getInt(null);
				}
				Field accessNoHideDescendantsField = ViewCompat.getField("IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS");
				if (accessNoHideDescendantsField.getType().equals(int.class)) {
					accessNoHideDescendants = accessNoHideDescendantsField.getInt(null);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
        int accessible = mState == State.NORMAL ? accessYes : accessNoHideDescendants;
        setImportantForAccessibility(accessible);
    }

    private static final int HIDE_WORKSPACE_DURATION = 100;

    Animator getChangeStateAnimation(final State state, boolean animated, int delay, int snapPage) {
        return getChangeStateAnimation(state, animated, delay, snapPage, null);
    }

    Animator getChangeStateAnimation(final State state, boolean animated, int delay, int snapPage,
            ArrayList<View> layerViews) {
        if (mState == state) {
            return null;
        }

        AnimatorSet anim = animated ? LauncherAnimUtils.createAnimatorSet() : null;

        final State oldState = mState;
        final boolean oldStateIsNormal = (oldState == State.NORMAL);
        final boolean oldStateIsSpringLoaded = (oldState == State.SPRING_LOADED);
        final boolean oldStateIsNormalHidden = (oldState == State.NORMAL_HIDDEN);
        final boolean oldStateIsOverviewHidden = (oldState == State.OVERVIEW_HIDDEN);
        final boolean oldStateIsOverview = (oldState == State.OVERVIEW);
        setState(state);
        final boolean stateIsNormal = (state == State.NORMAL);
        final boolean stateIsSpringLoaded = (state == State.SPRING_LOADED);
        final boolean stateIsNormalHidden = (state == State.NORMAL_HIDDEN);
        final boolean stateIsOverviewHidden = (state == State.OVERVIEW_HIDDEN);
        final boolean stateIsOverview = (state == State.OVERVIEW);
        
        float finalBackgroundAlpha = (!stateIsNormal) ? 1.0f : 0f;
        float finalHotseatAlpha = stateIsNormal ? 1f : 0f;
        float finalPageIndicatorAlpha = stateIsNormal ? 1f : 0f;
        float finalOverviewPanelAlpha = (!stateIsNormal) ? 1f : 0f;
        float finalWorkspaceTranslationY = (!stateIsNormal)?
                getOverviewModeTranslationY() : 0;

        boolean workspaceToAllApps = (oldStateIsNormal && stateIsNormalHidden);
        boolean overviewToAllApps = (oldStateIsOverview && stateIsOverviewHidden);
        boolean allAppsToWorkspace = (stateIsNormalHidden && stateIsNormal);
        boolean workspaceToOverview = (oldStateIsNormal && stateIsOverview);
        boolean overviewToWorkspace = (oldStateIsOverview && stateIsNormal);

        if (oldStateIsOverview) {
                disableFreeScroll();
        }


    	// Modify for AddExtraEmptyScreen Jing.Wu 20160105 start
        if (workspaceToOverview) {
        	if (!QCConfig.autoDeleteAndAddEmptyScreen) {
            	if (hasExtraEmptyScreen()) {
    				getScreenWithId(EXTRA_EMPTY_SCREEN_ID).setVisibility(View.VISIBLE);
    			} else {
    				addExtraEmptyScreen();
    			}
    			cellsShowAddOrDeleteButton(true, false);
            	getPageIndicator().changeMarkerVisibility(mScreenOrder.indexOf(EXTRA_EMPTY_SCREEN_ID), true);
			}
        	setMinScale(mOverviewModeShrinkFactor);
		} else if (stateIsNormal) {
			if (!QCConfig.autoDeleteAndAddEmptyScreen) {
	            if (hasExtraEmptyScreen()) {
	            	int snapToPage = getPageNearestToCenterOfScreen();
	            	int lastEmptyPage = Math.max(getChildCount()-1, mScreenOrder.indexOf(EXTRA_EMPTY_SCREEN_ID));
	            	if (snapToPage == lastEmptyPage) {
	                	snapPage = snapToPage-1;
	                	//setCurrentPage(snapPage);
	                	snapToPage(snapPage, 0, true, null);
					}
					getScreenWithId(EXTRA_EMPTY_SCREEN_ID).setVisibility(View.INVISIBLE);
		        	getPageIndicator().changeMarkerVisibility(lastEmptyPage, false);
				}
    			cellsShowAddOrDeleteButton(false, false);
			}
        	mLauncher.showTopOverview();
		}
        mNewScale = 1.0f;
    	// Modify for AddExtraEmptyScreen Jing.Wu 20160105 end

        if (state != State.NORMAL) {
            if (stateIsSpringLoaded) {
                //mNewScale = mSpringLoadedShrinkFactor;
            	mNewScale = mOverviewModeShrinkFactor;
            } else if (stateIsOverview || stateIsOverviewHidden) {
            	// Changed for MyUI---20150803
                mNewScale = mOverviewModeShrinkFactor;
                //finalWorkspaceTranslationY = -(11*LauncherApplication.getScreenDensity())+
                	//	(getResources().getDimensionPixelSize(R.dimen.dynamic_grid_search_bar_height)-getPaddingTop())
                	//	+(mOverviewModeWorkspaceHeight-(float)(this.getHeight()-getPaddingBottom()-getPaddingTop()))/2;
            }
        }
        if (workspaceToOverview || stateIsNormal) {
        	mNewScaleForWidgetResize = mNewScale;
		}

        final int duration;
        if (workspaceToAllApps || overviewToAllApps) {
            duration = HIDE_WORKSPACE_DURATION; //getResources().getInteger(R.integer.config_workspaceUnshrinkTime);
        } else if (workspaceToOverview||overviewToWorkspace||stateIsNormal) {
            duration = getResources().getInteger(R.integer.config_overviewTransitionTime);
        } else {
            duration = getResources().getInteger(R.integer.config_appsCustomizeWorkspaceShrinkTime);
        }
        
        if (snapPage == -1) {
            snapPage = getPageNearestToCenterOfScreen();
        }
        snapToPage(snapPage, duration, mZoomInInterpolator);

        // Initialize animation arrays for the first time if necessary
    	// Modify and change place for AddExtraEmptyScreen Jing.Wu 20160105 start
        initAnimationArrays();
    	// Modify and change place for AddExtraEmptyScreen Jing.Wu 20160105 end
        
        for (int i = 0; i < getChildCount(); i++) {
            final CellLayout cl = (CellLayout) getChildAt(i);
            boolean isCurrentPage = (i == snapPage);
            float initialAlpha = cl.getShortcutsAndWidgets().getAlpha();
            float finalAlpha;
            if (stateIsNormalHidden || stateIsOverviewHidden) {
                finalAlpha = QCConfig.defaultAlpha;
            } else if (stateIsNormal && mWorkspaceFadeInAdjacentScreens) {
                finalAlpha = (i == snapPage || i < numCustomPages()) ? QCConfig.defaultAlpha : 0f;
            } else {
                finalAlpha = QCConfig.defaultAlpha;
            }

            // If we are animating to/from the small state, then hide the side pages and fade the
            // current page in
            if (!mIsSwitchingState) {
                if (workspaceToAllApps || allAppsToWorkspace) {
                    if (allAppsToWorkspace && isCurrentPage) {
                        initialAlpha = 0f;
                    } else if (!isCurrentPage) {
                        initialAlpha = finalAlpha = 0f;
                    }
                    cl.setShortcutAndWidgetAlpha(initialAlpha);
                }
            }

            mOldAlphas[i] = initialAlpha;
            mNewAlphas[i] = finalAlpha;
            if (animated) {
                mOldBackgroundAlphas[i] = cl.getBackgroundAlpha();
                mNewBackgroundAlphas[i] = finalBackgroundAlpha;
            } else {
                cl.setBackgroundAlpha(finalBackgroundAlpha);
                cl.setShortcutAndWidgetAlpha(finalAlpha);
            }
        }
        
        //sunfeng @20150804 add pageNumber view start: 
        if(state == State.NORMAL){
        	hideWSPageView();
        	hideWsLongPageView();
        }else{
    		hideWSPageView();
        	hideWsLongPageView();
        	showWsLongPageView(getCurrentPage());
        }

        final View overviewPanel = mLauncher.getOverviewPanel();
        final View hotseat = mLauncher.getHotseat();
        final View pageIndicator = getPageIndicator();
        if (animated) {
        	if (stateIsNormal || workspaceToOverview) {
                LauncherViewPropertyAnimator scale = new LauncherViewPropertyAnimator(this);
                scale.scaleX(mNewScale)
                    .scaleY(mNewScale)
                    .translationY(finalWorkspaceTranslationY)
                    .setDuration(duration)
                    .setInterpolator(mZoomInInterpolator);
                anim.play(scale);
			}
            for (int index = 0; index < getChildCount(); index++) {
                final int i = index;
                final CellLayout cl = (CellLayout) getChildAt(i);
                float currentAlpha = cl.getShortcutsAndWidgets().getAlpha();
                if (mOldAlphas[i] == 0 && mNewAlphas[i] == 0) {
                    cl.setBackgroundAlpha(mNewBackgroundAlphas[i]);
                    cl.setShortcutAndWidgetAlpha(mNewAlphas[i]);
                } else {
                    if (layerViews != null) {
                        layerViews.add(cl);
                    }
                    if (mOldAlphas[i] != mNewAlphas[i] || currentAlpha != mNewAlphas[i]) {
                        LauncherViewPropertyAnimator alphaAnim =
                            new LauncherViewPropertyAnimator(cl.getShortcutsAndWidgets());
                        alphaAnim.alpha(mNewAlphas[i])
                            .setDuration(duration)
                            .setInterpolator(mZoomInInterpolator);
                        anim.play(alphaAnim);
                    }
                    if (mOldBackgroundAlphas[i] != 0 ||
                        mNewBackgroundAlphas[i] != 0) {
                        ValueAnimator bgAnim =
                                LauncherAnimUtils.ofFloat(cl, 0f, 1f);
                        bgAnim.setInterpolator(mZoomInInterpolator);
                        bgAnim.setDuration(duration);
                        bgAnim.addUpdateListener(new LauncherAnimatorUpdateListener() {
                                public void onAnimationUpdate(float a, float b) {
                                    /// M: [ALPS01480374] Check if i is larger than child count.
                                    if (i >= getChildCount()) {
                                        LauncherLog.d(TAG, "getChangeStateAnimation: i should not larger than child count");
                                        return;
                                    }

                                    cl.setBackgroundAlpha(
                                            a * mOldBackgroundAlphas[i] +
                                            b * mNewBackgroundAlphas[i]);
                                }
                            });
                        anim.play(bgAnim);
                    }
                }
            }
            
            AlphaUpdateListener alphaUpdateListener = null;
            Animator pageIndicatorAlpha = null;
            if (pageIndicator != null) {
                pageIndicatorAlpha = new LauncherViewPropertyAnimator(pageIndicator)
                    .alpha(finalPageIndicatorAlpha).withLayer();
                alphaUpdateListener = new AlphaUpdateListener(pageIndicator);
                pageIndicatorAlpha.addListener(alphaUpdateListener);
                //pageIndicatorAlpha.addUpdateListener(alphaUpdateListener);
            } else {
                // create a dummy animation so we don't need to do null checks later
                pageIndicatorAlpha = ValueAnimator.ofFloat(0, 0);
            }

            Animator hotseatAlpha = new LauncherViewPropertyAnimator(hotseat)
                .alpha(finalHotseatAlpha).withLayer();
            alphaUpdateListener = new AlphaUpdateListener(hotseat);
            hotseatAlpha.addListener(alphaUpdateListener);
            //hotseatAlpha.addUpdateListener(alphaUpdateListener);

            Animator overviewPanelAlpha = new LauncherViewPropertyAnimator(overviewPanel)
                .alpha(finalOverviewPanelAlpha).withLayer();
            alphaUpdateListener = new AlphaUpdateListener(overviewPanel);
            overviewPanelAlpha.addListener(alphaUpdateListener);
            //overviewPanelAlpha.addUpdateListener(alphaUpdateListener);

            // For animation optimations, we may need to provide the Launcher transition
            // with a set of views on which to force build layers in certain scenarios.
            hotseat.setLayerType(View.LAYER_TYPE_HARDWARE, null);
            overviewPanel.setLayerType(View.LAYER_TYPE_HARDWARE, null);
            if (layerViews != null) {
                layerViews.add(hotseat);
                layerViews.add(overviewPanel);
            }
			
            if (workspaceToOverview) {
            	pageIndicatorAlpha.setInterpolator(new DecelerateInterpolator(2));
                hotseatAlpha.setInterpolator(new DecelerateInterpolator(2));
                overviewPanelAlpha.setInterpolator(null);
            } else if (overviewToWorkspace) {
            	hideWsLongPageView();
                pageIndicatorAlpha.setInterpolator(null);
                hotseatAlpha.setInterpolator(null);
                overviewPanelAlpha.setInterpolator(new DecelerateInterpolator(2));
            }

            overviewPanelAlpha.setDuration(duration);
            pageIndicatorAlpha.setDuration(duration);
            hotseatAlpha.setDuration(duration);

            anim.play(overviewPanelAlpha);
            anim.play(hotseatAlpha);
            anim.play(pageIndicatorAlpha);
            anim.setStartDelay(delay);
        } else {
        	overviewPanel.setAlpha(finalOverviewPanelAlpha);
            AlphaUpdateListener.updateVisibility(overviewPanel);
            hotseat.setAlpha(finalHotseatAlpha);
            AlphaUpdateListener.updateVisibility(hotseat);
            if (pageIndicator != null) {
                pageIndicator.setAlpha(finalPageIndicatorAlpha);
                AlphaUpdateListener.updateVisibility(pageIndicator);
            }
            updateCustomContentVisibility();
            if (stateIsNormal || workspaceToOverview) {
            	setScaleX(mNewScale);
                setScaleY(mNewScale);
                setTranslationY(finalWorkspaceTranslationY);
			}
            
        	// Modify and change place for AddExtraEmptyScreen Jing.Wu 20160105 start
            if (stateIsNormal) {
				cancelMinScale();
			}
			for (int i = 0; i < getChildCount(); i++) {
				CellLayout cl = (CellLayout)getChildAt(i);
				cl.setShortcutAndWidgetAlpha(1f);
				//cl.setBackgroundAlpha(enable?1f:0f);
				cl.setAlpha(QCConfig.defaultAlpha);
			}
	    	// Modify and change place for AddExtraEmptyScreen Jing.Wu 20160105 end
        }
        /// M: [ALPS01257663] Correct usage of updateVoiceButtonProxyVisible().
        //mLauncher.updateVoiceButtonProxyVisible(false);

        if (stateIsNormal) {
            animateBackgroundGradient(0f, animated);
        } else {
            animateBackgroundGradient(getResources().getInteger(
                    R.integer.config_workspaceScrimAlpha) / 100f, animated);
        }

        postDelayed(new Runnable() {
        	@Override
        	public void run() {
    	        showPiflow(mState == State.NORMAL && !isNormalScaling);
        	}
        }, (animated&&(mState == State.NORMAL))?500:1);
        // Modify OverviewMode Jing.Wu 20150908 end
        return anim;
    }
    
    
    LinearLayout qingcheng_page_indicator;
    
    public void setQingchengPageIndicator(LinearLayout  arg){
    	
    	qingcheng_page_indicator = arg;
    }
    
    LinearLayout qingcheng_widget_page_indicator;
    
    public void setQingchengWidgetPageIndicator(LinearLayout  arg){
    	
    	qingcheng_widget_page_indicator = arg;
    }

	ArrayList<View> mPageViewWsList = new ArrayList<View>();
	ArrayList<View> mPageViewLongList = new ArrayList<View>();

	SharedPreferences preferences;
	SharedPreferences.Editor editor;
	ImageView inactive;
	ImageView active;
	ViewGroup tmp;

	  int[] hw= new int[2];
	  WindowManager wm;
	  
	public void showPageIndex(int childCount){ 
		int allPage = childCount;
		if(childCount == -1){
			allPage = getChildCount();
			if(mLauncher.hasCustomContentToLeft()){
				allPage -- ;
			}
		}

        showQingchengWorkSpacePage(qingcheng_page_indicator, allPage-(hasExtraEmptyScreen()?1:0), mCurrentPage, true);
        showQingchengWorkSpacePage(qingcheng_widget_page_indicator, allPage, mCurrentPage, false);
	}
	  
	int mTmpIndex = -1 ;
	//64 81 	
	public void showWsLongPageView(int index){
		if (mLauncher.hasCustomContentToLeft()) {
			index = index - 1;
		}
		showWidgetViewPage(mPageViewLongList, index, true);
		qingcheng_widget_page_indicator.setVisibility(View.VISIBLE);
		mLauncher.mRefreshPageClick(true);
		mPageNum.clear();
		for (int i = 0; i < mPageViewLongList.size(); i++) {
			mPageNum.add(getXy(i, mPageViewLongList.get(i)));
		}
	}
	public void hideWsLongPageView(){
		mPageNum.clear();
		qingcheng_widget_page_indicator.setVisibility(View.INVISIBLE);
		mLauncher.mRefreshPageClick(false);
	}
	

    int currentIndex  = -1;
	private void RefreshPageView(int index){
		if(qingcheng_page_indicator.getVisibility() == View.VISIBLE){
			currentIndex = index;
			showWidgetViewPage(mPageViewWsList,index , false);
		}
	}
	
	public void showWSPageView(){
		 int index = getCurrentPage();
		 if(mLauncher.hasCustomContentToLeft()){
			 index = index - 1;
		 }
		
		showWidgetViewPage(mPageViewWsList,index , false);
		qingcheng_page_indicator.setVisibility(View.VISIBLE);
		
		mPageNum.clear();
		for(int i=0;i<mPageViewWsList.size();i++){
//			getXy(i,mPageViewWsList.get(i));
			mPageNum.add(getXy(i,mPageViewWsList.get(i)));
		}
	}
	
	public void hideWSPageView(){
		mPageNum.clear();
		qingcheng_page_indicator.setVisibility(View.INVISIBLE);
	}
	

	private long enterWidgetPageIndicatorTime = 0;
	private int lastLocationYBeforeEnterWidgetPageIndicator;
	public int getDragToScreen(int mX,int mY){
		if ((mState==State.NORMAL && qingcheng_page_indicator!=null)
				|| (mState!=State.NORMAL && qingcheng_widget_page_indicator!=null)) {
			View parentView = mState==State.NORMAL?qingcheng_page_indicator:qingcheng_widget_page_indicator;
			int[] pageIndicatorLocation = new int[2];
			parentView.getLocationInWindow(pageIndicatorLocation);
			int top = pageIndicatorLocation[1];
			int bottom = top+parentView.getHeight();
			
			if (mY>top && mY<bottom) {
				if (enterWidgetPageIndicatorTime == 0) {
					enterWidgetPageIndicatorTime = System.currentTimeMillis();
				}
				boolean isFromWorkspace = lastLocationYBeforeEnterWidgetPageIndicator<mY;
				if (isFromWorkspace || (System.currentTimeMillis()-enterWidgetPageIndicatorTime)>300) {
			     	DisplayMetrics dm = new DisplayMetrics();
			        mLauncher.getWindowManager().getDefaultDisplay().getMetrics(dm);
			        int mScreenWidth = dm.widthPixels;
			        int mScreenHeight = dm.heightPixels;
					int w = wm.getDefaultDisplay().getWidth();
					
					int x = mX % mScreenWidth;
					int y = mY;//wm.getDefaultDisplay().getHeight();
//					Log.i(TAG,"  getDragToScreen======mX: "+ mX+" mY:" + mY +" x:"+ x+" Y:"+y );
					for(int i=0;i<mPageNum.size();i++){
						int y1 = mPageNum.get(i)[1];
						int x1= mPageNum.get(i)[0];
						if(y1 > y){
							return -1;
						}
						if(isExit(x, y, x1, y1)){
							return i;
						}
					}
				}
				
			} else {
				//enterWidgetPageIndicatorTime = 0;
				lastLocationYBeforeEnterWidgetPageIndicator = mY;
				return -1;
			}
		} else {
			return -1;
		}
		return -1;
	}
	
	private boolean isExit(int x,int y, int x1,int y1){
		if(x > x1 && x <x1+hw[0]&& y> y1 && y< y1+ hw[1] ){
			return true;
		}
		return false;
	}
	
	ArrayList<int[]> mPageNum = new ArrayList<int[]>();
	int mCellPageDistance = 20;
	
	public int[] getXy(int i,View child){
		int[] location =new int[2];
		int[] location1 =new int[2];
		child.getLocationInWindow(location);

		hw[0] = child.getWidth();
		hw[1] = child.getHeight();

	
		return location;
	}
	
	public void showQingchengWorkSpacePage(View v,int allpage,int cur, boolean workspace){
		if(/*cur >= allpage ||*/ cur < 0 || v==null||cur > allpage || allpage < 0){
			return ;
		}
		if(workspace){
			mPageViewWsList.clear();
		}else{
			mPageViewLongList.clear();
		}
		TextView active;
		tmp = (ViewGroup) v;
		tmp.removeAllViews();
		LinearLayout.LayoutParams lp =new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
		
		lp.gravity = Gravity.CENTER;
		if (LauncherApplication.getIsNormalScreenResolutionAndDensity()) {
			lp.width = (int) mLauncher.getResources().getDimension(R.dimen.workspace_page_add_w) ;
			lp.height = (int) mLauncher.getResources().getDimension(R.dimen.workspace_page_add_h);
		} else {
			lp.width = mLauncher.mApplication.getPxforXLayout(true, R.dimen.workspace_page_add_w, 0);
			lp.height = mLauncher.mApplication.getPxforYLayout(true, R.dimen.workspace_page_add_h, 0);
		}
		
		if(allpage >= 9){
			if (LauncherApplication.getIsNormalScreenResolutionAndDensity()) {
				mCellPageDistance = DynamicGrid.pxFromDp(8, getResources().getDisplayMetrics());
			} else {
				mCellPageDistance = mLauncher.mApplication.getPxforXLayout(false, 0, DynamicGrid.pxFromDp(8, getResources().getDisplayMetrics()));
			}
		}
		
		lp.leftMargin = mCellPageDistance;
		for(int i=0;i<allpage;i++){
			FrameLayout m =
					(FrameLayout) LayoutInflater.from(mLauncher).inflate(R.layout.work_click_page_view,null, false);
			active =(TextView) m.findViewById(R.id.active);
			inactive =(ImageView) m.findViewById(R.id.inactive);
			String date =" "+(i+1)+"   ";
			active.setText(/*date.substring(0, 3)*/""+(i+1));
			active.setTextSize(20);
//			m.setBackgroundResource(R.color.workspace_page_view_default_color);
			m.setBackgroundResource(R.drawable.icon_switch_dot_normal);

			m.setId(i);
			/**
			 * current page
			 */
			if(i ==cur){
//				m.setBackgroundResource(R.color.workspace_page_view_current_color);
				m.setBackgroundResource(R.drawable.icon_switch_dot_highlight);
			}
			
			/***
			 * set homepage
			 */

			active.setVisibility(View.VISIBLE);
			inactive.setVisibility(View.INVISIBLE);
			int mTmpDefaultPage = mDefaultPage;
//			if(mLauncher.hasCustomContentToLeft()){
//				mTmpDefaultPage = mTmpDefaultPage -1;
//			}

			if(i == mTmpDefaultPage && !workspace){
				inactive.setVisibility(View.VISIBLE);
				inactive.setImageResource(R.drawable.workspace_add_home);
				active.setVisibility(View.INVISIBLE);
			}
			
			m.setOnClickListener(new OnClickListener() {
				
				@Override
				public void onClick(View arg0) {
					// TODO Auto-generated method stub
					// Add for MyUI Jing.Wu 20151102 start
					if (isReordering(true)) {
						return;
					}
					// Add for MyUI Jing.Wu 20151102 end
					setPageNumberToScreen(tmp, arg0, arg0.getId());
				}
			});
			
			m.setOnLongClickListener(new OnLongClickListener() {
				@Override
				public boolean onLongClick(View arg0) {
					// TODO Auto-generated method stub
					// Add for MyUI Jing.Wu 20151102 start
					if (isReordering(true)) {
						return true;
					}
					// Add for MyUI Jing.Wu 20151102 end
					setHomePage(tmp, arg0, arg0.getId());
					return true;
				}
			});
			
			if(i == allpage -1){
				lp.rightMargin = mCellPageDistance;
			}
			
			tmp.addView(m , i, lp);
			if(workspace){
				mPageViewWsList.add(m);
			}else{
				mPageViewLongList.add(m);
			}
			
		}
		if (mState == State.NORMAL) {
			tmp.setVisibility(View.INVISIBLE);
		}
		
	}
	
	
	public void setPageNumberToScreen(View parent,View child ,int index){
		showWidgetViewPage(mPageViewLongList,index, true);
		 if(mLauncher.hasCustomContentToLeft()){
	        	index = index + 1;
	      }
		mLauncher.getWorkspace().snapToPage(index);
	}

    public void setHomePage(View parent, View child, int index) {
        //moveToScreen(index, true);

        if(mLauncher.hasCustomContentToLeft()){
            index += 1;
        }
        mDefaultPage = index;

        showWidgetViewPage(mPageViewLongList, index, true);
        preferences = mLauncher.getSharedPreferences(QCPreference.PREFERENCE_NAME, Context.MODE_PRIVATE);
        editor = preferences.edit();

        //sunfeng modify @20151020 homepage show error start:
        if (mLauncher.hasCustomContentToLeft()) {
            editor.putInt(QCPreference.KEY_DEFAULT_SCREEN, mDefaultPage);
        } else {
            editor.putInt(QCPreference.KEY_DEFAULT_SCREEN, mDefaultPage);
        }
        //sunfeng modify @20151020 homepage show error end:
        editor.commit();
    }

    public void showWidgetViewPage(ArrayList<View> view,int index ,boolean isLong){
        int mTmpDefaultPage = mDefaultPage;
        if(mLauncher.hasCustomContentToLeft()){
            mTmpDefaultPage = mTmpDefaultPage -1;
        }
		for(int i=0;i<view.size();i++){
			FrameLayout page = (FrameLayout) view.get(i);

			TextView active =(TextView) page.findViewById(R.id.active);
			inactive =(ImageView) page.findViewById(R.id.inactive);
			String date =" "+(i+1)+"   ";
//			active.setText(date.substring(0, 3));
			active.setText(/*date.substring(0, 3)*/""+(i+1));
			active.setTextSize(20);
//			page.setBackgroundResource(R.color.workspace_page_view_default_color);
			page.setBackgroundResource(R.drawable.workspace_page_view_default_color);

			if(i == mTmpDefaultPage){
//				page.setBackgroundResource(R.color.workspace_page_view_current_color);
				page.setBackgroundResource(R.drawable.workspace_page_view_current_color);
			}
			active.setVisibility(View.VISIBLE);
			inactive.setVisibility(View.INVISIBLE);
//			Log.i(TAG,"  ==showWidgetViewPage===== "+ mDefaultPage+" i:"+ i);

			if(i == mTmpDefaultPage && isLong){
				inactive.setVisibility(View.VISIBLE);
				inactive.setImageResource(R.drawable.workspace_add_home);
				active.setVisibility(View.INVISIBLE);
			}
		}
	}
	//sunfeng @20150805 add for pageview snapTopage end:
	
    Animator getNormalScaleAnimation(boolean enable, int delay, int snapPage,
            ArrayList<View> layerViews) {

        if (mState != State.NORMAL) {
            return null;
        }

        AnimatorSet anim = LauncherAnimUtils.createAnimatorSet();
        
        float finalWorkspaceTranslationY = 0;
        float finalHotseatTranslationY = 0;

        final View hotseat = mLauncher.getHotseat();
        final View pageIndicator = getPageIndicator();
        
        mNewScale = 1.0f;

        if (enable) {
			mNewScale = mNormalScaleShrinkFactor;
        	int halfScreenSize = (int)(LauncherApplication.getScreenHeightPixel()/2);
        	int workspaceInNormalScaleHalfScreenSize = (int)((halfScreenSize-LauncherApplication.statusBarHeight)*mNormalScaleShrinkFactor);
        	finalWorkspaceTranslationY = -(halfScreenSize-workspaceInNormalScaleHalfScreenSize-LauncherAppState.getInstance().getDynamicGrid().getDeviceProfile().searchBarSpaceHeightPx);
        	float mNormalScaleHotseatHeight = mLauncher.getHotseat().getHeight()*mNormalScaleShrinkFactor;
			finalHotseatTranslationY = ((float)mLauncher.getHotseat().getHeight()-mNormalScaleHotseatHeight)/2;
		}

        final int duration= getResources().getInteger(R.integer.config_appsCustomizeWorkspaceShrinkTime);

        if (snapPage == -1) {
            snapPage = getPageNearestToCenterOfScreen();
        }
        snapToPage(snapPage, duration, mZoomInInterpolator);

        LauncherViewPropertyAnimator workspaceScale = new LauncherViewPropertyAnimator(this);
        workspaceScale.scaleX(mNewScale)
            .scaleY(mNewScale)
            .translationY(finalWorkspaceTranslationY)
            .setDuration(duration)
            .setInterpolator(mZoomInInterpolator);

        LauncherViewPropertyAnimator hotseatScale = new LauncherViewPropertyAnimator(hotseat);
        hotseatScale.scaleX(mNewScale)
            .scaleY(mNewScale)
            .translationY(finalHotseatTranslationY)
            .setDuration(duration)
            .setInterpolator(mZoomInInterpolator);
        anim.play(workspaceScale);
        anim.play(hotseatScale);
        
        pageIndicator.setVisibility(enable ? View.INVISIBLE : View.VISIBLE);
        
        /**
         * show add sunfeng  page view
         */
        if(qingcheng_page_indicator!=null){
        	hideWSPageView();
        	hideWsLongPageView();
            if(enable){
            	showWSPageView();
            }else{
            	hideWSPageView();
            }
        }

        updateCustomContentVisibility();
        
        /// M: [ALPS01257663] Correct usage of updateVoiceButtonProxyVisible().
        //mLauncher.updateVoiceButtonProxyVisible(false);

        showPiflow(mState == State.NORMAL && !isNormalScaling);
        return anim;
    }

    static class AlphaUpdateListener implements AnimatorUpdateListener, AnimatorListener {
        View view;
        public AlphaUpdateListener(View v) {
            view = v;
        }

        @Override
        public void onAnimationUpdate(ValueAnimator arg0) {
            updateVisibility(view);
        }

        public static void updateVisibility(View view) {
            // We want to avoid the extra layout pass by setting the views to GONE unless
            // accessibility is on, in which case not setting them to GONE causes a glitch.
            int invisibleState = sAccessibilityEnabled ? GONE : INVISIBLE;
            if (view.getAlpha() < ALPHA_CUTOFF_THRESHOLD && view.getVisibility() != invisibleState) {
                view.setVisibility(invisibleState);
            } else if (view.getAlpha() > ALPHA_CUTOFF_THRESHOLD
                    && view.getVisibility() != VISIBLE) {
                view.setVisibility(VISIBLE);
            }
        }

        @Override
        public void onAnimationCancel(Animator arg0) {
        }

        @Override
        public void onAnimationEnd(Animator arg0) {
            updateVisibility(view);
        }

        @Override
        public void onAnimationRepeat(Animator arg0) {
        }

        @Override
        public void onAnimationStart(Animator arg0) {
            // We want the views to be visible for animation, so fade-in/out is visible
            /// M: [ALPS01380434] Set views to be visible for animation according to alpha value in onAnimationUpdate().
            //view.setVisibility(VISIBLE);
        }
    }

    @Override
    public void onLauncherTransitionPrepare(Launcher l, boolean animated, boolean toWorkspace) {
        onTransitionPrepare();
    }

    @Override
    public void onLauncherTransitionStart(Launcher l, boolean animated, boolean toWorkspace) {
    }

    @Override
    public void onLauncherTransitionStep(Launcher l, float t) {
        mTransitionProgress = t;
    }

    @Override
    public void onLauncherTransitionEnd(Launcher l, boolean animated, boolean toWorkspace) {
        Trace.beginSection("Workspace.onLauncherTransitionEnd");
        onTransitionEnd();
        Trace.endSection();
    }

    private void onTransitionPrepare() {
        mIsSwitchingState = true;

        // Invalidate here to ensure that the pages are rendered during the state change transition.
        invalidate();

        updateChildrenLayersEnabled(false);
        hideCustomContentIfNecessary();
    }

    void updateCustomContentVisibility() {
        int visibility = (mState == Workspace.State.NORMAL && !isNormalScaling) ? VISIBLE : INVISIBLE;
        if (hasCustomContent()) {
            mWorkspaceScreens.get(CUSTOM_CONTENT_SCREEN_ID).setVisibility(visibility);
        }
    }

    void showCustomContentIfNecessary() {
        boolean show  = mState == Workspace.State.NORMAL;
        if (show && hasCustomContent()) {
            mWorkspaceScreens.get(CUSTOM_CONTENT_SCREEN_ID).setVisibility(VISIBLE);
        }
    }

    void hideCustomContentIfNecessary() {
        boolean hide  = mState != Workspace.State.NORMAL;
        if (hide && hasCustomContent()) {
            disableLayoutTransitions();
            mWorkspaceScreens.get(CUSTOM_CONTENT_SCREEN_ID).setVisibility(INVISIBLE);
            enableLayoutTransitions();
        }
    }

    private void onTransitionEnd() {
        mIsSwitchingState = false;
        updateChildrenLayersEnabled(false);
        showCustomContentIfNecessary();
    }

    @Override
    public View getContent() {
        return this;
    }

    /**
     * Draw the View v into the given Canvas.
     *
     * @param v the view to draw
     * @param destCanvas the canvas to draw on
     * @param padding the horizontal and vertical padding to use when drawing
     */
    private static void drawDragView(View v, Canvas destCanvas, int padding) {
        final Rect clipRect = sTempRect;
        v.getDrawingRect(clipRect);

        boolean textVisible = false;

        destCanvas.save();
        if (v instanceof TextView) {
            /*Drawable d = ((TextView) v).getCompoundDrawables()[1];
            Rect bounds = getDrawableBounds(d);
            //clipRect.set(0, 0, bounds.width() + padding, bounds.height() + padding);
            d.draw(destCanvas);*/
            ShortcutInfo info = (ShortcutInfo)v.getTag();
            if(info.container == -101){
                destCanvas.translate(-v.getScrollX(), -v.getScrollY());
                destCanvas.clipRect(clipRect, Op.REPLACE);
                v.draw(destCanvas);
            }else{
                destCanvas.translate(-v.getScrollX()-25, -v.getScrollY()-25);
                destCanvas.clipRect(clipRect, Op.REPLACE);
                v.draw(destCanvas);
            }
        } else {
            if (v instanceof FolderIcon) {
                // For FolderIcons the text can bleed into the icon area, and so we need to
                // hide the text completely (which can't be achieved by clipping).
                if (((FolderIcon) v).getTextVisible()) {
                    ((FolderIcon) v).setTextVisible(false);
                    textVisible = true;
                }
            }
            destCanvas.translate(-v.getScrollX() + padding / 2, -v.getScrollY() + padding / 2);
            //destCanvas.translate(-v.getScrollX(), -v.getScrollY());
            destCanvas.clipRect(clipRect, Op.REPLACE);
            v.draw(destCanvas);

            // Restore text visibility of FolderIcon if necessary
            if (textVisible) {
                ((FolderIcon) v).setTextVisible(true);
            }
        }
        destCanvas.restore();
    }

    /**
     * Returns a new bitmap to show when the given View is being dragged around.
     * Responsibility for the bitmap is transferred to the caller.
     * @param expectedPadding padding to add to the drag view. If a different padding was used
     * its value will be changed
     */
    public Bitmap createDragBitmap(View v, AtomicInteger expectedPadding) {
        Bitmap b;

        int padding = expectedPadding.get();
        //nanjing 0314 modify for calendar
//        if (v instanceof TextView) {
//            //Drawable d = ((TextView) v).getCompoundDrawables()[1];
//            //Drawable d2 = ((TextView) v).getCompoundDrawablesRelative()[1];
//            //v.buildDrawingCache(true);
//            Bitmap bitmap =  v.getDrawingCache();
//            Drawable d = new BitmapDrawable(bitmap);
////            Drawable d = new BitmapDrawable(((TextView) v).getDrawingCache());
//            Rect bounds = getDrawableBounds(d);
//            b = Bitmap.createBitmap(bounds.width() + padding,
//                    bounds.height() + padding, Bitmap.Config.ARGB_8888);
//            expectedPadding.set(padding - bounds.left - bounds.top);
//        } else {
        b = Bitmap.createBitmap(v.getWidth() + padding, v.getHeight() + padding, Bitmap.Config.ARGB_8888);
//        }

        mCanvas.setBitmap(b);
        drawDragView(v, mCanvas, padding);
        mCanvas.setBitmap(null);

        return b;
    }

    /**
     * Returns a new bitmap to be used as the object outline, e.g. to visualize the drop location.
     * Responsibility for the bitmap is transferred to the caller.
     */
    private Bitmap createDragOutline(View v, int padding) {
        final int outlineColor = getResources().getColor(R.color.outline_color);
        final Bitmap b = Bitmap.createBitmap(
                v.getWidth() + padding, v.getHeight() + padding, Bitmap.Config.ARGB_8888);

        mCanvas.setBitmap(b);
        drawDragView(v, mCanvas, padding);
        mOutlineHelper.applyExpensiveOutlineWithBlur(b, mCanvas, outlineColor, outlineColor);
        mCanvas.setBitmap(null);
        return b;
    }

    /**
     * Returns a new bitmap to be used as the object outline, e.g. to visualize the drop location.
     * Responsibility for the bitmap is transferred to the caller.
     */
    private Bitmap createDragOutline(Bitmap orig, int padding, int w, int h,
            boolean clipAlpha) {
        final int outlineColor = getResources().getColor(R.color.outline_color);
        final Bitmap b = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        mCanvas.setBitmap(b);

        Rect src = new Rect(0, 0, orig.getWidth(), orig.getHeight());
        float scaleFactor = Math.min((w - padding) / (float) orig.getWidth(),
                (h - padding) / (float) orig.getHeight());
        int scaledWidth = (int) (scaleFactor * orig.getWidth());
        int scaledHeight = (int) (scaleFactor * orig.getHeight());
        Rect dst = new Rect(0, 0, scaledWidth, scaledHeight);

        // center the image
        dst.offset((w - scaledWidth) / 2, (h - scaledHeight) / 2);

        mCanvas.drawBitmap(orig, src, dst, null);
        mOutlineHelper.applyExpensiveOutlineWithBlur(b, mCanvas, outlineColor, outlineColor,
                clipAlpha);
        mCanvas.setBitmap(null);

        return b;
    }

    void startDrag(CellLayout.CellInfo cellInfo) {
        View child = cellInfo.cell;

        /// M: [ALPS01263567] Abnormal case, if user long press on all apps button and then
        /// long press on other shortcuts in hotseat, the dragInfo will be
        /// null, exception will happen, so need return directly.
        if (child != null && child.getTag() == null) {
            LauncherLog.d(TAG, "Abnormal start drag: cellInfo = " + cellInfo + ",child = " + child);
            return;
        }

        // Make sure the drag was started by a long press as opposed to a long click.
        if (!child.isInTouchMode()) {
            return;
        }

        mDragInfo = cellInfo;
        child.setVisibility(INVISIBLE);
        CellLayout layout = (CellLayout) child.getParent().getParent();
        layout.prepareChildForDrag(child);

        beginDragShared(child, this);
    }

    public void beginDragShared(View child, DragSource source) {
        child.clearFocus();
        child.setPressed(false);

        // The outline is used to visualize where the item will land if dropped
        mDragOutline = createDragOutline(child, DRAG_BITMAP_PADDING);

        mLauncher.onDragStarted(child);
        // The drag bitmap follows the touch point around on the screen
        AtomicInteger padding = new AtomicInteger(DRAG_BITMAP_PADDING);
        final Bitmap b = createDragBitmap(child, padding);

        final int bmpWidth = b.getWidth();
        final int bmpHeight = b.getHeight();

        float scale = mLauncher.getDragLayer().getLocationInDragLayer(child, mTempXY);
        int dragLayerX = Math.round(mTempXY[0] - (bmpWidth - scale * child.getWidth()) / 2);
        int dragLayerY = Math.round(mTempXY[1] - (bmpHeight - scale * bmpHeight) / 2
                        - padding.get() / 2);
        if (LauncherLog.DEBUG_DRAG) {
            LauncherLog.d(TAG, "beginDragShared: child = " + child + ", source = " + source
                    + ", dragLayerX = " + dragLayerX + ", dragLayerY = " + dragLayerY);
        }

        LauncherAppState app = LauncherAppState.getInstance();
        DeviceProfile grid = app.getDynamicGrid().getDeviceProfile();
        Point dragVisualizeOffset = null;
        Rect dragRect = null;
        if (child instanceof BubbleTextView) {
            int iconSize = grid.iconSizePx;
            int top = child.getPaddingTop();
            int left = (bmpWidth - iconSize) / 2;
            int right = left + iconSize;
            int bottom = top + iconSize;
            dragLayerY += top;
            // Note: The drag region is used to calculate drag layer offsets, but the
            // dragVisualizeOffset in addition to the dragRect (the size) to position the outline.
            dragVisualizeOffset = new Point(-padding.get() / 2, padding.get() / 2);
            dragRect = new Rect(left, top, right, bottom);
        } else if (child instanceof FolderIcon) {
            int previewSize = grid.folderIconSizePx;
            dragRect = new Rect(0, child.getPaddingTop(), child.getWidth(), previewSize);
        }

        // Clear the pressed state if necessary
        if (child instanceof BubbleTextView) {
            BubbleTextView icon = (BubbleTextView) child;
            icon.clearPressedBackground();
        }

        if (child.getTag() == null || !(child.getTag() instanceof ItemInfo)) {
            String msg = "Drag started with a view that has no tag set. This "
                    + "will cause a crash (issue 11627249) down the line. "
                    + "View: " + child + "  tag: " + child.getTag();
            throw new IllegalStateException(msg);
        }

        DragView dv = mDragController.startDrag(b, dragLayerX, dragLayerY, source, child.getTag(),
                DragController.DRAG_ACTION_MOVE, dragVisualizeOffset, dragRect, scale);
        dv.setIntrinsicIconScaleFactor(source.getIntrinsicIconScaleFactor());

        if (child.getParent() instanceof ShortcutAndWidgetContainer) {
            mDragSourceInternal = (ShortcutAndWidgetContainer) child.getParent();
        }

        b.recycle();
    }

    public void beginExternalDragShared(View child, DragSource source) {
        LauncherAppState app = LauncherAppState.getInstance();
        DeviceProfile grid = app.getDynamicGrid().getDeviceProfile();
        int iconSize = grid.iconSizePx;

        // Notify launcher of drag start
        mLauncher.onDragStarted(child);

        // Compose a new drag bitmap that is of the icon size
        AtomicInteger padding = new AtomicInteger(DRAG_BITMAP_PADDING);
        final Bitmap tmpB = createDragBitmap(child, padding);
        Bitmap b = Bitmap.createBitmap(iconSize, iconSize, Bitmap.Config.ARGB_8888);
        Paint p = new Paint();
        p.setFilterBitmap(true);
        mCanvas.setBitmap(b);
        mCanvas.drawBitmap(tmpB, new Rect(0, 0, tmpB.getWidth(), tmpB.getHeight()),
                new Rect(0, 0, iconSize, iconSize), p);
        mCanvas.setBitmap(null);

        // Find the child's location on the screen
        int bmpWidth = tmpB.getWidth();
        float iconScale = (float) bmpWidth / iconSize;
        float scale = mLauncher.getDragLayer().getLocationInDragLayer(child, mTempXY) * iconScale;
        int dragLayerX = Math.round(mTempXY[0] - (bmpWidth - scale * child.getWidth()) / 2);
        int dragLayerY = Math.round(mTempXY[1]);

        // Note: The drag region is used to calculate drag layer offsets, but the
        // dragVisualizeOffset in addition to the dragRect (the size) to position the outline.
        Point dragVisualizeOffset = new Point(-padding.get() / 2, padding.get() / 2);
        Rect dragRect = new Rect(0, 0, iconSize, iconSize);

        if (child.getTag() == null || !(child.getTag() instanceof ItemInfo)) {
            String msg = "Drag started with a view that has no tag set. This "
                    + "will cause a crash (issue 11627249) down the line. "
                    + "View: " + child + "  tag: " + child.getTag();
            throw new IllegalStateException(msg);
        }

        // Start the drag
        DragView dv = mDragController.startDrag(b, dragLayerX, dragLayerY, source, child.getTag(),
                DragController.DRAG_ACTION_MOVE, dragVisualizeOffset, dragRect, scale);
        dv.setIntrinsicIconScaleFactor(source.getIntrinsicIconScaleFactor());

        // Recycle temporary bitmaps
        tmpB.recycle();
    }

    void addApplicationShortcut(ShortcutInfo info, CellLayout target, long container, long screenId,
            int cellX, int cellY, boolean insertAtFirst, int intersectX, int intersectY) {
        View view = mLauncher.createShortcut(R.layout.application, target, (ShortcutInfo) info);

        final int[] cellXY = new int[2];
        target.findCellForSpanThatIntersects(cellXY, 1, 1, intersectX, intersectY);

        if (LauncherLog.DEBUG) {
            LauncherLog.d(TAG, "addApplicationShortcut: info = " + info + ", view = "
                    + view + ", container = " + container + ", screenId = " + screenId
                    + ", cellXY[0] = " + cellXY[0] + ", cellXY[1] = " + cellXY[1]
                    + ", insertAtFirst = " + insertAtFirst);
        }
        addInScreen(view, container, screenId, cellXY[0], cellXY[1], 1, 1, insertAtFirst);

        LauncherModel.addOrMoveItemInDatabase(mLauncher, info, container, screenId, cellXY[0],
                cellXY[1]);
    }

    public boolean transitionStateShouldAllowDrop() {
        return (!isSwitchingState() || mTransitionProgress > 0.5f);
    }

    /**
     * {@inheritDoc}
     */
    public boolean acceptDrop(DragObject d) {
        // If it's an external drop (e.g. from All Apps), check if it should be accepted
        CellLayout dropTargetLayout = mDropToLayout;
        if (d.dragSource != this) {
            // Don't accept the drop if we're not over a screen at time of drop
            if (dropTargetLayout == null) {
                return false;
            }
            if (!transitionStateShouldAllowDrop()) return false;

            mDragViewVisualCenter = getDragViewVisualCenter(d.x, d.y, d.xOffset, d.yOffset,
                    d.dragView, mDragViewVisualCenter);

            // We want the point to be mapped to the dragTarget.
            if (mLauncher.isHotseatLayout(dropTargetLayout)) {
                mapPointFromSelfToHotseatLayout(mLauncher.getHotseat(), mDragViewVisualCenter);
            } else {
                mapPointFromSelfToChild(dropTargetLayout, mDragViewVisualCenter, null);
            }

            int spanX = 1;
            int spanY = 1;
            if (mDragInfo != null) {
                final CellLayout.CellInfo dragCellInfo = mDragInfo;
                spanX = dragCellInfo.spanX;
                spanY = dragCellInfo.spanY;
            } else {
                final ItemInfo dragInfo = (ItemInfo) d.dragInfo;
                spanX = dragInfo.spanX;
                spanY = dragInfo.spanY;
            }

            int minSpanX = spanX;
            int minSpanY = spanY;
            if (d.dragInfo instanceof PendingAddWidgetInfo) {
                minSpanX = ((PendingAddWidgetInfo) d.dragInfo).minSpanX;
                minSpanY = ((PendingAddWidgetInfo) d.dragInfo).minSpanY;
            }

            mTargetCell = findNearestArea((int) mDragViewVisualCenter[0],
                    (int) mDragViewVisualCenter[1], minSpanX, minSpanY, dropTargetLayout,
                    mTargetCell);
            float distance = dropTargetLayout.getDistanceFromCell(mDragViewVisualCenter[0],
                    mDragViewVisualCenter[1], mTargetCell);
            if (mCreateUserFolderOnDrop && willCreateUserFolder((ItemInfo) d.dragInfo,
                    dropTargetLayout, mTargetCell, distance, true)) {
                return true;
            }

            if (mAddToExistingFolderOnDrop && willAddToExistingUserFolder((ItemInfo) d.dragInfo,
                    dropTargetLayout, mTargetCell, distance)) {
                return true;
            }

            int[] resultSpan = new int[2];
            mTargetCell = dropTargetLayout.performReorder((int) mDragViewVisualCenter[0],
                    (int) mDragViewVisualCenter[1], minSpanX, minSpanY, spanX, spanY,
                    null, mTargetCell, resultSpan, CellLayout.MODE_ACCEPT_DROP);
            boolean foundCell = mTargetCell[0] >= 0 && mTargetCell[1] >= 0;

            // Don't accept the drop if there's no room for the item
            if (!foundCell) {
                // Don't show the message if we are dropping on the AllApps button and the hotseat
                // is full
                boolean isHotseat = mLauncher.isHotseatLayout(dropTargetLayout);
                if (mTargetCell != null && isHotseat) {
                    Hotseat hotseat = mLauncher.getHotseat();
                    if (hotseat.isAllAppsButtonRank(
                            hotseat.getOrderInHotseat(mTargetCell[0], mTargetCell[1]))) {
                        return false;
                    }
                }

                mLauncher.showOutOfSpaceMessage(isHotseat);
                return false;
            }

            /// M: Don't accept the drop if there exists one Widget which providerName equals the providerName of the
            // dragInfo.
            if (d.dragInfo instanceof PendingAddWidgetInfo) {
                PendingAddWidgetInfo info = (PendingAddWidgetInfo) d.dragInfo;
                if (searchWidget(this, info.componentName.getClassName()) != null) {
                    if (!mScroller.isFinished()) {
                        mScroller.abortAnimation();
                    }
                    return false;
                }
            }
        }

        long screenId = getIdForScreen(dropTargetLayout);
        if (screenId == EXTRA_EMPTY_SCREEN_ID) {
            commitExtraEmptyScreen();
        }

        return true;
    }

    boolean willCreateUserFolder(ItemInfo info, CellLayout target, int[] targetCell, float
            distance, boolean considerTimeout) {
        if (distance > mMaxDistanceForFolderCreation) return false;
        View dropOverView = target.getChildAt(targetCell[0], targetCell[1]);

        if (dropOverView != null) {
            CellLayout.LayoutParams lp = (CellLayout.LayoutParams) dropOverView.getLayoutParams();
            if (lp.useTmpCoords && (lp.tmpCellX != lp.cellX || lp.tmpCellY != lp.tmpCellY)) {
                return false;
            }
        }

        boolean hasntMoved = false;
        if (mDragInfo != null) {
            hasntMoved = dropOverView == mDragInfo.cell;
        }

        if (dropOverView == null || hasntMoved || (considerTimeout && !mCreateUserFolderOnDrop)) {
            return false;
        }

        boolean aboveShortcut = (dropOverView.getTag() instanceof ShortcutInfo);
        boolean willBecomeShortcut =
                (info.itemType == LauncherSettings.Favorites.ITEM_TYPE_APPLICATION ||
                info.itemType == LauncherSettings.Favorites.ITEM_TYPE_SHORTCUT);

        return (aboveShortcut && willBecomeShortcut);
    }

    boolean willAddToExistingUserFolder(Object dragInfo, CellLayout target, int[] targetCell,
            float distance) {
        if (distance > mMaxDistanceForFolderCreation) return false;
        View dropOverView = target.getChildAt(targetCell[0], targetCell[1]);

        if (dropOverView != null) {
            CellLayout.LayoutParams lp = (CellLayout.LayoutParams) dropOverView.getLayoutParams();
            if (lp.useTmpCoords && (lp.tmpCellX != lp.cellX || lp.tmpCellY != lp.tmpCellY)) {
                return false;
            }
        }

        if (dropOverView instanceof FolderIcon) {
            FolderIcon fi = (FolderIcon) dropOverView;
            if (fi.acceptDrop(dragInfo)) {
                return true;
            }
        }
        return false;
    }

    /// M: ALPS01577456
    /// if no space in destination screen, then drag an application icon into existing folder and key up quickly,
    /// the je will be occur. so add this method to avoid the case: only allow add to existing folder if FolderIcon 
    /// can accept drop and is ring state {@
    boolean willAddToExistingUserFolderIfRingState(Object dragInfo, CellLayout target, int[] targetCell,
            float distance) {
        if (distance > mMaxDistanceForFolderCreation) return false;
        View dropOverView = target.getChildAt(targetCell[0], targetCell[1]);

        Log.d(TAG, "willAddToExistingUserFolderIfRingState, dropOverView: " + dropOverView);
        if (dropOverView != null) {
            CellLayout.LayoutParams lp = (CellLayout.LayoutParams) dropOverView.getLayoutParams();
            if (lp.useTmpCoords && (lp.tmpCellX != lp.cellX || lp.tmpCellY != lp.tmpCellY)) {
                return false;
            }
        }

        if (dropOverView instanceof FolderIcon) {
            FolderIcon fi = (FolderIcon) dropOverView;
            if (fi.acceptDrop(dragInfo) && fi.isRingState()) {
                fi.resetRingState();
                return true;
            }
        }
        return false;
    }
    /// @}

    boolean createUserFolderIfNecessary(View newView, long container, CellLayout target,
            int[] targetCell, float distance, boolean external, DragView dragView,
            Runnable postAnimationRunnable) {
        if (distance > mMaxDistanceForFolderCreation) return false;
        View v = target.getChildAt(targetCell[0], targetCell[1]);
        if (LauncherLog.DEBUG) {
            LauncherLog.d(TAG, "createUserFolderIfNecessary: newView = " + newView
                    + ", mDragInfo = " + mDragInfo + ", container = " + container + ", target = "
                    + target + ", targetCell[0] = " + targetCell[0] + ", targetCell[1] = "
                    + targetCell[1] + ", external = " + external + ", dragView = " + dragView
                    + ", v = " + v + ", mCreateUserFolderOnDrop = " + mCreateUserFolderOnDrop);
        }

        boolean hasntMoved = false;
        if (mDragInfo != null) {
            CellLayout cellParent = getParentCellLayoutForView(mDragInfo.cell);
            hasntMoved = (mDragInfo.cellX == targetCell[0] &&
                    mDragInfo.cellY == targetCell[1]) && (cellParent == target);
        }

        if (v == null || hasntMoved || !mCreateUserFolderOnDrop) {
            if (LauncherLog.DEBUG) {
                LauncherLog.d(TAG, "Do not create user folder: hasntMoved = " + hasntMoved + ", mCreateUserFolderOnDrop = "
                        + mCreateUserFolderOnDrop + ", v = " + v);
            }
            return false;
        }
        mCreateUserFolderOnDrop = false;
        final long screenId = (targetCell == null) ? mDragInfo.screenId : getIdForScreen(target);

        boolean aboveShortcut = (v.getTag() instanceof ShortcutInfo);
        boolean willBecomeShortcut = (newView.getTag() instanceof ShortcutInfo);

        if (LauncherLog.DEBUG) {
            LauncherLog.d(TAG, "createUserFolderIfNecessary: aboveShortcut = "
                    + aboveShortcut + ", willBecomeShortcut = " + willBecomeShortcut);
        }

        if (aboveShortcut && willBecomeShortcut) {
            ShortcutInfo sourceInfo = (ShortcutInfo) newView.getTag();
            ShortcutInfo destInfo = (ShortcutInfo) v.getTag();
            // if the drag started here, we need to remove it from the workspace
            if (!external) {
                getParentCellLayoutForView(mDragInfo.cell).removeView(mDragInfo.cell);
            }

            Rect folderLocation = new Rect();
            float scale = mLauncher.getDragLayer().getDescendantRectRelativeToSelf(v, folderLocation);
            target.removeView(v);

            FolderIcon fi =
                mLauncher.addFolder(target, container, screenId, targetCell[0], targetCell[1]);
            destInfo.cellX = -1;
            destInfo.cellY = -1;
            sourceInfo.cellX = -1;
            sourceInfo.cellY = -1;

            // If the dragView is null, we can't animate
            boolean animate = dragView != null;
            if (animate) {
                fi.performCreateAnimation(destInfo, v, sourceInfo, dragView, folderLocation, scale,
                        postAnimationRunnable);
            } else {
                fi.addItem(destInfo);
                fi.addItem(sourceInfo);
            }
            return true;
        }
        return false;
    }

    boolean addToExistingFolderIfNecessary(View newView, CellLayout target, int[] targetCell,
            float distance, DragObject d, boolean external) {
        if (distance > mMaxDistanceForFolderCreation) return false;

        View dropOverView = target.getChildAt(targetCell[0], targetCell[1]);
        if (LauncherLog.DEBUG) {
            LauncherLog.d(TAG, "createUserFolderIfNecessary: newView = " + newView + ", target = " + target
                    + ", targetCell[0] = " + targetCell[0] + ", targetCell[1] = " + targetCell[1] + ", external = "
                    + external + ", d = " + d + ", dropOverView = " + dropOverView);
        }
        if (!mAddToExistingFolderOnDrop) return false;
        mAddToExistingFolderOnDrop = false;

        if (dropOverView instanceof FolderIcon) {
            FolderIcon fi = (FolderIcon) dropOverView;
            if (fi.acceptDrop(d.dragInfo)) {
                fi.onDrop(d);

                // if the drag started here, we need to remove it from the workspace
                if (!external) {
                    getParentCellLayoutForView(mDragInfo.cell).removeView(mDragInfo.cell);
                }
                if (LauncherLog.DEBUG) {
                    LauncherLog.d(TAG, "addToExistingFolderIfNecessary: fi = " + fi
                            + ", d = " + d);
                }
                return true;
            }
        }
        return false;
    }

    public void onDrop(final DragObject d) {
        mDragViewVisualCenter = getDragViewVisualCenter(d.x, d.y, d.xOffset, d.yOffset, d.dragView,
                mDragViewVisualCenter);

        CellLayout dropTargetLayout = mDropToLayout;

        // We want the point to be mapped to the dragTarget.
        if (dropTargetLayout != null) {
            if (mLauncher.isHotseatLayout(dropTargetLayout)) {
                mapPointFromSelfToHotseatLayout(mLauncher.getHotseat(), mDragViewVisualCenter);
            } else {
            	if (getPageIndexForScreenId(getIdForScreen(dropTargetLayout))!=mCurrentPage
            			&& mTargetCell != null && mTargetCell[0] == -1 && mTargetCell[1] == -1) {
					snapToScreenId(getIdForScreen(dropTargetLayout));
					if (getPageIndexForScreenId(getIdForScreen(dropTargetLayout)) > mCurrentPage) {
						mTargetCell[0] = 0;
						mTargetCell[1] = (int) LauncherAppState.getInstance().getDynamicGrid().getDeviceProfile().numRows-1;
					} else {
						mTargetCell[0] = (int) LauncherAppState.getInstance().getDynamicGrid().getDeviceProfile().numColumns-1;
						mTargetCell[1] = (int) LauncherAppState.getInstance().getDynamicGrid().getDeviceProfile().numRows-1;
					}
				}
                mapPointFromSelfToChild(dropTargetLayout, mDragViewVisualCenter, null);
            }
        }
        if (LauncherLog.DEBUG_DRAG) {
            LauncherLog.d(TAG, "onDrop 1: drag view = " + d.dragView + ", dragInfo = " + d.dragInfo
                    + ", dragSource  = " + d.dragSource + ", dropTargetLayout = " + dropTargetLayout
                    + ", mDragInfo = " + mDragInfo + ", mInScrollArea = " + mInScrollArea
                    + ", this = " + this);
        }

        int snapScreen = -1;
        boolean resizeOnDrop = false;
        if (d.dragSource != this) {
            final int[] touchXY = new int[] { (int) mDragViewVisualCenter[0],
                    (int) mDragViewVisualCenter[1] };
            onDropExternal(touchXY, d.dragInfo, dropTargetLayout, false, d);
        } else if (mDragInfo != null) {
            final View cell = mDragInfo.cell;

            Runnable resizeRunnable = null;
            if (dropTargetLayout != null && !d.cancelled) {
                // Move internally
                boolean hasMovedLayouts = (getParentCellLayoutForView(cell) != dropTargetLayout);
                boolean hasMovedIntoHotseat = mLauncher.isHotseatLayout(dropTargetLayout);
                long container = hasMovedIntoHotseat ?
                        LauncherSettings.Favorites.CONTAINER_HOTSEAT :
                        LauncherSettings.Favorites.CONTAINER_DESKTOP;
                long screenId = (mTargetCell[0] < 0) ?
                        mDragInfo.screenId : getIdForScreen(dropTargetLayout);
                int spanX = mDragInfo != null ? mDragInfo.spanX : 1;
                int spanY = mDragInfo != null ? mDragInfo.spanY : 1;
                // First we find the cell nearest to point at which the item is
                // dropped, without any consideration to whether there is an item there.

                mTargetCell = findNearestArea((int) mDragViewVisualCenter[0], (int)
                        mDragViewVisualCenter[1], spanX, spanY, dropTargetLayout, mTargetCell);
                float distance = dropTargetLayout.getDistanceFromCell(mDragViewVisualCenter[0],
                        mDragViewVisualCenter[1], mTargetCell);
                if (LauncherLog.DEBUG_DRAG) {
                    LauncherLog.d(TAG, "onDrop 2: cell = " + cell + ", screenId = " + screenId
                            + ", mInScrollArea = " + mInScrollArea + ", mTargetCell = " + mTargetCell
                            + ", this = " + this);
                }

                // If the item being dropped is a shortcut and the nearest drop
                // cell also contains a shortcut, then create a folder with the two shortcuts.
                if (!mInScrollArea && createUserFolderIfNecessary(cell, container,
                        dropTargetLayout, mTargetCell, distance, false, d.dragView, null)) {
                    return;
                }

                if (addToExistingFolderIfNecessary(cell, dropTargetLayout, mTargetCell,
                        distance, d, false)) {
                    return;
                }

                // Aside from the special case where we're dropping a shortcut onto a shortcut,
                // we need to find the nearest cell location that is vacant
                ItemInfo item = (ItemInfo) d.dragInfo;
                int minSpanX = item.spanX;
                int minSpanY = item.spanY;
                if (item.minSpanX > 0 && item.minSpanY > 0) {
                    minSpanX = item.minSpanX;
                    minSpanY = item.minSpanY;
                }

                int[] resultSpan = new int[2];
                mTargetCell = dropTargetLayout.performReorder((int) mDragViewVisualCenter[0],
                        (int) mDragViewVisualCenter[1], minSpanX, minSpanY, spanX, spanY, cell,
                        mTargetCell, resultSpan, CellLayout.MODE_ON_DROP);

                /// M: We think a cell has been found only if the target cell
                /// and the span are both valid.
                boolean foundCell = mTargetCell[0] >= 0 && mTargetCell[1] >= 0 && resultSpan[0] > 0
                        && resultSpan[1] > 0;
                if (LauncherLog.DEBUG) {
                    LauncherLog.d(TAG, "onDrop 3: foundCell = " + foundCell + "mTargetCell = ("
                            + mTargetCell[0] + ", " + mTargetCell[1] + "), resultSpan = ("
                            + resultSpan[0] + "," + resultSpan[1] + "), item.span = (" + item.spanX
                            + ", " + item.spanY + ") ,item.minSpan = (" + item.minSpanX + ", "
                            + item.minSpanY + "),minSpan = (" + minSpanX + "," + minSpanY + ").");
                }

                // if the widget resizes on drop
                if (foundCell && (cell instanceof AppWidgetHostView) &&
                        (resultSpan[0] != item.spanX || resultSpan[1] != item.spanY)) {
                    resizeOnDrop = true;
                    item.spanX = resultSpan[0];
                    item.spanY = resultSpan[1];
                    AppWidgetHostView awhv = (AppWidgetHostView) cell;
                    AppWidgetResizeFrame.updateWidgetSizeRanges(awhv, mLauncher, resultSpan[0],
                            resultSpan[1]);
                }

                if (getScreenIdForPageIndex(mCurrentPage) != screenId && !hasMovedIntoHotseat) {
                    snapScreen = getPageIndexForScreenId(screenId);
                    snapToPage(snapScreen);
                }

                if (foundCell) {
                    final ItemInfo info = (ItemInfo) cell.getTag();
                    if (hasMovedLayouts) {
                        // Reparent the view
                        CellLayout parentCell = getParentCellLayoutForView(cell);
                        if (parentCell != null) {
                            parentCell.removeView(cell);
                        } else if (LauncherAppState.isDogfoodBuild()) {
                            throw new NullPointerException("mDragInfo.cell has null parent");
                        }
                        addInScreen(cell, container, screenId, mTargetCell[0], mTargetCell[1],
                                info.spanX, info.spanY);
                    }

                    // update the item's position after drop
                    CellLayout.LayoutParams lp = (CellLayout.LayoutParams) cell.getLayoutParams();
                    lp.cellX = lp.tmpCellX = mTargetCell[0];
                    lp.cellY = lp.tmpCellY = mTargetCell[1];
                    lp.cellHSpan = item.spanX;
                    lp.cellVSpan = item.spanY;
                    lp.isLockedToGrid = true;

                    if (container != LauncherSettings.Favorites.CONTAINER_HOTSEAT &&
                            cell instanceof LauncherAppWidgetHostView) {
                        final CellLayout cellLayout = dropTargetLayout;
                        // We post this call so that the widget has a chance to be placed
                        // in its final location

                        final LauncherAppWidgetHostView hostView = (LauncherAppWidgetHostView) cell;
                        AppWidgetProviderInfo pinfo = hostView.getAppWidgetInfo();
                        if (pinfo != null &&
                                pinfo.resizeMode != AppWidgetProviderInfo.RESIZE_NONE) {
                            final Runnable addResizeFrame = new Runnable() {
                                public void run() {
                                    DragLayer dragLayer = mLauncher.getDragLayer();
                                    dragLayer.addResizeFrame(info, hostView, cellLayout);
                                }
                            };
                            resizeRunnable = (new Runnable() {
                                public void run() {

                                    if (!isPageMoving()) {
                                    	postDelayed(addResizeFrame, getResources().getInteger(R.integer.config_appsCustomizeWorkspaceShrinkTime)+100);
                                        //addResizeFrame.run();
                                    } else if (mState == State.NORMAL) {
                                    	mDelayedResizeRunnable = addResizeFrame;
									}
                                }
                            });
                        }
                    }

                    LauncherModel.modifyItemInDatabase(mLauncher, info, container, screenId, lp.cellX,
                            lp.cellY, item.spanX, item.spanY);
                } else {
                    // If we can't find a drop location, we return the item to its original position
                    CellLayout.LayoutParams lp = (CellLayout.LayoutParams) cell.getLayoutParams();
                    mTargetCell[0] = lp.cellX;
                    mTargetCell[1] = lp.cellY;
                    CellLayout layout = (CellLayout) cell.getParent().getParent();
                    layout.markCellsAsOccupiedForView(cell);
                }
            }

            final CellLayout parent = (CellLayout) cell.getParent().getParent();
            final Runnable finalResizeRunnable = resizeRunnable;
            // Prepare it to be animated into its new position
            // This must be called after the view has been re-parented
            final Runnable onCompleteRunnable = new Runnable() {
                @Override
                public void run() {
                    mAnimatingViewIntoPlace = false;
                    updateChildrenLayersEnabled(false);
                    if (finalResizeRunnable != null) {
                        finalResizeRunnable.run();
                    }
                }
            };
            mAnimatingViewIntoPlace = true;
            if (d.dragView.hasDrawn()) {
                final ItemInfo info = (ItemInfo) cell.getTag();
                if (info.itemType == LauncherSettings.Favorites.ITEM_TYPE_APPWIDGET) {
                    int animationType = resizeOnDrop ? ANIMATE_INTO_POSITION_AND_RESIZE :
                            ANIMATE_INTO_POSITION_AND_DISAPPEAR;
                    animateWidgetDrop(info, parent, d.dragView,
                            onCompleteRunnable, animationType, cell, false);
                } else {
                    int duration = snapScreen < 0 ? -1 : ADJACENT_SCREEN_DROP_DURATION;
                    mLauncher.getDragLayer().animateViewIntoPosition(d.dragView, cell, duration,
                            onCompleteRunnable, this);
                }
            } else {
                d.deferDragViewCleanupPostAnimation = false;
                cell.setVisibility(VISIBLE);
            }
            parent.onDropChild(cell);
        }
    }
    
    // Add auto reorder function for MyUI Jing.Wu 20150925 start
    public boolean isReorderPlayed = false;
    public void autoReorder() {
    	if (!(mState == State.OVERVIEW || mState == State.OVERVIEW_HIDDEN)) {
			return;
		}
    	Pair<ArrayList<ItemInfo>, int[][]> mPair = LauncherModel.getNeedReorderItems(mLauncher, mCurrentPage);
    	if (mPair!=null) {
    		final CellLayout mCellLayout = (CellLayout) getChildAt(mCurrentPage);
    		final ShortcutAndWidgetContainer mContainer = mCellLayout.getShortcutsAndWidgets();
    		final ArrayList<ItemInfo> mNeedReorderItemInfos = mPair.first;
    		final int[][] newLocations = mPair.second;
    		
    		mCellLayout.setUseTempCoords(false);
    		for (int i = 0; i < mNeedReorderItemInfos.size(); i++) {
				ItemInfo mInfo = mNeedReorderItemInfos.get(i);
				View mChild = mCellLayout.getChildAt(mInfo.cellX, mInfo.cellY);
				isReorderPlayed = mCellLayout.animateChildToPosition(mChild, newLocations[i][0], newLocations[i][1], 300, 0, true, false);
			}
    		postDelayed(new Runnable() {
				@Override
				public void run() {
					// TODO Auto-generated method stub
		    		for (int i = 0; i < mNeedReorderItemInfos.size(); i++) {
						ItemInfo mInfo = mNeedReorderItemInfos.get(i);
						View mChild = mCellLayout.getChildAt(mInfo.cellX, mInfo.cellY);
		                // update the item's position after drop
		                CellLayout.LayoutParams lp = (CellLayout.LayoutParams) mChild.getLayoutParams();
		                if (!isReorderPlayed) {
		                	lp.cellX = lp.tmpCellX = newLocations[i][0];
			                lp.cellY = lp.tmpCellY = newLocations[i][1];
			                lp.cellHSpan = mInfo.spanX;
			                lp.cellVSpan = mInfo.spanY;
			                lp.isLockedToGrid = true;
			                mContainer.setupLp(lp);
			                mChild.requestLayout();
						}

		                LauncherModel.modifyItemInDatabase(mLauncher, mInfo, mInfo.container, mInfo.screenId, lp.cellX,
		                        lp.cellY, mInfo.spanX, mInfo.spanY);
		    		}
                    updateChildrenLayersEnabled(false);
                
				}
			}, 350);
        
		}
    }
    // Add auto reorder function for MyUI Jing.Wu 20150925 end

    public void setFinalScrollForPageChange(int pageIndex) {
        CellLayout cl = (CellLayout) getChildAt(pageIndex);
        if (cl != null) {
            mSavedScrollX = getScrollX();
            mSavedTranslationX = cl.getTranslationX();
            mSavedRotationY = cl.getRotationY();
            final int newX = getScrollForPage(pageIndex);
            setScrollX(newX);
            cl.setTranslationX(0f);
            cl.setRotationY(0f);
        }
    }

    public void resetFinalScrollForPageChange(int pageIndex) {
        if (pageIndex >= 0) {
        	try {
	            CellLayout cl = (CellLayout) getChildAt(pageIndex);
	            setScrollX(mSavedScrollX);
				cl.setTranslationX(mSavedTranslationX);
				cl.setRotationY(mSavedRotationY);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
        }
    }

    public void getViewLocationRelativeToSelf(View v, int[] location) {
        getLocationInWindow(location);
        int x = location[0];
        int y = location[1];

        v.getLocationInWindow(location);
        int vX = location[0];
        int vY = location[1];

        location[0] = vX - x;
        location[1] = vY - y;
    }

    public void onDragEnter(DragObject d) {
        if (LauncherLog.DEBUG_DRAG) {
            LauncherLog.d(TAG, "onDragEnter: d = " + d + ", mDragTargetLayout = "
                    + mDragTargetLayout);
        }

        mDragEnforcer.onDragEnter();
        mCreateUserFolderOnDrop = false;
        mAddToExistingFolderOnDrop = false;

        mDropToLayout = null;
        CellLayout layout = getCurrentDropLayout();
        setCurrentDropLayout(layout);
        setCurrentDragOverlappingLayout(layout);

        if (!workspaceInModalState()) {
            mLauncher.getDragLayer().showPageHints();
        }
    }

    /** Return a rect that has the cellWidth/cellHeight (left, top), and
     * widthGap/heightGap (right, bottom) */
    static Rect getCellLayoutMetrics(Launcher launcher, int orientation) {
        LauncherAppState app = LauncherAppState.getInstance();
        DeviceProfile grid = app.getDynamicGrid().getDeviceProfile();

        Display display = launcher.getWindowManager().getDefaultDisplay();
        Point smallestSize = new Point();
        Point largestSize = new Point();
        display.getCurrentSizeRange(smallestSize, largestSize);
        int countX = (int) grid.numColumns;
        int countY = (int) grid.numRows;
        if (orientation == CellLayout.LANDSCAPE) {
            if (mLandscapeCellLayoutMetrics == null) {
                Rect padding = grid.getWorkspacePadding(CellLayout.LANDSCAPE);
                int width = largestSize.x - padding.left - padding.right;
                int height = smallestSize.y - padding.top - padding.bottom;
                mLandscapeCellLayoutMetrics = new Rect();
                mLandscapeCellLayoutMetrics.set(
                        grid.calculateCellWidth(width, countX),
                        grid.calculateCellHeight(height, countY), 0, 0);
            }
            return mLandscapeCellLayoutMetrics;
        } else if (orientation == CellLayout.PORTRAIT) {
            if (mPortraitCellLayoutMetrics == null) {
                Rect padding = grid.getWorkspacePadding(CellLayout.PORTRAIT);
                int width = smallestSize.x - padding.left - padding.right;
                int height = largestSize.y - padding.top - padding.bottom;
                mPortraitCellLayoutMetrics = new Rect();
                mPortraitCellLayoutMetrics.set(
                        grid.calculateCellWidth(width, countX),
                        grid.calculateCellHeight(height, countY), 0, 0);
            }
            return mPortraitCellLayoutMetrics;
        }
        return null;
    }
    
    // Add for MyUI Jing.Wu 20160311 start
    public void turnOnCreateFolderFlag() {
    	mCreateUserFolderOnDrop = true;
    }
    public void turnOnAddExistingFolderFlag() {
    	mAddToExistingFolderOnDrop = true;
    }
    // Add for MyUI Jing.Wu 20160311 end

    public void onDragExit(DragObject d) {
        mDragEnforcer.onDragExit();

        // Here we store the final page that will be dropped to, if the workspace in fact
        // receives the drop
        if (mInScrollArea) {
            if (isPageMoving()) {
                // If the user drops while the page is scrolling, we should use that page as the
                // destination instead of the page that is being hovered over.
                mDropToLayout = (CellLayout) getPageAt(getNextPage());
            } else {
                mDropToLayout = mDragOverlappingLayout;
            }
        } else {
            mDropToLayout = mDragTargetLayout;
        }

        if (mDragMode == DRAG_MODE_CREATE_FOLDER) {
            mCreateUserFolderOnDrop = true;
        } else if (mDragMode == DRAG_MODE_ADD_TO_FOLDER) {
            mAddToExistingFolderOnDrop = true;
        }

        // Reset the scroll area and previous drag target
        onResetScrollArea();
        if (LauncherLog.DEBUG_DRAG) {
            LauncherLog.d(TAG, "doDragExit: drag source = " + (d != null ? d.dragSource : null)
                    + ", drag info = " + (d != null ? d.dragInfo : null) + ", mDragTargetLayout = "
                    + mDragTargetLayout + ", mIsPageMoving = " + mIsPageMoving);
        }
        setCurrentDropLayout(null);
        setCurrentDragOverlappingLayout(null);

        mSpringLoadedDragController.cancel();

        if (!mIsPageMoving) {
            hideOutlines();
        }
        mLauncher.getDragLayer().hidePageHints();
    }

    void setCurrentDropLayout(CellLayout layout) {
        if (mDragTargetLayout != null) {
            mDragTargetLayout.revertTempState();
            mDragTargetLayout.onDragExit();
        }
        mDragTargetLayout = layout;
        if (mDragTargetLayout != null) {
            mDragTargetLayout.onDragEnter();
        }
        cleanupReorder(true);
        cleanupFolderCreation();
        setCurrentDropOverCell(-1, -1);
    }

    void setCurrentDragOverlappingLayout(CellLayout layout) {
        if (mDragOverlappingLayout != null) {
            mDragOverlappingLayout.setIsDragOverlapping(false);
        }
        mDragOverlappingLayout = layout;
        if (mDragOverlappingLayout != null) {
            mDragOverlappingLayout.setIsDragOverlapping(true);
        }
        invalidate();
    }

    void setCurrentDropOverCell(int x, int y) {
        if (x != mDragOverX || y != mDragOverY) {
            mDragOverX = x;
            mDragOverY = y;
            setDragMode(DRAG_MODE_NONE);
        }
    }

    void setDragMode(int dragMode) {
        if (dragMode != mDragMode) {
            if (dragMode == DRAG_MODE_NONE) {
                cleanupAddToFolder();
                // We don't want to cancel the re-order alarm every time the target cell changes
                // as this feels to slow / unresponsive.
                cleanupReorder(false);
                cleanupFolderCreation();
            } else if (dragMode == DRAG_MODE_ADD_TO_FOLDER) {
                cleanupReorder(true);
                cleanupFolderCreation();
            } else if (dragMode == DRAG_MODE_CREATE_FOLDER) {
                cleanupAddToFolder();
                cleanupReorder(true);
            } else if (dragMode == DRAG_MODE_REORDER) {
                cleanupAddToFolder();
                cleanupFolderCreation();
            }
            mDragMode = dragMode;
        }
    }

    private void cleanupFolderCreation() {
        if (mDragFolderRingAnimator != null) {
            mDragFolderRingAnimator.animateToNaturalState();
            mDragFolderRingAnimator = null;
        }
        mFolderCreationAlarm.setOnAlarmListener(null);
        mFolderCreationAlarm.cancelAlarm();
    }

    private void cleanupAddToFolder() {
        if (mDragOverFolderIcon != null) {
            mDragOverFolderIcon.onDragExit(null);
            mDragOverFolderIcon = null;
        }
    }

    private void cleanupReorder(boolean cancelAlarm) {
        // Any pending reorders are canceled
        if (cancelAlarm) {
            mReorderAlarm.cancelAlarm();
        }
        mLastReorderX = -1;
        mLastReorderY = -1;
    }

   /*
    *
    * Convert the 2D coordinate xy from the parent View's coordinate space to this CellLayout's
    * coordinate space. The argument xy is modified with the return result.
    *
    * if cachedInverseMatrix is not null, this method will just use that matrix instead of
    * computing it itself; we use this to avoid redundant matrix inversions in
    * findMatchingPageForDragOver
    *
    */
   void mapPointFromSelfToChild(View v, float[] xy, Matrix cachedInverseMatrix) {
       xy[0] = xy[0] - v.getLeft();
       xy[1] = xy[1] - v.getTop();
   }

   boolean isPointInSelfOverHotseat(int x, int y, Rect r) {
       if (r == null) {
           r = new Rect();
       }
       mTempPt[0] = x;
       mTempPt[1] = y;
       mLauncher.getDragLayer().getDescendantCoordRelativeToSelf(this, mTempPt, true);

       LauncherAppState app = LauncherAppState.getInstance();
       DeviceProfile grid = app.getDynamicGrid().getDeviceProfile();
       r = grid.getHotseatRect();
       if (r.contains(mTempPt[0], mTempPt[1])) {
           return true;
       }
       return false;
   }

   void mapPointFromSelfToHotseatLayout(Hotseat hotseat, float[] xy) {
       mTempPt[0] = (int) xy[0];
       mTempPt[1] = (int) xy[1];
       mLauncher.getDragLayer().getDescendantCoordRelativeToSelf(this, mTempPt, true);
       mLauncher.getDragLayer().mapCoordInSelfToDescendent(hotseat.getLayout(), mTempPt);

       xy[0] = mTempPt[0];
       xy[1] = mTempPt[1];
   }

   /*
    *
    * Convert the 2D coordinate xy from this CellLayout's coordinate space to
    * the parent View's coordinate space. The argument xy is modified with the return result.
    *
    */
   void mapPointFromChildToSelf(View v, float[] xy) {
       xy[0] += v.getLeft();
       xy[1] += v.getTop();
   }

   static private float squaredDistance(float[] point1, float[] point2) {
        float distanceX = point1[0] - point2[0];
        float distanceY = point2[1] - point2[1];
        return distanceX * distanceX + distanceY * distanceY;
   }

    /*
     *
     * This method returns the CellLayout that is currently being dragged to. In order to drag
     * to a CellLayout, either the touch point must be directly over the CellLayout, or as a second
     * strategy, we see if the dragView is overlapping any CellLayout and choose the closest one
     *
     * Return null if no CellLayout is currently being dragged over
     *
     */
    private CellLayout findMatchingPageForDragOver(
            DragView dragView, float originX, float originY, boolean exact) {
        // We loop through all the screens (ie CellLayouts) and see which ones overlap
        // with the item being dragged and then choose the one that's closest to the touch point
    	// Modify for MyUI Jing.Wu 20160221 start
        int screenStart = 0;
        int screenEnd = getChildCount()-1;
    	if (!QCConfig.autoDeleteAndAddEmptyScreen) {
			screenStart = mShouldStartPageNumb;
			screenEnd = mShouldEndPageNumb;
		}
    	// Modify for MyUI Jing.Wu 20160221 end
        CellLayout bestMatchingScreen = null;
        float smallestDistSoFar = Float.MAX_VALUE;

        for (int i = screenStart; i <= screenEnd; i++) {
            // The custom content screen is not a valid drag over option
            if (mScreenOrder.get(i) == CUSTOM_CONTENT_SCREEN_ID) {
                continue;
            }

            CellLayout cl = (CellLayout) getChildAt(i);

            final float[] touchXy = {originX, originY};
            // Transform the touch coordinates to the CellLayout's local coordinates
            // If the touch point is within the bounds of the cell layout, we can return immediately
            cl.getMatrix().invert(mTempInverseMatrix);
            mapPointFromSelfToChild(cl, touchXy, mTempInverseMatrix);

            if (touchXy[0] >= 0 && touchXy[0] <= cl.getWidth() &&
                    touchXy[1] >= 0 && touchXy[1] <= cl.getHeight()) {
                return cl;
            }

            if (!exact) {
                // Get the center of the cell layout in screen coordinates
                final float[] cellLayoutCenter = mTempCellLayoutCenterCoordinates;
                cellLayoutCenter[0] = cl.getWidth()/2;
                cellLayoutCenter[1] = cl.getHeight()/2;
                mapPointFromChildToSelf(cl, cellLayoutCenter);

                touchXy[0] = originX;
                touchXy[1] = originY;

                // Calculate the distance between the center of the CellLayout
                // and the touch point
                float dist = squaredDistance(touchXy, cellLayoutCenter);

                if (dist < smallestDistSoFar) {
                    smallestDistSoFar = dist;
                    bestMatchingScreen = cl;
                }

                /// M: modify to cycle sliding screen.
                if (isSupportCycleSlidingScreen()) {
                    int page = indexOfChild(bestMatchingScreen);
                    if (page == screenEnd) {
                        bestMatchingScreen = (CellLayout) getChildAt(0);
                    } else if (page == 0) {
                        bestMatchingScreen = (CellLayout) getChildAt(screenEnd);
                    }
                }
            }
        }

        return bestMatchingScreen;
    }

    // This is used to compute the visual center of the dragView. This point is then
    // used to visualize drop locations and determine where to drop an item. The idea is that
    // the visual center represents the user's interpretation of where the item is, and hence
    // is the appropriate point to use when determining drop location.
    private float[] getDragViewVisualCenter(int x, int y, int xOffset, int yOffset,
            DragView dragView, float[] recycle) {
        float res[];
        if (recycle == null) {
            res = new float[2];
        } else {
            res = recycle;
        }

        // First off, the drag view has been shifted in a way that is not represented in the
        // x and y values or the x/yOffsets. Here we account for that shift.
        x += getResources().getDimensionPixelSize(R.dimen.dragViewOffsetX);
        y += getResources().getDimensionPixelSize(R.dimen.dragViewOffsetY);

        // These represent the visual top and left of drag view if a dragRect was provided.
        // If a dragRect was not provided, then they correspond to the actual view left and
        // top, as the dragRect is in that case taken to be the entire dragView.
        // R.dimen.dragViewOffsetY.
        int left = x - xOffset;
        int top = y - yOffset;

        // In order to find the visual center, we shift by half the dragRect
        res[0] = left + dragView.getDragRegion().width() / 2;
        res[1] = top + dragView.getDragRegion().height() / 2;

        return res;
    }

    private boolean isDragWidget(DragObject d) {
        return (d.dragInfo instanceof LauncherAppWidgetInfo ||
                d.dragInfo instanceof PendingAddWidgetInfo);
    }
    private boolean isExternalDragWidget(DragObject d) {
        return d.dragSource != this && isDragWidget(d);
    }

    public void onDragOver(DragObject d) {
        if (LauncherLog.DEBUG_DRAG) {
            LauncherLog.d(TAG, "onDragOver: d = " + d + ", dragInfo = " + d.dragInfo + ", mInScrollArea = " + mInScrollArea
                    + ", mIsSwitchingState = " + mIsSwitchingState);
        }

        // Skip drag over events while we are dragging over side pages
        if (mInScrollArea || !transitionStateShouldAllowDrop()) return;

        Rect r = new Rect();
        CellLayout layout = null;
        ItemInfo item = (ItemInfo) d.dragInfo;
        if (item == null) {
            if (LauncherAppState.isDogfoodBuild()) {
                throw new NullPointerException("DragObject has null info");
            }
            return;
        }
        
        int[] location =new int[2];
		d.dragView.getLocationInWindow(location);
		int dw = d.dragView.getWidth();
		int dh = d.dragView.getHeight();

        int index = getDragToScreen((location[0] + dw/2), (location[1] + dh/2));
        if(index != -1 && !isReordering(true)){
        	showWidgetViewPage(mPageViewWsList,index, false);
        	if(mLauncher.hasCustomContentToLeft()){
        		index =index + 1;
        	}
        	snapToPage(index,PAGE_SNAP_ANIMATION_DURATION);
        }

        // Ensure that we have proper spans for the item that we are dropping
        if (item.spanX < 0 || item.spanY < 0) throw new RuntimeException("Improper spans found");
        mDragViewVisualCenter = getDragViewVisualCenter(d.x, d.y, d.xOffset, d.yOffset,
            d.dragView, mDragViewVisualCenter);

        final View child = (mDragInfo == null) ? null : mDragInfo.cell;
        // Identify whether we have dragged over a side page
        if (workspaceInModalState()) {
            if (mLauncher.getHotseat() != null && !isExternalDragWidget(d)) {
                if (isPointInSelfOverHotseat(d.x, d.y, r)) {
                    layout = mLauncher.getHotseat().getLayout();
                }
            }
            if (layout == null) {
                layout = findMatchingPageForDragOver(d.dragView, d.x, d.y, false);
            }
            if (layout != mDragTargetLayout) {
                setCurrentDropLayout(layout);
                setCurrentDragOverlappingLayout(layout);

                boolean isInSpringLoadedMode = (mState == State.SPRING_LOADED);
                if (isInSpringLoadedMode) {
                    if (mLauncher.isHotseatLayout(layout)) {
                        mSpringLoadedDragController.cancel();
                    } else {
                        mSpringLoadedDragController.setAlarm(mDragTargetLayout);
                    }
                }
            }
        } else {
            // Test to see if we are over the hotseat otherwise just use the current page
            if (mLauncher.getHotseat() != null && !isDragWidget(d)) {
                if (isPointInSelfOverHotseat(d.x, d.y, r)) {
                    layout = mLauncher.getHotseat().getLayout();
                }
            }
            if (layout == null) {
                layout = getCurrentDropLayout();
            }
            if (layout != mDragTargetLayout) {
                setCurrentDropLayout(layout);
                setCurrentDragOverlappingLayout(layout);
            }
        }

        // Handle the drag over
        if (mDragTargetLayout != null) {
            // We want the point to be mapped to the dragTarget.
            if (mLauncher.isHotseatLayout(mDragTargetLayout)) {
                mapPointFromSelfToHotseatLayout(mLauncher.getHotseat(), mDragViewVisualCenter);
            } else {
                mapPointFromSelfToChild(mDragTargetLayout, mDragViewVisualCenter, null);
            }

            ItemInfo info = (ItemInfo) d.dragInfo;

            int minSpanX = item.spanX;
            int minSpanY = item.spanY;
            if (item.minSpanX > 0 && item.minSpanY > 0) {
                minSpanX = item.minSpanX;
                minSpanY = item.minSpanY;
            }

            mTargetCell = findNearestArea((int) mDragViewVisualCenter[0],
                    (int) mDragViewVisualCenter[1], minSpanX, minSpanY,
                    mDragTargetLayout, mTargetCell);
            int reorderX = mTargetCell[0];
            int reorderY = mTargetCell[1];

            setCurrentDropOverCell(mTargetCell[0], mTargetCell[1]);

            float targetCellDistance = mDragTargetLayout.getDistanceFromCell(
                    mDragViewVisualCenter[0], mDragViewVisualCenter[1], mTargetCell);

            final View dragOverView = mDragTargetLayout.getChildAt(mTargetCell[0],
                    mTargetCell[1]);

            manageFolderFeedback(info, mDragTargetLayout, mTargetCell,
                    targetCellDistance, dragOverView);

            boolean nearestDropOccupied = mDragTargetLayout.isNearestDropLocationOccupied((int)
                    mDragViewVisualCenter[0], (int) mDragViewVisualCenter[1], item.spanX,
                    item.spanY, child, mTargetCell);

            if (!nearestDropOccupied) {
                mDragTargetLayout.visualizeDropLocation(child, mDragOutline,
                        (int) mDragViewVisualCenter[0], (int) mDragViewVisualCenter[1],
                        mTargetCell[0], mTargetCell[1], item.spanX, item.spanY, false,
                        d.dragView.getDragVisualizeOffset(), d.dragView.getDragRegion());
            } else if ((mDragMode == DRAG_MODE_NONE || mDragMode == DRAG_MODE_REORDER)
                    && !mReorderAlarm.alarmPending() && (mLastReorderX != reorderX ||
                    mLastReorderY != reorderY)) {

                int[] resultSpan = new int[2];
                mDragTargetLayout.performReorder((int) mDragViewVisualCenter[0],
                        (int) mDragViewVisualCenter[1], minSpanX, minSpanY, item.spanX, item.spanY,
                        child, mTargetCell, resultSpan, CellLayout.MODE_SHOW_REORDER_HINT);

                // Otherwise, if we aren't adding to or creating a folder and there's no pending
                // reorder, then we schedule a reorder
                ReorderAlarmListener listener = new ReorderAlarmListener(mDragViewVisualCenter,
                        minSpanX, minSpanY, item.spanX, item.spanY, d.dragView, child);
                mReorderAlarm.setOnAlarmListener(listener);
                mReorderAlarm.setAlarm(REORDER_TIMEOUT);
            }

            if (mDragMode == DRAG_MODE_CREATE_FOLDER || mDragMode == DRAG_MODE_ADD_TO_FOLDER ||
                    !nearestDropOccupied) {
                if (mDragTargetLayout != null) {
                	isFolderDragMode = true;
                    mDragTargetLayout.revertTempState();
                }
            } else {
				isFolderDragMode = false;
			}
        }
    }

    private void manageFolderFeedback(ItemInfo info, CellLayout targetLayout,
            int[] targetCell, float distance, View dragOverView) {
        boolean userFolderPending = willCreateUserFolder(info, targetLayout, targetCell, distance,
                false);

        if (mDragMode == DRAG_MODE_NONE && userFolderPending &&
                !mFolderCreationAlarm.alarmPending()) {
            mFolderCreationAlarm.setOnAlarmListener(new
                    FolderCreationAlarmListener(targetLayout, targetCell[0], targetCell[1]));
            mFolderCreationAlarm.setAlarm(FOLDER_CREATION_TIMEOUT);
            return;
        }

        boolean willAddToFolder =
                willAddToExistingUserFolder(info, targetLayout, targetCell, distance);

        if (willAddToFolder && mDragMode == DRAG_MODE_NONE) {
            mDragOverFolderIcon = ((FolderIcon) dragOverView);
            mDragOverFolderIcon.onDragEnter(info);
            if (targetLayout != null) {
                targetLayout.clearDragOutlines();
            }
            setDragMode(DRAG_MODE_ADD_TO_FOLDER);
            return;
        }

        if (mDragMode == DRAG_MODE_ADD_TO_FOLDER && !willAddToFolder) {
            setDragMode(DRAG_MODE_NONE);
        }
        if (mDragMode == DRAG_MODE_CREATE_FOLDER && !userFolderPending) {
            setDragMode(DRAG_MODE_NONE);
        }

        return;
    }

    class FolderCreationAlarmListener implements OnAlarmListener {
        CellLayout layout;
        int cellX;
        int cellY;

        public FolderCreationAlarmListener(CellLayout layout, int cellX, int cellY) {
            this.layout = layout;
            this.cellX = cellX;
            this.cellY = cellY;
        }

        public void onAlarm(Alarm alarm) {
            setDragMode(DRAG_MODE_CREATE_FOLDER);
            if (mDragFolderRingAnimator != null) {
                // This shouldn't happen ever, but just in case, make sure we clean up the mess.
            	mDragFolderRingAnimator.animateToNaturalState();
            	//mDragFolderRingAnimator.setLastCell();
            }
            mDragFolderRingAnimator = new FolderRingAnimator(mLauncher, null);
            mDragFolderRingAnimator.setCell(cellX, cellY);
            mDragFolderRingAnimator.setCellLayout(layout);
            mDragFolderRingAnimator.animateToAcceptState();
            layout.showFolderAccept(mDragFolderRingAnimator);
            layout.clearDragOutlines();
        }
    }

    class ReorderAlarmListener implements OnAlarmListener {
        float[] dragViewCenter;
        int minSpanX, minSpanY, spanX, spanY;
        DragView dragView;
        View child;

        public ReorderAlarmListener(float[] dragViewCenter, int minSpanX, int minSpanY, int spanX,
                int spanY, DragView dragView, View child) {
            this.dragViewCenter = dragViewCenter;
            this.minSpanX = minSpanX;
            this.minSpanY = minSpanY;
            this.spanX = spanX;
            this.spanY = spanY;
            this.child = child;
            this.dragView = dragView;
        }

        public void onAlarm(Alarm alarm) {
            int[] resultSpan = new int[2];
            mTargetCell = findNearestArea((int) mDragViewVisualCenter[0],
                    (int) mDragViewVisualCenter[1], minSpanX, minSpanY, mDragTargetLayout,
                    mTargetCell);
            mLastReorderX = mTargetCell[0];
            mLastReorderY = mTargetCell[1];

            mTargetCell = mDragTargetLayout.performReorder((int) mDragViewVisualCenter[0],
                (int) mDragViewVisualCenter[1], minSpanX, minSpanY, spanX, spanY,
                child, mTargetCell, resultSpan, CellLayout.MODE_DRAG_OVER);

            if (mTargetCell[0] < 0 || mTargetCell[1] < 0) {
                mDragTargetLayout.revertTempState();
            } else {
                setDragMode(DRAG_MODE_REORDER);
            }

            boolean resize = resultSpan[0] != spanX || resultSpan[1] != spanY;
            mDragTargetLayout.visualizeDropLocation(child, mDragOutline,
                (int) mDragViewVisualCenter[0], (int) mDragViewVisualCenter[1],
                mTargetCell[0], mTargetCell[1], resultSpan[0], resultSpan[1], resize,
                dragView.getDragVisualizeOffset(), dragView.getDragRegion());
        }
    }

    @Override
    public void getHitRectRelativeToDragLayer(Rect outRect) {
        // We want the workspace to have the whole area of the display (it will find the correct
        // cell layout to drop to in the existing drag/drop logic.
        mLauncher.getDragLayer().getDescendantRectRelativeToSelf(this, outRect);
    }

    /**
     * Add the item specified by dragInfo to the given layout.
     * @return true if successful
     */
    public boolean addExternalItemToScreen(ItemInfo dragInfo, CellLayout layout) {
        if (LauncherLog.DEBUG) {
            LauncherLog.d(TAG, "addExternalItemToScreen: dragInfo = " + dragInfo
                    + ", layout = " + layout);
        }
        if (layout.findCellForSpan(mTempEstimate, dragInfo.spanX, dragInfo.spanY)) {
            onDropExternal(dragInfo.dropPos, (ItemInfo) dragInfo, (CellLayout) layout, false);
            return true;
        }
        mLauncher.showOutOfSpaceMessage(mLauncher.isHotseatLayout(layout));
        return false;
    }

    private void onDropExternal(int[] touchXY, Object dragInfo,
            CellLayout cellLayout, boolean insertAtFirst) {
        onDropExternal(touchXY, dragInfo, cellLayout, insertAtFirst, null);
    }

    /**
     * Drop an item that didn't originate on one of the workspace screens.
     * It may have come from Launcher (e.g. from all apps or customize), or it may have
     * come from another app altogether.
     *
     * NOTE: This can also be called when we are outside of a drag event, when we want
     * to add an item to one of the workspace screens.
     */
    private void onDropExternal(final int[] touchXY, final Object dragInfo,
            final CellLayout cellLayout, boolean insertAtFirst, DragObject d) {
        final Runnable exitSpringLoadedRunnable = new Runnable() {
            @Override
            public void run() {
                mLauncher.exitSpringLoadedDragModeDelayed(true,
                        Launcher.EXIT_SPRINGLOADED_MODE_SHORT_TIMEOUT, null);
            }
        };

        ItemInfo info = (ItemInfo) dragInfo;
        int spanX = info.spanX;
        int spanY = info.spanY;
        if (mDragInfo != null) {
            spanX = mDragInfo.spanX;
            spanY = mDragInfo.spanY;
        }

        final long container = mLauncher.isHotseatLayout(cellLayout) ?
                LauncherSettings.Favorites.CONTAINER_HOTSEAT :
                    LauncherSettings.Favorites.CONTAINER_DESKTOP;
        final long screenId = getIdForScreen(cellLayout);

        ///M. ALPS01925678, when boot up, it didn't bind item finish,
        ///commitExtraEmptyScreen will be fail, so do it again.
        if (screenId == EXTRA_EMPTY_SCREEN_ID) {
            if (LauncherLog.DEBUG) {
                LauncherLog.d(TAG, "onDropExternal: screenId = " + screenId +
                    "mLauncher.isWorkspaceLoading() = " + mLauncher.isWorkspaceLoading());
            }
            int index = getPageIndexForScreenId(EXTRA_EMPTY_SCREEN_ID);
            CellLayout cl = mWorkspaceScreens.get(EXTRA_EMPTY_SCREEN_ID);
            mWorkspaceScreens.remove(EXTRA_EMPTY_SCREEN_ID);
            mScreenOrder.remove(EXTRA_EMPTY_SCREEN_ID);

            long newId = LauncherAppState.getLauncherProvider().generateNewScreenId();
            mWorkspaceScreens.put(newId, cl);
            mScreenOrder.add(newId);

        	// Add for AddExtraEmptyScreen Jing.Wu 20160105 start
            if (!QCConfig.autoDeleteAndAddEmptyScreen) {
            	addExtraEmptyScreen();
			}
        	// Add for AddExtraEmptyScreen Jing.Wu 20160105 end

            // Update the page indicator marker
            if (getPageIndicator() != null) {
                getPageIndicator().updateMarker(index, getPageIndicatorMarker(index));
                getPageIndicator().changeMarkerVisibility(index, true);
            }
            // Update the model for the new screen

            mLauncher.getModel().updateWorkspaceScreenOrder(mLauncher, mScreenOrder);
        }
        /// M.

        if (!mLauncher.isHotseatLayout(cellLayout)
                && screenId != getScreenIdForPageIndex(mCurrentPage)
                && mState != State.SPRING_LOADED) {
            snapToScreenId(screenId, null);
        }

        if (LauncherLog.DEBUG) {
            LauncherLog.d(TAG, "onDropExternal: touchXY[0] = "
                    + ((touchXY != null) ? touchXY[0] : -1) + ", touchXY[1] = "
                    + ((touchXY != null) ? touchXY[1] : -1) + ", dragInfo = " + dragInfo
                    + ",info = " + info + ", cellLayout = " + cellLayout + ", insertAtFirst = "
                    + insertAtFirst + ", dragInfo = " + d.dragInfo + ", screenId = " + screenId
                    + ", container = " + container);
        }


        if (info instanceof PendingAddItemInfo) {
            final PendingAddItemInfo pendingInfo = (PendingAddItemInfo) dragInfo;

            boolean findNearestVacantCell = true;
            if (pendingInfo.itemType == LauncherSettings.Favorites.ITEM_TYPE_SHORTCUT) {
                mTargetCell = findNearestArea((int) touchXY[0], (int) touchXY[1], spanX, spanY,
                        cellLayout, mTargetCell);
                float distance = cellLayout.getDistanceFromCell(mDragViewVisualCenter[0],
                        mDragViewVisualCenter[1], mTargetCell);
                if (willCreateUserFolder((ItemInfo) d.dragInfo, cellLayout, mTargetCell,
                        distance, true) || willAddToExistingUserFolder((ItemInfo) d.dragInfo,
                                cellLayout, mTargetCell, distance)) {
                    findNearestVacantCell = false;
                }
            }

            final ItemInfo item = (ItemInfo) d.dragInfo;
            boolean updateWidgetSize = false;
            if (findNearestVacantCell) {
                int minSpanX = item.spanX;
                int minSpanY = item.spanY;
                if (item.minSpanX > 0 && item.minSpanY > 0) {
                    minSpanX = item.minSpanX;
                    minSpanY = item.minSpanY;
                }
                int[] resultSpan = new int[2];
                mTargetCell = cellLayout.performReorder((int) mDragViewVisualCenter[0],
                        (int) mDragViewVisualCenter[1], minSpanX, minSpanY, info.spanX, info.spanY,
                        null, mTargetCell, resultSpan, CellLayout.MODE_ON_DROP_EXTERNAL);

                if (resultSpan[0] != item.spanX || resultSpan[1] != item.spanY) {
                    updateWidgetSize = true;
                }
                item.spanX = resultSpan[0];
                item.spanY = resultSpan[1];
            }

            Runnable onAnimationCompleteRunnable = new Runnable() {
                @Override
                public void run() {
                    // Normally removeExtraEmptyScreen is called in Workspace#onDragEnd, but when
                    // adding an item that may not be dropped right away (due to a config activity)
                    // we defer the removal until the activity returns.
                    deferRemoveExtraEmptyScreen();

                    // When dragging and dropping from customization tray, we deal with creating
                    // widgets/shortcuts/folders in a slightly different way
                    switch (pendingInfo.itemType) {
                    case LauncherSettings.Favorites.ITEM_TYPE_APPWIDGET:
                        int span[] = new int[2];
                        span[0] = item.spanX;
                        span[1] = item.spanY;
                        mLauncher.addAppWidgetFromDrop((PendingAddWidgetInfo) pendingInfo,
                                container, screenId, mTargetCell, span, null);
                        break;
                    case LauncherSettings.Favorites.ITEM_TYPE_SHORTCUT:
                        mLauncher.processShortcutFromDrop(pendingInfo.componentName,
                                container, screenId, mTargetCell, null);
                        break;
                    default:
                        throw new IllegalStateException("Unknown item type: " +
                                pendingInfo.itemType);
                    }
                }
            };
            View finalView = pendingInfo.itemType == LauncherSettings.Favorites.ITEM_TYPE_APPWIDGET
                    ? ((PendingAddWidgetInfo) pendingInfo).boundWidget : null;

            if (finalView instanceof AppWidgetHostView && updateWidgetSize) {
                AppWidgetHostView awhv = (AppWidgetHostView) finalView;
                AppWidgetResizeFrame.updateWidgetSizeRanges(awhv, mLauncher, item.spanX,
                        item.spanY);
            }

            int animationStyle = ANIMATE_INTO_POSITION_AND_DISAPPEAR;
            if (pendingInfo.itemType == LauncherSettings.Favorites.ITEM_TYPE_APPWIDGET &&
                    ((PendingAddWidgetInfo) pendingInfo).info.configure != null) {
                animationStyle = ANIMATE_INTO_POSITION_AND_REMAIN;
            }
            animateWidgetDrop(info, cellLayout, d.dragView, onAnimationCompleteRunnable,
                    animationStyle, finalView, true);
        } else {
            // This is for other drag/drop cases, like dragging from All Apps
            View view = null;

            switch (info.itemType) {
            case LauncherSettings.Favorites.ITEM_TYPE_APPLICATION:
            case LauncherSettings.Favorites.ITEM_TYPE_SHORTCUT:
                if (info.container == NO_ID && info instanceof AppInfo) {
                    // Came from all apps -- make a copy
                    info = new ShortcutInfo((AppInfo) info);
                }
                view = mLauncher.createShortcut(R.layout.application, cellLayout,
                        (ShortcutInfo) info);
                break;
            case LauncherSettings.Favorites.ITEM_TYPE_FOLDER:
                view = FolderIcon.fromXml(R.layout.folder_icon, mLauncher, cellLayout,
                        (FolderInfo) info, mIconCache);
                break;
            default:
                throw new IllegalStateException("Unknown item type: " + info.itemType);
            }

            // First we find the cell nearest to point at which the item is
            // dropped, without any consideration to whether there is an item there.
            if (touchXY != null) {
                mTargetCell = findNearestArea((int) touchXY[0], (int) touchXY[1], spanX, spanY,
                        cellLayout, mTargetCell);
                float distance = cellLayout.getDistanceFromCell(mDragViewVisualCenter[0],
                        mDragViewVisualCenter[1], mTargetCell);
                d.postAnimationRunnable = exitSpringLoadedRunnable;
                if (createUserFolderIfNecessary(view, container, cellLayout, mTargetCell, distance,
                        true, d.dragView, d.postAnimationRunnable)) {
                    return;
                }
                if (addToExistingFolderIfNecessary(view, cellLayout, mTargetCell, distance, d,
                        true)) {
                    return;
                }
            }

            if (touchXY != null) {
                // when dragging and dropping, just find the closest free spot
                mTargetCell = cellLayout.performReorder((int) mDragViewVisualCenter[0],
                        (int) mDragViewVisualCenter[1], 1, 1, 1, 1,
                        null, mTargetCell, null, CellLayout.MODE_ON_DROP_EXTERNAL);
            } else {
                cellLayout.findCellForSpan(mTargetCell, 1, 1);
            }
            // Add the item to DB before adding to screen ensures that the container and other
            // values of the info is properly updated.
            LauncherModel.addOrMoveItemInDatabase(mLauncher, info, container, screenId,
                    mTargetCell[0], mTargetCell[1]);

            addInScreen(view, container, screenId, mTargetCell[0], mTargetCell[1], info.spanX,
                    info.spanY, insertAtFirst);
            cellLayout.onDropChild(view);
            cellLayout.getShortcutsAndWidgets().measureChild(view);

            if (d.dragView != null) {
                // We wrap the animation call in the temporary set and reset of the current
                // cellLayout to its final transform -- this means we animate the drag view to
                // the correct final location.
                setFinalTransitionTransform(cellLayout);
                mLauncher.getDragLayer().animateViewIntoPosition(d.dragView, view,
                        exitSpringLoadedRunnable, this);
                resetTransitionTransform(cellLayout);
            }
        }
    }

    public Bitmap createWidgetBitmap(ItemInfo widgetInfo, View layout) {
        int[] unScaledSize = mLauncher.getWorkspace().estimateItemSize(widgetInfo.spanX,
                widgetInfo.spanY, widgetInfo, false);
        int visibility = layout.getVisibility();
        layout.setVisibility(VISIBLE);

        int width = MeasureSpec.makeMeasureSpec(unScaledSize[0], MeasureSpec.EXACTLY);
        int height = MeasureSpec.makeMeasureSpec(unScaledSize[1], MeasureSpec.EXACTLY);
        Bitmap b = Bitmap.createBitmap(unScaledSize[0], unScaledSize[1],
                Bitmap.Config.ARGB_8888);
        mCanvas.setBitmap(b);

        layout.measure(width, height);
        layout.layout(0, 0, unScaledSize[0], unScaledSize[1]);
        layout.draw(mCanvas);
        mCanvas.setBitmap(null);
        layout.setVisibility(visibility);
        return b;
    }

    private void getFinalPositionForDropAnimation(int[] loc, float[] scaleXY,
            DragView dragView, CellLayout layout, ItemInfo info, int[] targetCell,
            boolean external, boolean scale) {
        // Now we animate the dragView, (ie. the widget or shortcut preview) into its final
        // location and size on the home screen.
        int spanX = info.spanX;
        int spanY = info.spanY;

        Rect r = estimateItemPosition(layout, info, targetCell[0], targetCell[1], spanX, spanY);
        loc[0] = r.left;
        loc[1] = r.top;

        setFinalTransitionTransform(layout);
        float cellLayoutScale =
                mLauncher.getDragLayer().getDescendantCoordRelativeToSelf(layout, loc, true);
        resetTransitionTransform(layout);

        float dragViewScaleX;
        float dragViewScaleY;
        if (scale) {
            dragViewScaleX = (1.0f * r.width()) / dragView.getMeasuredWidth();
            dragViewScaleY = (1.0f * r.height()) / dragView.getMeasuredHeight();
        } else {
            dragViewScaleX = 1f;
            dragViewScaleY = 1f;
        }

        // The animation will scale the dragView about its center, so we need to center about
        // the final location.
        loc[0] -= (dragView.getMeasuredWidth() - cellLayoutScale * r.width()) / 2;
        loc[1] -= (dragView.getMeasuredHeight() - cellLayoutScale * r.height()) / 2;

        scaleXY[0] = dragViewScaleX * cellLayoutScale;
        scaleXY[1] = dragViewScaleY * cellLayoutScale;
    }

    public void animateWidgetDrop(ItemInfo info, CellLayout cellLayout, DragView dragView,
            final Runnable onCompleteRunnable, int animationType, final View finalView,
            boolean external) {
        Rect from = new Rect();
        mLauncher.getDragLayer().getViewRectRelativeToSelf(dragView, from);

        int[] finalPos = new int[2];
        float scaleXY[] = new float[2];
        boolean scalePreview = !(info instanceof PendingAddShortcutInfo);
        getFinalPositionForDropAnimation(finalPos, scaleXY, dragView, cellLayout, info, mTargetCell,
                external, scalePreview);

        Resources res = mLauncher.getResources();
        final int duration = res.getInteger(R.integer.config_dropAnimMaxDuration) - 200;

        if (LauncherLog.DEBUG) {
            LauncherLog.d(TAG, "animateWidgetDrop: info = " + info + ", animationType = " + animationType + ", finalPos = ("
                    + finalPos[0] + ", " + finalPos[1] + "), scaleXY = (" + scaleXY[0] + ", " + scaleXY[1]
                    + "), scalePreview = " + scalePreview + ",external = " + external);
        }

        // In the case where we've prebound the widget, we remove it from the DragLayer
        if (finalView instanceof AppWidgetHostView && external) {
            Log.d(TAG, "6557954 Animate widget drop, final view is appWidgetHostView");
            mLauncher.getDragLayer().removeView(finalView);
        }
        if ((animationType == ANIMATE_INTO_POSITION_AND_RESIZE || external) && finalView != null) {
            Bitmap crossFadeBitmap = createWidgetBitmap(info, finalView);
            dragView.setCrossFadeBitmap(crossFadeBitmap);
            dragView.crossFade((int) (duration * 0.8f));
        } else if (info.itemType == LauncherSettings.Favorites.ITEM_TYPE_APPWIDGET && external) {
            scaleXY[0] = scaleXY[1] = Math.min(scaleXY[0],  scaleXY[1]);
        }

        DragLayer dragLayer = mLauncher.getDragLayer();
        if (animationType == CANCEL_TWO_STAGE_WIDGET_DROP_ANIMATION) {
            mLauncher.getDragLayer().animateViewIntoPosition(dragView, finalPos, 0f, 0.1f, 0.1f,
                    DragLayer.ANIMATION_END_DISAPPEAR, onCompleteRunnable, duration);
        } else {
            int endStyle;
            if (animationType == ANIMATE_INTO_POSITION_AND_REMAIN) {
                endStyle = DragLayer.ANIMATION_END_REMAIN_VISIBLE;
            } else {
                endStyle = DragLayer.ANIMATION_END_DISAPPEAR;;
            }

            Runnable onComplete = new Runnable() {
                @Override
                public void run() {
                    if (finalView != null) {
                        finalView.setVisibility(VISIBLE);
                    }
                    if (onCompleteRunnable != null) {
                        onCompleteRunnable.run();
                    }
                }
            };
            dragLayer.animateViewIntoPosition(dragView, from.left, from.top, finalPos[0],
                    finalPos[1], 1, 1, 1, scaleXY[0], scaleXY[1], onComplete, endStyle,
                    duration, this);
        }
    }

    public void setFinalTransitionTransform(CellLayout layout) {
        if (isSwitchingState()) {
            mCurrentScale = getScaleX();
            setScaleX(mNewScale);
            setScaleY(mNewScale);
        }
    }
    public void resetTransitionTransform(CellLayout layout) {
        if (isSwitchingState()) {
            setScaleX(mCurrentScale);
            setScaleY(mCurrentScale);
        }
    }

    /**
     * Return the current {@link CellLayout}, correctly picking the destination
     * screen while a scroll is in progress.
     */
    public CellLayout getCurrentDropLayout() {
    	int pageNumb = getNextPage();
    	if (!QCConfig.autoDeleteAndAddEmptyScreen && (pageNumb<mShouldStartPageNumb|| pageNumb>mShouldEndPageNumb)) {
			pageNumb = Math.max(mShouldStartPageNumb, Math.min(pageNumb, mShouldEndPageNumb));
		}
        return (CellLayout) getChildAt(pageNumb);
    }

    /**
     * Return the current CellInfo describing our current drag; this method exists
     * so that Launcher can sync this object with the correct info when the activity is created/
     * destroyed
     *
     */
    public CellLayout.CellInfo getDragInfo() {
        return mDragInfo;
    }

    public int getCurrentPageOffsetFromCustomContent() {
        return getNextPage() - numCustomPages();
    }

    /**
     * Calculate the nearest cell where the given object would be dropped.
     *
     * pixelX and pixelY should be in the coordinate system of layout
     */
    private int[] findNearestArea(int pixelX, int pixelY,
            int spanX, int spanY, CellLayout layout, int[] recycle) {
        return layout.findNearestArea(
                pixelX, pixelY, spanX, spanY, recycle);
    }

    void setup(DragController dragController) {
        mSpringLoadedDragController = new SpringLoadedDragController(mLauncher);
        mDragController = dragController;

        // hardware layers on children are enabled on startup, but should be disabled until
        // needed
        updateChildrenLayersEnabled(false);
    }

    /**
     * Called at the end of a drag which originated on the workspace.
     */
    public void onDropCompleted(final View target, final DragObject d,
            final boolean isFlingToDelete, final boolean success) {
        if (LauncherLog.DEBUG) {
            LauncherLog.d(TAG, "onDropCompleted: target = " + target + ", d = " + d
                    + ", isFlingToDelete = " + isFlingToDelete + ", mDragInfo = " + mDragInfo + ", success = " + success);
        }

        if (mDeferDropAfterUninstall) {
            mDeferredAction = new Runnable() {
                public void run() {
                    onDropCompleted(target, d, isFlingToDelete, success);
                    mDeferredAction = null;
                }
            };
            return;
        }

        boolean beingCalledAfterUninstall = mDeferredAction != null;

        if (success && !(beingCalledAfterUninstall && !mUninstallSuccessful)) {
            if (target != this && mDragInfo != null) {
                CellLayout parentCell = getParentCellLayoutForView(mDragInfo.cell);
                if (parentCell != null) {
                    parentCell.removeView(mDragInfo.cell);
                } else if (LauncherAppState.isDogfoodBuild()) {
                    throw new NullPointerException("mDragInfo.cell has null parent");
                }
                if (mDragInfo.cell instanceof DropTarget) {
                    mDragController.removeDropTarget((DropTarget) mDragInfo.cell);
                }
            }
        /// M: [ALPS01257939] Check if target is null.
        } else if (mDragInfo != null && target != null) {
            CellLayout cellLayout;
            if (mLauncher.isHotseatLayout(target)) {
                cellLayout = mLauncher.getHotseat().getLayout();
            } else {
                cellLayout = getScreenWithId(mDragInfo.screenId);
            }
            if (cellLayout == null && LauncherAppState.isDogfoodBuild()) {
                throw new RuntimeException("Invalid state: cellLayout == null in "
                        + "Workspace#onDropCompleted. Please file a bug. ");
            }
            if (cellLayout != null) {
                cellLayout.onDropChild(mDragInfo.cell);
            }
        }
        if ((d.cancelled || (beingCalledAfterUninstall && !mUninstallSuccessful))
                && mDragInfo.cell != null) {
            mDragInfo.cell.setVisibility(VISIBLE);
        }

        mDragOutline = null;
        mDragInfo = null;
    }

    public void deferCompleteDropAfterUninstallActivity() {
        mDeferDropAfterUninstall = true;
    }

    /// maybe move this into a smaller part
    public void onUninstallActivityReturned(boolean success) {
        mDeferDropAfterUninstall = false;
        mUninstallSuccessful = success;
        if (mDeferredAction != null) {
            mDeferredAction.run();
        }
    }

    void updateItemLocationsInDatabase(CellLayout cl) {
        int count = cl.getShortcutsAndWidgets().getChildCount();

        long screenId = getIdForScreen(cl);
        int container = Favorites.CONTAINER_DESKTOP;

        if (mLauncher.isHotseatLayout(cl)) {
            screenId = -1;
            container = Favorites.CONTAINER_HOTSEAT;
        }

        for (int i = 0; i < count; i++) {
            View v = cl.getShortcutsAndWidgets().getChildAt(i);
            ItemInfo info = (ItemInfo) v.getTag();
            // Null check required as the AllApps button doesn't have an item info
            if (info != null && info.requiresDbUpdate) {
                info.requiresDbUpdate = false;
                LauncherModel.modifyItemInDatabase(mLauncher, info, container, screenId, info.cellX,
                        info.cellY, info.spanX, info.spanY);
                
                // Add for MyUI---20150902
                if (info instanceof ShortcutInfo && info.spanX == 1 && info.spanY == 1) {
	    			if (v!=null) {
	        			v.setScaleX(1f);
	        			v.setScaleY(1f);
	                    if (v instanceof BubbleTextView) {
							BubbleTextView mShortcut = (BubbleTextView)v;
							mShortcut.setTextVisibility(true);
						} else if (v instanceof FolderIcon) {
							FolderIcon mFolder = (FolderIcon)v;
							if (mFolder.getFolderInfo().container != LauncherSettings.Favorites.CONTAINER_HOTSEAT) {
								mFolder.setTextVisible(true);
							}
						}
					}
				}
                
            }
        }
    }

    ArrayList<ComponentName> getUniqueComponents(boolean stripDuplicates, ArrayList<ComponentName> duplicates) {
        ArrayList<ComponentName> uniqueIntents = new ArrayList<ComponentName>();
        getUniqueIntents((CellLayout) mLauncher.getHotseat().getLayout(), uniqueIntents, duplicates, false);
        int count = getChildCount();
        for (int i = 0; i < count; i++) {
            CellLayout cl = (CellLayout) getChildAt(i);
            getUniqueIntents(cl, uniqueIntents, duplicates, false);
        }
        return uniqueIntents;
    }

    void getUniqueIntents(CellLayout cl, ArrayList<ComponentName> uniqueIntents,
            ArrayList<ComponentName> duplicates, boolean stripDuplicates) {
        int count = cl.getShortcutsAndWidgets().getChildCount();

        ArrayList<View> children = new ArrayList<View>();
        for (int i = 0; i < count; i++) {
            View v = cl.getShortcutsAndWidgets().getChildAt(i);
            children.add(v);
        }

        for (int i = 0; i < count; i++) {
            View v = children.get(i);
            ItemInfo info = (ItemInfo) v.getTag();
            // Null check required as the AllApps button doesn't have an item info
            if (info instanceof ShortcutInfo) {
                ShortcutInfo si = (ShortcutInfo) info;
                ComponentName cn = si.intent.getComponent();

                Uri dataUri = si.intent.getData();
                // If dataUri is not null / empty or if this component isn't one that would
                // have previously showed up in the AllApps list, then this is a widget-type
                // shortcut, so ignore it.
                if (dataUri != null && !dataUri.equals(Uri.EMPTY)) {
                    continue;
                }

                if (!uniqueIntents.contains(cn)) {
                    uniqueIntents.add(cn);
                } else {
                    if (stripDuplicates) {
                        cl.removeViewInLayout(v);
                        LauncherModel.deleteItemFromDatabase(mLauncher, si);
                    }
                    if (duplicates != null) {
                        duplicates.add(cn);
                    }
                }
            }
            if (v instanceof FolderIcon) {
                FolderIcon fi = (FolderIcon) v;
                ArrayList<View> items = fi.getFolder().getItemsInReadingOrder();
                for (int j = 0; j < items.size(); j++) {
                    if (items.get(j).getTag() instanceof ShortcutInfo) {
                        ShortcutInfo si = (ShortcutInfo) items.get(j).getTag();
                        ComponentName cn = si.intent.getComponent();

                        Uri dataUri = si.intent.getData();
                        // If dataUri is not null / empty or if this component isn't one that would
                        // have previously showed up in the AllApps list, then this is a widget-type
                        // shortcut, so ignore it.
                        if (dataUri != null && !dataUri.equals(Uri.EMPTY)) {
                            continue;
                        }

                        if (!uniqueIntents.contains(cn)) {
                            uniqueIntents.add(cn);
                        }  else {
                            if (stripDuplicates) {
                                fi.getFolderInfo().remove(si);
                                LauncherModel.deleteItemFromDatabase(mLauncher, si);
                            }
                            if (duplicates != null) {
                                duplicates.add(cn);
                            }
                        }
                    }
                }
            }
        }
    }

    void saveWorkspaceToDb() {
        saveWorkspaceScreenToDb((CellLayout) mLauncher.getHotseat().getLayout());
        int count = getChildCount();
        for (int i = 0; i < count; i++) {
            CellLayout cl = (CellLayout) getChildAt(i);
            saveWorkspaceScreenToDb(cl);
        }
    }

    void saveWorkspaceScreenToDb(CellLayout cl) {
        int count = cl.getShortcutsAndWidgets().getChildCount();

        long screenId = getIdForScreen(cl);
        int container = Favorites.CONTAINER_DESKTOP;

        Hotseat hotseat = mLauncher.getHotseat();
        if (mLauncher.isHotseatLayout(cl)) {
            screenId = -1;
            container = Favorites.CONTAINER_HOTSEAT;
        }

        for (int i = 0; i < count; i++) {
            View v = cl.getShortcutsAndWidgets().getChildAt(i);
            ItemInfo info = (ItemInfo) v.getTag();
            // Null check required as the AllApps button doesn't have an item info
            if (info != null) {
                int cellX = info.cellX;
                int cellY = info.cellY;
                if (container == Favorites.CONTAINER_HOTSEAT) {
                    cellX = hotseat.getCellXFromOrder((int) info.screenId);
                    cellY = hotseat.getCellYFromOrder((int) info.screenId);
                }
                LauncherModel.addItemToDatabase(mLauncher, info, container, screenId, cellX,
                        cellY, false);
            }
            if (v instanceof FolderIcon) {
                FolderIcon fi = (FolderIcon) v;
                fi.getFolder().addItemLocationsInDatabase();
            }
        }
    }

    @Override
    public float getIntrinsicIconScaleFactor() {
        return 1f;
    }

    @Override
    public boolean supportsFlingToDelete() {
        return true;
    }

    @Override
    public boolean supportsAppInfoDropTarget() {
        return true;
    }

    @Override
    public boolean supportsDeleteDropTarget() {
        return true;
    }

    @Override
    public void onFlingToDelete(DragObject d, int x, int y, PointF vec) {
        // Do nothing
    }

    @Override
    public void onFlingToDeleteCompleted() {
        // Do nothing
    }

    public boolean isDropEnabled() {
        return true;
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        super.onRestoreInstanceState(state);
        if (LauncherLog.DEBUG) {
            LauncherLog.d(TAG, "onRestoreInstanceState: state = " + state
                    + ", mCurrentPage = " + mCurrentPage);
        }
        Launcher.setScreen(mCurrentPage);
    }

    @Override
    protected void dispatchRestoreInstanceState(SparseArray<Parcelable> container) {
        // We don't dispatch restoreInstanceState to our children using this code path.
        // Some pages will be restored immediately as their items are bound immediately, and
        // others we will need to wait until after their items are bound.
        mSavedStates = container;
    }

    public void restoreInstanceStateForChild(int child) {
        if (mSavedStates != null) {
            mRestoredPages.add(child);
            CellLayout cl = (CellLayout) getChildAt(child);
            if (cl != null) {
                cl.restoreInstanceState(mSavedStates);
            }
        }
    }

    public void restoreInstanceStateForRemainingPages() {
        int count = getChildCount();
        for (int i = 0; i < count; i++) {
            if (!mRestoredPages.contains(i)) {
                restoreInstanceStateForChild(i);
            }
        }
        mRestoredPages.clear();
        mSavedStates = null;
    }

    @Override
    public void scrollLeft() {
        if (!workspaceInModalState() && !mIsSwitchingState) {
            super.scrollLeft();
        }
        Folder openFolder = getOpenFolder();
        if (openFolder != null) {
            openFolder.completeDragExit();
        }
    }

    @Override
    public void scrollRight() {
        if (!workspaceInModalState() && !mIsSwitchingState) {
            super.scrollRight();
        }
        Folder openFolder = getOpenFolder();
        if (openFolder != null) {
            openFolder.completeDragExit();
        }
    }

    @Override
    public boolean onEnterScrollArea(int x, int y, int direction) {
        // Ignore the scroll area if we are dragging over the hot seat
        boolean isPortrait = !LauncherAppState.isScreenLandscape(getContext());
        if (mLauncher.getHotseat() != null && isPortrait) {
            Rect r = new Rect();
            mLauncher.getHotseat().getHitRect(r);
            if (r.contains(x, y)) {
                return false;
            }
        }

        boolean result = false;
        if (!workspaceInModalState() && !mIsSwitchingState && getOpenFolder() == null) {
            mInScrollArea = true;

            int page = getNextPage() +
                       (direction == DragController.SCROLL_LEFT ? -1 : 1);

            // Add to fix BUG that icon can be added into ExtraEmptyScreen when normalScale Jing.Wu 20160222 start
        	if (!QCConfig.autoDeleteAndAddEmptyScreen && (page<mShouldStartPageNumb|| page>mShouldEndPageNumb)) {
        		page = Math.max(mShouldStartPageNumb, Math.min(page, mShouldEndPageNumb));
    		}
            // Add to fix BUG that icon can be added into ExtraEmptyScreen when normalScale Jing.Wu 20160222 end

            /// M: modify to cycle sliding screen.
            if (isSupportCycleSlidingScreen()) {
                if (direction == DragController.SCROLL_RIGHT && page == getChildCount()) {
                    page = 0;
                } else if (direction == DragController.SCROLL_LEFT    && page == -1) {
                    page = getChildCount() - 1;
                }
            }

            // We always want to exit the current layout to ensure parity of enter / exit
            setCurrentDropLayout(null);

            if (0 <= page && page < getChildCount()) {
                // Ensure that we are not dragging over to the custom content screen
                if (getScreenIdForPageIndex(page) == CUSTOM_CONTENT_SCREEN_ID) {
                    return false;
                }

                CellLayout layout = (CellLayout) getChildAt(page);
                setCurrentDragOverlappingLayout(layout);

                // Workspace is responsible for drawing the edge glow on adjacent pages,
                // so we need to redraw the workspace when this may have changed.
                invalidate();
                result = true;
            }
        }
        return result;
    }

    @Override
    public boolean onExitScrollArea() {
        boolean result = false;
        if (mInScrollArea) {
            invalidate();
            CellLayout layout = getCurrentDropLayout();
            setCurrentDropLayout(layout);
            setCurrentDragOverlappingLayout(layout);

            result = true;
            mInScrollArea = false;
        }
        return result;
    }

    private void onResetScrollArea() {
        setCurrentDragOverlappingLayout(null);
        mInScrollArea = false;
    }

    /**
     * Returns a specific CellLayout
     */
    CellLayout getParentCellLayoutForView(View v) {
        ArrayList<CellLayout> layouts = getWorkspaceAndHotseatCellLayouts();
        for (CellLayout layout : layouts) {
            if (layout.getShortcutsAndWidgets().indexOfChild(v) > -1) {
                return layout;
            }
        }
        return null;
    }

    /**
     * Returns a list of all the CellLayouts in the workspace.
     */
    ArrayList<CellLayout> getWorkspaceAndHotseatCellLayouts() {
        ArrayList<CellLayout> layouts = new ArrayList<CellLayout>();
        int screenCount = getChildCount();
        for (int screen = 0; screen < screenCount; screen++) {
            layouts.add(((CellLayout) getChildAt(screen)));
        }
        if (mLauncher.getHotseat() != null) {
            layouts.add(mLauncher.getHotseat().getLayout());
        }
        return layouts;
    }

    /**
     * We should only use this to search for specific children.  Do not use this method to modify
     * ShortcutsAndWidgetsContainer directly. Includes ShortcutAndWidgetContainers from
     * the hotseat and workspace pages
     */
    ArrayList<ShortcutAndWidgetContainer> getAllShortcutAndWidgetContainers() {
        ArrayList<ShortcutAndWidgetContainer> childrenLayouts =
                new ArrayList<ShortcutAndWidgetContainer>();
        int screenCount = getChildCount();
        for (int screen = 0; screen < screenCount; screen++) {
            childrenLayouts.add(((CellLayout) getChildAt(screen)).getShortcutsAndWidgets());
        }
        if (mLauncher.getHotseat() != null) {
            childrenLayouts.add(mLauncher.getHotseat().getLayout().getShortcutsAndWidgets());
        }
        return childrenLayouts;
    }

    public Folder getFolderForTag(final Object tag) {
        return (Folder) getFirstMatch(new ItemOperator() {

            @Override
            public boolean evaluate(ItemInfo info, View v, View parent) {
                return (v instanceof Folder) && (((Folder) v).getInfo() == tag)
                        && ((Folder) v).getInfo().opened;
            }
        });
    }

    public View getViewForTag(final Object tag) {
        return getFirstMatch(new ItemOperator() {

            @Override
            public boolean evaluate(ItemInfo info, View v, View parent) {
                return info == tag;
            }
        });
    }

    public LauncherAppWidgetHostView getWidgetForAppWidgetId(final int appWidgetId) {
        return (LauncherAppWidgetHostView) getFirstMatch(new ItemOperator() {

            @Override
            public boolean evaluate(ItemInfo info, View v, View parent) {
                return (info instanceof LauncherAppWidgetInfo) &&
                        ((LauncherAppWidgetInfo) info).appWidgetId == appWidgetId;
            }
        });
    }

    private View getFirstMatch(final ItemOperator operator) {
        final View[] value = new View[1];
        mapOverItems(MAP_NO_RECURSE, new ItemOperator() {
            @Override
            public boolean evaluate(ItemInfo info, View v, View parent) {
                if (operator.evaluate(info, v, parent)) {
                    value[0] = v;
                    return true;
                }
                return false;
            }
        });
        return value[0];
    }

    void clearDropTargets() {
        mapOverItems(MAP_NO_RECURSE, new ItemOperator() {
            @Override
            public boolean evaluate(ItemInfo info, View v, View parent) {
                if (v instanceof DropTarget) {
                    mDragController.removeDropTarget((DropTarget) v);
                }
                // not done, process all the shortcuts
                return false;
            }
        });
    }

    // Removes ALL items that match a given package name, this is usually called when a package
    // has been removed and we want to remove all components (widgets, shortcuts, apps) that
    // belong to that package.
    void removeItemsByPackageName(final ArrayList<String> packages, final UserHandleCompat user) {
        final HashSet<String> packageNames = new HashSet<String>();
        packageNames.addAll(packages);

        if (LauncherLog.DEBUG) {
            LauncherLog.d(TAG, "removeFinalItem: packageNames = " + packageNames);
        }

        // Filter out all the ItemInfos that this is going to affect
        final HashSet<ItemInfo> infos = new HashSet<ItemInfo>();
        final HashSet<ComponentName> cns = new HashSet<ComponentName>();
        ArrayList<CellLayout> cellLayouts = getWorkspaceAndHotseatCellLayouts();
        for (CellLayout layoutParent : cellLayouts) {
            ViewGroup layout = layoutParent.getShortcutsAndWidgets();
            int childCount = layout.getChildCount();
            for (int i = 0; i < childCount; ++i) {
                View view = layout.getChildAt(i);
                infos.add((ItemInfo) view.getTag());
            }
        }
        LauncherModel.ItemInfoFilter filter = new LauncherModel.ItemInfoFilter() {
            @Override
            public boolean filterItem(ItemInfo parent, ItemInfo info,
                                      ComponentName cn) {
                if (packageNames.contains(cn.getPackageName())
                        && info.user.equals(user)) {
                    cns.add(cn);
                    return true;
                }
                return false;
            }
        };
        LauncherModel.filterItemInfos(infos, filter);

        // Remove the affected components
        removeItemsByComponentName(cns, user);
    }

    // Removes items that match the application info specified, when applications are removed
    // as a part of an update, this is called to ensure that other widgets and application
    // shortcuts are not removed.
    void removeItemsByApplicationInfo(final ArrayList<AppInfo> appInfos, UserHandleCompat user) {
        // Just create a hash table of all the specific components that this will affect
        HashSet<ComponentName> cns = new HashSet<ComponentName>();
        for (AppInfo info : appInfos) {
            cns.add(info.componentName);
        }

        // Remove all the things
        removeItemsByComponentName(cns, user);
    }

    void removeItemsByComponentName(final HashSet<ComponentName> componentNames,
            final UserHandleCompat user) {
        ArrayList<CellLayout> cellLayouts = getWorkspaceAndHotseatCellLayouts();
        for (final CellLayout layoutParent: cellLayouts) {
            final ViewGroup layout = layoutParent.getShortcutsAndWidgets();

            final HashMap<ItemInfo, View> children = new HashMap<ItemInfo, View>();
            for (int j = 0; j < layout.getChildCount(); j++) {
                final View view = layout.getChildAt(j);
                children.put((ItemInfo) view.getTag(), view);
            }

            final ArrayList<View> childrenToRemove = new ArrayList<View>();
            final HashMap<FolderInfo, ArrayList<ShortcutInfo>> folderAppsToRemove =
                    new HashMap<FolderInfo, ArrayList<ShortcutInfo>>();
            LauncherModel.ItemInfoFilter filter = new LauncherModel.ItemInfoFilter() {
                @Override
                public boolean filterItem(ItemInfo parent, ItemInfo info,
                                          ComponentName cn) {
                    if (parent instanceof FolderInfo) {
                        if (componentNames.contains(cn) && info.user.equals(user)) {
                            FolderInfo folder = (FolderInfo) parent;
                            ArrayList<ShortcutInfo> appsToRemove;
                            if (folderAppsToRemove.containsKey(folder)) {
                                appsToRemove = folderAppsToRemove.get(folder);
                            } else {
                                appsToRemove = new ArrayList<ShortcutInfo>();
                                folderAppsToRemove.put(folder, appsToRemove);
                            }
                            appsToRemove.add((ShortcutInfo) info);
                            return true;
                        }
                    } else {
                        if (componentNames.contains(cn) && info.user.equals(user)) {
                            childrenToRemove.add(children.get(info));
                            return true;
                        }
                        /// M: compare package name
                        Iterator<ComponentName> iterator = componentNames.iterator();
                        while(iterator.hasNext()) {
                            ComponentName item = iterator.next();
                            if(item.getPackageName().equals(cn.getPackageName())) {
                                childrenToRemove.add(children.get(info));
                                return true;
                            }
                        }
                    }
                    return false;
                }
            };
            LauncherModel.filterItemInfos(children.keySet(), filter);

            // Remove all the apps from their folders
            for (FolderInfo folder : folderAppsToRemove.keySet()) {
                ArrayList<ShortcutInfo> appsToRemove = folderAppsToRemove.get(folder);
                for (ShortcutInfo info : appsToRemove) {
                    folder.remove(info);
                }
            }

            // Remove all the other children
            for (View child : childrenToRemove) {
                // Note: We can not remove the view directly from CellLayoutChildren as this
                // does not re-mark the spaces as unoccupied.
                layoutParent.removeViewInLayout(child);
                if (child instanceof DropTarget) {
                    mDragController.removeDropTarget((DropTarget) child);
                }
            }

            if (childrenToRemove.size() > 0) {
                layout.requestLayout();
                layout.invalidate();
            }
        }

        if (QCConfig.autoDeleteAndAddEmptyScreen) {
        	removeExtraEmptyScreen(false, true);
        	// Strip all the empty screens
            stripEmptyScreens();
		} else {
			cellsShowAddOrDeleteButton(true, true);
		}
        /// M
        
    }

    interface ItemOperator {
        /**
         * Process the next itemInfo, possibly with side-effect on {@link ItemOperator#value}.
         *
         * @param info info for the shortcut
         * @param view view for the shortcut
         * @param parent containing folder, or null
         * @return true if done, false to continue the map
         */
        public boolean evaluate(ItemInfo info, View view, View parent);
    }

    /**
     * Map the operator over the shortcuts and widgets, return the first-non-null value.
     *
     * @param recurse true: iterate over folder children. false: op get the folders themselves.
     * @param op the operator to map over the shortcuts
     */
    void mapOverItems(boolean recurse, ItemOperator op) {
        ArrayList<ShortcutAndWidgetContainer> containers = getAllShortcutAndWidgetContainers();
        final int containerCount = containers.size();
        for (int containerIdx = 0; containerIdx < containerCount; containerIdx++) {
            ShortcutAndWidgetContainer container = containers.get(containerIdx);
            // map over all the shortcuts on the workspace
            final int itemCount = container.getChildCount();
            for (int itemIdx = 0; itemIdx < itemCount; itemIdx++) {
                View item = container.getChildAt(itemIdx);
                ItemInfo info = (ItemInfo) item.getTag();
                if (recurse && info instanceof FolderInfo && item instanceof FolderIcon) {
                    FolderIcon folder = (FolderIcon) item;
                    ArrayList<View> folderChildren = folder.getFolder().getItemsInReadingOrder();
                    // map over all the children in the folder
                    final int childCount = folderChildren.size();
                    for (int childIdx = 0; childIdx < childCount; childIdx++) {
                        View child = folderChildren.get(childIdx);
                        info = (ItemInfo) child.getTag();
                        if (op.evaluate(info, child, folder)) {
                            return;
                        }
                    }
                } else {
                    if (op.evaluate(info, item, null)) {
                        return;
                    }
                }
            }
        }
    }

    void updateShortcutsAndWidgets(ArrayList<AppInfo> apps) {
        // Break the appinfo list per user
        final HashMap<UserHandleCompat, ArrayList<AppInfo>> appsPerUser =
                new HashMap<UserHandleCompat, ArrayList<AppInfo>>();
        for (AppInfo info : apps) {
            ArrayList<AppInfo> filtered = appsPerUser.get(info.user);
            if (filtered == null) {
                filtered = new ArrayList<AppInfo>();
                appsPerUser.put(info.user, filtered);
            }
            filtered.add(info);
        }

        for (Map.Entry<UserHandleCompat, ArrayList<AppInfo>> entry : appsPerUser.entrySet()) {
            updateShortcutsAndWidgetsPerUser(entry.getValue(), entry.getKey());
        }
    }

    private void updateShortcutsAndWidgetsPerUser(ArrayList<AppInfo> apps,
            final UserHandleCompat user) {
        // Create a map of the apps to test against
        final HashMap<ComponentName, AppInfo> appsMap = new HashMap<ComponentName, AppInfo>();
        final HashSet<String> pkgNames = new HashSet<String>();
        for (AppInfo ai : apps) {
            appsMap.put(ai.componentName, ai);
            pkgNames.add(ai.componentName.getPackageName());
        }
        final HashSet<ComponentName> iconsToRemove = new HashSet<ComponentName>();

        mapOverItems(MAP_RECURSE, new ItemOperator() {
            @Override
            public boolean evaluate(ItemInfo info, View v, View parent) {
                if (info instanceof ShortcutInfo && v instanceof BubbleTextView) {
                    ShortcutInfo shortcutInfo = (ShortcutInfo) info;
                    ComponentName cn = shortcutInfo.getTargetComponent();
                    AppInfo appInfo = appsMap.get(cn);
                    if (user.equals(shortcutInfo.user) && cn != null
                            && LauncherModel.isShortcutInfoUpdateable(info)
                            && pkgNames.contains(cn.getPackageName())) {
                        boolean promiseStateChanged = false;
                        boolean infoUpdated = false;
                        if (shortcutInfo.isPromise()) {
                            if (shortcutInfo.hasStatusFlag(ShortcutInfo.FLAG_AUTOINTALL_ICON)) {
                                // Auto install icon
                                PackageManager pm = getContext().getPackageManager();
                                ResolveInfo matched = pm.resolveActivity(
                                        new Intent(Intent.ACTION_MAIN)
                                        .setComponent(cn).addCategory(Intent.CATEGORY_LAUNCHER),
                                        PackageManager.MATCH_DEFAULT_ONLY);
                                if (matched == null) {
                                    // Try to find the best match activity.
                                    Intent intent = pm.getLaunchIntentForPackage(
                                            cn.getPackageName());
                                    if (intent != null) {
                                        cn = intent.getComponent();
                                        appInfo = appsMap.get(cn);
                                    }

                                    if ((intent == null) || (appsMap == null)) {
                                        // Could not find a default activity. Remove this item.
                                        iconsToRemove.add(shortcutInfo.getTargetComponent());

                                        // process next shortcut.
                                        return false;
                                    }
                                    shortcutInfo.promisedIntent = intent;
                                }
                            }

                            // Restore the shortcut.
                            shortcutInfo.intent = shortcutInfo.promisedIntent;
                            shortcutInfo.promisedIntent = null;
                            shortcutInfo.status &= ~ShortcutInfo.FLAG_RESTORED_ICON
                                    & ~ShortcutInfo.FLAG_AUTOINTALL_ICON
                                    & ~ShortcutInfo.FLAG_INSTALL_SESSION_ACTIVE;

                            promiseStateChanged = true;
                            infoUpdated = true;
                            shortcutInfo.updateIcon(mIconCache);
                            LauncherModel.updateItemInDatabase(getContext(), shortcutInfo);
                        }


                        if (appInfo != null) {
                            shortcutInfo.updateIcon(mIconCache);
                            shortcutInfo.title = appInfo.title.toString();
                            shortcutInfo.contentDescription = appInfo.contentDescription;
                            infoUpdated = true;
                        }

                        if (infoUpdated) {
                            BubbleTextView shortcut = (BubbleTextView) v;
                            shortcut.applyFromShortcutInfo(shortcutInfo,
                                    mIconCache, true, promiseStateChanged);

                            if (parent != null) {
                                parent.invalidate();
                            }
                        }
                    }
                }
                // process all the shortcuts
                return false;
            }
        });

        if (!iconsToRemove.isEmpty()) {
            removeItemsByComponentName(iconsToRemove, user);
        }
        if (user.equals(UserHandleCompat.myUserHandle())) {
            restorePendingWidgets(pkgNames);
        }
    }

    public void removeAbandonedPromise(String packageName, UserHandleCompat user) {
        ArrayList<String> packages = new ArrayList<String>(1);
        packages.add(packageName);
        LauncherModel.deletePackageFromDatabase(mLauncher, packageName, user);
        removeItemsByPackageName(packages, user);
    }

    public void updatePackageBadge(final String packageName, final UserHandleCompat user) {
        mapOverItems(MAP_RECURSE, new ItemOperator() {
            @Override
            public boolean evaluate(ItemInfo info, View v, View parent) {
                if (info instanceof ShortcutInfo && v instanceof BubbleTextView) {
                    ShortcutInfo shortcutInfo = (ShortcutInfo) info;
                    ComponentName cn = shortcutInfo.getTargetComponent();
                    if (user.equals(shortcutInfo.user) && cn != null
                            && shortcutInfo.isPromise()
                            && packageName.equals(cn.getPackageName())) {
                        if (shortcutInfo.hasStatusFlag(ShortcutInfo.FLAG_AUTOINTALL_ICON)) {
                            // For auto install apps update the icon as well as label.
                            mIconCache.getTitleAndIcon(shortcutInfo,
                                    shortcutInfo.promisedIntent, user, true);
                        } else {
                            // Only update the icon for restored apps.
                            shortcutInfo.updateIcon(mIconCache);
                        }
                        BubbleTextView shortcut = (BubbleTextView) v;
                        shortcut.applyFromShortcutInfo(shortcutInfo, mIconCache, true, false);

                        if (parent != null) {
                            parent.invalidate();
                        }
                    }
                }
                // process all the shortcuts
                return false;
            }
        });
    }

    public void updatePackageState(ArrayList<PackageInstallInfo> installInfos) {
        HashSet<String> completedPackages = new HashSet<String>();

        for (final PackageInstallInfo installInfo : installInfos) {
            mapOverItems(MAP_RECURSE, new ItemOperator() {
                @Override
                public boolean evaluate(ItemInfo info, View v, View parent) {
                    if (info instanceof ShortcutInfo && v instanceof BubbleTextView) {
                        ShortcutInfo si = (ShortcutInfo) info;
                        ComponentName cn = si.getTargetComponent();
                        if (si.isPromise() && (cn != null)
                                && installInfo.packageName.equals(cn.getPackageName())) {
                            si.setInstallProgress(installInfo.progress);
                            if (installInfo.state == PackageInstallerCompat.STATUS_FAILED) {
                                // Mark this info as broken.
                                si.status &= ~ShortcutInfo.FLAG_INSTALL_SESSION_ACTIVE;
                            }
                            ((BubbleTextView)v).applyState(false);
                        }
                    } else if (v instanceof PendingAppWidgetHostView
                            && info instanceof LauncherAppWidgetInfo
                            && ((LauncherAppWidgetInfo) info).providerName.getPackageName()
                                .equals(installInfo.packageName)) {
                        ((LauncherAppWidgetInfo) info).installProgress = installInfo.progress;
                        ((PendingAppWidgetHostView) v).applyState();
                    }

                    // process all the shortcuts
                    return false;
                }
            });

            if (installInfo.state == PackageInstallerCompat.STATUS_INSTALLED) {
                completedPackages.add(installInfo.packageName);
            }
        }

        // Note that package states are sent only for myUser
        if (!completedPackages.isEmpty()) {
            restorePendingWidgets(completedPackages);
        }
    }

    private void restorePendingWidgets(final Set<String> installedPackaged) {
        final ArrayList<LauncherAppWidgetInfo> changedInfo = new ArrayList<LauncherAppWidgetInfo>();

        // Iterate non recursively as widgets can't be inside a folder.
        mapOverItems(MAP_NO_RECURSE, new ItemOperator() {

            @Override
            public boolean evaluate(ItemInfo info, View v, View parent) {
                if (info instanceof LauncherAppWidgetInfo) {
                    LauncherAppWidgetInfo widgetInfo = (LauncherAppWidgetInfo) info;
                    if (widgetInfo.hasRestoreFlag(LauncherAppWidgetInfo.FLAG_PROVIDER_NOT_READY)
                            && installedPackaged.contains(widgetInfo.providerName.getPackageName())) {

                        changedInfo.add(widgetInfo);

                        // Remove the provider not ready flag
                        widgetInfo.restoreStatus &= ~LauncherAppWidgetInfo.FLAG_PROVIDER_NOT_READY;
                        LauncherModel.updateItemInDatabase(getContext(), widgetInfo);
                    }
                }
                // process all the widget
                return false;
            }
        });
        if (!changedInfo.isEmpty()) {
            DeferredWidgetRefresh widgetRefresh = new DeferredWidgetRefresh(changedInfo,
                    mLauncher.getAppWidgetHost());
            if (LauncherModel.findAppWidgetProviderInfoWithComponent(getContext(),
                    changedInfo.get(0).providerName) != null) {
                // Re-inflate the widgets which have changed status
                widgetRefresh.run();
            } else {
                // widgetRefresh will automatically run when the packages are updated.
            }
        }
    }

    private void moveToScreen(int page, boolean animate) {
    	if (QCLog.DEBUG) {
			QCLog.d(TAG, "moveToScreen() and page = "+page);
		}
        if (!workspaceInModalState()) {
            if (animate) {
                snapToPage(page);
            } else {
                setCurrentPage(page);
            }
        }
        View child = getChildAt(page);
        if (child != null) {
            child.requestFocus();
        }
    }

    void moveToDefaultScreen(boolean animate) {
		//sunfeng modify @ 20150909 for newspage to otherPage ,Title hideshow error start:
		mLauncher.showStatusBar(true);
		//sunfeng modify @ 20150909 for newspage to otherPage ,Title hideshow error end:

        //workground for ota
        if(mDefaultPage == 0 && mLauncher.hasCustomContentToLeft()){
            moveToScreen(1, animate);
        }else{
            moveToScreen(mDefaultPage, animate);
        }
    }

    void moveToCustomContentScreen(boolean animate) {
        if (hasCustomContent()) {
            int ccIndex = getPageIndexForScreenId(CUSTOM_CONTENT_SCREEN_ID);
            if (animate) {
                snapToPage(ccIndex);
            } else {
                setCurrentPage(ccIndex);
            }
            View child = getChildAt(ccIndex);
            if (child != null) {
                child.requestFocus();
            }
         }
        exitWidgetResizeMode();
    }

    @Override
    protected PageIndicator.PageMarkerResources getPageIndicatorMarker(int pageIndex) {
        long screenId = getScreenIdForPageIndex(pageIndex);
        if (screenId == EXTRA_EMPTY_SCREEN_ID) {
            int count = mScreenOrder.size() - numCustomPages();
            if (count > 1) {
            	// Modify for MyUI Jing.Wu 20151204 start
                //return new PageIndicator.PageMarkerResources(R.drawable.ic_pageindicator_current,
                //      R.drawable.ic_pageindicator_add);
                return new PageIndicator.PageMarkerResources(R.drawable.workspace_page_view_current_color,
                        R.drawable.workspace_page_view_default_color);
            	// Modify for MyUI Jing.Wu 20151204 end
            }
        }

        return super.getPageIndicatorMarker(pageIndex);
    }

    @Override
    public void syncPages() {
    }

    @Override
    public void syncPageItems(int page, boolean immediate) {
    }

    protected String getPageIndicatorDescription() {
        String settings = getResources().getString(R.string.settings_button_text);
        return getCurrentPageDescription() + ", " + settings;
    }

    protected String getCurrentPageDescription() {
        int page = (mNextPage != INVALID_PAGE) ? mNextPage : mCurrentPage;
        int delta = numCustomPages();
        if (hasCustomContent() && getNextPage() == 0) {
            return mCustomContentDescription;
        }
        return String.format(getContext().getString(R.string.workspace_scroll_format),
                page + 1 - delta, getChildCount() - delta);
    }

    public void getLocationInDragLayer(int[] loc) {
        mLauncher.getDragLayer().getLocationInDragLayer(this, loc);
    }

    /**
     * Used as a workaround to ensure that the AppWidgetService receives the
     * PACKAGE_ADDED broadcast before updating widgets.
     */
    private class DeferredWidgetRefresh implements Runnable {
        private final ArrayList<LauncherAppWidgetInfo> mInfos;
        private final LauncherAppWidgetHost mHost;
        private final Handler mHandler;

        private boolean mRefreshPending;

        public DeferredWidgetRefresh(ArrayList<LauncherAppWidgetInfo> infos,
                LauncherAppWidgetHost host) {
            mInfos = infos;
            mHost = host;
            mHandler = new Handler();
            mRefreshPending = true;

            mHost.addProviderChangeListener(this);
            // Force refresh after 10 seconds, if we don't get the provider changed event.
            // This could happen when the provider is no longer available in the app.
            mHandler.postDelayed(this, 10000);
        }

        @Override
        public void run() {
            mHost.removeProviderChangeListener(this);
            mHandler.removeCallbacks(this);

            if (!mRefreshPending) {
                return;
            }

            mRefreshPending = false;

            for (LauncherAppWidgetInfo info : mInfos) {
                if (info.hostView instanceof PendingAppWidgetHostView) {
                    PendingAppWidgetHostView view = (PendingAppWidgetHostView) info.hostView;
                    mLauncher.removeAppWidget(info);

                    CellLayout cl = (CellLayout) view.getParent().getParent();
                    // Remove the current widget
                    cl.removeView(view);
                    mLauncher.bindAppWidget(info);
                }
            }
        }
    }

    /**
     * M: Whether all the items in folder will be removed or not.
     *
     * @param info
     * @param packageNames
     * @param appsToRemoveFromFolder
     * @return true, all the items in folder will be removed.
     */
    private boolean isNeedToDelayRemoveFolderItems(FolderInfo info, HashSet<ComponentName> componentNames,
            ArrayList<ShortcutInfo> appsToRemoveFromFolder) {
        final ArrayList<ShortcutInfo> contents = info.contents;
        final int contentsCount = contents.size();
        int removeFolderItemsCount = getRemoveFolderItems(info, componentNames, appsToRemoveFromFolder);
        if (LauncherLog.DEBUG) {
            LauncherLog.d(TAG, "isNeedToDelayRemoveFolderItems info = " + info + ", componentNames = " + componentNames
                    + ", contentsCount = " + contentsCount + ", removeFolderItemsCount = " + removeFolderItemsCount);
        }

        return (removeFolderItemsCount >= (contentsCount - 1));
    }

    /**
     * M: When uninstall one app, if the foler item is the shortcut of the app, it will be removed.
     *
     * @param info
     * @param packageNames
     * @param appsToRemoveFromFolder
     * @return the count of the folder items will be removed.
     */
    private int getRemoveFolderItems(FolderInfo info, HashSet<ComponentName> componentNames,
            ArrayList<ShortcutInfo> appsToRemoveFromFolder) {
        final ArrayList<ShortcutInfo> contents = info.contents;
        final int contentsCount = contents.size();

        for (int k = 0; k < contentsCount; k++) {
            final ShortcutInfo appInfo = contents.get(k);
            final Intent intent = appInfo.intent;
            final ComponentName name = intent.getComponent();

            if (name != null && componentNames.contains(name)) {
                appsToRemoveFromFolder.add(appInfo);
            }
        }

        if (LauncherLog.DEBUG) {
            LauncherLog.d(TAG, "getRemoveFolderItems info = " + info + ", componentNames = " + componentNames
                    + ", appsToRemoveFromFolder.size() = " + appsToRemoveFromFolder.size());
        }
        return appsToRemoveFromFolder.size();
    }

    /**
     * M: Remove folder items.
     *
     * @param info
     * @param appsToRemoveFromFolder
     */
    private void removeFolderItems(FolderInfo info, ArrayList<ShortcutInfo> appsToRemoveFromFolder) {
        for (ShortcutInfo item : appsToRemoveFromFolder) {
            info.remove(item);
            LauncherModel.deleteItemFromDatabase(mLauncher, item);
        }
    }

    /**
     * M: Support cycle sliding screen or not.
     * @return true: support cycle sliding screen.
     */
    @Override
    public boolean isSupportCycleSlidingScreen() {
        return mLauncher.getIsSupportCycleSlidingScreens();
    }

	/**
     * M: Remove shortcuts of the hide apps in workspace and folder, remove
     * widgets by request, add for OP09.
     *
     * @param apps
     */
    void removeItemsByAppInfo(final ArrayList<AppInfo> apps) {
        final HashSet<ComponentName> componentNames = new HashSet<ComponentName>();
        final int appCount = apps.size();
        for (int i = 0; i < appCount; i++) {
            componentNames.add(apps.get(i).componentName);
        }

        if (LauncherLog.DEBUG_EDIT) {
            LauncherLog.d(TAG, "removeItemsByComponentName: apps = " + apps + ",componentNames = "
                    + componentNames);
        }

        final ArrayList<CellLayout> cellLayouts = getWorkspaceAndHotseatCellLayouts();
        for (final CellLayout layoutParent : cellLayouts) {
            final ViewGroup layout = layoutParent.getShortcutsAndWidgets();

            // Avoid ANRs by treating each screen separately
            post(new Runnable() {
                public void run() {
                    final ArrayList<View> childrenToRemove = new ArrayList<View>();
                    final ArrayList<FolderInfo> folderInfosToRemove = new ArrayList<FolderInfo>();

                    int childCount = layout.getChildCount();
                    for (int j = 0; j < childCount; j++) {
                        final View view = layout.getChildAt(j);
                        Object tag = view.getTag();

                        if (tag instanceof ShortcutInfo) {
                            final ShortcutInfo info = (ShortcutInfo) tag;
                            final Intent intent = info.intent;
                            final ComponentName name = intent.getComponent();
                            if (name != null && componentNames.contains(name)) {
                                LauncherModel.deleteItemFromDatabase(mLauncher, info);
                                childrenToRemove.add(view);
                            }
                        } else if (tag instanceof FolderInfo) {
                            final FolderInfo info = (FolderInfo) tag;
                            final ArrayList<ShortcutInfo> contents = info.contents;
                            final int contentsCount = contents.size();
                            final ArrayList<ShortcutInfo> appsToRemoveFromFolder = new ArrayList<ShortcutInfo>();

                            // If the folder will be removed completely,
                            // delay to remove, else remove folder items.
                            if (isFolderNeedRemoved(info, componentNames, appsToRemoveFromFolder)) {
                                folderInfosToRemove.add(info);
                            } else {
                                removeFolderItems(info, appsToRemoveFromFolder);
                            }
                        }
                    }

                    /// Remove items in folder, if there are two folders
                    /// with two same shortcuts, uninstall this application, JE
                    /// will happens in original design.
                    final int delayFolderCount = folderInfosToRemove.size();
                    for (int j = 0; j < delayFolderCount; j++) {
                        FolderInfo info = folderInfosToRemove.get(j);
                        final ArrayList<ShortcutInfo> appsToRemoveFromFolder = new ArrayList<ShortcutInfo>();
                        getRemoveFolderItemsByComponent(info, componentNames,
                                appsToRemoveFromFolder);
                        removeFolderItems(info, appsToRemoveFromFolder);
                    }

                    childCount = childrenToRemove.size();
                    for (int j = 0; j < childCount; j++) {
                        View child = childrenToRemove.get(j);
                        // Note: We can not remove the view directly from
                        // CellLayoutChildren as this
                        // does not re-mark the spaces as unoccupied.
                        layoutParent.removeViewInLayout(child);
                        if (child instanceof DropTarget) {
                            mDragController.removeDropTarget((DropTarget) child);
                        }
                    }

                    if (childCount > 0) {
                        layout.requestLayout();
                        layout.invalidate();
                    }
                }
            });
        }

        // TODO: whether we need to post a new Runnable to remove all items in
        // database like in removeFinalItems.
    }

    /**
     * M: Whether the folder should be removed, this means there will be at most
     * one item in the folder.
     *
     * @param info
     * @param componentNames
     * @param appsToRemoveFromFolder
     * @return True if the folder will be removed.
     */
    private boolean isFolderNeedRemoved(FolderInfo info, HashSet<ComponentName> componentNames,
            ArrayList<ShortcutInfo> appsToRemoveFromFolder) {
        final ArrayList<ShortcutInfo> contents = info.contents;
        final int contentsCount = contents.size();
        final int needRemoveItemCount = getRemoveFolderItemsByComponent(info, componentNames,
                appsToRemoveFromFolder);
        return (needRemoveItemCount >= (contentsCount - 1));
    }

    /**
     * M: When uninstall an application, remove the shortcut with the same
     * component name in the folder.
     *
     * @param info
     * @param packageNames
     * @param appsToRemoveFromFolder
     * @return the count of the folder items will be removed.
     */
    private int getRemoveFolderItemsByComponent(FolderInfo info,
            HashSet<ComponentName> componentNames, ArrayList<ShortcutInfo> appsToRemoveFromFolder) {
        final ArrayList<ShortcutInfo> contents = info.contents;
        final int contentsCount = contents.size();

        for (int k = 0; k < contentsCount; k++) {
            final ShortcutInfo appInfo = contents.get(k);
            final Intent intent = appInfo.intent;
            final ComponentName name = intent.getComponent();

            if (name != null && componentNames.contains(name)) {
                appsToRemoveFromFolder.add(appInfo);
            }
        }

        if (LauncherLog.DEBUG_EDIT) {
            LauncherLog.d(TAG, "getRemoveFolderItems info = " + info + ", componentNames = "
                    + componentNames + ",contentsCount = " + contentsCount
                    + ", appsToRemoveFromFolder.size() = " + appsToRemoveFromFolder.size());
        }
        return appsToRemoveFromFolder.size();
    }

    /**
     * M: Check if shortcut info need to be updated.
     * 
     * @param shortcut The shortcut to check if need to be updated.
     * @param apps The app which was updated. 
     * @return true if shortcut need to update.
     */
    private boolean updateShortcutInfoCheck(BubbleTextView shortcut, ArrayList<AppInfo> apps) {
        ShortcutInfo info = (ShortcutInfo) shortcut.getTag();
        // We need to check for ACTION_MAIN otherwise getComponent() might
        // return null for some shortcuts (for instance, for shortcuts to
        // web pages.)
        final Intent intent = info.intent;
        final ComponentName name = intent.getComponent();
        if (info.itemType == LauncherSettings.Favorites.ITEM_TYPE_APPLICATION 
                && Intent.ACTION_MAIN.equals(intent.getAction()) && name != null) {
            final int appCount = apps.size();
            for (int k = 0; k < appCount; k++) {
                AppInfo app = apps.get(k);
                if (app.componentName.equals(name)) {
                    info.updateIcon(mIconCache);
                    info.title = app.title.toString();
                    shortcut.applyFromShortcutInfo(info, mIconCache, true);
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * M: [Performance] Set HW layer on down event to improve response time.
     */
    @Override
    public boolean onTouchEvent(MotionEvent event) {
    	if (mLauncher.getIsSwitchAnimationing()) {
			return true;
		}
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                LauncherLog.d(TAG, "onTouchEvent: Set HW layer on down event");
                updateChildrenLayersEnabled(true);
                break;
        }

        return super.onTouchEvent(event);
    }

    /**M: Added for unread message feature.@{**/
    /**
     * M: Update unread number of shortcuts and folders in workspace and hotseat.
     */
    public void updateShortcutsAndFoldersUnread() {
        if (LauncherLog.DEBUG_UNREAD) {
            LauncherLog.d(TAG, "updateShortcutsAndFolderUnread: this = " + this);
        }
        final ArrayList<ShortcutAndWidgetContainer> childrenLayouts = getAllShortcutAndWidgetContainers();
        int childCount = 0;
        View view = null;
        Object tag = null;
        for (ShortcutAndWidgetContainer layout : childrenLayouts) {
            childCount = layout.getChildCount();
            for (int j = 0; j < childCount; j++) {
                view = layout.getChildAt(j);
                tag = view.getTag();
                if (LauncherLog.DEBUG_UNREAD) {
                    LauncherLog.d(TAG, "updateShortcutsAndFoldersUnread: tag = " + tag + ", j = "
                            + j + ", view = " + view);
                }
                if (tag instanceof ShortcutInfo) {
                    final ShortcutInfo info = (ShortcutInfo) tag;
                    final Intent intent = info.intent;
                    final ComponentName componentName = intent.getComponent();
                    info.unreadNum = MTKUnreadLoader.getUnreadNumberOfComponent(componentName);
                    ((BubbleTextView) view).invalidate();
                } else if (tag instanceof FolderInfo) {
                    ((FolderIcon) view).updateFolderUnreadNum();
                    ((FolderIcon) view).invalidate();
                }
            }
        }
    }

    /**
     * M: Update unread number of shortcuts and folders in workspace and hotseat
     * with the given component.
     *
     * @param component
     * @param unreadNum
     */
    public void updateComponentUnreadChanged(ComponentName component, int unreadNum) {
        if (LauncherLog.DEBUG_UNREAD) {
            LauncherLog.d(TAG, "updateComponentUnreadChanged: component = " + component
                    + ", unreadNum = " + unreadNum);
        }
        final ArrayList<ShortcutAndWidgetContainer> childrenLayouts = getAllShortcutAndWidgetContainers();
        int childCount = 0;
        View view = null;
        Object tag = null;
        for (ShortcutAndWidgetContainer layout : childrenLayouts) {
            childCount = layout.getChildCount();
            for (int j = 0; j < childCount; j++) {
                view = layout.getChildAt(j);

				/// M: ALPS01642099, NULL pointer check
				if (view != null) {
                	tag = view.getTag();
				} else {
					if (LauncherLog.DEBUG_UNREAD) {
                        LauncherLog.d(TAG, "updateComponentUnreadChanged: view is null pointer");
                    }
					continue;
				}
				/// M.
				
				if (LauncherLog.DEBUG_UNREAD) {
                    LauncherLog.d(TAG, "updateComponentUnreadChanged: component = " + component
                            + ",tag = " + tag + ",j = " + j + ",view = " + view);
                }
                if (tag instanceof ShortcutInfo) {
                    final ShortcutInfo info = (ShortcutInfo) tag;
                    final Intent intent = info.intent;
                    final ComponentName componentName = intent.getComponent();
                    if (LauncherLog.DEBUG_UNREAD) {
                        LauncherLog.d(TAG, "updateComponentUnreadChanged 2: find component = "
                                + component + ",intent = " + intent + ",componentName = " + componentName);
                    }
                    if (componentName != null && componentName.equals(component)) {
                        LauncherLog.d(TAG, "updateComponentUnreadChanged 1: find component = "
                                + component + ",tag = " + tag + ",j = " + j + ",cellX = "
                                + info.cellX + ",cellY = " + info.cellY);
                        info.unreadNum = unreadNum;
                        ((BubbleTextView) view).invalidate();
                    }
                } else if (tag instanceof FolderInfo) {
                    ((FolderIcon) view).updateFolderUnreadNum(component, unreadNum);
                    ((FolderIcon) view).invalidate();
                }
            }
        }

        /// M: Update shortcut within folder if open folder exists.
        Folder openFolder = getOpenFolder();
        if (openFolder != null) {
            openFolder.updateContentUnreadNum();
        }
    }
    /**@}**/

    ///M. ALPS01888456. when receive  configuration change, cancel drag.
    public void cancelDrag() {
        mDragController.cancelDrag();
        mSpringLoadedDragController.cancel();
    }
    ///M.
    
    // Add for MyUI---20150714
    private boolean mUpdateWallpaperOffsetImmediately = false;
    public void updateWallpaperOffsetImmediately() {
        mUpdateWallpaperOffsetImmediately = true;
    }
    private void updateWallpaperOffset() {
    	if (QCLog.DEBUG) {
			QCLog.d(TAG, "mWallpaperManager updateWallpaperOffsets()");
		}
        boolean updateNow = false;
        boolean keepUpdating = true;
        if (mUpdateWallpaperOffsetImmediately) {
            updateNow = true;
            keepUpdating = false;
            mWallpaperOffset.jumpToFinal();
            mUpdateWallpaperOffsetImmediately = false;
        } else {
            updateNow = keepUpdating = mWallpaperOffset.computeScrollOffset();
        }
        if (updateNow) {
        	mWallpaperOffset.updateOffset(true);
        }
        if (keepUpdating) {
            invalidate();
        }
    	
    }

    DisplayMetrics dm = null;
    @Override
    protected void dispatchDraw(Canvas canvas) {
    	if (QCLog.DEBUG) {
			QCLog.d(TAG, "workspace dispatchDraw() and mIsPageMoving = "+mIsPageMoving);
		}
    	//sunfeng modify @20150824 for JLLEL-332 start:
    	if(dm == null){
	     	dm = new DisplayMetrics();
	        mLauncher.getWindowManager().getDefaultDisplay().getMetrics(dm);
    	}

        if (QCConfig.supportSlideEffect && mLauncher.hasCustomContentToLeft() && getScrollX() <= dm.widthPixels) {
            super.dispatchDraw(canvas);
            return;
        }

    	//sunfeng modify @20150824 for JLLEL-332 end:
        if(QCConfig.supportSlideEffect && mIsPageMoving){
            drawAnimations(canvas);
        }else{
            super.dispatchDraw(canvas);
        }
    }
    
    private Paint paint;
    private boolean mIsStaticWallpaper;
    public static void setSlideEffect(int effect){
        mSlideEffect = effect;
    }

    public static int mSlideEffect = UI_SLIDE_EFFECT_JUMP;
    private final float SLIDE_EFFECT_ALPHA_LIMIT = 0.1f;
    private final float SLIDE_EFFECT_SCALE_LIMIT = 0.5f;
    private final float SLIDE_EFFECT_ROTATE_LIMIT = 0.75f;

    private final Camera mCamera = new Camera();
    @Override
    protected void makeSlideEffect(Canvas canvas, View v, int index){
        // Modify for switch effect animation Jing.Wu 20150915 start
        if(isSwitchingState() || (!mLauncher.getIsSwitchAnimationing() && mState != State.NORMAL) || 
        		mDragController.isDragging() || isNormalScaling){
            // Modify for switch effect animation Jing.Wu 20150915 end
        	//Don't set effect when in SPRING_LOADED&SMALL mode.
            //Not set effect when dragging. Add for not only UI_SLIDE_EFFECT_ROTATION
            if(v.getAlpha() != QCConfig.defaultAlpha){
                v.setAlpha(QCConfig.defaultAlpha);
            }
            return;
        }

        int childOffset = 0;
        int childCount = getChildCount();
        if(isSupportCycleSlidingScreen()){
            if(index<0){
                childOffset = mScrollLeftX;//getChildOffset(0)-getMeasuredWidth();
            }else if(index>=childCount){
                childOffset = mScrollRightX;//getChildOffset(childCount-1)+getMeasuredWidth();
            }else{
                childOffset = getChildOffset(index);
            }
        }else{
            childOffset = getChildOffset(index);
        }

        float deltaX = getScrollX()-childOffset+getPaddingLeft();
        float absDeltaX = Math.abs(deltaX);
        float pageWidth = v.getWidth();
        float pageHeight = v.getHeight();
        float aspectRatio = pageHeight/pageWidth;
        float horizonRate = 1-absDeltaX/pageWidth;
        boolean needResetAlpha = true;//Add for opaque when switch from fade to other
        //Don't set alpha to zero, or shouldDrawChild() maybe return false, and child view will never redraw.
        float alpha = SLIDE_EFFECT_ALPHA_LIMIT+horizonRate*(1-SLIDE_EFFECT_ALPHA_LIMIT);
        float scale = SLIDE_EFFECT_SCALE_LIMIT+horizonRate*(1-SLIDE_EFFECT_SCALE_LIMIT);
        if(isSupportCycleSlidingScreen()){
            if(index>=childCount){
                //canvas.translate(getMeasuredWidth()*childCount, 0);
                canvas.translate(LauncherApplication.getScreenWidthPixel()*childCount, 0);
            }else if(index<0){
                //canvas.translate(-getMeasuredWidth()*childCount, 0);
                canvas.translate(-LauncherApplication.getScreenWidthPixel()*childCount, 0);
            }
        }
        if (QCLog.DEBUG) {
			QCLog.d(TAG, "makeSlideEffect() index="+index
					+",childOffset="+childOffset
					+",getScrollX()="+getScrollX()
	        		+",deltaX="+deltaX
	        		+",absDeltaX="+absDeltaX
	        		+",pageWidth="+pageWidth
	        		+",pageHeight="+pageHeight
	        		+",aspectRatio="+aspectRatio
	        		+",horizonRate="+horizonRate
	        		+",alpha="+alpha
	        		+",scale="+scale);
		}
        switch(mSlideEffect){
        case UI_SLIDE_EFFECT_ROTATION:{
            float rotateCenterX = childOffset+pageWidth/2.0f;
            if(isSupportCycleSlidingScreen()){
                if(index<0){
                    rotateCenterX = mScrollRightX-pageWidth/2.0f;
                }else if(index>=childCount){
                    rotateCenterX = mScrollLeftX+pageWidth/2.0f;
                }
            }
            float angle = -90*deltaX/pageWidth;//Negative means outboard of cube; Positive will be inside
            float postScale = SLIDE_EFFECT_ROTATE_LIMIT 
                    + Math.abs(2*absDeltaX/pageWidth-1)*(1-SLIDE_EFFECT_ROTATE_LIMIT); 
            Matrix matrix = new Matrix();
            
            mCamera.save();
            //mCamera.setLocation(mChildrenCellRect.left, mChildrenCellRect.top, -8);
            //mCamera.translate(mChildrenCellRect.left+rotateCenterX+getPaddingLeft(), -(mChildrenCellRect.top+pageHeight/2+getPaddingTop()), 0);
            mCamera.rotateY(angle);
            mCamera.getMatrix(matrix);
            mCamera.restore();
            
            matrix.preTranslate(-rotateCenterX, -pageHeight/2);
            //matrix.preScale(postScale, postScale, mChildrenCellRect.left+rotateCenterX+getPaddingLeft(), mChildrenCellRect.top+pageHeight/2+getPaddingTop());
            matrix.preScale(postScale, postScale, rotateCenterX, pageHeight/2);
            matrix.postTranslate(rotateCenterX+deltaX, pageHeight/2);
            
            canvas.concat(matrix);
            v.setAlpha(alpha);
            // Modify for switch effect animation Jing.Wu 20150915 start
            if (mLauncher.getIsNeedResetAlpha()) {
                // Modify for switch effect animation Jing.Wu 20150915 end
				needResetAlpha = true;
			} else {
				needResetAlpha = false;
			}
        }
            break;
        case UI_SLIDE_EFFECT_CUBE:{
            if(deltaX != 0){
                float rotateCenterX = calCubeRotateCenterX(pageWidth, childOffset, deltaX);
                if(isSupportCycleSlidingScreen()){
                    if(index<0){
                        rotateCenterX = mScrollRightX;
                    }else if(index>=childCount){
                        rotateCenterX = 0;
                    }
                }
                float angle = -90f*deltaX/pageWidth;//Negative means outboard of cube; Positive will be inside
                Matrix matrix = new Matrix();
                mCamera.save();
                mCamera.rotateY(angle);
                mCamera.getMatrix(matrix);
                mCamera.restore();
                matrix.preTranslate(-rotateCenterX, -pageHeight/2);
                matrix.postTranslate(rotateCenterX, pageHeight/2);
                canvas.concat(matrix);
            }
        }
            break;
        case UI_SLIDE_EFFECT_FADE_INOUT:
            v.setAlpha(alpha);
            // Modify for switch effect animation Jing.Wu 20150915 start
            if (mLauncher.getIsNeedResetAlpha()) {
                // Modify for switch effect animation Jing.Wu 20150915 end
				needResetAlpha = true;
			} else {
				needResetAlpha = false;
			}
            break;
        case UI_SLIDE_EFFECT_LAYER:
            if(deltaX>0){
                //deltaX>0 mean this is left page on screen
                canvas.translate(deltaX, 0);
                if(childOffset < 0){
                    canvas.scale(scale, scale, mMaxScrollX+pageWidth/2, pageHeight/2);
                }else if(childOffset > mMaxScrollX){
                    canvas.scale(scale, scale);
                }else{
                    canvas.scale(scale, scale,childOffset+pageWidth/2, pageHeight/2);
                }
                v.setAlpha(alpha);
                // Modify for switch effect animation Jing.Wu 20150915 start
            if (mLauncher.getIsNeedResetAlpha()) {
                // Modify for switch effect animation Jing.Wu 20150915 end
				needResetAlpha = true;
			} else {
				needResetAlpha = false;
			}
            }
            break;
        case UI_SLIDE_EFFECT_JUMP:
            //Both left and right screen translate on positive Y axis
            canvas.translate(0, absDeltaX*aspectRatio);
            break;
        case UI_SLIDE_EFFECT_DEFAULT:
        default:
            break;
        }
        if(needResetAlpha && v.getAlpha() != QCConfig.defaultAlpha){
            v.setAlpha(QCConfig.defaultAlpha);
        }
    }
    
    private float calCubeRotateCenterX(float width, float offset, float deltaX){
        float halfScreenX = width/2.0f;
        float viewCenterX = offset+halfScreenX;
        float leftRight = deltaX/Math.abs(deltaX);
        float rotateCenterX = viewCenterX+leftRight*halfScreenX;
        return rotateCenterX;
    }
    
    protected void createMasquradeWallpaper(){
    	if (QCLog.DEBUG) {
			QCLog.d(TAG, "mWallpaperManager createMasquradeWallpaper()");
		}
		//move here for reduce new paint count     
        paint = new Paint(Paint.FILTER_BITMAP_FLAG|Paint.DITHER_FLAG);
        paint.setAntiAlias(true);    	
		//end move
        BitmapDrawable mDrawable = (BitmapDrawable)mWallpaperManager.getDrawable();
        mWallpaperBitmap = mDrawable.getBitmap();
    }

    @Override
    protected void drawMasquradeWallpaper(Canvas canvas){
    	if (QCLog.DEBUG) {
			QCLog.d(TAG, "mIsStaticWallpaper = "+mIsStaticWallpaper);
		}
        if(!mIsStaticWallpaper){
            return;//we can't do masqurade for live wallpaper.
        }
        
        //Rect canvasRect = canvas.getClipBounds();
        Rect canvasRect = new Rect();
		int left = mChildrenCellRect.left;
		int top = mChildrenCellRect.top;
		int right = mChildrenCellRect.right;
		int bottom = mChildrenCellRect.bottom;
		if (QCLog.DEBUG) {
			QCLog.d(TAG, "mWallpaperManager drawMasquradeWallpaper()"+
	    			",canvas.left:"+left+
	    			",canvas.top:"+top+
	    			",canvas.right:"+right+
	    			",canvas.bottom:"+bottom+
	    			",mWallpaperBitmap.getHeight():"+mWallpaperBitmap.getHeight()+
	    			",getMeasuredHeight():"+getMeasuredHeight()+
	    			",getgetMeasuredWidth():"+getMeasuredWidth()+
	    			",getScrollX():"+getScrollX());
		}

        int scrollX = getScrollX();
        if(scrollX < 0){
            canvas.save();
			canvasRect.set(left+scrollX, top, left+(int)LauncherApplication.getScreenWidthPixel()+scrollX, bottom);
			canvas.clipRect(canvasRect);
            //canvas.translate(scrollX, 0);
            int alpha = (int)(255*(1.0f-Math.abs(scrollX)*1.0f/LauncherApplication.getScreenWidthPixel())+0.5);
            Rect src = new Rect(mWallpaperBitmap.getWidth()-(int)LauncherApplication.getScreenWidthPixel(), 0, mWallpaperBitmap.getWidth(), mWallpaperBitmap.getHeight());
            //Rect dst = new Rect(0, 0, getMeasuredWidth(), getMeasuredHeight());
            //Rect dst = new Rect(0, 0, (int)LauncherApplication.getScreenWidth(), (int)LauncherApplication.getScreenHeight());
            Rect dst = new Rect(left+scrollX, top, left+(int)LauncherApplication.getScreenWidthPixel()+scrollX, bottom);
            paint.setAlpha(255-alpha);
            canvas.drawBitmap(mWallpaperBitmap, src, dst, paint);
            canvas.restore();
        }else{
            canvas.save();
			canvasRect.set(right+(scrollX-mMaxScrollX)-(int)LauncherApplication.getScreenWidthPixel(), top, right+(scrollX-mMaxScrollX), bottom);
			canvas.clipRect(canvasRect);
            //canvas.translate(scrollX-right, 0);
            int alpha = (int)(255*(1.0f-Math.abs(scrollX-mMaxScrollX)*1.0f/LauncherApplication.getScreenWidthPixel())+0.5);
            paint.setAlpha(255-alpha);
            canvas.drawBitmap(mWallpaperBitmap, right+(scrollX-mMaxScrollX)-(int)LauncherApplication.getScreenWidthPixel(), top, paint);
            canvas.restore();
        }
//        canvas.clipRect(left, top, right, bottom);
//        canvas.save();
    }

}
