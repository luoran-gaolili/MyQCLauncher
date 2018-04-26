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
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.animation.ValueAnimator.AnimatorUpdateListener;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Parcelable;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.qingcheng.home.R;
//sunfeng add @20150802 for folder start:
import android.graphics.BitmapFactory;

import com.qingcheng.home.config.QCConfig;
import com.qingcheng.home.database.QCPreference;
import com.qingcheng.home.util.QCLog;

import android.util.DisplayMetrics;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Calendar;

//sunfeng add @20150802 for folder end:
import com.qingcheng.home.DropTarget.DragObject;
import com.qingcheng.home.FolderInfo.FolderListener;
import com.mediatek.launcher3.ext.LauncherExtPlugin;
import com.mediatek.launcher3.ext.LauncherLog;

import java.util.ArrayList;


/**
 * An icon that can appear on in the workspace representing an {@link UserFolder}.
 */
public class FolderIcon extends FrameLayout implements FolderListener {
    private static final String TAG = "FolderIcon";

    private Launcher mLauncher;
    private Folder mFolder;
    private FolderInfo mInfo;
    private static boolean sStaticValuesDirty = true;

    private CheckLongPressHelper mLongPressHelper;

    // The number of icons to display in the
   // private static final int NUM_ITEMS_IN_PREVIEW = 3;
    private static final int NUM_ITEMS_IN_PREVIEW = 4;
    private static final int NUM_COLUMN_IN_PREVIEW = 2;
 
    private static final int CONSUMPTION_ANIMATION_DURATION = 100;
    private static final int DROP_IN_ANIMATION_DURATION = 400;
    private static final int INITIAL_ITEM_ANIMATION_DURATION = 350;
    private static final int FINAL_ITEM_ANIMATION_DURATION = 200;

    // The degree to which the inner ring grows when accepting drop
    private static final float INNER_RING_GROWTH_FACTOR = 0.15f;

    // The degree to which the outer ring is scaled in its natural state
    private static final float OUTER_RING_GROWTH_FACTOR = 0.3f;

    private static final float SHORTCUT_GROWTH_FACTOR = 0.3f;
    
    // The amount of vertical spread between items in the stack [0...1]
    private static final float PERSPECTIVE_SHIFT_FACTOR = 0.18f;

    // Flag as to whether or not to draw an outer ring. Currently none is designed.
    public static final boolean HAS_OUTER_RING = true;

    // Flag whether the folder should open itself when an item is dragged over is enabled.
    public static final boolean SPRING_LOADING_ENABLED = true;

    // The degree to which the item in the back of the stack is scaled [0...1]
    // (0 means it's not scaled at all, 1 means it's scaled to nothing)
    private static final float PERSPECTIVE_SCALE_FACTOR = 0.35f;

    // Delay when drag enters until the folder opens, in miliseconds.
    private static final int ON_OPEN_DELAY = 800;

    public static Drawable sSharedFolderLeaveBehind = null;

    private ImageView mPreviewBackground;
    private BubbleTextView mFolderName;

    FolderRingAnimator mFolderRingAnimator = null;
    
    private static final int FOLDER_RING_ANIMATOR_STATE_NORMAL = 0;
    private static final int FOLDER_RING_ANIMATOR_STATE_RINGING = 1;
    private int mFolderRingAnimatorState = FOLDER_RING_ANIMATOR_STATE_NORMAL;

    // These variables are all associated with the drawing of the preview; they are stored
    // as member variables for shared usage and to avoid computation on each frame
    private int mIntrinsicIconSize;
    private float mBaselineIconScale;
    private int mBaselineIconSize;
    private int mAvailableSpaceInPreview;
    private int mTotalWidth = -1;
    private int mPreviewOffsetX;
    private int mPreviewOffsetY;
    private float mMaxPerspectiveShift;
    boolean mAnimating = false;
    private Rect mOldBounds = new Rect();

    private float mSlop;

    private PreviewItemDrawingParams mParams = new PreviewItemDrawingParams(0, 0, 0, 0);
    private PreviewItemDrawingParams mAnimParams = new PreviewItemDrawingParams(0, 0, 0, 0);
    private ArrayList<ShortcutInfo> mHiddenItems = new ArrayList<ShortcutInfo>();

    private Alarm mOpenAlarm = new Alarm();
    private ItemInfo mDragInfo;

    // M:[OP09][CF] @{
    boolean mSupportEditAndHideApps;
    private String shishiwendu = "";
    private String shishicode = "";
    private boolean changedSize;
    // M:[OP09][CF] }@

    public FolderIcon(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public FolderIcon(Context context) {
        super(context);
        init();
    }

    private void init() {
        mLongPressHelper = new CheckLongPressHelper(this);
        //M:[OP09][CF] @{
        mSupportEditAndHideApps = LauncherExtPlugin.getInstance().getWorkspaceExt(getContext())
                .supportEditAndHideApps();
        //M:[OP09][CF] }@
    }

    public boolean isDropEnabled() {
        final ViewGroup cellLayoutChildren = (ViewGroup) getParent();
        final ViewGroup cellLayout = (ViewGroup) cellLayoutChildren.getParent();
        final Workspace workspace = (Workspace) cellLayout.getParent();
        return !workspace.workspaceInModalState();
    }

    static FolderIcon fromXml(int resId, Launcher launcher, ViewGroup group,
            FolderInfo folderInfo, IconCache iconCache) {
        return fromXml(resId, launcher, group, folderInfo, iconCache, false);
    }
    
    static FolderIcon fromXml(int resId, Launcher launcher, ViewGroup group,
            FolderInfo folderInfo, IconCache iconCache, boolean fromAllApp) {
        @SuppressWarnings("all") // suppress dead code warning
        final boolean error = INITIAL_ITEM_ANIMATION_DURATION >= DROP_IN_ANIMATION_DURATION;
        if (error) {
            throw new IllegalStateException("DROP_IN_ANIMATION_DURATION must be greater than " +
                    "INITIAL_ITEM_ANIMATION_DURATION, as sequencing of adding first two items " +
                    "is dependent on this");
        }
        LauncherAppState app = LauncherAppState.getInstance();
        DeviceProfile grid = app.getDynamicGrid().getDeviceProfile();

        FolderIcon icon = (FolderIcon) LayoutInflater.from(launcher).inflate(resId, group, false);
        icon.setClipToPadding(false);
        icon.mFolderName = (BubbleTextView) icon.findViewById(R.id.folder_icon_name);
        icon.mFolderName.setText(folderInfo.title);
//        icon.mFolderName.setTextSize(TypedValue.COMPLEX_UNIT_PX, grid.iconTextSizePx);
//        icon.mFolderName.setCompoundDrawables(null, null, null, null);
//        icon.mFolderName.setCompoundDrawablePadding(0);
//        icon.mFolderName.setPadding(0, 0, 0, 0);
        FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) icon.mFolderName.getLayoutParams();
        int cellHeight = 0;
        if (launcher.getWorkspace()!=null && launcher.getWorkspace().getChildAt(0)!=null) {
        	CellLayout mCellLayout = (CellLayout)launcher.getWorkspace().getChildAt(0);
        	cellHeight = mCellLayout.getCellHeight();
		} else {
			cellHeight = grid.cellHeightPx;
		}
        int topPadding = icon.getPaddingTop();
        if (icon.getPaddingTop()==0) {
			topPadding = (int)((cellHeight-grid.iconSizePx-grid.iconDrawablePaddingPx-grid.iconTextSizePx)/2);
		}
        lp.topMargin = ((topPadding+grid.iconSizePx)-((int)(cellHeight/2)))+grid.iconDrawablePaddingPx;
        
        QCLog.d(TAG, "fromXml() and topPadding = "+topPadding+" , bottom = "+(topPadding+grid.iconSizePx)+" , top = "+((int)(cellHeight/2)-5));
        
        /// M: Customize folder name layout params for CT project.
        //LauncherExtPlugin.getInstance().getWorkspaceExt(launcher)
                //.customizeFolderNameLayoutParams(lp, grid.iconSizePx, grid.iconDrawablePaddingPx);

        // Offset the preview background to center this view accordingly
        icon.mPreviewBackground = (ImageView) icon.findViewById(R.id.preview_background);
        lp = (FrameLayout.LayoutParams) icon.mPreviewBackground.getLayoutParams();
        //lp.topMargin = grid.folderBackgroundOffset;
        lp.width = grid.folderIconSizePx;
        lp.height = grid.folderIconSizePx;

        /// M: Customize folder icon layout params for CT project.
        //LauncherExtPlugin.getInstance().getWorkspaceExt(launcher)
                //.customizeFolderPreviewLayoutParams(lp);
        
        icon.setTag(folderInfo);
        icon.setOnClickListener(launcher);
        icon.mInfo = folderInfo;
        icon.mLauncher = launcher;
        icon.setContentDescription(String.format(launcher.getString(R.string.folder_name_format),
                folderInfo.title));
        Folder folder = Folder.fromXml(launcher);
        folder.setDragController(launcher.getDragController());
        folder.setFolderIcon(icon);
        folder.setIsPageViewFolder(fromAllApp);
        folder.bind(folderInfo);
        icon.mFolder = folder;
        if (icon != null) { 
        	icon.mPreviewBackground.setBackgroundResource(R.drawable.portal_ring_inner_holo);
        }//end move
		
        icon.mFolderRingAnimator = new FolderRingAnimator(launcher, icon);
        folderInfo.addListener(icon);

        icon.setOnFocusChangeListener(launcher.mFocusHandler);
        return icon;
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        sStaticValuesDirty = true;
        return super.onSaveInstanceState();
    }

