package com.qingcheng.home.custom;

/**
 * Created by Administrator on 2017/2/28.
 */

public class ViewItemType {

    public static int VIEW_ITEM_DEFAULT = -1;
    public static final int VIEW_ITEM_QUICKSEARCH = 0;
    public static final int VIEW_ITEM_RECOMMENDAPPS = 1;
    //public static int VIEW_ITEM_MUSIC_WIDGET = 2;
    public static final int VIEW_ITEM_INTELCARDS_WIDGET=2;
    public static final int VIEW_ITEM_NEWS = 3;
    public static final int VIEW_ITEM_READER = 4;
    public static final int VIEW_ITEM_MANAGE = 5;
    public static int VIEW_ITEM_COUNTS = 6;

//    public static String MUSIC_WIDGET_PACKAGE_NAME = "cn.kuwo.player";
//    public static String MUSIC_WIDGET_CLASS_NAME = "cn.kuwo.ui.widget.SmallAppWidgetProvider";
    public static String INTELCARDS_WIDGET_CLASS_NAME="com.ragentek.intelcards";

    //SharedPreferenceSwitchName
    public static final String BINDER_SP_NAME="binderSwitchlist";
    public static final int SWITCH_COUNT=4;
    public static final String RECAPP_SP="switch_preference_recapp";
    public static final String INTELCARDS_SP="switch_preference_intelcards";
    public static final String NEWS_SP="switch_preference_news";
    public static final String READERS_SP="switch_preference_readers";
    public static final String [] SP_NAMES={"switch_preference_recapp","switch_preference_intelcards"
    ,"switch_preference_news","switch_preference_readers"};

    //SharedPreferenceOrderhName
    public static final String RECAPP_ODER_SP="order_preference_recapp";
    public static final String INTELCARDS_ORDER_SP="order_preference_intelcards";
    public static final String NEWS_ORDER_SP="order_preference_news";
    public static final String READERS_ORDER_SP="order_preference_readers";
}
