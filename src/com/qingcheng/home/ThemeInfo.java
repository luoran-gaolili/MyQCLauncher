package com.qingcheng.home;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import com.qingcheng.home.database.QCPreference;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.util.Xml;

public class ThemeInfo {

    private static final String TAG = "Launcher.ThemeInfo";

    public final static int THEME_VERSION_V1 = 1;
    public final static int THEME_VERSION_V2 = 2;
    public final static int SUPPORTED_MAX_THEME_VERSION = THEME_VERSION_V2;

    private boolean mInitialized = false;
    private float mVersion = 1.0f;
    private String mFileName = QCPreference.getInternalThemePath()+QCPreference.THEME_ZIP;
    private String mMaskV1Name = "mask.png";
    private String mMaskV2Name = "icon_mask_v2.png";
    private String mBGName = "rgk_icon_background.png";
    private String mTopOffsetName = "icon_top_offset.png";
    private String mFolderOutPaddingName = "folder_out_padding.png";
    private String mFolderInnerPaddingName = "folder_inner_padding.png";
    private String mFolderInnerName = "portal_ring_inner_holo.png";

    private Object mInitLock = new Object();

    private Context mContext;
    Drawable mMaskV1Drawable = null;
    Drawable mMaskV2Drawable = null;
    Drawable mBGDrawable = null;
    Drawable mTopOffsetDrawable = null;
    Drawable mBorderDrawable = null;
	//sunfeng modfiy @20150802 for folder start
    public Drawable mFolderOutPadding = null;
	//sunfeng modfiy @20150802 for folder end
    Drawable mFolderInnerPadding = null;
    private Drawable mFolderInnerDrawable;

    public ThemeInfo(Context context){
        mContext = context;
    }

    public boolean isThemeInitialized(){
        synchronized(mInitLock){
            return mInitialized;
        }
    }

    public boolean initializeIfNeed() {
        synchronized (mInitLock) {
            if (mInitialized) {
                return true;
            }
            Log.d(TAG, "start init theme resources");

            parseThemeDescription(QCPreference.THEME_DESCRIPTION);
            mBGDrawable = createDrawableFromZip(QCPreference.THEME_ICON_PATH + mBGName);
            mMaskV1Drawable = createDrawableFromZip(QCPreference.THEME_ICON_PATH + mMaskV1Name);
            mMaskV2Drawable = createDrawableFromZip(QCPreference.THEME_ICON_PATH + mMaskV2Name);
            mTopOffsetDrawable = createDrawableFromZip(QCPreference.THEME_ICON_PATH + mTopOffsetName);
            mFolderOutPadding = createDrawableFromZip(QCPreference.THEME_ICON_PATH + mFolderOutPaddingName);
            mFolderInnerPadding = createDrawableFromZip(QCPreference.THEME_ICON_PATH + mFolderInnerPaddingName);
            mFolderInnerDrawable = createDrawableFromZip(QCPreference.THEME_ICON_PATH + mFolderInnerName);

            mInitialized = true;
            return true;

        }
    }

    public int getVersion(){
        int version = (int)mVersion;
        if(version > SUPPORTED_MAX_THEME_VERSION){
            return SUPPORTED_MAX_THEME_VERSION;
        }else{
            return version;
        }
    }

    public Drawable getBackground() {
        return mBGDrawable;
    }

    public Drawable getMask() {
        final int version = (int)mVersion;
        switch(version){
        case THEME_VERSION_V1:
            return mMaskV1Drawable;
        case THEME_VERSION_V2:
            return mMaskV2Drawable;
        default:
            if(version > SUPPORTED_MAX_THEME_VERSION){
                return mMaskV2Drawable;
            }else{
                return mMaskV1Drawable;
            }
        }
    }

    public Drawable getTopOffset() {
        return mTopOffsetDrawable;
    }

    public void clear(){
        synchronized (mInitLock) {
            mInitialized = false;
            mVersion = 1.0f;
            mMaskV1Drawable = null;
            mMaskV2Drawable = null;
            mBGDrawable = null;
            mTopOffsetDrawable = null;
            mFolderInnerPadding = null;
            mFolderOutPadding = null;
        }
    }

    private final static String TAG_VERSION="version";
    private final static String TAG_TITLE="title";
    /*
    <title>默认主题</title>
     */
    private void parseThemeDescription(String description) {
        InputStream is = null;
        try {
            if(LauncherAppState.isCustomTheme()){
                StringBuilder sb = new StringBuilder();
                sb.append(mContext.getFilesDir().getPath());
                sb.append(QCPreference.THEME_DIR);
                sb.append("/");
                sb.append(description);
                Log.e(TAG, "parseThemeDescription: custom theme description path = " + sb.toString());
                is = new FileInputStream(sb.toString());
            }else {
                is = mContext.getAssets().open(description);
            }
            XmlPullParser parser = Xml.newPullParser();
            parser.setInput(is, "utf-8");
            int eventType = parser.getEventType();
            boolean stopped = false;
            boolean stoppedByTitle = false;
            while (eventType != XmlPullParser.END_DOCUMENT && (!stopped || !stoppedByTitle)) {
                switch (eventType) {
                    case XmlPullParser.START_DOCUMENT:
                        break;
                    case XmlPullParser.START_TAG:
                        String name = parser.getName();
                        if (name.equals(TAG_VERSION)) {
                            String version = parser.nextText();
                            String regex = "[a-zA-Z]";
                            Pattern pattern = Pattern.compile(regex);
                            Matcher matcher = pattern.matcher(version);
                            String v = matcher.replaceAll("");
                            mVersion = Float.valueOf(v);
                            stopped = true;
                        } else if (TAG_TITLE.equals(name)) {
                            String title = parser.nextText();
                            Log.d(TAG, "parseThemeDescription: title = " + title);
                            if ("默认主题".equals(title)) {
                                LauncherAppState.getInstance().mShowCustomIconAni = true;
                            } else {
                                LauncherAppState.getInstance().mShowCustomIconAni = false;
                            }
                            stoppedByTitle = true;
                        }
                        break;
                }
                eventType = parser.next();
            }
        } catch (XmlPullParserException e) {
            Log.w(TAG, "XmlPullParserException");
            e.printStackTrace();
        } catch (IOException e) {
            is = null;
            Log.w(TAG, "creat is failed for " + QCPreference.THEME_DESCRIPTION);
            e.printStackTrace();
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

    }

    private Drawable createDrawableFromZip(String entryName) {
        try {
            InputStream is = null;
            if(LauncherAppState.isCustomTheme()){
                StringBuilder sb = new StringBuilder();
                sb.append(mContext.getFilesDir().getPath());
                sb.append(QCPreference.THEME_DIR);
                sb.append("/");
                sb.append(entryName);
                Log.e(TAG, "createDrawableFromZip: custom theme entryName path = " + sb.toString());
                is = new FileInputStream(sb.toString());
            }else {
                mContext.getAssets().open(entryName);
            }
            if (is != null) {
                Drawable d = Drawable.createFromResourceStream(mContext.getResources(), null, is, null);

                is.close();
                return d;
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    public Drawable getFolderOutPadding() {
        return mFolderOutPadding;
    }

    public Drawable getFolderInnerPadding() {
        return mFolderInnerPadding;
    }
    public Drawable getFolderInner() {
        return mFolderInnerDrawable;
    }
}
