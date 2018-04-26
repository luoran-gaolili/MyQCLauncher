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

import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningTaskInfo;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PaintFlagsDrawFilter;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.PorterDuff.Mode;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.PaintDrawable;
import android.os.Build;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.Pair;
import android.util.SparseArray;
import android.view.View;
import android.widget.Toast;

import com.mediatek.launcher3.ext.LauncherLog;
import com.qingcheng.home.ThemeInfo;
import com.qingcheng.home.R;
import com.qingcheng.home.database.QCPreference;
import com.qingcheng.home.util.QCLog;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;


/**
 * Various utilities shared amongst the Launcher's classes.
 */
public final class Utilities {
    private static final String TAG = "Launcher.Utilities";

    private static int sIconWidth = -1;
    private static int sIconHeight = -1;
    public static int sIconTextureWidth = -1;
    public static int sIconTextureHeight = -1;
    
    private static int sIconFrameTSpace = 0;
    private static int sIconFrameLSpace = 0;
    private static int sIconFrameBSpace = 0;
    private static int sIconFrameRSpace = 0;

    private static final Rect sOldBounds = new Rect();
    private static final Canvas sCanvas = new Canvas();

    static {
        sCanvas.setDrawFilter(new PaintFlagsDrawFilter(Paint.DITHER_FLAG,
                Paint.FILTER_BITMAP_FLAG));
    }
    static int sColors[] = { 0xffff0000, 0xff00ff00, 0xff0000ff };
    static int sColorIndex = 0;

    static int[] sLoc0 = new int[2];
    static int[] sLoc1 = new int[2];

    // To turn on these properties, type
    // adb shell setprop log.tag.PROPERTY_NAME [VERBOSE | SUPPRESS]
    static final String FORCE_ENABLE_ROTATION_PROPERTY = "launcher_force_rotate";
    public static boolean sForceEnableRotation = isPropertyEnabled(FORCE_ENABLE_ROTATION_PROPERTY);

    /**
     * Returns a FastBitmapDrawable with the icon, accurately sized.
     */
    public static FastBitmapDrawable createIconDrawable(Bitmap icon) {
        FastBitmapDrawable d = new FastBitmapDrawable(icon);
        d.setFilterBitmap(true);
        resizeIconDrawable(d);
        return d;
    }

    /**
     * Resizes an icon drawable to the correct icon size.
     */
    static void resizeIconDrawable(Drawable icon) {
        icon.setBounds(0, 0, sIconTextureWidth, sIconTextureHeight);
    }

    private static boolean isPropertyEnabled(String propertyName) {
        return Log.isLoggable(propertyName, Log.VERBOSE);
    }

    public static boolean isRotationEnabled(Context c) {
        boolean enableRotation = sForceEnableRotation ||
                c.getResources().getBoolean(R.bool.allow_rotation);
        return enableRotation;
    }

    /**
     * Indicates if the device is running LMP or higher.
     */
    public static boolean isLmpOrAbove() {
        return Build.VERSION.SDK_INT >= 21;//Build.VERSION_CODES.L;
    }

    public static final boolean ATLEAST_LOLLIPOP =
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP;

    /**
     * Returns a bitmap suitable for the all apps view. Used to convert pre-ICS
     * icon bitmaps that are stored in the database (which were 74x74 pixels at hdpi size)
     * to the proper size (48dp)
     */
    static Bitmap createIconBitmap(Bitmap icon, Context context) {
        int textureWidth = sIconTextureWidth;
        int textureHeight = sIconTextureHeight;
        int sourceWidth = icon.getWidth();
        int sourceHeight = icon.getHeight();
        if (sourceWidth > textureWidth && sourceHeight > textureHeight) {
            // Icon is bigger than it should be; clip it (solves the GB->ICS migration case)
            return Bitmap.createBitmap(icon,
                    (sourceWidth - textureWidth) / 2,
                    (sourceHeight - textureHeight) / 2,
                    textureWidth, textureHeight);
        } else if (sourceWidth == textureWidth && sourceHeight == textureHeight) {
            // Icon is the right size, no need to change it
            return icon;
        } else {
            // Icon is too small, render to a larger bitmap
            final Resources resources = context.getResources();
            return createIconBitmap(new BitmapDrawable(resources, icon), context);
        }
    }

    /**
     * Returns a bitmap suitable for the all apps view.
     */
    public static Bitmap createIconBitmap(Drawable icon, Context context) {
        synchronized (sCanvas) { // we share the statics :-(
            if (sIconWidth == -1) {
                initStatics(context);
            }

            int width = sIconWidth;
            int height = sIconHeight;

            if (icon instanceof PaintDrawable) {
                PaintDrawable painter = (PaintDrawable) icon;
                painter.setIntrinsicWidth(width);
                painter.setIntrinsicHeight(height);
            } else if (icon instanceof BitmapDrawable) {
                // Ensure the bitmap has a density.
                BitmapDrawable bitmapDrawable = (BitmapDrawable) icon;
                Bitmap bitmap = bitmapDrawable.getBitmap();
                if (bitmap.getDensity() == Bitmap.DENSITY_NONE) {
                    bitmapDrawable.setTargetDensity(context.getResources().getDisplayMetrics());
                }
            }
            int sourceWidth = icon.getIntrinsicWidth();
            int sourceHeight = icon.getIntrinsicHeight();
            if (sourceWidth > 0 && sourceHeight > 0) {
                // Scale the icon proportionally to the icon dimensions
                final float ratio = (float) sourceWidth / sourceHeight;
                if (sourceWidth > sourceHeight) {
                    height = (int) (width / ratio);
                } else if (sourceHeight > sourceWidth) {
                    width = (int) (height * ratio);
                }
            }

            // no intrinsic size --> use default size
            int textureWidth = sIconTextureWidth;
            int textureHeight = sIconTextureHeight;

            final Bitmap bitmap = Bitmap.createBitmap(textureWidth, textureHeight,
                    Bitmap.Config.ARGB_8888);
            final Canvas canvas = sCanvas;
            canvas.setBitmap(bitmap);

            final int left = (textureWidth-width) / 2;
            final int top = (textureHeight-height) / 2;

            @SuppressWarnings("all") // suppress dead code warning
            final boolean debug = false;
            if (debug) {
                // draw a big box for the icon for debugging
                canvas.drawColor(sColors[sColorIndex]);
                if (++sColorIndex >= sColors.length) sColorIndex = 0;
                Paint debugPaint = new Paint();
                debugPaint.setColor(0xffcccc00);
                canvas.drawRect(left, top, left+width, top+height, debugPaint);
            }

            sOldBounds.set(icon.getBounds());
            icon.setBounds(left, top, left+width, top+height);
            icon.draw(canvas);
            icon.setBounds(sOldBounds);
            canvas.setBitmap(null);

            return bitmap;
        }
    }

