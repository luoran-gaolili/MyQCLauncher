package com.qingcheng.home;


import java.io.IOException;
import java.io.InputStream;
import com.qingcheng.home.database.QCPreference;

import android.app.WallpaperManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import android.view.View;


public class StartPage {
    private final static String TAG = "Launcher.StartPage";

    public static void onResume(Context context, View view) {
        SharedPreferences preferences = context.getSharedPreferences(QCPreference.PREFERENCE_NAME, Context.MODE_PRIVATE);
        boolean isFirst = preferences.getBoolean(QCPreference.KEY_IS_FIRST, true);
        if (isFirst /*|| preferences.getBoolean(QCPreference.KEY_THEME_LOST, false)*/) {
            SharedPreferences.Editor editor = preferences.edit();
            try {
                initWallpaper(context);
            } catch (Exception e) {
                e.printStackTrace();
                Log.e(TAG, "onResume: " + e);
            }

            editor.putBoolean(QCPreference.KEY_IS_FIRST, false);
            editor.commit();

            waitAndFinish(2000, view);
        } else {
            waitAndFinish(2000, view);
        }
    }

    public static boolean initTheme(Context context, boolean isForce) {
            return true;
    }

    private static void initWallpaper(Context context) {
        WallpaperManager wpm = WallpaperManager.getInstance(context);
        try {
            InputStream stream = null;
            String wallpaperPath = QCPreference.WALLPAPER_PATH_IN_THEME_PNG;
            stream = context.getAssets().open(wallpaperPath);
            if (stream != null) {
                wpm.setStream(stream);
            }
            stream.close();
        } catch (IOException e) {
            try{
                InputStream stream = null;
                String wallpaperPath = QCPreference.WALLPAPER_PATH_IN_THEME_JPG;
                stream = context.getAssets().open(wallpaperPath);
                if (stream != null) {
                    wpm.setStream(stream);
                }
                stream.close();
            }catch (IOException e1){
                e1.printStackTrace();
            }
            Log.e(TAG, "Failed to set wallpaper when change theme: " + e);
        }
    }

    private static void waitAndFinish(int time, final View view) {
        view.postDelayed(new Runnable() {
            @Override
            public void run() {
               view.setVisibility(View.GONE);
            }
        }, time);
    }
}
