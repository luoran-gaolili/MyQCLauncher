/**
sunfeng add @20150802 for folder start

*/

package com.qingcheng.home.jni;

import android.graphics.Bitmap;
import android.util.Log;

public final class ImageUtils {
	static {
		try {
			Log.d("test", "ImageUtils System.loadLibrary()");
			System.loadLibrary("bitmaputils-jni");
		} catch (UnsatisfiedLinkError ule) {
			System.err.println("WARNING: Could not load library 12425242!");
		}
	}

	public static native void nativeToBlur(Bitmap paramBitmap1,
			Bitmap paramBitmap2, int paramInt);
}