    public static Bitmap createIconBitmap2(Drawable icon, Context context, ThemeInfo info) {
        synchronized (sCanvas) { // we share the statics :-(
            if (sIconWidth == -1) {
                initStatics(context);
            }

            int width = sIconWidth;
            int height = sIconHeight;

            if (icon instanceof PaintDrawable) {
                PaintDrawable painter = (PaintDrawable) icon;
                painter.setIntrinsicWidth(width);
                painter.setIntrinsicHeight(height);
            } else if (icon instanceof BitmapDrawable) {
                // Ensure the bitmap has a density.
                BitmapDrawable bitmapDrawable = (BitmapDrawable) icon;
                Bitmap bitmap = bitmapDrawable.getBitmap();
                if (bitmap.getDensity() == Bitmap.DENSITY_NONE) {
                    bitmapDrawable.setTargetDensity(context.getResources().getDisplayMetrics());
                }
            }
            int sourceWidth = icon.getIntrinsicWidth();
            int sourceHeight = icon.getIntrinsicHeight();
            if (sourceWidth > 0 && sourceHeight > 0) {
                // Scale the icon proportionally to the icon dimensions
                final float ratio = (float) sourceWidth / sourceHeight;
                if (sourceWidth > sourceHeight) {
                    height = (int) (width / ratio);
                } else if (sourceHeight > sourceWidth) {
                    width = (int) (height * ratio);
                }
            }

            // no intrinsic size --> use default size
            int textureWidth = sIconTextureWidth;
            int textureHeight = sIconTextureHeight;

            final Bitmap bitmap = Bitmap.createBitmap(textureWidth, textureHeight,
                    Bitmap.Config.ARGB_8888);
            final Canvas canvas = sCanvas;
            canvas.setBitmap(bitmap);

            final int left = (textureWidth-width) / 2;
            final int top = (textureHeight-height) / 2;

            @SuppressWarnings("all") // suppress dead code warning
            final boolean debug = false;
            if (debug) {
                // draw a big box for the icon for debugging
                canvas.drawColor(sColors[sColorIndex]);
                if (++sColorIndex >= sColors.length) sColorIndex = 0;
                Paint debugPaint = new Paint();
                debugPaint.setColor(0xffcccc00);
                canvas.drawRect(left, top, left+width, top+height, debugPaint);
            }

            sOldBounds.set(icon.getBounds());
            icon.setBounds(left, top, left+width, top+height);
            icon.draw(canvas);
            icon.setBounds(sOldBounds);
            canvas.setBitmap(null);

            Bitmap output = getRoundedCornerBitmap(bitmap, info);
            return output;
        }
    }

    private static Bitmap getRoundedCornerBitmap(Bitmap bitmap, ThemeInfo info) {
        if (info == null || info.getMask() == null) {
            Bitmap output = Bitmap.createBitmap(bitmap.getWidth(), bitmap
                    .getHeight(), Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(output);

            final int color = 0xff424242;
            final Paint paint = new Paint();
            final Rect rect = new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight());
            final RectF rectF = new RectF(rect);

            paint.setAntiAlias(true);
            canvas.drawARGB(0, 0, 0, 0);
            paint.setColor(color);
            canvas.drawRoundRect(rectF, 30, 30, paint);

            paint.setXfermode(new PorterDuffXfermode(Mode.SRC_IN));
            canvas.drawBitmap(bitmap, rect, rect, paint);

            return output;
        }else{
            BitmapDrawable maskDrawable = (BitmapDrawable) info.getMask();
            Bitmap dstBmp = maskDrawable.getBitmap();
            int width;
            int height;

            if(bitmap.getWidth() > dstBmp.getWidth()){
                width = dstBmp.getWidth();
                height = dstBmp.getHeight();
            }else{
                width = bitmap.getWidth();
                height = bitmap.getHeight();
            }

            Bitmap output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(output);

            final int color = 0xff424242;
            Paint paint = new Paint(Paint.FILTER_BITMAP_FLAG);
            final Rect src_rect = new Rect(0, 0, dstBmp.getWidth(), dstBmp.getHeight());

            paint.setAntiAlias(true);
            canvas.drawARGB(0, 0, 0, 0);
            paint.setColor(color);

            final Rect dst_rect = new Rect(0, 0, width, height);

            canvas.drawBitmap(dstBmp, src_rect, dst_rect, paint);

            paint.setXfermode(new PorterDuffXfermode(Mode.SRC_OUT));
            canvas.drawBitmap(bitmap, src_rect, src_rect, paint);

            return output;
        }
    }