    public static class FolderRingAnimator {
        public int mCellX;
        public int mCellY;
        private CellLayout mCellLayout;
        private static int mLastCellX;
        private static int mLastCellY;
        private static CellLayout mLastCellLayout;
        public float mShorcutIconSize;
        public float mOuterRingSize;
        public float mInnerRingSize;
        public FolderIcon mFolderIcon = null;
        public static Drawable sSharedOuterRingDrawable = null;
        public static Drawable sSharedInnerRingDrawable = null;
        public static int sPreviewSize = -1;
        public static int sPreviewPadding = -1;  
		//Add by liuzhigang for adapt theme
        private static int sIconHorizontalPadding=0;
        private static int sIconVerticalPadding=0;
        private static int sPreviewExtraOffsetY=0;

        private boolean isAcceptAnimating = false;
        private boolean isNeutralAnimator = false;
        private ValueAnimator mAcceptAnimator;
        private ValueAnimator mNeutralAnimator;

        public FolderRingAnimator(Launcher launcher, FolderIcon folderIcon) {
            mFolderIcon = folderIcon;
            Resources res = launcher.getResources();

            Drawable drawable = null;
            ThemeInfo theme = null;
            LauncherAppState app = LauncherAppState.getInstance();
            IconCache iconCache = app.getIconCache();
            if (iconCache != null) {
                theme = iconCache.getTheme();
            }


            drawable = theme.getFolderInner();

            if (folderIcon != null) {
                if (drawable == null) {
                    drawable = res.getDrawable(R.drawable.portal_ring_inner_holo);
                }
                mBackground = drawable;
                folderIcon.mPreviewBackground.setBackground(drawable);
            }


            // We need to reload the static values when configuration changes in case they are
            // different in another configuration
            if (sStaticValuesDirty) {
                // Change for MyUI---20150825
                //sPreviewSize = res.getDimensionPixelSize(R.dimen.folder_preview_size);
                //sPreviewSize = res.getDimensionPixelSize(R.dimen.app_icon_size);
                sPreviewSize = LauncherAppState.getInstance().getDynamicGrid().getDeviceProfile().iconSizePx;
                sPreviewPadding = res.getDimensionPixelSize(R.dimen.folder_preview_padding);
                sIconHorizontalPadding = res.getDimensionPixelSize(R.dimen.folder_icon_hpadding);
                sIconVerticalPadding = res.getDimensionPixelSize(R.dimen.folder_icon_vpadding);
                sPreviewExtraOffsetY = res.getDimensionPixelSize(R.dimen.folder_preview_extra_offsety);

                sSharedOuterRingDrawable = res.getDrawable(R.drawable.portal_ring_outer_holo);
                if (drawable != null) {
                    sSharedInnerRingDrawable = drawable;
                } else {
                    sSharedInnerRingDrawable = res.getDrawable(R.drawable.portal_ring_inner_holo);
                }
                sSharedFolderLeaveBehind = res.getDrawable(R.drawable.portal_ring_rest);
                sStaticValuesDirty = false;

            }
        }

