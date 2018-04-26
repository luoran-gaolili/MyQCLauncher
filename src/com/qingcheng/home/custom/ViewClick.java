package com.qingcheng.home.custom;

import android.view.View;


/**
 * Created by Administrator on 2017/2/27.
 */

public interface ViewClick {
    void handleViewClick(View view);

    void addObserver(ObserverTouchEvent sdkNewsBinder);
}
