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

import android.appwidget.AppWidgetHostView;
import android.content.ComponentName;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Paint;
import android.graphics.Paint.FontMetrics;
import android.graphics.Point;
import android.graphics.PointF;
import android.graphics.Rect;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.Gravity;
import android.view.Surface;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;

import com.qingcheng.home.R;
import com.qingcheng.home.config.QCConfig;
import com.qingcheng.home.util.QCLog;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;


class DeviceProfileQuery {
    DeviceProfile profile;
    float widthDps;
    float heightDps;
    float value;
    PointF dimens;

    DeviceProfileQuery(DeviceProfile p, float v) {
        widthDps = p.minWidthDps;
        heightDps = p.minHeightDps;
        value = v;
        dimens = new PointF(widthDps, heightDps);
        profile = p;
    }
}

public class DeviceProfile {
    public static interface DeviceProfileCallbacks {
        public void onAvailableSizeChanged(DeviceProfile grid);
    }
    
    LauncherApplication mApplication;
    
    String name;
    float minWidthDps;
    float minHeightDps;
    float numRows;
    float numColumns;
    float numHotseatIcons;
    float iconSize;
    private float iconTextSize;
    private int iconDrawablePaddingOriginalPx;
    private float hotseatIconSize;

    int defaultLayoutId;
    int defaultNoAllAppsLayoutId;

    public boolean isLandscape;
    boolean isTablet;
    boolean isLargeTablet;
    boolean isLayoutRtl;
    boolean transposeLayoutWithOrientation;

    int desiredWorkspaceLeftRightMarginPx;
    int edgeMarginPx;
    Rect defaultWidgetPadding;

    int widthPx;
    int heightPx;
    int availableWidthPx;
    int availableHeightPx;
    int defaultPageSpacingPx;

    int overviewModeMinIconZoneHeightPx;
    int overviewModeMaxIconZoneHeightPx;
    int overviewModeBarItemWidthPx;
    int overviewModeBarSpacerWidthPx;
    float overviewModeIconZoneRatio;
    float overviewModeScaleFactor;

    int iconSizePx;
    int iconTextSizePx;
    int iconDrawablePaddingPx;
    int cellWidthPx;
    int cellHeightPx;
    int allAppsIconSizePx;
    int allAppsIconTextSizePx;
    int allAppsCellWidthPx;
    int allAppsCellHeightPx;
    int allAppsCellPaddingPx;
    int folderBackgroundOffset;
    int folderIconSizePx;
    int folderCellWidthPx;
    int folderCellHeightPx;
    int hotseatCellWidthPx;
    int hotseatCellHeightPx;
    int hotseatIconSizePx;
    int hotseatBarHeightPx;
    int hotseatAllAppsRank;
    int allAppsNumRows;
    int allAppsNumCols;
    int searchBarSpaceWidthPx;
    int searchBarSpaceMaxWidthPx;
    int searchBarSpaceHeightPx;
    int searchBarHeightPx;
    int pageIndicatorHeightPx;
    int allAppsButtonVisualSize;

    float dragViewScale;

    int allAppsShortEdgeCount = -1;
    int allAppsLongEdgeCount = -1;

    private ArrayList<DeviceProfileCallbacks> mCallbacks = new ArrayList<DeviceProfileCallbacks>();

    DeviceProfile(String n, float w, float h, float r, float c,
                  float is, float its, float hs, float his, int dlId, int dnalId) {
        // Ensure that we have an odd number of hotseat items (since we need to place all apps)
        if (!LauncherAppState.isDisableAllApps() && hs % 2 == 0) {
            throw new RuntimeException("All Device Profiles must have an odd number of hotseat spaces");
        }

        name = n;
        minWidthDps = w;
        minHeightDps = h;
        numRows = r;
        numColumns = c;
        iconSize = is;
        iconTextSize = its;
        numHotseatIcons = hs;
        hotseatIconSize = his;
        defaultLayoutId = dlId;
        defaultNoAllAppsLayoutId = dnalId;
    }

    DeviceProfile() {
    }