        public void animateToAcceptState() {
        	if (QCLog.DEBUG) {
    			QCLog.d(TAG, "animateToAcceptState()");
    		}
            if (mNeutralAnimator != null) {
                mNeutralAnimator.cancel();
            }
            mAcceptAnimator = LauncherAnimUtils.ofFloat(mCellLayout, 0f, 1f);
            mAcceptAnimator.setDuration(CONSUMPTION_ANIMATION_DURATION);

            final int previewSize = sPreviewSize;
            mAcceptAnimator.addUpdateListener(new AnimatorUpdateListener() {
                public void onAnimationUpdate(ValueAnimator animation) {
                    final float percent = (Float) animation.getAnimatedValue();
                    // Change for MyUI---20150825
                    //mOuterRingSize = (1 + percent * OUTER_RING_GROWTH_FACTOR) * previewSize;
                    //mInnerRingSize = (1 + percent * INNER_RING_GROWTH_FACTOR) * previewSize;
                    mOuterRingSize = previewSize;
                    mInnerRingSize = previewSize;
                    mShorcutIconSize = ((1-SHORTCUT_GROWTH_FACTOR) + (1-percent) * SHORTCUT_GROWTH_FACTOR) * previewSize;
                    if (mCellLayout != null) {
                        mCellLayout.invalidate();
                    }
                }
            });
            mAcceptAnimator.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationStart(Animator animation) {
                	isAcceptAnimating = true;
                    if (mFolderIcon != null) {
                        mFolderIcon.mPreviewBackground.setVisibility(INVISIBLE);
                    }
                    View mChild = mCellLayout.getChildAt(mCellX, mCellY);
                    if (mChild instanceof BubbleTextView) {
						BubbleTextView mShortcut = (BubbleTextView)mChild;
						mShortcut.setTextVisibility(false);
					} else if (mChild instanceof FolderIcon) {
						FolderIcon mFolder = (FolderIcon)mChild;
						mFolder.setTextVisible(false);
					}
                }

				@Override
				public void onAnimationEnd(Animator animation) {
					// TODO Auto-generated method stub
					super.onAnimationEnd(animation);
					isAcceptAnimating = false;
				}

				@Override
				public void onAnimationCancel(Animator animation) {
					// TODO Auto-generated method stub
					super.onAnimationCancel(animation);
					isAcceptAnimating = false;
				}
            });
            mAcceptAnimator.start();
        }

        public void animateToNaturalState() {
        	if (QCLog.DEBUG) {
    			QCLog.d(TAG, "animateToNaturalState()");
    		}
            if (mAcceptAnimator != null) {
                mAcceptAnimator.cancel();
            }
            mNeutralAnimator = LauncherAnimUtils.ofFloat(mCellLayout, 0f, 1f);
            mNeutralAnimator.setDuration(CONSUMPTION_ANIMATION_DURATION);

            final int previewSize = sPreviewSize;
            mNeutralAnimator.addUpdateListener(new AnimatorUpdateListener() {
                public void onAnimationUpdate(ValueAnimator animation) {
                    final float percent = (Float) animation.getAnimatedValue();
                    // Change for MyUI---20150825
                    //mOuterRingSize = (1 + (1 - percent) * OUTER_RING_GROWTH_FACTOR) * previewSize;
                    //mInnerRingSize = (1 + (1 - percent) * INNER_RING_GROWTH_FACTOR) * previewSize;
                    mOuterRingSize = previewSize;
                    mInnerRingSize = previewSize;
                    //mShorcutIconSize = ((1-SHORTCUT_GROWTH_FACTOR) + percent * SHORTCUT_GROWTH_FACTOR) * previewSize;
                    mShorcutIconSize = previewSize;
                    if (mCellLayout != null) {
                        mCellLayout.invalidate();
                    }
                }
            });
            mNeutralAnimator.addListener(new AnimatorListenerAdapter() {
				@Override
				public void onAnimationStart(Animator animation) {
					// TODO Auto-generated method stub
					super.onAnimationStart(animation);
					isNeutralAnimator = true;
				}
				
                @Override
                public void onAnimationEnd(Animator animation) {
					isNeutralAnimator = false;
                    if (mCellLayout != null) {
                        mCellLayout.hideFolderAccept(FolderRingAnimator.this);
                    }
                    if (mFolderIcon != null) {
                        mFolderIcon.mPreviewBackground.setVisibility(VISIBLE);
                    }
                    View mChild = mCellLayout.getChildAt(mCellX, mCellY);
                    if (mChild instanceof BubbleTextView) {
						BubbleTextView mShortcut = (BubbleTextView)mChild;
						mShortcut.setTextVisibility(true);
					} else if (mChild instanceof FolderIcon) {
						FolderIcon mFolder = (FolderIcon)mChild;
						if (mFolder.getFolderInfo().container != LauncherSettings.Favorites.CONTAINER_HOTSEAT || QCConfig.supportHotseatText) {
							mFolder.setTextVisible(true);
						}
					}
                }

				@Override
				public void onAnimationCancel(Animator animation) {
					// TODO Auto-generated method stub
					super.onAnimationCancel(animation);
					isNeutralAnimator = false;
		        	if (mLastCellLayout!=null) {
		    			View mChild = mLastCellLayout.getChildAt(mLastCellX, mLastCellY);
		    			if (mChild!=null) {
		    				if (QCLog.DEBUG) {
								QCLog.d(TAG, "setLastCell() and setScaleX(1f)");
							}
		        			mChild.setScaleX(1f);
		        			mChild.setScaleY(1f);
		                    if (mChild instanceof BubbleTextView) {
								BubbleTextView mShortcut = (BubbleTextView)mChild;
								mShortcut.setTextVisibility(true);
							} else if (mChild instanceof FolderIcon) {
								FolderIcon mFolder = (FolderIcon)mChild;
								if (mFolder.getFolderInfo().container != LauncherSettings.Favorites.CONTAINER_HOTSEAT || QCConfig.supportHotseatText) {
									mFolder.setTextVisible(true);
								}
							}
						} else {
		    				if (QCLog.DEBUG) {
								QCLog.d(TAG, "setLastCell() and mChild = null");
							}
						}
					} else {
						if (QCLog.DEBUG) {
							QCLog.d(TAG, "setLastCell() and mLastCellLayout=null");
						}
					}
				}
                
            });
            mNeutralAnimator.start();
        }

        // Location is expressed in window coordinates
        public void getCell(int[] loc) {
            loc[0] = mCellX;
            loc[1] = mCellY;
        }

        // Location is expressed in window coordinates
        public void setCell(int x, int y) {
        	//setLastCell();
        	mLastCellX = mCellX;
        	mLastCellY = mCellY;
            mCellX = x;
            mCellY = y;
        }
        
        public void setLastCell() {
        	mLastCellX = mCellX;
        	mLastCellY = mCellY;
        	mLastCellLayout = mCellLayout;
        	if (mLastCellLayout!=null) {
    			View mChild = mLastCellLayout.getChildAt(mLastCellX, mLastCellY);
    			if (mChild!=null) {
    				if (QCLog.DEBUG) {
						QCLog.d(TAG, "setLastCell() and setScaleX(1f)");
					}
        			mChild.setScaleX(1f);
        			mChild.setScaleY(1f);
				} else {
    				if (QCLog.DEBUG) {
						QCLog.d(TAG, "setLastCell() and mChild = null");
					}
				}
			} else {
				if (QCLog.DEBUG) {
					QCLog.d(TAG, "setLastCell() and mLastCellLayout=null");
				}
			}
        }

        public void setCellLayout(CellLayout layout) {
        	mLastCellLayout = mCellLayout;
            mCellLayout = layout;
        }

        public float getShortcutSize() {
            return mShorcutIconSize;
        }

        public float getOuterRingSize() {
            return mOuterRingSize;
        }

        public float getInnerRingSize() {
            return mInnerRingSize;
        }
    }

    public Folder getFolder() {
        return mFolder;
    }

    FolderInfo getFolderInfo() {
        return mInfo;
    }

    private boolean willAcceptItem(ItemInfo item) {
        final int itemType = item.itemType;
        return ((itemType == LauncherSettings.Favorites.ITEM_TYPE_APPLICATION ||
                itemType == LauncherSettings.Favorites.ITEM_TYPE_SHORTCUT) &&
                !mFolder.isFull() && item != mInfo && !mInfo.opened);
    }

    public boolean acceptDrop(Object dragInfo) {
        final ItemInfo item = (ItemInfo) dragInfo;
        return !mFolder.isDestroyed() && willAcceptItem(item);
    }

    public void addItem(ShortcutInfo item) {
        mInfo.add(item);
    }

    public void onDragEnter(Object dragInfo) {
        if (mFolder.isDestroyed() || !willAcceptItem((ItemInfo) dragInfo)) return;
        CellLayout.LayoutParams lp = (CellLayout.LayoutParams) getLayoutParams();
        CellLayout layout = (CellLayout) getParent().getParent();
        mFolderRingAnimator.setCell(lp.cellX, lp.cellY);
        mFolderRingAnimator.setCellLayout(layout);
        mFolderRingAnimator.animateToAcceptState();
        layout.showFolderAccept(mFolderRingAnimator);
        mOpenAlarm.setOnAlarmListener(mOnOpenListener);
        if (SPRING_LOADING_ENABLED &&
                ((dragInfo instanceof AppInfo) || (dragInfo instanceof ShortcutInfo))) {
            // TODO: we currently don't support spring-loading for PendingAddShortcutInfos even
            // though widget-style shortcuts can be added to folders. The issue is that we need
            // to deal with configuration activities which are currently handled in
            // Workspace#onDropExternal.
            mOpenAlarm.setAlarm(ON_OPEN_DELAY);
        }
        mDragInfo = (ItemInfo) dragInfo;
    }

    public void onDragOver(Object dragInfo) {
    }

    OnAlarmListener mOnOpenListener = new OnAlarmListener() {
        public void onAlarm(Alarm alarm) {
            ShortcutInfo item;
            if (mDragInfo instanceof AppInfo) {
                // Came from all apps -- make a copy.
                item = ((AppInfo) mDragInfo).makeShortcut();
                item.spanX = 1;
                item.spanY = 1;
            }
            ///M: Added to filter out the PendingAddItemInfo instance.@{
            else if (mDragInfo instanceof PendingAddItemInfo) {
                if (LauncherLog.DEBUG) {
                    LauncherLog.d(TAG, "onAlarm: mDragInfo instanceof PendingAddItemInfo");
                }
                return;
            }
            ///M: @}
            else {
                item = (ShortcutInfo) mDragInfo;
            }
            mFolder.beginExternalDrag(item);
            mLauncher.openFolder(FolderIcon.this);
        }
    };

    public void performCreateAnimation(final ShortcutInfo destInfo, final View destView,
            final ShortcutInfo srcInfo, final DragView srcView, Rect dstRect,
            float scaleRelativeToDragLayer, Runnable postAnimationRunnable) {

        // These correspond two the drawable and view that the icon was dropped _onto_
        Drawable animateDrawable = getTopDrawable((TextView) destView);
        computePreviewDrawingParams(animateDrawable.getIntrinsicWidth(),
                destView.getMeasuredWidth());

        // This will animate the first item from it's position as an icon into its
        // position as the first item in the preview
        animateFirstItem(animateDrawable, INITIAL_ITEM_ANIMATION_DURATION, false, null);
        addItem(destInfo);

        // This will animate the dragView (srcView) into the new folder
        onDrop(srcInfo, srcView, dstRect, scaleRelativeToDragLayer, 1, postAnimationRunnable, null);
    }

    public void performDestroyAnimation(final View finalView, Runnable onCompleteRunnable) {
        Drawable animateDrawable = getTopDrawable((TextView) finalView);
        computePreviewDrawingParams(animateDrawable.getIntrinsicWidth(),
                finalView.getMeasuredWidth());

        // This will animate the first item from it's position as an icon into its
        // position as the first item in the preview
        animateFirstItem(animateDrawable, FINAL_ITEM_ANIMATION_DURATION, true,
                onCompleteRunnable);
    }

    public void onDragExit(Object dragInfo) {
        onDragExit();
    }

    public void onDragExit() {
    	if (QCLog.DEBUG) {
			QCLog.d(TAG, "onDragExit()");
		}
        mFolderRingAnimator.animateToNaturalState();
        mOpenAlarm.cancelAlarm();
    }

    private void onDrop(final ShortcutInfo item, DragView animateView, Rect finalRect,
            float scaleRelativeToDragLayer, int index, Runnable postAnimationRunnable,
            DragObject d) {
        if (LauncherLog.DEBUG) {
            LauncherLog.d(TAG, "onDrop: item = " + item + ", animateView = "
                    + animateView + ", finalRect = " + finalRect + ", scaleRelativeToDragLayer = "
                    + scaleRelativeToDragLayer + ", index = " + index + ", d = " + d);
        }

        item.cellX = -1;
        item.cellY = -1;

        // Typically, the animateView corresponds to the DragView; however, if this is being done
        // after a configuration activity (ie. for a Shortcut being dragged from AllApps) we
        // will not have a view to animate
        if (animateView != null) {
            DragLayer dragLayer = mLauncher.getDragLayer();
            Rect from = new Rect();
            dragLayer.getViewRectRelativeToSelf(animateView, from);
            Rect to = finalRect;
            if (to == null) {
                to = new Rect();
                Workspace workspace = mLauncher.getWorkspace();
                if (!mSupportEditAndHideApps) {
                    // Set cellLayout and this to it's final state to compute
                    //final animation locations
                    workspace.setFinalTransitionTransform((CellLayout) getParent().getParent());
                }
                float scaleX = getScaleX();
                float scaleY = getScaleY();
                setScaleX(1.0f);
                setScaleY(1.0f);
                scaleRelativeToDragLayer = dragLayer.getDescendantRectRelativeToSelf(this, to);
                // Finished computing final animation locations, restore current state
                setScaleX(scaleX);
                setScaleY(scaleY);
                if (!mSupportEditAndHideApps) {
                    workspace.resetTransitionTransform((CellLayout) getParent().getParent());
                }
            }

            int[] center = new int[2];
            float scale = getLocalCenterForIndex(index, center);
            center[0] = (int) Math.round(scaleRelativeToDragLayer * center[0]);
            center[1] = (int) Math.round(scaleRelativeToDragLayer * center[1]);

            to.offset(center[0] - animateView.getMeasuredWidth() / 2,
                      center[1] - animateView.getMeasuredHeight() / 2);

            float finalAlpha = index < NUM_ITEMS_IN_PREVIEW ? 0.5f : 0f;

            float finalScale = scale * scaleRelativeToDragLayer;
            dragLayer.animateView(animateView, from, to, finalAlpha,
                    1, 1, finalScale, finalScale, DROP_IN_ANIMATION_DURATION,
                    new DecelerateInterpolator(2), new AccelerateInterpolator(2),
                    postAnimationRunnable, DragLayer.ANIMATION_END_DISAPPEAR, null);
            addItem(item);
            mHiddenItems.add(item);
            mFolder.hideItem(item);
            postDelayed(new Runnable() {
                public void run() {
					FolderRingAnimator.sSharedOuterRingDrawable.setBounds(0, 0, FolderRingAnimator.sPreviewSize, FolderRingAnimator.sPreviewSize);
                	FolderRingAnimator.sSharedInnerRingDrawable.setBounds(0, 0, FolderRingAnimator.sPreviewSize, FolderRingAnimator.sPreviewSize);
                	CellLayout mCellLayout = (CellLayout) getParent().getParent();
                    if (mCellLayout != null) {
                    	mCellLayout.invalidateDrawable(FolderRingAnimator.sSharedOuterRingDrawable);
                    	mCellLayout.invalidateDrawable(FolderRingAnimator.sSharedInnerRingDrawable);
                    }

                    mHiddenItems.remove(item);
                    mFolder.showItem(item);
                    invalidate();
                }
            }, DROP_IN_ANIMATION_DURATION);
        } else {
            addItem(item);
        }
    }

    public void onDrop(DragObject d) {

        if(LauncherLog.DEBUG) {
            LauncherLog.d(TAG, "onDrop: DragObject = " + d);
        }

        ShortcutInfo item;
        if (d.dragInfo instanceof AppInfo) {
            // Came from all apps -- make a copy
            item = ((AppInfo) d.dragInfo).makeShortcut();
        } else {
            item = (ShortcutInfo) d.dragInfo;
        }
        mFolder.notifyDrop();
        onDrop(item, d.dragView, null, 1.0f, mInfo.contents.size(), d.postAnimationRunnable, d);
    }

    /***
     * 
     * sunfeng modify @20150928 
     */
    private int getHotSeatAndBootomShortCut(){
    	PageIndicator page = mLauncher.getWorkspace().getPageIndicator();
		int[] location =new int[2];
		page.getLocationInWindow(location);
		
    	if(page.getTop() > 100){
    		return page.getTop();
    	}
    	
    	if(location[1] > 100){
    		return location[1];
    	}
    	
		
        DisplayMetrics dm = new DisplayMetrics();
        mLauncher.getWindowManager().getDefaultDisplay().getMetrics(dm);
        int mScreenWidth = dm.widthPixels;
        int mScreenHeight = dm.heightPixels;
        
        if(mScreenHeight ==720 || mScreenWidth == 720 ){
        	return 1070;
        }
        /***
    	 * 1080P
    	 * getXY:[804, 1247]
    	 * getXY:[780, 1623]
    	 * 1435    1080*1920 Bottom icon 
    	 */
        if(mScreenHeight ==1080 || mScreenWidth == 1080 ){
        	return 1435;
        }
        return 0;
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        changedSize = true;
    }

    private void computePreviewDrawingParams(int drawableSize, int totalSize) {
		int mChangeY = getHotSeatAndBootomShortCut();
       boolean mMove = false;
       if((mOnClickY > mChangeY && getXy(0, this)[1] <mChangeY)||mOnClickY < mChangeY && getXy(0, this)[1] > mChangeY){
   			mMove = true;
   		}
        if (mIntrinsicIconSize != drawableSize || mTotalWidth != totalSize || mMove || changedSize) {
            if(changedSize){
                changedSize = false;
            }
            LauncherAppState app = LauncherAppState.getInstance();
//            DeviceProfile grid = app.getDynamicGrid().getDeviceProfile();

            mIntrinsicIconSize = drawableSize;
            mTotalWidth = totalSize;
            final int previewSize = FolderRingAnimator.sPreviewSize;
            final int previewPadding = FolderRingAnimator.sPreviewPadding;

            mAvailableSpaceInPreview = (previewSize - 2 * previewPadding);
            // cos(45) = 0.707  + ~= 0.1) = 0.8f
            int adjustedAvailableSpace = (int) ((mAvailableSpaceInPreview / 2) * (1 + 0.8f));

            int unscaledHeight = (int) (mIntrinsicIconSize * (1 + PERSPECTIVE_SHIFT_FACTOR));

            mBaselineIconScale = (1.0f * adjustedAvailableSpace / unscaledHeight);

            mBaselineIconSize = (int) (mIntrinsicIconSize * mBaselineIconScale);
            mMaxPerspectiveShift = mBaselineIconSize * PERSPECTIVE_SHIFT_FACTOR;

            mPreviewOffsetX = (mTotalWidth - mAvailableSpaceInPreview) / 2;
//            mPreviewOffsetY = previewPadding;
            if (mInfo.container == LauncherSettings.Favorites.CONTAINER_HOTSEAT) {
                mPreviewOffsetY = previewPadding;
            } else {
//                mPreviewOffsetY = previewPadding + FolderRingAnimator.sPreviewExtraOffsetY;
                mPreviewOffsetY = getPaddingTop()+previewPadding;
            }

            /// M: Customize folder preview Y Offset for CT project.
			//mPreviewOffsetY = LauncherExtPlugin.getInstance().getWorkspaceExt(mLauncher)
			//                    .customizeFolderPreviewOffsetY(mPreviewOffsetY, grid.folderBackgroundOffset);

            if (QCLog.DEBUG) {
    			QCLog.d(TAG, "computePreviewDrawingParams() 6 and mPreviewOffsetY = "+mPreviewOffsetY);
    		}

        }
    }
 
	  int[] hw= new int[2];
	public int[] getXy(int i,View child){
		int[] location =new int[2];
		int[] location1 =new int[2];
		child.getLocationInWindow(location);
//		child.getLocationOnScreen(location1);
//		hw[0] = child.getWidth();
//		hw[1] = child.getHeight();
//		Log.i(TAG," =getXy==  : "+Arrays.toString(location)+" w: "+hw[0]+" h: "+hw[1]+" wW:"+getWidth()+" wH"+getHeight());
	
		return location;
	}
    
    //add by zhangkun for folder blur effect 2013-08-27
    public int getAvailableSpaceInPreview(){
    	return mAvailableSpaceInPreview;
    }
    
    public int getPreviewOffsetY(){
    	return mPreviewOffsetX;
    }
    //end add
    
    private void computePreviewDrawingParams(Drawable d) {
        computePreviewDrawingParams(d.getIntrinsicWidth(), getMeasuredWidth());
    }

    class PreviewItemDrawingParams {
        PreviewItemDrawingParams(float transX, float transY, float scale, int overlayAlpha) {
            this.transX = transX;
            this.transY = transY;
            this.scale = scale;
            this.overlayAlpha = overlayAlpha;
        }
        float transX;
        float transY;
        float scale;
        int overlayAlpha;
        Drawable drawable;
    }

    private float getLocalCenterForIndex(int index, int[] center) {
        mParams = computePreviewItemDrawingParams(Math.min(NUM_ITEMS_IN_PREVIEW, index), mParams);

        mParams.transX += mPreviewOffsetX;
        mParams.transY += mPreviewOffsetY;
        float offsetX = mParams.transX + (mParams.scale * mIntrinsicIconSize) / 2;
        float offsetY = mParams.transY + (mParams.scale * mIntrinsicIconSize) / 2;

        center[0] = (int) Math.round(offsetX);
        center[1] = (int) Math.round(offsetY);
        return mParams.scale;
    }

    private PreviewItemDrawingParams computePreviewItemDrawingParams(int index,
            PreviewItemDrawingParams params) {
		float hPadding = FolderRingAnimator.sIconHorizontalPadding;
        float vPadding = FolderRingAnimator.sIconVerticalPadding;
        float scaledSize = (mAvailableSpaceInPreview- hPadding*(NUM_COLUMN_IN_PREVIEW-1))/NUM_COLUMN_IN_PREVIEW;
        
        float scale = scaledSize/mIntrinsicIconSize;
        int column = index%NUM_COLUMN_IN_PREVIEW;
        int row = index/NUM_COLUMN_IN_PREVIEW;
        float transX = (scaledSize+hPadding)*column;
        float transY = (scaledSize+vPadding)*row;
        int overlayAlpha = 0;

        if (params == null) {
            params = new PreviewItemDrawingParams(transX, transY, scale, overlayAlpha);
        } else {
            params.transX = transX;
            params.transY = transY;
            params.scale = scale;
            params.overlayAlpha = overlayAlpha;
        }
        
        
        return params;
    }

    private void drawPreviewItem(Canvas canvas, PreviewItemDrawingParams params) {
        canvas.save();
        canvas.translate(params.transX + mPreviewOffsetX, params.transY + mPreviewOffsetY);
        canvas.scale(params.scale, params.scale);
        Drawable d = params.drawable;

        if (d != null) {
            mOldBounds.set(d.getBounds());
            d.setBounds(0, 0, mIntrinsicIconSize, mIntrinsicIconSize);
            if (d instanceof FastBitmapDrawable) {
                FastBitmapDrawable fd = (FastBitmapDrawable) d;
                int oldBrightness = fd.getBrightness();
                fd.setBrightness(params.overlayAlpha);
                d.draw(canvas);
                fd.setBrightness(oldBrightness);
            } else {
                d.setColorFilter(Color.argb(params.overlayAlpha, 255, 255, 255),
                        PorterDuff.Mode.SRC_ATOP);
                d.draw(canvas);
                d.clearColorFilter();
            }
            d.setBounds(mOldBounds);
        }
        canvas.restore();
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        super.dispatchDraw(canvas);

        if (mFolder == null) return;
        if (mFolder.getItemCount() == 0 && !mAnimating) return;

        ArrayList<View> items = mFolder.getItemsInReadingOrder();
        Drawable d;
        TextView v;
        // Update our drawing parameters if necessary
        try {
			if (mAnimating) {
			    computePreviewDrawingParams(mAnimParams.drawable);
			} else {
			    v = (TextView) items.get(0);
			    d = getTopDrawable(v);
			    computePreviewDrawingParams(d);
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

        int nItemsInPreview = Math.min(items.size(), NUM_ITEMS_IN_PREVIEW);
        if (!mAnimating) {
            for (int i = nItemsInPreview - 1; i >= 0; i--) {
                v = (TextView) items.get(i);
                if (!mHiddenItems.contains(v.getTag())) {
                    ShortcutInfo info = (ShortcutInfo) v.getTag();
                    d = getTopDrawable(v);
                    mParams = computePreviewItemDrawingParams(i, mParams);
                    mParams.drawable = d;
                    if (LauncherAppState.getInstance().mShowCustomIconAni) {
                        try {
                            if (info != null && info.intent != null && info.intent.getComponent() != null
                                    && ("com.android.calendar").equals(info.intent.getComponent().getPackageName())) {
                                drawCalendarItem(canvas, mParams);
                            } /*else if (info != null && info.intent != null && info.intent.getComponent() != null
                                    && ("com.greenorange.weather").equals(info.intent.getComponent().getPackageName())) {
                                drawWeatherItem(canvas, mParams);
                            }*/else if (info != null && info.intent != null && info.intent.getComponent() != null
                                    && "com.android.deskclock".equals(info.intent.getComponent().getPackageName())) {
                                drawClockItem(canvas, mParams);
                            } else {
                                drawPreviewItem(canvas, mParams);
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "dispatchDraw: " + e.toString());
                        }
                    } else {
                        drawPreviewItem(canvas, mParams);
                    }
                }
            }
        } else {
            drawPreviewItem(canvas, mAnimParams);
        }
        /**M: Draw unread event number.@{**/
        MTKUnreadLoader.drawUnreadEventIfNeed(canvas, this);
        /**@}**/
    }


    /*画日历*/
    private void drawCalendarItem(Canvas canvas, PreviewItemDrawingParams params) {
        canvas.save();
        canvas.translate(params.transX + mPreviewOffsetX, params.transY + mPreviewOffsetY);
        canvas.scale(params.scale, params.scale);
        Drawable d = params.drawable;

        Log.e("drawPreviewItem", d.getIntrinsicHeight() + "  " + d.getIntrinsicWidth() + "  " + mIntrinsicIconSize + "  scale = " + params.scale);
        if (d != null) {
            mOldBounds.set(d.getBounds());
            d.setBounds(0, 0, mIntrinsicIconSize, mIntrinsicIconSize);
            if (d instanceof FastBitmapDrawable) {
                FastBitmapDrawable fd = (FastBitmapDrawable) d;
                int oldBrightness = fd.getBrightness();
                fd.setBrightness(params.overlayAlpha);
                d.draw(canvas);
                fd.setBrightness(oldBrightness);
            } else {
                d.setColorFilter(Color.argb(params.overlayAlpha, 255, 255, 255),
                        PorterDuff.Mode.SRC_ATOP);
                d.draw(canvas);
                d.clearColorFilter();
            }
            d.setBounds(mOldBounds);
        }
        canvas.restore();

        Resources res = getContext().getResources();
        Drawable unreadBgNinePatchDrawable = res.getDrawable(R.drawable.week_monday);
        Calendar c = Calendar.getInstance();
        String way = String.valueOf(c.get(Calendar.DAY_OF_WEEK));
        if ("1".equals(way)) {
            unreadBgNinePatchDrawable = res.getDrawable(R.drawable.week_sunday);
        } else if ("2".equals(way)) {
            unreadBgNinePatchDrawable = res.getDrawable(R.drawable.week_monday);
        } else if ("3".equals(way)) {
            unreadBgNinePatchDrawable = res.getDrawable(R.drawable.week_tuesday);
        } else if ("4".equals(way)) {
            unreadBgNinePatchDrawable = res.getDrawable(R.drawable.week_wednesday);
        } else if ("5".equals(way)) {
            unreadBgNinePatchDrawable = res.getDrawable(R.drawable.week_thursday);
        } else if ("6".equals(way)) {
            unreadBgNinePatchDrawable = res.getDrawable(R.drawable.week_friday);
        } else if ("7".equals(way)) {
            unreadBgNinePatchDrawable = res.getDrawable(R.drawable.week_saturday);
        }
        int unreadBgWidth = unreadBgNinePatchDrawable.getIntrinsicWidth();
        int unreadBgHeight = unreadBgNinePatchDrawable.getIntrinsicHeight();
        unreadBgNinePatchDrawable.setBounds(0, 0, unreadBgWidth, unreadBgHeight);

         /*这边画下面的日期*/
        SimpleDateFormat sDateFormat = new SimpleDateFormat("dd");
        String date = sDateFormat.format(new java.util.Date());
        int mdateleft = Integer.parseInt(String.valueOf(date.charAt(0)));
        int mdateright = Integer.parseInt(String.valueOf(date.charAt(1)));

        Drawable date_left = res.getDrawable(BubbleTextView.dateNumber[mdateleft]);
        int left_drawble_width = date_left.getIntrinsicWidth();
        int left_drawble_height = date_left.getIntrinsicHeight();
        Rect leftRect = new Rect(0, 0, left_drawble_width, left_drawble_height);
        date_left.setBounds(leftRect);

        Drawable date_right = res.getDrawable(BubbleTextView.dateNumber[mdateright]);
        int right_drawble_width = date_right.getIntrinsicWidth();
        int right_drawble_height = date_right.getIntrinsicHeight();
        Rect rightRect = new Rect(0, 0, right_drawble_width, right_drawble_height);
        date_right.setBounds(rightRect);

        float canvas_scale = (float)(params.scale / 1.5);
        canvas.save();
        canvas.translate(params.transX + mPreviewOffsetX + mIntrinsicIconSize / 2 * params.scale - left_drawble_width * canvas_scale,
                params.transY + mPreviewOffsetY + unreadBgHeight * canvas_scale + 11);
        canvas.scale(canvas_scale, canvas_scale);

        if (date_left != null) {
            date_left.draw(canvas);
        }
        canvas.restore();

        canvas.save();
        canvas.translate(params.transX + mPreviewOffsetX + mIntrinsicIconSize / 2 * params.scale,
                params.transY + mPreviewOffsetY + unreadBgHeight * canvas_scale + 11);
        canvas.scale(canvas_scale, canvas_scale);

        if (date_right != null) {
            date_right.draw(canvas);
        }
        canvas.restore();


        canvas.save();
        canvas.translate(params.transX + mPreviewOffsetX + mIntrinsicIconSize / 2 * params.scale - unreadBgWidth / 2 * canvas_scale,
                params.transY + mPreviewOffsetY + 10);
        canvas.scale(canvas_scale, canvas_scale);

        if (unreadBgNinePatchDrawable != null) {
            unreadBgNinePatchDrawable.draw(canvas);
        }
        canvas.restore();
    }

    private void drawClockItem(Canvas canvas, PreviewItemDrawingParams params){
        Resources res = getContext().getResources();
        canvas.save();
        canvas.translate(params.transX + mPreviewOffsetX, params.transY + mPreviewOffsetY);
        canvas.scale(params.scale, params.scale);
        Drawable d = res.getDrawable(R.drawable.clockicon);

        if (d != null) {
            mOldBounds.set(d.getBounds());
            d.setBounds(0, 0, mIntrinsicIconSize, mIntrinsicIconSize);
            if (d instanceof FastBitmapDrawable) {
                FastBitmapDrawable fd = (FastBitmapDrawable) d;
                int oldBrightness = fd.getBrightness();
                fd.setBrightness(params.overlayAlpha);
                d.draw(canvas);
                fd.setBrightness(oldBrightness);
            } else {
                d.setColorFilter(Color.argb(params.overlayAlpha, 255, 255, 255),
                        PorterDuff.Mode.SRC_ATOP);
                d.draw(canvas);
                d.clearColorFilter();
            }
            d.setBounds(mOldBounds);
        }
        canvas.restore();
    }


    private void drawWeatherItem(Canvas canvas, PreviewItemDrawingParams params) {
        Resources res = getContext().getResources();
        Boolean isnull = false;
        if (("").equals(shishicode) || "".equals(shishiwendu)) {
            getWeatherDatas(getContext());
            if ("".equals(shishicode) || "".equals(shishiwendu)) {
                isnull = true;
            }
        }

        canvas.save();
        canvas.translate(params.transX + mPreviewOffsetX, params.transY + mPreviewOffsetY);
        canvas.scale(params.scale, params.scale);
        Drawable d;
        if (isnull) {
            d = params.drawable;
        } else {
            d = res.getDrawable(BubbleTextView.judge(shishicode));
        }

        if (d != null) {
            mOldBounds.set(d.getBounds());
            d.setBounds(0, 0, mIntrinsicIconSize, mIntrinsicIconSize);
            if (d instanceof FastBitmapDrawable) {
                FastBitmapDrawable fd = (FastBitmapDrawable) d;
                int oldBrightness = fd.getBrightness();
                fd.setBrightness(params.overlayAlpha);
                d.draw(canvas);
                fd.setBrightness(oldBrightness);
            } else {
                d.setColorFilter(Color.argb(params.overlayAlpha, 255, 255, 255),
                        PorterDuff.Mode.SRC_ATOP);
                d.draw(canvas);
                d.clearColorFilter();
            }
            d.setBounds(mOldBounds);
        }
        canvas.restore();
        if (isnull) {
            return;
        }

        //画上面的温度
        int wendubefore = Integer.parseInt(shishiwendu);
        boolean isfushu = wendubefore > 0 ? false : true;
        int wendu = Math.abs(wendubefore);
        //wendu = 9;
        if (wendu > 0 && wendu < 10) {
            Drawable date_left = res.getDrawable(BubbleTextView.wenduNumber[wendu]);
            int left_drawble_width = date_left.getIntrinsicWidth();
            int left_drawble_height = date_left.getIntrinsicHeight();
            Rect leftRect = new Rect(0, 0, left_drawble_width, left_drawble_height);
            date_left.setBounds(leftRect);
            float date_left_x = params.transX + mPreviewOffsetX + mIntrinsicIconSize * params.scale / 2 - left_drawble_width * params.scale / 2;
            float date_left_y = params.transY + mPreviewOffsetY + 15 * params.scale;

            Drawable yuanquan = res.getDrawable(R.drawable.wendu_yuanquan);
            int yuanquan_width = yuanquan.getIntrinsicWidth();
            int yuanquan_height = yuanquan.getIntrinsicHeight();
            Rect yuanquanRect = new Rect(0, 0, yuanquan_width, yuanquan_height);
            yuanquan.setBounds(yuanquanRect);
            float yuanquan_x = params.transX + mPreviewOffsetX + mIntrinsicIconSize * params.scale / 2 + left_drawble_width * params.scale / 2;
            float yuanquan_y = params.transY + mPreviewOffsetY + 15 * params.scale;

            if (isfushu) {
                Drawable fushu = res.getDrawable(R.drawable.wendu_fushu);
                int fushu_width = fushu.getIntrinsicWidth();
                int fushu_height = fushu.getIntrinsicHeight();
                Rect fushuRect = new Rect(0, 0, fushu_width, fushu_height);
                fushu.setBounds(fushuRect);
                float fushu_x = params.transX + mPreviewOffsetX + mIntrinsicIconSize * params.scale / 2 - left_drawble_width * params.scale / 2 - fushu_width * params.scale;
                float fushu_y = params.transY + mPreviewOffsetY + 15 * params.scale;

                canvas.save();
                canvas.translate(fushu_x, fushu_y);
                canvas.scale(params.scale, params.scale);
                if (fushu != null) {
                    fushu.draw(canvas);
                }
                canvas.restore();
            }

            canvas.save();
            canvas.translate(date_left_x, date_left_y);
            canvas.scale(params.scale, params.scale);
            if (date_left != null) {
                date_left.draw(canvas);
            }
            canvas.restore();

            canvas.save();
            canvas.translate(yuanquan_x, yuanquan_y);
            canvas.scale(params.scale, params.scale);
            if (yuanquan != null) {
                yuanquan.draw(canvas);
            }
            canvas.restore();
        }else if (wendu >= 10) {
            int mdateleft = Integer.parseInt(String.valueOf(shishiwendu.charAt(0)));
            int mdateright = Integer.parseInt(String.valueOf(shishiwendu.charAt(1)));
            Drawable date_left = res.getDrawable(BubbleTextView.wenduNumber[mdateleft]);
            int left_drawble_width = date_left.getIntrinsicWidth();
            int left_drawble_height = date_left.getIntrinsicHeight();
            Rect leftRect = new Rect(0, 0, left_drawble_width, left_drawble_height);
            date_left.setBounds(leftRect);
            float date_left_x = params.transX + mPreviewOffsetX + mIntrinsicIconSize * params.scale / 2 - left_drawble_width * params.scale;
            float date_left_y = params.transY + mPreviewOffsetY + 15 * params.scale;

            Drawable date_right = res.getDrawable(BubbleTextView.wenduNumber[mdateright]);
            int right_drawble_width = date_right.getIntrinsicWidth();
            int right_drawble_height = date_right.getIntrinsicHeight();
            Rect rightRect = new Rect(0, 0, right_drawble_width, right_drawble_height);
            date_right.setBounds(rightRect);
            float date_right_x = params.transX + mPreviewOffsetX + mIntrinsicIconSize * params.scale / 2;
            float date_right_y = date_left_y;

            Drawable yuanquan = res.getDrawable(R.drawable.wendu_yuanquan);
            int yuanquan_width = yuanquan.getIntrinsicWidth();
            int yuanquan_height = yuanquan.getIntrinsicHeight();
            Rect yuanquanRect = new Rect(0, 0, yuanquan_width, yuanquan_height);
            yuanquan.setBounds(yuanquanRect);
            float yuanquan_x = params.transX + mPreviewOffsetX + mIntrinsicIconSize * params.scale / 2 + left_drawble_width * params.scale ;
            float yuanquan_y = date_left_y;

            if (isfushu) {
                Drawable fushu = res.getDrawable(R.drawable.wendu_fushu);
                int fushu_width = fushu.getIntrinsicWidth();
                int fushu_height = fushu.getIntrinsicHeight();
                Rect fushuRect = new Rect(0, 0, fushu_width, fushu_height);
                fushu.setBounds(fushuRect);
                float fushu_x = params.transX + mPreviewOffsetX + mIntrinsicIconSize * params.scale / 2 - left_drawble_width * params.scale  - fushu_width * params.scale;
                float fushu_y = date_left_y;

                canvas.save();
                canvas.translate(fushu_x, fushu_y);
                canvas.scale(params.scale, params.scale);
                if (fushu != null) {
                    fushu.draw(canvas);
                }
                canvas.restore();
            }

            canvas.save();
            canvas.translate(date_left_x, date_left_y);
            canvas.scale(params.scale, params.scale);
            if (date_left != null) {
                date_left.draw(canvas);
            }
            canvas.restore();

            canvas.save();
            canvas.translate(date_right_x, date_right_y);
            canvas.scale(params.scale, params.scale);
            if (date_right != null) {
                date_right.draw(canvas);
            }
            canvas.restore();

            canvas.save();
            canvas.translate(yuanquan_x, yuanquan_y);
            canvas.scale(params.scale, params.scale);
            if (yuanquan != null) {
                yuanquan.draw(canvas);
            }
            canvas.restore();
        }
    }

    /*获取天气缓存的数据*/
    public void getWeatherDatas(Context context) {
        Uri lsappdatas = Uri.parse(BubbleTextView.Url);
        Cursor c = context.getContentResolver().query(lsappdatas, null, null, null, null);
        try {
            if (c == null || !c.moveToFirst()) {
                if (c != null) {
                    c.close();
                }
            } else {
                shishiwendu = c.getString(c.getColumnIndex(BubbleTextView.SHISHIWENDU));
                shishicode = c.getString(c.getColumnIndex(BubbleTextView.SHISHICODE));
                if(TextUtils.isEmpty(shishicode)){
                    shishicode = "";
                }
                if(TextUtils.isEmpty(shishiwendu)){
                    shishiwendu = "";
                }
                Log.e(TAG, "getWeatherDatas: folder icon shishicode = " + shishicode + " shishiwendu = " + shishiwendu);
                c.close();
            }
        } catch (Exception e) {
            if (c != null) {
                c.close();
            }
            e.printStackTrace();
        }
    }

    private Drawable getTopDrawable(TextView v) {
        Drawable d = v.getCompoundDrawables()[1];
        return (d instanceof PreloadIconDrawable) ? ((PreloadIconDrawable) d).mIcon : d;
    }

    private void animateFirstItem(final Drawable d, int duration, final boolean reverse,
            final Runnable onCompleteRunnable) {
        final PreviewItemDrawingParams finalParams = computePreviewItemDrawingParams(0, null);

        final float scale0 = 1.0f;
        final float transX0 = (mAvailableSpaceInPreview - d.getIntrinsicWidth()) / 2;
        final float transY0 = (mAvailableSpaceInPreview - d.getIntrinsicHeight()) / 2 + getPaddingTop();
        mAnimParams.drawable = d;

        ValueAnimator va = LauncherAnimUtils.ofFloat(this, 0f, 1.0f);
        va.addUpdateListener(new AnimatorUpdateListener(){
            public void onAnimationUpdate(ValueAnimator animation) {
                float progress = (Float) animation.getAnimatedValue();
                if (reverse) {
                    progress = 1 - progress;
                    mPreviewBackground.setAlpha(progress);
                }

                mAnimParams.transX = transX0 + progress * (finalParams.transX - transX0);
                mAnimParams.transY = transY0 + progress * (finalParams.transY - transY0);
                mAnimParams.scale = scale0 + progress * (finalParams.scale - scale0);
                invalidate();
            }
        });
        va.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animation) {
                mAnimating = true;
            }
            @Override
            public void onAnimationEnd(Animator animation) {
                mAnimating = false;
                if (onCompleteRunnable != null) {
                    onCompleteRunnable.run();
                }
            }
        });
        va.setDuration(duration);
        va.start();
    }

    public void setTextVisible(boolean visible) {
        if (visible) {
            mFolderName.setVisibility(VISIBLE);
        } else {
            mFolderName.setVisibility(INVISIBLE);
        }
    }

    public boolean getTextVisible() {
        return mFolderName.getVisibility() == VISIBLE;
    }

    public void onItemsChanged() {
        invalidate();
        requestLayout();
    }

    public void onAdd(ShortcutInfo item) {
        if (LauncherLog.DEBUG) {
            LauncherLog.d(TAG, "onAdd item = " + item);
        }
        
        /**
         * M: added for unread feature, when add a item to a folder, we need to update
         * the unread num of the folder.@{
         */
        final ComponentName componentName = item.intent.getComponent();
        updateFolderUnreadNum(componentName, item.unreadNum);
        /**@}**/

        invalidate();
        requestLayout();
    }

    public void onRemove(ShortcutInfo item) {
        if (LauncherLog.DEBUG) {
            LauncherLog.d(TAG, "onRemove item = " + item);
        }
        
        /**M: added for Unread feature, when remove a item from a folder, we need to update
         *  the unread num of the folder.@{
         */
        final ComponentName componentName = item.intent.getComponent();
        updateFolderUnreadNum(componentName, item.unreadNum);
        /**@}**/

        invalidate();
        requestLayout();
    }

    public void onTitleChanged(CharSequence title) {
        if (mInfo!=null) {
        	mInfo.title = title.toString();
		}
    	if (mFolderName!=null) {
    		mFolderName.setText(title.toString());
    		setContentDescription(String.format(getContext().getString(R.string.folder_name_format),
                    title));
		}
    }

    int mOnClickY;
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        // Call the superclass onTouchEvent first, because sometimes it changes the state to
        // isPressed() on an ACTION_UP
    	
        boolean result = super.onTouchEvent(event);

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
            	mOnClickY = getXy(0, this)[1];
                mLongPressHelper.postCheckForLongPress();
                break;
            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP:
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

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        mSlop = ViewConfiguration.get(getContext()).getScaledTouchSlop();
    }

    @Override
    public void cancelLongPress() {
        super.cancelLongPress();

        mLongPressHelper.cancelLongPress();
    }
    public boolean isRingState() {
        if(LauncherLog.DEBUG_DRAG) {
            LauncherLog.d(TAG, "isRingState: mFolderRingAnimatorState = " + mFolderRingAnimatorState);
        }
        return mFolderRingAnimatorState == FOLDER_RING_ANIMATOR_STATE_RINGING;
    }
    
    public void resetRingState() {
        if(LauncherLog.DEBUG_DRAG) {
            LauncherLog.d(TAG, "resetRingState: FOLDER_RING_ANIMATOR_STATE_NORMAL");
        }
        mFolderRingAnimatorState = FOLDER_RING_ANIMATOR_STATE_NORMAL;
    }

    /**M: Added for unread message feature.@{**/
    
   /**
    * M: Update the unread message number of the shortcut with the given value.
    *
    * @param unreadNum the number of the unread message.
    */
   public void setFolderUnreadNum(int unreadNum) {
       if (LauncherLog.DEBUG_UNREAD) {
           LauncherLog.d(TAG, "setFolderUnreadNum: unreadNum = " + unreadNum + ", mInfo = " + mInfo
                   + ", this = " + this);
       }

       if (unreadNum <= 0) {
           mInfo.unreadNum = 0;
       } else {
           mInfo.unreadNum = unreadNum;
       }
   }

   /**
    * M: Update unread number of the folder, the number is the total unread number
    * of all shortcuts in folder, duplicate shortcut will be only count once.
    */
   public void updateFolderUnreadNum() {
       final ArrayList<ShortcutInfo> contents = mInfo.contents;
       final int contentsCount = contents.size();
       int unreadNumTotal = 0;
       final ArrayList<ComponentName> components = new ArrayList<ComponentName>();
       ShortcutInfo shortcutInfo = null;
       ComponentName componentName = null;
       int unreadNum = 0;
       for (int i = 0; i < contentsCount; i++) {
           shortcutInfo = contents.get(i);
           componentName = shortcutInfo.intent.getComponent();
           unreadNum = MTKUnreadLoader.getUnreadNumberOfComponent(componentName);
           if (unreadNum > 0) {
               shortcutInfo.unreadNum = unreadNum;
               int j = 0;
               for (j = 0; j < components.size(); j++) {
                   if (componentName != null && componentName.equals(components.get(j))) {
                       break;
                   }
               }
               if (LauncherLog.DEBUG_UNREAD) {
                   LauncherLog.d(TAG, "updateFolderUnreadNum: unreadNumTotal = " + unreadNumTotal
                           + ", j = " + j + ", components.size() = " + components.size());
               }
               if (j >= components.size()) {
                   components.add(componentName);
                   unreadNumTotal += unreadNum;
               }
           }
       }
       if (LauncherLog.DEBUG_UNREAD) {
           LauncherLog.d(TAG, "updateFolderUnreadNum 1 end: unreadNumTotal = " + unreadNumTotal);
       }
       setFolderUnreadNum(unreadNumTotal);
   }

   /**
    * M: Update the unread message of the shortcut with the given information.
    *
    * @param unreadNum the number of the unread message.
    */
   public void updateFolderUnreadNum(ComponentName component, int unreadNum) {
       final ArrayList<ShortcutInfo> contents = mInfo.contents;
       final int contentsCount = contents.size();
       int unreadNumTotal = 0;
       ShortcutInfo appInfo = null;
       ComponentName name = null;
       final ArrayList<ComponentName> components = new ArrayList<ComponentName>();
       for (int i = 0; i < contentsCount; i++) {
           appInfo = contents.get(i);
           name = appInfo.intent.getComponent();
           if (name != null && name.equals(component)) {
               appInfo.unreadNum = unreadNum;
           }
           if (appInfo.unreadNum > 0) {
               int j = 0;
               for (j = 0; j < components.size(); j++) {
                   if (name != null && name.equals(components.get(j))) {
                       break;
                   }
               }
               if (LauncherLog.DEBUG_UNREAD) {
                   LauncherLog.d(TAG, "updateFolderUnreadNum: unreadNumTotal = " + unreadNumTotal
                           + ", j = " + j + ", components.size() = " + components.size());
               }
               if (j >= components.size()) {
                   components.add(name);
                   unreadNumTotal += appInfo.unreadNum;
               }
           }
       }
       if (LauncherLog.DEBUG_UNREAD) {
           LauncherLog.d(TAG, "updateFolderUnreadNum 2 end: unreadNumTotal = " + unreadNumTotal);
       }
       setFolderUnreadNum(unreadNumTotal);
   }
   /**@**/
   
   
	static Drawable mBackground;

	@Override
	public void invalidate() {
		// TODO Auto-generated method stub
		super.invalidate();
		if (mBackground != null && mBackground != null) {
			mPreviewBackground.setBackground(mBackground);
			mPreviewBackground.invalidate();
		}

	}

   
}