    public static Bitmap createIconBitmap(Drawable icon, Context context, ThemeInfo info){
        synchronized (sCanvas) { // we share the statics :-(
            if(!info.isThemeInitialized()){
                return createIconBitmapWithoutTheme(icon, context);
            }else{
                Bitmap bmp = null;
                switch(info.getVersion()){
                case ThemeInfo.THEME_VERSION_V1:
                    bmp=createIconBitmapV1(icon, context, info);
                    break;
                case ThemeInfo.THEME_VERSION_V2:
                    bmp=createIconBitmapV2(icon, context, info);
                    break;
                default:
                    bmp = createIconBitmapWithoutTheme(icon, context);
                    break;
                }
                return bmp;
            }
        }
    }

    static Bitmap getRoundedCornerBitmap(Drawable src, Drawable dst) {
        BitmapDrawable iconDrawable = (BitmapDrawable) src;
        Bitmap srcBmp = iconDrawable.getBitmap();

        BitmapDrawable maskDrawable = (BitmapDrawable) dst;
        Bitmap dstBmp = maskDrawable.getBitmap();
        int width = 0;
        int height = 0;
        
        if(srcBmp.getWidth() > dstBmp.getWidth()){
            width = dstBmp.getWidth();
            height = dstBmp.getHeight();            
        }else{
            width = srcBmp.getWidth();
            height = srcBmp.getHeight();            
        }

        Bitmap output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(output);

        final int color = 0xff424242;
        final Paint paint = new Paint();
        final Rect src_rect = new Rect(0, 0, dstBmp.getWidth(), dstBmp.getHeight());

        paint.setAntiAlias(true);
        canvas.drawARGB(0, 0, 0, 0);
        paint.setColor(color);
        
        final Rect dst_rect = new Rect(0, 0, width, height);
    
        canvas.drawBitmap(dstBmp, src_rect, dst_rect, paint);
        paint.setXfermode(new PorterDuffXfermode(Mode.SRC_OUT));            
        if(srcBmp.getWidth() < dstBmp.getWidth()){
            canvas.drawBitmap(srcBmp, src_rect, src_rect, paint);    
        }else{
            canvas.drawBitmap(srcBmp, new Rect(0, 0, srcBmp.getWidth(), srcBmp.getHeight()), new Rect(0, 0, dstBmp.getWidth(), dstBmp.getHeight()), paint);            
        }

        return output;
    }

    private static Bitmap createIconBitmapWithoutTheme(Drawable icon, Context context){
        synchronized (sCanvas) { // we share the statics :-(
            if (sIconWidth == -1) {
                initStatics(context);
            }

            int width = sIconWidth - (sIconFrameLSpace + sIconFrameRSpace);
            int height = sIconHeight - (sIconFrameTSpace + sIconFrameBSpace);

            int origWidth = width;
            int origHeight = height;

            if (icon instanceof PaintDrawable) {
                PaintDrawable painter = (PaintDrawable) icon;
                painter.setIntrinsicWidth(width);
                painter.setIntrinsicHeight(height);
            } else if (icon instanceof BitmapDrawable) {
                // Ensure the bitmap has a density.
                BitmapDrawable bitmapDrawable = (BitmapDrawable) icon;
                Bitmap bitmap = bitmapDrawable.getBitmap();
                //                if (bitmap.getDensity() == Bitmap.DENSITY_NONE) {
                bitmapDrawable.setTargetDensity(context.getResources().getDisplayMetrics());
                //                }
            }
            int sourceWidth = icon.getIntrinsicWidth();
            int sourceHeight = icon.getIntrinsicHeight();

            if (sourceWidth > 0 && sourceHeight > 0) {
                // There are intrinsic sizes.
                if (width < sourceWidth || height < sourceHeight) {
                    // It's too big, scale it down.
                    final float ratio = (float) sourceWidth / sourceHeight;
                    if (sourceWidth > sourceHeight) {
                        height = (int) (width / ratio);
                    } else if (sourceHeight > sourceWidth) {
                        width = (int) (height * ratio);
                    }
                } else if (sourceWidth < width && sourceHeight < height) {
                    // It's small, use the size they gave us.
                    width = sourceWidth;
                    height = sourceHeight;
                }
            }


            // no intrinsic size --> use default size
            int textureWidth = sIconTextureWidth;
            int textureHeight = sIconTextureHeight;

            final Bitmap bitmap = Bitmap.createBitmap(textureWidth, textureHeight,
                    Bitmap.Config.ARGB_8888);
            final Canvas canvas = sCanvas;
            canvas.setBitmap(bitmap);

            final int left = (origWidth-width) / 2 + sIconFrameLSpace;
            final int top = (origHeight-height) / 2 + sIconFrameTSpace;

//            Drawable frameDrawable = null; //= sFrameDrawable;
            Drawable roundDrawable = null;
//            final Resources resources = context.getResources();
//            frameDrawable = resources.getDrawable(R.drawable.qingcheng_icon_background);
//            sOldBounds.set(frameDrawable.getBounds());
//            frameDrawable.setBounds(0, 0, sIconWidth, sIconHeight);
//            frameDrawable.draw(canvas);
//            frameDrawable.setBounds(sOldBounds);
           
            //modify by zhangkun 2013-07-02 for crop application for current theme
            if(roundDrawable == null){
               roundDrawable = icon;
            }
            sOldBounds.set(roundDrawable.getBounds());
            roundDrawable.setBounds(left, top, left+width, top+height);
            roundDrawable.draw(canvas);
            roundDrawable.setBounds(sOldBounds);
            //end modify

            return bitmap;
        }
    }

