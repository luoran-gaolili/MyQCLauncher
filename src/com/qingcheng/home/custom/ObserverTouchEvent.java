package com.qingcheng.home.custom;

import android.view.MotionEvent;

/**
 * Created by Administrator on 2017/2/27.
 */
public interface ObserverTouchEvent {
    boolean interceptTouchEvent(MotionEvent ev);
}
