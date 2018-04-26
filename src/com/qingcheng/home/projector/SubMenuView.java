package com.qingcheng.home.projector;

import android.content.Context;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * Created by zhuzi on 17-5-12.
 */

public class SubMenuView extends TextView {

    private PathPoint point;

    public SubMenuView(Context context) {
        super(context);
    }

    public SubMenuView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public void setPoint(PathPoint point) {
        this.point = point;
        this.setTranslationX(point.mX);
        this.setTranslationY(point.mY);
        invalidate();
    }
    public PathPoint getPoint() {
       return point;
    }
}