    private static Bitmap createIconBitmapV1(Drawable icon, Context context, ThemeInfo info){
        synchronized (sCanvas) { // we share the statics :-(
            if (sIconWidth == -1) {
                initStatics(context);
            }

            int width = sIconWidth - (sIconFrameLSpace + sIconFrameRSpace);
            int height = sIconHeight - (sIconFrameTSpace + sIconFrameBSpace);

            int origWidth = width;
            int origHeight = height;

            if (icon instanceof PaintDrawable) {
                PaintDrawable painter = (PaintDrawable) icon;
                painter.setIntrinsicWidth(width);
                painter.setIntrinsicHeight(height);
            } else if (icon instanceof BitmapDrawable) {
                // Ensure the bitmap has a density.
                BitmapDrawable bitmapDrawable = (BitmapDrawable) icon;
                Bitmap bitmap = bitmapDrawable.getBitmap();
                //if (bitmap.getDensity() == Bitmap.DENSITY_NONE) {
                bitmapDrawable.setTargetDensity(context.getResources().getDisplayMetrics());
                //}
            }
            int sourceWidth = icon.getIntrinsicWidth();
            int sourceHeight = icon.getIntrinsicHeight();

            if (sourceWidth > 0 && sourceHeight > 0) {
                // There are intrinsic sizes.
                if (width < sourceWidth || height < sourceHeight) {
                    // It's too big, scale it down.
                    final float ratio = (float) sourceWidth / sourceHeight;
                    if (sourceWidth > sourceHeight) {
                        height = (int) (width / ratio);
                    } else if (sourceHeight > sourceWidth) {
                        width = (int) (height * ratio);
                    }
                } else if (sourceWidth < width && sourceHeight < height) {
                    // It's small, use the size they gave us.
                    width = sourceWidth;
                    height = sourceHeight;
                }
            }


            // no intrinsic size --> use default size
            int textureWidth = sIconTextureWidth;
            int textureHeight = sIconTextureHeight;

            final Bitmap bitmap = Bitmap.createBitmap(textureWidth, textureHeight,
                    Bitmap.Config.ARGB_8888);
            final Canvas canvas = sCanvas;
            canvas.setBitmap(bitmap);

            final int left = (origWidth-width) / 2 + sIconFrameLSpace;
            final int top = (origHeight-height) / 2 + sIconFrameTSpace;

//            Drawable frameDrawable = info.getBackground();
            Drawable roundDrawable = null;
            final Resources resources = context.getResources();

            Drawable mask = info.getMask();
            if(mask != null){
                Bitmap bmp = getRoundedCornerBitmap(icon, mask);
                roundDrawable = new BitmapDrawable(bmp);
            }
//            if (frameDrawable == null) {
//                frameDrawable = resources.getDrawable(R.drawable.qingcheng_icon_background);
//            }
//
//            if (frameDrawable != null) {
//                sOldBounds.set(frameDrawable.getBounds());
//                frameDrawable.setBounds(0, 0, sIconWidth, sIconHeight);
//                frameDrawable.draw(canvas);
//                frameDrawable.setBounds(sOldBounds);
//            }
//
            //modify by zhangkun 2013-07-02 for crop application for current theme
            if(roundDrawable == null){
               roundDrawable = icon;
            }
            sOldBounds.set(roundDrawable.getBounds());
            roundDrawable.setBounds(left, top, left+width, top+height);
            roundDrawable.draw(canvas);
            roundDrawable.setBounds(sOldBounds);
            //end modify
            return bitmap;
        }
    }

