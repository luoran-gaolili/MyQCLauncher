package com.qingcheng.home.projector;

import android.animation.TypeEvaluator;

/**
 * Created by zhuzi on 17-5-12.
 */

public class PathEvaluator implements TypeEvaluator<PathPoint> {

    @Override
    public PathPoint evaluate(float t, PathPoint startValue, PathPoint endValue) {
        float x, y;
        float oneMiunsT = 1 - t;

        if (endValue.mOperation == PathPoint.THIRD_CURVE) {
            x = startValue.mX * oneMiunsT * oneMiunsT * oneMiunsT + 3 * endValue.mContorl0X * t * oneMiunsT * oneMiunsT + 3 * endValue.mContorl1X * t * t * oneMiunsT + endValue.mX * t * t * t;
            y = startValue.mY * oneMiunsT * oneMiunsT * oneMiunsT + 3 * endValue.mContorl0Y * t * oneMiunsT * oneMiunsT + 3 * endValue.mContorl1Y * t * t * oneMiunsT + endValue.mY * t * t * t;

        } else if (endValue.mOperation == PathPoint.SECOND_CURVE) {
            x = oneMiunsT * oneMiunsT * startValue.mX + 2 * t * oneMiunsT * endValue.mContorl0X + t * t * endValue.mX;
            y = oneMiunsT * oneMiunsT * startValue.mY + 2 * t * oneMiunsT * endValue.mContorl0Y + t * t * endValue.mY;

        } else if (endValue.mOperation == PathPoint.LINE) {

            x = startValue.mX + t * (endValue.mX - startValue.mX);
            y = startValue.mY + t * (endValue.mY - startValue.mY);
        } else {
            x = endValue.mX;
            y = endValue.mY;
        }
        return PathPoint.moveTo(x, y);
    }
}