    DeviceProfile(Context context,
                  ArrayList<DeviceProfile> profiles,
                  float minWidth, float minHeight,
                  int wPx, int hPx,
                  int awPx, int ahPx,
                  Resources res) {
    	
        mApplication = (LauncherApplication)context.getApplicationContext();
        
        DisplayMetrics dm = res.getDisplayMetrics();
        ArrayList<DeviceProfileQuery> points =
                new ArrayList<DeviceProfileQuery>();
        transposeLayoutWithOrientation =
                res.getBoolean(R.bool.hotseat_transpose_layout_with_orientation);
        minWidthDps = minWidth;
        minHeightDps = minHeight;

        ComponentName cn = new ComponentName(context.getPackageName(),
                this.getClass().getName());
        defaultWidgetPadding = AppWidgetHostView.getDefaultPaddingForWidget(context, cn, null);
        edgeMarginPx = res.getDimensionPixelSize(R.dimen.dynamic_grid_edge_margin);
        desiredWorkspaceLeftRightMarginPx = 2 * edgeMarginPx;
        pageIndicatorHeightPx = mApplication.getPxforYLayout(true, R.dimen.dynamic_grid_page_indicator_height, 0);
        defaultPageSpacingPx =
                res.getDimensionPixelSize(R.dimen.dynamic_grid_workspace_page_spacing);
        allAppsCellPaddingPx =
                res.getDimensionPixelSize(R.dimen.dynamic_grid_all_apps_cell_padding);
        overviewModeMinIconZoneHeightPx =
                res.getDimensionPixelSize(R.dimen.dynamic_grid_overview_min_icon_zone_height);
        overviewModeMaxIconZoneHeightPx =
                res.getDimensionPixelSize(R.dimen.dynamic_grid_overview_max_icon_zone_height);
        overviewModeBarItemWidthPx =
                res.getDimensionPixelSize(R.dimen.dynamic_grid_overview_bar_item_width);
        overviewModeBarSpacerWidthPx =
                res.getDimensionPixelSize(R.dimen.dynamic_grid_overview_bar_spacer_width);
        overviewModeIconZoneRatio =
                res.getInteger(R.integer.config_dynamic_grid_overview_icon_zone_percentage) / 100f;
        overviewModeScaleFactor =
                res.getInteger(R.integer.config_dynamic_grid_overview_scale_percentage) / 100f;

        // Find the closes profile given the width/height
        for (DeviceProfile p : profiles) {
            points.add(new DeviceProfileQuery(p, 0f));
        }
        DeviceProfile closestProfile = findClosestDeviceProfile(minWidth, minHeight, points);
        if (QCLog.DEBUG) {
			QCLog.d("DeviceProfile", "closestProfile's name = "+closestProfile.name+", closestProfile = "+closestProfile);
		}

        // Snap to the closest row count
        numRows = closestProfile.numRows;
        // Snap to the closest column count
        numColumns = closestProfile.numColumns;
        // Snap to the closest hotseat size
        numHotseatIcons = closestProfile.numHotseatIcons;

        // Add for MyUI Jing.Wu 20161210 start
        if (LauncherApplication.getHasDeviceLayoutFile()) {
        	int rowsId = context.getResources().getIdentifier(LauncherApplication.getDeviceName()+"_rows", "integer", context.getPackageName());
        	int columnsId = context.getResources().getIdentifier(LauncherApplication.getDeviceName()+"_columns", "integer", context.getPackageName());
        	int hotNumbId = context.getResources().getIdentifier(LauncherApplication.getDeviceName()+"_hotseat_numb", "integer", context.getPackageName());
        	if (rowsId!=0 && columnsId!=0 && hotNumbId!=0) {
        		numRows = context.getResources().getInteger(rowsId);
        		numColumns = context.getResources().getInteger(columnsId);
        		numHotseatIcons = context.getResources().getInteger(hotNumbId);
        		LauncherApplication.setHasDeviceLayoutParameter(true);
			}
		}
        // Add for MyUI Jing.Wu 20161210 end
        
        hotseatAllAppsRank = (int) (numHotseatIcons / 2);

        // Snap to the closest default layout id
        defaultLayoutId = closestProfile.defaultLayoutId;

        // Snap to the closest default no all-apps layout id
        defaultNoAllAppsLayoutId = closestProfile.defaultNoAllAppsLayoutId;

        // Interpolate the icon size
        points.clear();
        for (DeviceProfile p : profiles) {
            points.add(new DeviceProfileQuery(p, p.iconSize));
        }
        iconSize = invDistWeightedInterpolate(minWidth, minHeight, points);

        // AllApps uses the original non-scaled icon size
        allAppsIconSizePx = DynamicGrid.pxFromDp(iconSize, dm);

        // Interpolate the icon text size
        points.clear();
        for (DeviceProfile p : profiles) {
            points.add(new DeviceProfileQuery(p, p.iconTextSize));
        }
        iconTextSize = invDistWeightedInterpolate(minWidth, minHeight, points);
        iconDrawablePaddingOriginalPx =
                res.getDimensionPixelSize(R.dimen.dynamic_grid_icon_drawable_padding);
        // AllApps uses the original non-scaled icon text size
        allAppsIconTextSizePx = DynamicGrid.pxFromDp(iconTextSize, dm);

        // Interpolate the hotseat icon size
        points.clear();
        for (DeviceProfile p : profiles) {
            points.add(new DeviceProfileQuery(p, p.hotseatIconSize));
        }
        // Hotseat
        hotseatIconSize = invDistWeightedInterpolate(minWidth, minHeight, points);

        // If the partner customization apk contains any grid overrides, apply them
        applyPartnerDeviceProfileOverrides(context, dm);

        // Calculate the remaining vars
        updateFromConfiguration(context, res, wPx, hPx, awPx, ahPx);
        updateAvailableDimensions(context);
        computeAllAppsButtonSize(context);
    }

    /**
     * Apply any Partner customization grid overrides.
     *
     * Currently we support: all apps row / column count.
     */
    private void applyPartnerDeviceProfileOverrides(Context ctx, DisplayMetrics dm) {
        Partner p = Partner.get(ctx.getPackageManager());
        if (p != null) {
            DeviceProfile partnerDp = p.getDeviceProfileOverride(dm);
            if (partnerDp != null) {
                if (partnerDp.numRows > 0 && partnerDp.numColumns > 0) {
                    numRows = partnerDp.numRows;
                    numColumns = partnerDp.numColumns;
                }
                if (partnerDp.allAppsShortEdgeCount > 0 && partnerDp.allAppsLongEdgeCount > 0) {
                    allAppsShortEdgeCount = partnerDp.allAppsShortEdgeCount;
                    allAppsLongEdgeCount = partnerDp.allAppsLongEdgeCount;
                }
                if (partnerDp.iconSize > 0) {
                    iconSize = partnerDp.iconSize;
                    // AllApps uses the original non-scaled icon size
                    allAppsIconSizePx = DynamicGrid.pxFromDp(iconSize, dm);
                }
            }
        }
    }

