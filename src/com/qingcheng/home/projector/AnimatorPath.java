package com.qingcheng.home.projector;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Created by zhuzi on 17-5-12.
 */

public class AnimatorPath {
    private List<PathPoint> mPoints = new ArrayList<>();

    /**
     * @param x
     * @param y
     */
    public void moveTo(float x, float y) {
        mPoints.add(PathPoint.moveTo(x, y));
    }

    /**
     * @param x
     * @param y
     */
    public void lineTo(float x, float y) {
        mPoints.add(PathPoint.lineTo(x, y));
    }

    /**
     * @param c0X
     * @param c0Y
     * @param x
     * @param y
     */
    public void secondBesselCurveTo(float c0X, float c0Y, float x, float y) {
        mPoints.add(PathPoint.secondBesselCurveTo(c0X, c0Y, x, y));
    }

    /**
     * @param c0X
     * @param c0Y
     * @param c1X
     * @param c1Y
     * @param x
     * @param y
     */
    public void thirdBesselCurveTo(float c0X, float c0Y, float c1X, float c1Y, float x, float y) {
        mPoints.add(PathPoint.thirdBesselCurveTo(c0X, c0Y, c1X, c1Y, x, y));
    }

    public Collection<PathPoint> getPoints() {
        return mPoints;
    }
}
