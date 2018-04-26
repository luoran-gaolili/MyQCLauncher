package com.qingcheng.home.projector;

/**
 * Created by zhuzi on 17-5-12.
 */

public class PathPoint {

    public static final int MOVE=0;
    public static final int LINE=1;

    public static final int SECOND_CURVE =2;

    public static final int THIRD_CURVE=3;

    public float mX,mY;

    public float mContorl0X,mContorl0Y;
    public float mContorl1X,mContorl1Y;

    public int mOperation;


    public PathPoint(int mOperation,float mX, float mY ) {
        this.mX = mX;
        this.mY = mY;
        this.mOperation = mOperation;
    }

    /**
     * @param mX
     * @param mY
     * @param mContorl0X
     * @param mContorl0Y
     */
    public PathPoint(float mContorl0X, float mContorl0Y,float mX, float mY) {
        this.mX = mX;
        this.mY = mY;
        this.mContorl0X = mContorl0X;
        this.mContorl0Y = mContorl0Y;
        this.mOperation = SECOND_CURVE;
    }


    public PathPoint(float mContorl0x, float mContorl0Y, float mContorl1x, float mContorl1Y,float mX, float mY) {
        this.mX = mX;
        this.mY = mY;
        this.mContorl0X = mContorl0x;
        this.mContorl0Y = mContorl0Y;
        this.mContorl1X = mContorl1x;
        this.mContorl1Y = mContorl1Y;
        this.mOperation = THIRD_CURVE;
    }


    public static PathPoint moveTo(float x, float y){
        return new PathPoint(MOVE,x,y);
    }
    public static PathPoint lineTo(float x,float y){
        return  new PathPoint(LINE,x,y);
    }
    public static PathPoint secondBesselCurveTo(float c0X, float c0Y,float x,float y){
        return new PathPoint(c0X,c0Y,x,y);
    }
    public static PathPoint thirdBesselCurveTo(float c0X, float c0Y, float c1X, float c1Y, float x, float y){
        return new PathPoint(c0X,c0Y,c1X,c1Y,x,y);
    }
}