    /**
     * Determine the exact visual footprint of the all apps button, taking into account scaling
     * and internal padding of the drawable.
     */
    private void computeAllAppsButtonSize(Context context) {
        Resources res = context.getResources();
        float padding = res.getInteger(R.integer.config_allAppsButtonPaddingPercent) / 100f;
        LauncherAppState app = LauncherAppState.getInstance();
        allAppsButtonVisualSize = (int) (hotseatIconSizePx * (1 - padding));
    }

    void addCallback(DeviceProfileCallbacks cb) {
        mCallbacks.add(cb);
        cb.onAvailableSizeChanged(this);
    }
    void removeCallback(DeviceProfileCallbacks cb) {
        mCallbacks.remove(cb);
    }

    private int getDeviceOrientation(Context context) {
        WindowManager windowManager =  (WindowManager)
                context.getSystemService(Context.WINDOW_SERVICE);
        Resources resources = context.getResources();
        DisplayMetrics dm = resources.getDisplayMetrics();
        Configuration config = resources.getConfiguration();
        int rotation = windowManager.getDefaultDisplay().getRotation();

        boolean isLandscape = (config.orientation == Configuration.ORIENTATION_LANDSCAPE) &&
                (rotation == Surface.ROTATION_0 || rotation == Surface.ROTATION_180);
        boolean isRotatedPortrait = (config.orientation == Configuration.ORIENTATION_PORTRAIT) &&
                (rotation == Surface.ROTATION_90 || rotation == Surface.ROTATION_270);
        if (isLandscape || isRotatedPortrait) {
            return CellLayout.LANDSCAPE;
        } else {
            return CellLayout.PORTRAIT;
        }
    }

    private void updateAvailableDimensions(Context context) {
        WindowManager windowManager =  (WindowManager)
                context.getSystemService(Context.WINDOW_SERVICE);
        Display display = windowManager.getDefaultDisplay();
        Resources resources = context.getResources();
        DisplayMetrics dm = resources.getDisplayMetrics();
        Configuration config = resources.getConfiguration();

        // There are three possible configurations that the dynamic grid accounts for, portrait,
        // landscape with the nav bar at the bottom, and landscape with the nav bar at the side.
        // To prevent waiting for fitSystemWindows(), we make the observation that in landscape,
        // the height is the smallest height (either with the nav bar at the bottom or to the
        // side) and otherwise, the height is simply the largest possible height for a portrait
        // device.
        Point size = new Point();
        Point smallestSize = new Point();
        Point largestSize = new Point();
        display.getSize(size);
        display.getCurrentSizeRange(smallestSize, largestSize);
        availableWidthPx = size.x;
        if (config.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            availableHeightPx = smallestSize.y;
        } else {
            availableHeightPx = largestSize.y;
        }

        // Check to see if the icons fit in the new available height.  If not, then we need to
        // shrink the icon size.
        float scale = 1f;
        int drawablePadding = iconDrawablePaddingOriginalPx;
        updateIconSize(1f, drawablePadding, resources, dm);
        float usedHeight = (cellHeightPx * numRows);

        Rect workspacePadding = getWorkspacePadding();
        int maxHeight = (availableHeightPx - workspacePadding.top - workspacePadding.bottom);
        if (usedHeight > maxHeight) {
            scale = maxHeight / usedHeight;
            drawablePadding = 0;
        }
        updateIconSize(scale, drawablePadding, resources, dm);

        // Make the callbacks
        for (DeviceProfileCallbacks cb : mCallbacks) {
            cb.onAvailableSizeChanged(this);
        }
    }
    
    void updateIconTextSizePx(Context context) {
    	iconTextSizePx = (int) DynamicGrid.pxFromSp(iconTextSize, context.getResources().getDisplayMetrics());
    }