    private static Bitmap createIconBitmapV2(Drawable icon, Context context, ThemeInfo info){
        synchronized (sCanvas) { // we share the statics :-(
            if (sIconWidth == -1) {
                initStatics(context);
            }

            Drawable iconBg = info.getBackground();
            Drawable iconMask = info.getMask();

            // no intrinsic size --> use default size
            int textureWidth = sIconTextureWidth;
            int textureHeight = sIconTextureHeight;
            int width = iconMask.getIntrinsicWidth();
            int height = iconMask.getIntrinsicHeight();

            int topOffset = 0;
            Drawable topOffsetDrawable = info.getTopOffset();
            if(topOffsetDrawable != null){
                topOffset = topOffsetDrawable.getIntrinsicHeight();
            }

            final Bitmap bitmap = Bitmap.createBitmap(textureWidth, textureHeight,
                    Bitmap.Config.ARGB_8888);
            final Canvas canvas = sCanvas;
            canvas.setBitmap(bitmap);

            sOldBounds.set(iconBg.getBounds());
            iconBg.setBounds(0, 0, sIconWidth, sIconHeight);
            iconBg.draw(canvas);
            iconBg.setBounds(sOldBounds);

            //sunfeng modfiy     @20150812 start:
            int left = (textureWidth-width) / 2;
            int top = (textureHeight-height) / 2 - topOffset;
            if(left < 0 ){
            	left = 0;
            }
            if(top < 0 ){
            	top = 0;
            }
            if(topOffset < 0 ){
            	topOffset = 0;
            }
            Bitmap masked = createBitmapWithMaskV2(icon, context, iconMask);

            try {
            	int mMaskW = masked.getHeight();
				if(masked.getWidth() > sIconWidth){
					masked = getSmallBitmap(masked);
				}
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

            canvas.drawBitmap(masked, left, top, new Paint(Paint.FILTER_BITMAP_FLAG));
            return bitmap;
        }
    }
    
    private static Bitmap getSmallBitmap(Bitmap bitmap){
    	Matrix matrix =new Matrix();
    	float scale = (float)((float) sIconWidth /(float) bitmap.getWidth());
//    	Log.i("sunfeng","  ==== scale "+ scale +" w:"+ bitmap.getWidth()+" sIconWidth:"+sIconWidth+"  sIconTextureWidth:"+sIconTextureWidth);
    	matrix.postScale(scale, scale);
    	return Bitmap.createBitmap(bitmap , 0, 0, bitmap.getWidth(), bitmap.getHeight(),matrix,true);
//    	return bitmap;
    }
    

    private static Bitmap createBitmapWithMaskV2(Drawable icon, Context context, Drawable mask){
        synchronized (sCanvas) {
            int width = mask.getIntrinsicWidth();
            int height = mask.getIntrinsicHeight();
            if (icon instanceof PaintDrawable) {
                PaintDrawable painter = (PaintDrawable) icon;
                painter.setIntrinsicWidth(width);
                painter.setIntrinsicHeight(height);
            } else if (icon instanceof BitmapDrawable) {
                // Ensure the bitmap has a density.
                BitmapDrawable bitmapDrawable = (BitmapDrawable) icon;
                Bitmap bitmap = bitmapDrawable.getBitmap();
                /*Removed by someone not add history
                if (bitmap.getDensity() == Bitmap.DENSITY_NONE) {
                    bitmapDrawable.setTargetDensity(context.getResources().getDisplayMetrics());
                }
                */
                bitmapDrawable.setTargetDensity(context.getResources().getDisplayMetrics());
            }

            int sourceWidth = icon.getIntrinsicWidth();
            int sourceHeight = icon.getIntrinsicHeight();
            if (sourceWidth > 0 && sourceHeight > 0) {
                // There are intrinsic sizes.
            	// Deleted for MyUI---20150721
               if (width < sourceWidth || height < sourceHeight) {
                    // It's too big, scale it down.
                    final float ratio = (float) sourceWidth / sourceHeight;
                    if (sourceWidth > sourceHeight) {
                        height = (int) (width / ratio);
                    } else if (sourceHeight > sourceWidth) {
                        width = (int) (height * ratio);
                    }
                }
            }
//            w: 96 h:96  mask w:168 h:168 w:168 h:168
//            w: 96 h:96  mask w:168 h:168
//            Log.i("sunfeng","createBitmapWithMaskV2 ==w: " +icon.getIntrinsicWidth()+" h:"+icon.getIntrinsicHeight()+"  mask w:"+mask.getIntrinsicWidth()+" h:"+mask.getIntrinsicWidth()+" w:"+width+" h:"+height);
            final Bitmap bitmap = Bitmap.createBitmap(width, height,
                    Bitmap.Config.ARGB_8888);
            final Canvas canvas = new Canvas();
            Paint paint = new Paint(Paint.FILTER_BITMAP_FLAG);
            paint.setAntiAlias(true);
            canvas.setBitmap(bitmap);
            canvas.drawBitmap(((BitmapDrawable)mask).getBitmap(), 0, 0, paint);
            paint.setXfermode(new PorterDuffXfermode(Mode.SRC_OUT));
            Rect src = new Rect(0,0,sourceWidth, sourceHeight);
            Rect dst = new Rect(0, 0, width, height);
//	           if(icon.getIntrinsicWidth()<mask.getIntrinsicWidth()){
//	        	   
//	           }
            canvas.drawBitmap(((BitmapDrawable)icon).getBitmap(), src, dst, paint);
            canvas.setBitmap(null);
            return bitmap;
        }
    }

 //sunfeng modfiy     @20150812 end:
    /**
     * Returns a Bitmap representing the thumbnail of the specified Bitmap.
     *
     * @param bitmap The bitmap to get a thumbnail of.
     * @param context The application's context.
     *
     * @return A thumbnail for the specified bitmap or the bitmap itself if the
     *         thumbnail could not be created.
     */
    static Bitmap resampleIconBitmap(Bitmap bitmap, Context context) {
        synchronized (sCanvas) { // we share the statics :-(
            if (sIconWidth == -1) {
                initStatics(context);
            }

            if (bitmap.getWidth() == sIconWidth && bitmap.getHeight() == sIconHeight) {
                return bitmap;
            } else {
                final Resources resources = context.getResources();
                return createIconBitmap(new BitmapDrawable(resources, bitmap), context);
            }
        }
    }

    /**
     * Given a coordinate relative to the descendant, find the coordinate in a parent view's
     * coordinates.
     *
     * @param descendant The descendant to which the passed coordinate is relative.
     * @param root The root view to make the coordinates relative to.
     * @param coord The coordinate that we want mapped.
     * @param includeRootScroll Whether or not to account for the scroll of the descendant:
     *          sometimes this is relevant as in a child's coordinates within the descendant.
     * @return The factor by which this descendant is scaled relative to this DragLayer. Caution
     *         this scale factor is assumed to be equal in X and Y, and so if at any point this
     *         assumption fails, we will need to return a pair of scale factors.
     */
    public static float getDescendantCoordRelativeToParent(View descendant, View root,
                                                           int[] coord, boolean includeRootScroll) {
        ArrayList<View> ancestorChain = new ArrayList<View>();

        float[] pt = {coord[0], coord[1]};

        View v = descendant;
        while(v != root && v != null) {
            ancestorChain.add(v);
            v = (View) v.getParent();
        }
        ancestorChain.add(root);

        float scale = 1.0f;
        int count = ancestorChain.size();
        for (int i = 0; i < count; i++) {
            View v0 = ancestorChain.get(i);
            // For TextViews, scroll has a meaning which relates to the text position
            // which is very strange... ignore the scroll.
            if (v0 != descendant || includeRootScroll) {
                pt[0] -= v0.getScrollX();
                pt[1] -= v0.getScrollY();
            }

            v0.getMatrix().mapPoints(pt);
            pt[0] += v0.getLeft();
            pt[1] += v0.getTop();
            scale *= v0.getScaleX();
        }

        coord[0] = (int) Math.round(pt[0]);
        coord[1] = (int) Math.round(pt[1]);
        return scale;
    }

    /**
     * Inverse of {@link #getDescendantCoordRelativeToSelf(View, int[])}.
     */
    public static float mapCoordInSelfToDescendent(View descendant, View root,
                                                   int[] coord) {
        ArrayList<View> ancestorChain = new ArrayList<View>();

        float[] pt = {coord[0], coord[1]};

        View v = descendant;
        while(v != root) {
            ancestorChain.add(v);
            v = (View) v.getParent();
        }
        ancestorChain.add(root);

        float scale = 1.0f;
        Matrix inverse = new Matrix();
        int count = ancestorChain.size();
        for (int i = count - 1; i >= 0; i--) {
            View ancestor = ancestorChain.get(i);
            View next = i > 0 ? ancestorChain.get(i-1) : null;

            pt[0] += ancestor.getScrollX();
            pt[1] += ancestor.getScrollY();

            if (next != null) {
                pt[0] -= next.getLeft();
                pt[1] -= next.getTop();
                next.getMatrix().invert(inverse);
                inverse.mapPoints(pt);
                scale *= next.getScaleX();
            }
        }

        coord[0] = (int) Math.round(pt[0]);
        coord[1] = (int) Math.round(pt[1]);
        return scale;
    }

    /**
     * Utility method to determine whether the given point, in local coordinates,
     * is inside the view, where the area of the view is expanded by the slop factor.
     * This method is called while processing touch-move events to determine if the event
     * is still within the view.
     */
    public static boolean pointInView(View v, float localX, float localY, float slop) {
        return localX >= -slop && localY >= -slop && localX < (v.getWidth() + slop) &&
                localY < (v.getHeight() + slop);
    }

    /// M: Change to public for smart book feature.
    public static void initStatics(Context context) {
        final Resources resources = context.getResources();
        sIconWidth = sIconHeight = (int) resources.getDimension(R.dimen.app_icon_size);
        sIconTextureWidth = sIconTextureHeight = sIconWidth;
        
        sIconFrameTSpace = (int) resources.getDimension(R.dimen.icon_frame_tspace);
        sIconFrameBSpace = (int) resources.getDimension(R.dimen.icon_frame_bspace);
        sIconFrameLSpace = (int) resources.getDimension(R.dimen.icon_frame_lspace);
        sIconFrameRSpace = (int) resources.getDimension(R.dimen.icon_frame_rspace);
        
    }

    public static void setIconSize(int widthPx) {
        sIconWidth = sIconHeight = widthPx;
        sIconTextureWidth = sIconTextureHeight = widthPx;
    }

    public static void scaleRect(Rect r, float scale) {
        if (scale != 1.0f) {
            r.left = (int) (r.left * scale + 0.5f);
            r.top = (int) (r.top * scale + 0.5f);
            r.right = (int) (r.right * scale + 0.5f);
            r.bottom = (int) (r.bottom * scale + 0.5f);
        }
    }

    public static int[] getCenterDeltaInScreenSpace(View v0, View v1, int[] delta) {
        v0.getLocationInWindow(sLoc0);
        v1.getLocationInWindow(sLoc1);

        sLoc0[0] += (v0.getMeasuredWidth() * v0.getScaleX()) / 2;
        sLoc0[1] += (v0.getMeasuredHeight() * v0.getScaleY()) / 2;
        sLoc1[0] += (v1.getMeasuredWidth() * v1.getScaleX()) / 2;
        sLoc1[1] += (v1.getMeasuredHeight() * v1.getScaleY()) / 2;

        if (delta == null) {
            delta = new int[2];
        }

        delta[0] = sLoc1[0] - sLoc0[0];
        delta[1] = sLoc1[1] - sLoc0[1];

        return delta;
    }

    public static void scaleRectAboutCenter(Rect r, float scale) {
        int cx = r.centerX();
        int cy = r.centerY();
        r.offset(-cx, -cy);
        Utilities.scaleRect(r, scale);
        r.offset(cx, cy);
    }

    public static void startActivityForResultSafely(
            Activity activity, Intent intent, int requestCode) {
        try {
            activity.startActivityForResult(intent, requestCode);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(activity, R.string.activity_not_found, Toast.LENGTH_SHORT).show();
        } catch (SecurityException e) {
            Toast.makeText(activity, R.string.activity_not_found, Toast.LENGTH_SHORT).show();
            Log.e(TAG, "Launcher does not have the permission to launch " + intent +
                    ". Make sure to create a MAIN intent-filter for the corresponding activity " +
                    "or use the exported attribute for this activity.", e);
        }
    }

    static boolean isSystemApp(Context context, Intent intent) {
        PackageManager pm = context.getPackageManager();
        ComponentName cn = intent.getComponent();
        String packageName = null;
        if (cn == null) {
            ResolveInfo info = pm.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY);
            if ((info != null) && (info.activityInfo != null)) {
                packageName = info.activityInfo.packageName;
            }
        } else {
            packageName = cn.getPackageName();
        }
        if (packageName != null) {
            try {
                PackageInfo info = pm.getPackageInfo(packageName, 0);
                return (info != null) && (info.applicationInfo != null) &&
                        ((info.applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0);
            } catch (NameNotFoundException e) {
                return false;
            }
        } else {
            return false;
        }
    }

    /**
     * This picks a dominant color, looking for high-saturation, high-value, repeated hues.
     * @param bitmap The bitmap to scan
     * @param samples The approximate max number of samples to use.
     */
    static int findDominantColorByHue(Bitmap bitmap, int samples) {
        final int height = bitmap.getHeight();
        final int width = bitmap.getWidth();
        int sampleStride = (int) Math.sqrt((height * width) / samples);
        if (sampleStride < 1) {
            sampleStride = 1;
        }

        // This is an out-param, for getting the hsv values for an rgb
        float[] hsv = new float[3];

        // First get the best hue, by creating a histogram over 360 hue buckets,
        // where each pixel contributes a score weighted by saturation, value, and alpha.
        float[] hueScoreHistogram = new float[360];
        float highScore = -1;
        int bestHue = -1;

        for (int y = 0; y < height; y += sampleStride) {
            for (int x = 0; x < width; x += sampleStride) {
                int argb = bitmap.getPixel(x, y);
                int alpha = 0xFF & (argb >> 24);
                if (alpha < 0x80) {
                    // Drop mostly-transparent pixels.
                    continue;
                }
                // Remove the alpha channel.
                int rgb = argb | 0xFF000000;
                Color.colorToHSV(rgb, hsv);
                // Bucket colors by the 360 integer hues.
                int hue = (int) hsv[0];
                if (hue < 0 || hue >= hueScoreHistogram.length) {
                    // Defensively avoid array bounds violations.
                    continue;
                }
                float score = hsv[1] * hsv[2];
                hueScoreHistogram[hue] += score;
                if (hueScoreHistogram[hue] > highScore) {
                    highScore = hueScoreHistogram[hue];
                    bestHue = hue;
                }
            }
        }

        SparseArray<Float> rgbScores = new SparseArray<Float>();
        int bestColor = 0xff000000;
        highScore = -1;
        // Go back over the RGB colors that match the winning hue,
        // creating a histogram of weighted s*v scores, for up to 100*100 [s,v] buckets.
        // The highest-scoring RGB color wins.
        for (int y = 0; y < height; y += sampleStride) {
            for (int x = 0; x < width; x += sampleStride) {
                int rgb = bitmap.getPixel(x, y) | 0xff000000;
                Color.colorToHSV(rgb, hsv);
                int hue = (int) hsv[0];
                if (hue == bestHue) {
                    float s = hsv[1];
                    float v = hsv[2];
                    int bucket = (int) (s * 100) + (int) (v * 10000);
                    // Score by cumulative saturation * value.
                    float score = s * v;
                    Float oldTotal = rgbScores.get(bucket);
                    float newTotal = oldTotal == null ? score : oldTotal + score;
                    rgbScores.put(bucket, newTotal);
                    if (newTotal > highScore) {
                        highScore = newTotal;
                        // All the colors in the winning bucket are very similar. Last in wins.
                        bestColor = rgb;
                    }
                }
            }
        }
        return bestColor;
    }

    /*
     * Finds a system apk which had a broadcast receiver listening to a particular action.
     * @param action intent action used to find the apk
     * @return a pair of apk package name and the resources.
     */
    static Pair<String, Resources> findSystemApk(String action, PackageManager pm) {
        final Intent intent = new Intent(action);
        for (ResolveInfo info : pm.queryBroadcastReceivers(intent, 0)) {
            if (info.activityInfo != null &&
                    (info.activityInfo.applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0) {
                final String packageName = info.activityInfo.packageName;
                try {
                    final Resources res = pm.getResourcesForApplication(packageName);
                    return Pair.create(packageName, res);
                } catch (NameNotFoundException e) {
                    Log.w(TAG, "Failed to find resources for " + packageName);
                }
            }
        }
        return null;
    }

    /**
     * M: Check whether the given component name is enabled.
     *
     * @param context
     * @param cmpName
     * @return true if the component is in default or enable state, and the application is also in default or enable state,
     *         false if in disable or disable user state.
     */
    static boolean isComponentEnabled(final Context context, final ComponentName cmpName) {
        final String pkgName = cmpName.getPackageName();
        final PackageManager pm = context.getPackageManager();
        // Check whether the package has been uninstalled or the component already removed.
        ActivityInfo aInfo = null;
        try {
            aInfo = pm.getActivityInfo(cmpName, 0);
        } catch (NameNotFoundException e) {
            LauncherLog.w(TAG, "isComponentEnabled NameNotFoundException: pkgName = " + pkgName);
        }

        if (aInfo == null) {
            LauncherLog.d(TAG, "isComponentEnabled return false because component " + cmpName + " has been uninstalled!");
            return false;
        }

        final int pkgEnableState = pm.getApplicationEnabledSetting(pkgName);
        if (LauncherLog.DEBUG) {
            LauncherLog.d(TAG, "isComponentEnabled: cmpName = " + cmpName + ",pkgEnableState = " + pkgEnableState);
        }
        if (pkgEnableState == PackageManager.COMPONENT_ENABLED_STATE_DEFAULT
                || pkgEnableState == PackageManager.COMPONENT_ENABLED_STATE_ENABLED) {
            final int cmpEnableState = pm.getComponentEnabledSetting(cmpName);
            if (LauncherLog.DEBUG) {
                LauncherLog.d(TAG, "isComponentEnabled: cmpEnableState = " + cmpEnableState);
            }
            if (cmpEnableState == PackageManager.COMPONENT_ENABLED_STATE_DEFAULT
                    || cmpEnableState == PackageManager.COMPONENT_ENABLED_STATE_ENABLED) {
                return true;
            }
        }

        return false;
    }

    /**
     * M: The app is system app or not.
     *
     * @param info
     * @return
     */
    public static boolean isSystemApp(AppInfo info) {
        if (info == null) {
            return false;
        }
        return (info.flags & AppInfo.DOWNLOADED_FLAG) == 0;
    }

    public static float dp2px(Resources resources, float dp) {
        final float scale = resources.getDisplayMetrics().density;
        return  dp * scale + 0.5f;
    }

    public static float sp2px(Resources resources, float sp){
        final float scale = resources.getDisplayMetrics().scaledDensity;
        return sp * scale;
    }

	// Add for MyUI Jing.Wu 20151124 start
    public static boolean isAppInstalled(Context context, String packageName) {
    	if (packageName == null || packageName.isEmpty()) {
			return false;
		} else {
	    	//final PackageManager mPackageManager = context.getPackageManager();
			//List<PackageInfo> mPackageInfos = mPackageManager.getInstalledPackages(0);
			//boolean flag = false;
			//if (mPackageInfos!=null) {
			//	String tempName = null;
			//	for (int i = 0; i < mPackageInfos.size(); i++) {
			//		tempName = mPackageInfos.get(i).packageName;
			//		if (tempName!=null && tempName.equals(packageName)) {
			//			flag = true;
			//			break;
			//		}
			//	}
			//}
			//return flag;
			try {
				context.getPackageManager().getApplicationInfo(packageName, PackageManager.GET_UNINSTALLED_PACKAGES);
				return true;
			} catch (Exception e) {
				// TODO: handle exception
				e.printStackTrace();
				return false;
			}
		}
    }
    
    public static boolean isTopActivity(Context mContext, ComponentName component) {
    	if (mContext == null || component == null 
    			|| component.getPackageName() == null || component.getPackageName().isEmpty()
    	    	|| component.getClassName() == null || component.getClassName().isEmpty()) {
			return false;
		}
    	ActivityManager manager = (ActivityManager)mContext.getSystemService(Context.ACTIVITY_SERVICE);
    	List<RunningTaskInfo> mInfos = manager.getRunningTasks(1);
    	if (mInfos != null && !mInfos.isEmpty() && mInfos.size()>0) {
			if (QCLog.DEBUG) {
				QCLog.d(TAG, "Top activity is: "+mInfos.get(0).topActivity.getPackageName()+"/"+mInfos.get(0).topActivity.getClassName());
			}
			if (mInfos.get(0).topActivity.getPackageName().equals(component.getPackageName())
					|| mInfos.get(0).topActivity.getClassName().equals(component.getClassName())) {
				return true;
			}
		}
    	return false;
    }
    
    public static boolean checkActivityNumbInList(Context mContext, ComponentName component) {
    	if (mContext == null || component == null 
    			|| component.getPackageName() == null || component.getPackageName().isEmpty()
    	    	|| component.getClassName() == null || component.getClassName().isEmpty()) {
			return false;
		}
    	ActivityManager manager = (ActivityManager)mContext.getSystemService(Context.ACTIVITY_SERVICE);
    	List<RunningTaskInfo> mInfos = manager.getRunningTasks(200);
    	if (mInfos != null && !mInfos.isEmpty() && mInfos.size()>0) {
    		boolean result = false;
    		for (int i = 0; i < mInfos.size(); i++) {
				RunningTaskInfo mInfo = mInfos.get(i);
				if (mInfo.baseActivity!=null 
						&& mInfo.baseActivity.getPackageName().equals(component.getPackageName())
						&& mInfo.baseActivity.getClassName().equals(component.getClassName())
						&& mInfo.numActivities<2) {
					result = true;
					break;
				}
			}
    		return result;
		}
    	return false;
    }
    
    public static long fileLength(File file) {
    	try {
    		if (file.exists()) {
				if (file.isFile()) {
					return getFileSize(file);
				} else if (file.isDirectory()) {
					File[] files = file.listFiles();
					long size = 0;
					for (int i = 0; i < files.length; i++) {
						size = size+fileLength(files[i]);
					}
					return size;
				}
			}
		} catch (Exception e) {
			// TODO: handle exception
			return 0;
		}
    	return 0;
    }
    private static long getFileSize(File file) {
    	long size = 0;
    	FileInputStream mFileInputStream = null;
    	try {
			if (file.exists() && file.isFile()) {
				mFileInputStream = new FileInputStream(file);
				size = Math.max(file.length(), mFileInputStream.available());
				mFileInputStream.close();
				mFileInputStream = null;
			}
		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
		} finally {
			if (mFileInputStream != null) {
				try {
					mFileInputStream.close();
					mFileInputStream = null;
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
    	return size;
    }
    
    private static boolean deleteFile(File file) {
    	try {
    		if (file.exists()) {
    			if (file.isFile()) {
    				return file.delete();
    			} else if (file.isDirectory()) {
					File[] files = file.listFiles();
					boolean b = true;
					for (int i = 0; i < files.length; i++) {
						b = b & deleteFile(files[i]);
					}
					return b;
    			}
    		}
		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
			return false;
		}
    	return false;
    }
    
    public static boolean getPathReadble(String path) {
    	try {
			File mFile = new File(path);
			if (mFile.canRead() && mFile.canWrite()) {
				return true;
			}
		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
		}
    	return false;
    }
	// Add for MyUI Jing.Wu 20151124 end


    public static final boolean ATLEAST_JB_MR1 =
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1;
}
