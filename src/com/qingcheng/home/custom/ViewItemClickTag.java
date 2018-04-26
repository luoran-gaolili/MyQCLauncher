package com.qingcheng.home.custom;

/**
 * Created by Administrator on 2017/2/28.
 */

public class ViewItemClickTag extends ViewItemType {
    public static String TAG_REMOVE_ITEM = "DeleteItem";
    public static String TAG_NEWS = "News";

    public String mTag = "";
    public int mViewItemType = VIEW_ITEM_DEFAULT;

    public ViewItemClickTag(int itemType, String tag) {
        mViewItemType = itemType;
        mTag = tag;
    }
}