    private void updateIconSize(float scale, int drawablePadding, Resources resources,
                                DisplayMetrics dm) {
        iconSizePx = (int) (DynamicGrid.pxFromDp(iconSize, dm) * scale);
        iconTextSizePx = (int) (DynamicGrid.pxFromSp(iconTextSize, dm) * scale);
        iconDrawablePaddingPx = drawablePadding;
        hotseatIconSizePx = (int) (DynamicGrid.pxFromDp(hotseatIconSize, dm) * scale);

        // Search Bar
        searchBarSpaceMaxWidthPx = resources.getDimensionPixelSize(R.dimen.dynamic_grid_search_bar_max_width);
        searchBarHeightPx = mApplication.getPxforYLayout(true, R.dimen.dynamic_grid_search_bar_height, 0);
        searchBarSpaceWidthPx = Math.min(searchBarSpaceMaxWidthPx, widthPx);
        searchBarSpaceHeightPx = searchBarHeightPx + getSearchBarTopOffset();

        // Calculate the actual text height
        Paint textPaint = new Paint();
        textPaint.setTextSize(iconTextSizePx);
        FontMetrics fm = textPaint.getFontMetrics();
        cellWidthPx = iconSizePx;
        cellHeightPx = iconSizePx + iconDrawablePaddingPx + (int) Math.ceil(fm.bottom - fm.top);
        final float scaleDps = resources.getDimensionPixelSize(R.dimen.dragViewScale);
        dragViewScale = (iconSizePx + scaleDps) / iconSizePx;

        // Hotseat
        if (QCConfig.supportHotseatText) {
			hotseatBarHeightPx = cellHeightPx+edgeMarginPx;
			hotseatCellWidthPx = cellWidthPx;
			hotseatCellHeightPx = cellHeightPx;
		} else {
			hotseatBarHeightPx = iconSizePx;
			hotseatCellWidthPx = iconSizePx;
			hotseatCellHeightPx = iconSizePx;
		}

        // Folder
        folderCellWidthPx = cellWidthPx + 3 * edgeMarginPx;
        folderCellHeightPx = cellHeightPx + edgeMarginPx;
        folderBackgroundOffset = -edgeMarginPx;
        // Modify for MyUI Jing.Wu 20160127 start
        folderIconSizePx = iconSizePx;    // + 2 * -folderBackgroundOffset;
        // Modify for MyUI Jing.Wu 20160127 end

        // All Apps
        Rect padding = getWorkspacePadding(isLandscape ?
                CellLayout.LANDSCAPE : CellLayout.PORTRAIT);
        int pageIndicatorOffset =
                resources.getDimensionPixelSize(R.dimen.apps_customize_page_indicator_offset);
        allAppsCellWidthPx = allAppsIconSizePx;
        allAppsCellHeightPx = allAppsIconSizePx + drawablePadding + iconTextSizePx;
        int maxLongEdgeCellCount =
                resources.getInteger(R.integer.config_dynamic_grid_max_long_edge_cell_count);
        int maxShortEdgeCellCount =
                resources.getInteger(R.integer.config_dynamic_grid_max_short_edge_cell_count);
        int minEdgeCellCount =
                resources.getInteger(R.integer.config_dynamic_grid_min_edge_cell_count);
        int maxRows = (isLandscape ? maxShortEdgeCellCount : maxLongEdgeCellCount);
        int maxCols = (isLandscape ? maxLongEdgeCellCount : maxShortEdgeCellCount);

        if (allAppsShortEdgeCount > 0 && allAppsLongEdgeCount > 0) {
            allAppsNumRows = isLandscape ? allAppsShortEdgeCount : allAppsLongEdgeCount;
            allAppsNumCols = isLandscape ? allAppsLongEdgeCount : allAppsShortEdgeCount;
        } else {
            allAppsNumRows = (availableHeightPx - pageIndicatorHeightPx) /
                    (allAppsCellHeightPx + allAppsCellPaddingPx);
            allAppsNumRows = Math.max(minEdgeCellCount, Math.min(maxRows, allAppsNumRows));
            allAppsNumCols = (availableWidthPx) /
                    (allAppsCellWidthPx + allAppsCellPaddingPx);
            allAppsNumCols = Math.max(minEdgeCellCount, Math.min(maxCols, allAppsNumCols));
        }
    }

    void updateFromConfiguration(Context context, Resources resources, int wPx, int hPx,
                                 int awPx, int ahPx) {
        Configuration configuration = resources.getConfiguration();
        isLandscape = (configuration.orientation == Configuration.ORIENTATION_LANDSCAPE);
        //Change for MyUI---20150707
        //isTablet = resources.getBoolean(R.bool.is_tablet);
        isTablet = !mApplication.getIsPhone();
        isLargeTablet = resources.getBoolean(R.bool.is_large_tablet);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN_MR1) {
            isLayoutRtl = (configuration.getLayoutDirection() == View.LAYOUT_DIRECTION_RTL);
        } else {
            isLayoutRtl = false;
        }
        widthPx = wPx;
        heightPx = hPx;
        availableWidthPx = awPx;
        availableHeightPx = ahPx;

