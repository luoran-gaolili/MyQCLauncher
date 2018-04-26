package com.qingcheng.home.database;

import com.qingcheng.home.LauncherApplication;

import android.content.SharedPreferences;
import android.os.Environment;

public class QCPreference implements SharedPreferences.OnSharedPreferenceChangeListener{
	private static final String TAG = "QCPreference";
	
	public static final String PREFERENCE_NAME = "com.qingcheng.home.featureprefs";
    public static final String THIRDAPP_UNREAD_NUMB = "third_app_unread_numb";
	
    private static final String HOME_FILE_PATH = "/data/system/theme/";
    public static final String THEME_FOLDER_PATH = ".current_use/theme/";
    public static String getInternalThemePath() {
        return LauncherApplication.getInternalStoragePath()+THEME_FOLDER_PATH;
    }
    
    public static final String THEME_FILE_SUFFIX = ".zip";
    public static final String THEME_ZIP = "qingcheng_current_theme" + THEME_FILE_SUFFIX;
    public static final String THEME_DIR = "/theme";

    public static final String THEME_HOME_WALLPAPER_PATH = "HomeWallpaper/";
    public static final String THEME_ICON_PATH = "Icon/";
    public static final String THEME_ICON = "themeIcon";
    public static final String THEME_DESCRIPTION = "description.xml";
    public static final String WALLPAPER_PATH_IN_THEME_JPG =  THEME_HOME_WALLPAPER_PATH + "default_wallpaper.jpg";
    public static final String WALLPAPER_PATH_IN_THEME_PNG =  THEME_HOME_WALLPAPER_PATH + "default_wallpaper.png";
    
    //intent
    public final static String INTENT_ACTION_SWITCH_THEME = "com.qingcheng.thememgr.action.SWITCH_THEME";
    public final static String INTENT_ACTION_UPDATE_THEME = "com.qingcheng.thememgr.action.UPDATE_THEME";
    public final static String INTENT_ACTION_REFLUSH_WORKSPACE = "com.qingcheng.home.launcher.action.REFLUSH_WORKSPACE";
    public final static String INTENT_ACTION_SWITCH_SLIDE_TYPE = "com.qingcheng.home.launcher.action.SWITCH_SLIDE_TYPE";

	// Add for navigationbar hide Jing.Wu 20150915 start
    public final static String INTENT_ACTION_HIDE_NAVIGATIONBAR = "com.qingcheng.navigationbar.changevisibility";
	// Add for navigationbar hide Jing.Wu 20150915 end
    
    // Add for MyUI and Launcher Settings Jing.Wu 20150701 start
	public static final String KEY_IS_FIRST = "isfirst";
    public static final String KEY_SCREEN_DENSITY = "screen_density";
    public static final String KEY_HAS_NAV_BAR = "has_navigationbar";
    public static final String KEY_NAV_BAR_HEIGHT = "navigationbar_height";
    public static final String KEY_INTERNAL_PATH = "internal_path";
    public static final String KEY_THEME_LOST = "theme_lost";
    public static final String KEY_OLD_COUNTRY = "old_country";
    public static final String KEY_OLD_LANGUAGE = "old_language";
    
    // unrealized
	public static final String KEY_SCREEN_LAYOUT = "screen_layout";
	public static final String KEY_LAYOUT_ROWS = "layout_rows";
	public static final String KEY_LAYOUT_COLUMNS = "layout_columns";
	
	public static final String KEY_DEFAULT_SCREEN = "default_screen_upgrade";
    public static final String KEY_SLIDE_ANIMATION = "workspace_animation";
    public static final String KEY_CUSTOM_THEME= "com_qingcheng_home_custom_theme";
    public static final String KEY_DEFAULT_BROWSER = "default_browser";
    
	public static final String KEY_AUTOREORDER_ICONS = "autoreorder_icons";
	public static final String KEY_ICONS_UNREADINFO = "icons_unreadinfo";
	public static final String KEY_CYCLE_SLIDE = "cycle_slide";
	public static final String KEY_NEWS_PAGE = "news_page";
	public static final String KEY_PROJECTOR = "projector";
    // Add for MyUI and Launcher Settings Jing.Wu 20150701 end
    
	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
			String key) {
		// TODO Auto-generated method stub
		
	}

}