        updateAvailableDimensions(context);
    }

    private float dist(PointF p0, PointF p1) {
        return (float) Math.sqrt((p1.x - p0.x)*(p1.x-p0.x) +
                (p1.y-p0.y)*(p1.y-p0.y));
    }

    private float weight(PointF a, PointF b,
                        float pow) {
        float d = dist(a, b);
        if (d == 0f) {
            return Float.POSITIVE_INFINITY;
        }
        return (float) (1f / Math.pow(d, pow));
    }

    /** Returns the closest device profile given the width and height and a list of profiles */
    private DeviceProfile findClosestDeviceProfile(float width, float height,
                                                   ArrayList<DeviceProfileQuery> points) {
        return findClosestDeviceProfiles(width, height, points).get(0).profile;
    }

    /** Returns the closest device profiles ordered by closeness to the specified width and height */
    private ArrayList<DeviceProfileQuery> findClosestDeviceProfiles(float width, float height,
                                                   ArrayList<DeviceProfileQuery> points) {
        final PointF xy = new PointF(width, height);

        // Sort the profiles by their closeness to the dimensions
        ArrayList<DeviceProfileQuery> pointsByNearness = points;
        Collections.sort(pointsByNearness, new Comparator<DeviceProfileQuery>() {
            public int compare(DeviceProfileQuery a, DeviceProfileQuery b) {
                return (int) (dist(xy, a.dimens) - dist(xy, b.dimens));
            }
        });

        return pointsByNearness;
    }

    private float invDistWeightedInterpolate(float width, float height,
                ArrayList<DeviceProfileQuery> points) {
        float sum = 0;
        float weights = 0;
        float pow = 5;
        float kNearestNeighbors = 3;
        final PointF xy = new PointF(width, height);

        ArrayList<DeviceProfileQuery> pointsByNearness = findClosestDeviceProfiles(width, height,
                points);

        for (int i = 0; i < pointsByNearness.size(); ++i) {
            DeviceProfileQuery p = pointsByNearness.get(i);
            if (i < kNearestNeighbors) {
                float w = weight(xy, p.dimens, pow);
                if (w == Float.POSITIVE_INFINITY) {
                    return p.value;
                }
                weights += w;
            }
        }

        for (int i = 0; i < pointsByNearness.size(); ++i) {
            DeviceProfileQuery p = pointsByNearness.get(i);
            if (i < kNearestNeighbors) {
                float w = weight(xy, p.dimens, pow);
                sum += w * p.value / weights;
            }
        }

        return sum;
    }

    /** Returns the search bar top offset */
    int getSearchBarTopOffset() {
        if (isTablet() && !isVerticalBarLayout()) {
            return 4 * edgeMarginPx;
        } else {
            return 0;
        }
    }

    /** Returns the search bar bounds in the current orientation */
    Rect getSearchBarBounds() {
        return getSearchBarBounds(isLandscape ? CellLayout.LANDSCAPE : CellLayout.PORTRAIT);
    }
    /** Returns the search bar bounds in the specified orientation */
    Rect getSearchBarBounds(int orientation) {
        Rect bounds = new Rect();
        if (orientation == CellLayout.LANDSCAPE &&
                transposeLayoutWithOrientation) {
            if (isLayoutRtl) {
                bounds.set(availableWidthPx - searchBarSpaceHeightPx, edgeMarginPx,
                        availableWidthPx, availableHeightPx - edgeMarginPx);
            } else {
                bounds.set(0, edgeMarginPx, searchBarSpaceHeightPx,
                        availableHeightPx - edgeMarginPx);
            }
        } else {
            if (isTablet()) {
                // Pad the left and right of the workspace to ensure consistent spacing
                // between all icons
                int width = (orientation == CellLayout.LANDSCAPE)
                        ? Math.max(widthPx, heightPx)
                        : Math.min(widthPx, heightPx);
                // XXX: If the icon size changes across orientations, we will have to take
                //      that into account here too.
                int gap = (int) ((width - 2 * edgeMarginPx -
                        (numColumns * cellWidthPx)) / (2 * (numColumns + 1)));
                bounds.set(edgeMarginPx + gap, getSearchBarTopOffset(),
                        availableWidthPx - (edgeMarginPx + gap),
                        searchBarSpaceHeightPx);
            } else {
                bounds.set(desiredWorkspaceLeftRightMarginPx - defaultWidgetPadding.left,
                        getSearchBarTopOffset(),
                        availableWidthPx - (desiredWorkspaceLeftRightMarginPx -
                        defaultWidgetPadding.right), searchBarSpaceHeightPx);
            }
        }
        return bounds;
    }

    /** Returns the bounds of the workspace page indicators. */
    Rect getWorkspacePageIndicatorBounds(Rect insets) {
        Rect workspacePadding = getWorkspacePadding();
        if (isLandscape && transposeLayoutWithOrientation) {
            if (isLayoutRtl) {
                return new Rect(workspacePadding.left, workspacePadding.top,
                        workspacePadding.left + pageIndicatorHeightPx,
                        heightPx - workspacePadding.bottom - insets.bottom);
            } else {
                int pageIndicatorLeft = widthPx - workspacePadding.right;
                return new Rect(pageIndicatorLeft, workspacePadding.top,
                        pageIndicatorLeft + pageIndicatorHeightPx,
                        heightPx - workspacePadding.bottom - insets.bottom);
            }
        } else {
            int pageIndicatorTop = heightPx - insets.bottom - workspacePadding.bottom;
            return new Rect(workspacePadding.left, pageIndicatorTop,
                    widthPx - workspacePadding.right, pageIndicatorTop + pageIndicatorHeightPx);
        }
    }

    /** Returns the workspace padding in the specified orientation */
    Rect getWorkspacePadding() {
        return getWorkspacePadding(isLandscape ? CellLayout.LANDSCAPE : CellLayout.PORTRAIT);
    }
    Rect getWorkspacePadding(int orientation) {
        Rect searchBarBounds = getSearchBarBounds(orientation);
        Rect padding = new Rect();
        if (orientation == CellLayout.LANDSCAPE &&
                transposeLayoutWithOrientation) {
            // Pad the left and right of the workspace with search/hotseat bar sizes
            if (isLayoutRtl) {
                padding.set(hotseatBarHeightPx, edgeMarginPx,
                        searchBarBounds.width(), edgeMarginPx);
            } else {
                padding.set(searchBarBounds.width(), edgeMarginPx + mApplication.statusBarHeight,
                        hotseatBarHeightPx + mApplication.navBarHeight, edgeMarginPx);
            }
        } else {
            if (isTablet()) {
                // Pad the left and right of the workspace to ensure consistent spacing
                // between all icons
                float gapScale = 1f + (dragViewScale - 1f) / 2f;
                int width = (orientation == CellLayout.LANDSCAPE)
                        ? Math.max(widthPx, heightPx)
                        : Math.min(widthPx, heightPx);
                int height = (orientation != CellLayout.LANDSCAPE)
                        ? Math.max(widthPx, heightPx)
                        : Math.min(widthPx, heightPx);
                int paddingTop = searchBarBounds.bottom;
                int paddingBottom = hotseatBarHeightPx + pageIndicatorHeightPx;
                int availableWidth = Math.max(0, width - (int) ((numColumns * cellWidthPx) +
                        (numColumns * gapScale * cellWidthPx)));
                int availableHeight = Math.max(0, height - paddingTop - paddingBottom
                        - (int) (2 * numRows * cellHeightPx));
                padding.set(availableWidth / 2, paddingTop + availableHeight / 2,
                        availableWidth / 2, paddingBottom + availableHeight / 2);
            } else {
                // Pad the top and bottom of the workspace with search/hotseat bar sizes
            	//Change for MyUI---20150707
                /*padding.set(desiredWorkspaceLeftRightMarginPx - defaultWidgetPadding.left,
                        searchBarBounds.top,
                        desiredWorkspaceLeftRightMarginPx - defaultWidgetPadding.right,
                        hotseatBarHeightPx + pageIndicatorHeightPx);*/
                padding.set(desiredWorkspaceLeftRightMarginPx - defaultWidgetPadding.left,
                		mApplication.statusBarHeight,
                		desiredWorkspaceLeftRightMarginPx - defaultWidgetPadding.right,
                hotseatBarHeightPx + pageIndicatorHeightPx + mApplication.navBarHeight);
            }
        }
        return padding;
    }

    int getWorkspacePageSpacing(int orientation) {
        if ((orientation == CellLayout.LANDSCAPE &&
                transposeLayoutWithOrientation) || isLargeTablet()) {
            // In landscape mode the page spacing is set to the default.
            return defaultPageSpacingPx;
        } else {
            // In portrait, we want the pages spaced such that there is no
            // overhang of the previous / next page into the current page viewport.
            // We assume symmetrical padding in portrait mode.
            return Math.max(defaultPageSpacingPx, 2 * getWorkspacePadding().left);
        }
    }

    Rect getOverviewModeButtonBarRect() {
        /*int zoneHeight = (int) (overviewModeIconZoneRatio * availableHeightPx);
        zoneHeight = Math.min(overviewModeMaxIconZoneHeightPx,
                Math.max(overviewModeMinIconZoneHeightPx, zoneHeight));
        if (QCLog.DEBUG) {
			QCLog.d("DeviceProfile", "overviewModeIconZoneRatio = "+overviewModeIconZoneRatio
					+"availableHeightPx = "+availableHeightPx
					+"overviewModeMaxIconZoneHeightPx = "+overviewModeMaxIconZoneHeightPx
					+"overviewModeMinIconZoneHeightPx = "+overviewModeMinIconZoneHeightPx
					+"zoneHeight = "+zoneHeight
					+"availableHeightPx - zoneHeight = "+(availableHeightPx - zoneHeight)
					);
		}*/
        // Modify for MyUI and uniform size Jing.Wu 20160129 start
    	int zoneHeight = mApplication.getPxforYLayout(true, R.dimen.overview_panel_height, 0);
        // Modify for MyUI and uniform size Jing.Wu 20160129 end
        return new Rect(0, availableHeightPx - zoneHeight, 0, availableHeightPx);
    }
    
    float getNormalScale() {
        Rect workspacePadding = getWorkspacePadding();
        //int pageSpace = availableHeightPx - workspacePadding.top - workspacePadding.bottom;
        float pageSpace = LauncherApplication.getScreenHeightPixel() - workspacePadding.top - workspacePadding.bottom;
        float originalTotalHeight = pageSpace+hotseatBarHeightPx;
        float normalScaleTotalHeight = LauncherApplication.getScreenHeightPixel()-searchBarHeightPx-mApplication.navBarHeight
        		-mApplication.getPxforYLayout(true, R.dimen.normal_scale_page_indicator_area_height, 0);
        return normalScaleTotalHeight/originalTotalHeight;
    }

    float getOverviewModeScale() {
        Rect workspacePadding = getWorkspacePadding();
        Rect overviewBar = getOverviewModeButtonBarRect();
        //int pageSpace = availableHeightPx - workspacePadding.top - workspacePadding.bottom;
        float originalPageSpace = LauncherApplication.getScreenHeightPixel()-workspacePadding.top-workspacePadding.bottom;
        float overviewPageSpace = LauncherApplication.getScreenHeightPixel()-searchBarSpaceHeightPx
        		-mApplication.navBarHeight-overviewBar.height()
        		-mApplication.getPxforYLayout(true, R.dimen.overview_page_indicator_area_height, 0);
        
        if (QCLog.DEBUG) {
			QCLog.d("DeviceProfile", "getOverviewModeScale() and return "
					+(overviewPageSpace/originalPageSpace));
		}
        // Modify for MyUI and uniform size Jing.Wu 20160129 start
        //return (overviewModeScaleFactor * (pageSpace - overviewBar.height())) / pageSpace;
        return overviewPageSpace/originalPageSpace;
        // Modify for MyUI and uniform size Jing.Wu 20160129 end
    }

    // The rect returned will be extended to below the system ui that covers the workspace
    Rect getHotseatRect() {
        if (isVerticalBarLayout()) {
            return new Rect(availableWidthPx - hotseatBarHeightPx, 0,
                    Integer.MAX_VALUE, availableHeightPx);
        } else {
            return new Rect(0, availableHeightPx - hotseatBarHeightPx,
                    availableWidthPx, Integer.MAX_VALUE);
        }
    }

    int calculateCellWidth(int width, int countX) {
        return width / countX;
    }
    int calculateCellHeight(int height, int countY) {
        return height / countY;
    }

    boolean isPhone() {
        return !isTablet && !isLargeTablet;
    }
    boolean isTablet() {
        return isTablet;
    }
    boolean isLargeTablet() {
        return isLargeTablet;
    }

    boolean isVerticalBarLayout() {
        return isLandscape && transposeLayoutWithOrientation;
    }

    boolean shouldFadeAdjacentWorkspaceScreens() {
        return isVerticalBarLayout() || isLargeTablet();
    }

    int getVisibleChildCount(ViewGroup parent) {
        int visibleChildren = 0;
        for (int i = 0; i < parent.getChildCount(); i++) {
            if (parent.getChildAt(i).getVisibility() != View.GONE) {
                visibleChildren++;
            }
        }
        return visibleChildren;
    }

    int calculateOverviewModeWidth(int visibleChildCount) {
        return visibleChildCount * overviewModeBarItemWidthPx +
                (visibleChildCount-1) * overviewModeBarSpacerWidthPx;
    }
    
    // Add for MyUI--------20150709
    public float getNumRows() {
		return numRows;
	}
	public void setNumRows(float numRows) {
		this.numRows = numRows;
	}
    public float getNumColumns() {
		return numColumns;
	}
	public void setNumColumns(float numColumns) {
		this.numColumns = numColumns;
	}

    public void layout(Launcher launcher) {
        FrameLayout.LayoutParams lp;
        Resources res = launcher.getResources();
        boolean hasVerticalBarLayout = isVerticalBarLayout();

        // Layout the search bar space
        View searchBar = launcher.getSearchBar();
        lp = (FrameLayout.LayoutParams) searchBar.getLayoutParams();
        if (hasVerticalBarLayout) {
            // Vertical search bar space
            lp.gravity = Gravity.TOP | Gravity.LEFT;
            lp.width = searchBarSpaceHeightPx;
            lp.height = LayoutParams.WRAP_CONTENT;
            searchBar.setPadding(
                    0, 2 * edgeMarginPx, 0,
                    2 * edgeMarginPx);

            LinearLayout targets = (LinearLayout) searchBar.findViewById(R.id.drag_target_bar);
            targets.setOrientation(LinearLayout.VERTICAL);
        } else {
            // Horizontal search bar space
            lp.gravity = Gravity.TOP | Gravity.CENTER_HORIZONTAL;
            lp.width = LayoutParams.MATCH_PARENT;
            lp.height = searchBarSpaceHeightPx;
            searchBar.setPadding(
                    2 * edgeMarginPx,
                    getSearchBarTopOffset(),
                    2 * edgeMarginPx, 0);
        }
        searchBar.setLayoutParams(lp);

        // Layout the workspace
        PagedView workspace = (PagedView) launcher.findViewById(R.id.workspace);
        lp = (FrameLayout.LayoutParams) workspace.getLayoutParams();
        lp.gravity = Gravity.CENTER;
        int orientation = isLandscape ? CellLayout.LANDSCAPE : CellLayout.PORTRAIT;
        Rect padding = getWorkspacePadding(orientation);
        workspace.setLayoutParams(lp);
        workspace.setPadding(padding.left, padding.top, padding.right, padding.bottom);
        workspace.setPageSpacing(getWorkspacePageSpacing(orientation));

        // Layout the hotseat
        View hotseat = launcher.findViewById(R.id.hotseat);
        lp = (FrameLayout.LayoutParams) hotseat.getLayoutParams();
        if (hasVerticalBarLayout) {
            // Vertical hotseat
            lp.gravity = Gravity.RIGHT;
            lp.width = hotseatBarHeightPx;
            lp.height = 1080 - mApplication.statusBarHeight;
            lp.rightMargin = mApplication.navBarHeight;
            hotseat.findViewById(R.id.layout).setPadding(0, 8 * edgeMarginPx, 0, 2 * edgeMarginPx);
        } else if (isTablet()) {
            // Pad the hotseat with the workspace padding calculated above
            lp.gravity = Gravity.BOTTOM;
            lp.width = LayoutParams.MATCH_PARENT;
            lp.height = hotseatBarHeightPx;
            hotseat.setPadding(edgeMarginPx + padding.left, 0,
                    edgeMarginPx + padding.right,
                    2 * edgeMarginPx);
        } else {
            // For phones, layout the hotseat without any bottom margin
            // to ensure that we have space for the folders
            lp.gravity = Gravity.BOTTOM;
            lp.width = LayoutParams.MATCH_PARENT;
            lp.height = hotseatBarHeightPx;
            lp.bottomMargin = mApplication.navBarHeight;
            //Change for MyUI---20150707
            //hotseat.findViewById(R.id.layout).setPadding(2 * edgeMarginPx, 0,
            //        2 * edgeMarginPx, 0);
            hotseat.findViewById(R.id.layout).setPadding(padding.left, 0,
                    padding.right, 0);
            
        }
        hotseat.setLayoutParams(lp);

        // Layout the page indicators
        View pageIndicator = launcher.findViewById(R.id.page_indicator);
        if (pageIndicator != null) {
            if (hasVerticalBarLayout) {
                // Hide the page indicators when we have vertical search/hotseat
                pageIndicator.setVisibility(View.GONE);
            } else {
                // Put the page indicators above the hotseat
                lp = (FrameLayout.LayoutParams) pageIndicator.getLayoutParams();
                lp.gravity = Gravity.CENTER_HORIZONTAL | Gravity.BOTTOM;
                lp.width = LayoutParams.WRAP_CONTENT;
                lp.height = pageIndicatorHeightPx;
                lp.bottomMargin = hotseatBarHeightPx + mApplication.navBarHeight;
                pageIndicator.setPadding(0, 0, 0, mApplication.getPxforYLayout(false, 0, DynamicGrid.pxFromDp(6, res.getDisplayMetrics())));
                pageIndicator.setLayoutParams(lp);
            }
        }

        // Layout the Overview Mode
        ViewGroup overviewMode = launcher.getOverviewPanel();
        if (overviewMode != null) {
            Rect r = getOverviewModeButtonBarRect();
            lp = (FrameLayout.LayoutParams) overviewMode.getLayoutParams();
            lp.gravity = Gravity.CENTER_HORIZONTAL | Gravity.BOTTOM;
            //lp.width = Math.min(availableWidthPx,
            //        calculateOverviewModeWidth(getVisibleChildCount(overviewMode)));
            //lp.height = res.getDimensionPixelSize(R.dimen.overview_panel_height);
            lp.width = LayoutParams.MATCH_PARENT;
            lp.height = r.height();
            lp.bottomMargin = mApplication.navBarHeight;
            overviewMode.setLayoutParams(lp);
            
            if (!LauncherApplication.getIsNormalScreenResolutionAndDensity()) {
                LinearLayout topOverview = (LinearLayout)overviewMode.findViewById(R.id.overview_top);
                if (topOverview != null) {
                    lp = (FrameLayout.LayoutParams) topOverview.getLayoutParams();
                    lp.width = LayoutParams.MATCH_PARENT;
                    lp.height = mApplication.getPxforYLayout(true, R.dimen.overview_panel_item_height, 0);
                    lp.bottomMargin = mApplication.getPxforYLayout(true, R.dimen.overview_panel_scrollbar_height, 0);
                    topOverview.setLayoutParams(lp);
        		}
                HorizontalScrollView overSwitchHScrollView = (HorizontalScrollView)overviewMode.findViewById(R.id.overview_switch_scrollview);
                if (overSwitchHScrollView != null) {
        			overSwitchHScrollView.setPadding(0, 0, 0, 
        					mApplication.getPxforYLayout(true, R.dimen.overview_panel_scrollbar_height, 0));
        			overSwitchHScrollView.setScrollBarSize(
        					mApplication.getPxforYLayout(true, R.dimen.overview_panel_scrollbar_height, 0));
        		}
                LinearLayout overSwitchContent = (LinearLayout)overviewMode.findViewById(R.id.overview_switch_detail);
                if (overSwitchContent != null) {
                	lp = (FrameLayout.LayoutParams)overSwitchContent.getLayoutParams();
                	lp.width = LayoutParams.WRAP_CONTENT;
                	lp.height = mApplication.getPxforYLayout(true, R.dimen.overview_panel_item_height, 0);
                	lp.gravity = Gravity.TOP;
                    overSwitchContent.setLayoutParams(lp);
        		}
                ViewGroup appsTabHost = launcher.getAppsTabHost();
                if (appsTabHost != null) {
                    lp = (FrameLayout.LayoutParams) appsTabHost.getLayoutParams();
                    lp.width = LayoutParams.MATCH_PARENT;
                    lp.height = mApplication.getPxforYLayout(true, R.dimen.overview_panel_item_height, 0);
                    lp.bottomMargin = mApplication.getPxforYLayout(true, R.dimen.overview_panel_scrollbar_height, 0);
                    appsTabHost.setLayoutParams(lp);
        		}
                HorizontalScrollView overWallpaperHScrollView = (HorizontalScrollView)overviewMode.findViewById(R.id.overview_wallpaper_scrollview);
                if (overWallpaperHScrollView != null) {
                	overWallpaperHScrollView.setPadding(0, 0, 0, 
        					mApplication.getPxforYLayout(true, R.dimen.overview_panel_scrollbar_height, 0));
                	overWallpaperHScrollView.setScrollBarSize(
        					mApplication.getPxforYLayout(true, R.dimen.overview_panel_scrollbar_height, 0));
        		}
                LinearLayout overWallpaperContent = (LinearLayout)overviewMode.findViewById(R.id.overview_wallpaper_detail);
                if (overWallpaperContent != null) {
                	lp = (FrameLayout.LayoutParams)overWallpaperContent.getLayoutParams();
                	lp.width = LayoutParams.WRAP_CONTENT;
                	lp.height = mApplication.getPxforYLayout(true, R.dimen.overview_panel_item_height, 0);
                	lp.gravity = Gravity.TOP;
                    overWallpaperContent.setLayoutParams(lp);
        		}
			}
        }
        
    }
}
