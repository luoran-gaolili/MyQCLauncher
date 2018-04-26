package com.qingcheng.home.custom.binder;

import android.appwidget.AppWidgetHost;
import android.appwidget.AppWidgetHostView;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProviderInfo;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.qingcheng.home.Launcher;
import com.qingcheng.home.LauncherAppState;
import com.qingcheng.home.R;
import com.qingcheng.home.custom.DataBindAdapter;
import com.qingcheng.home.custom.DataBinder;
import com.qingcheng.home.custom.ItemData;
import com.qingcheng.home.custom.ViewItemType;

import java.util.List;


public class IntelcardsBinder extends DataBinder<IntelcardsBinder.ViewHolder> {

    private static final String TAG = "IntelcardsBinder";
//    private AppWidgetHost mAppWidgetHost = null;
    AppWidgetManager appWidgetManager = null;
    private static final int HOST_ID = 1024;
    private Context mContext;

    private AppWidgetHostView hostView;
    private boolean isFromShowItem = false;
    private int mWidgetId;

    public IntelcardsBinder(Context context, DataBindAdapter dataBindAdapter) {
        super(dataBindAdapter);
        mContext = context;
    }

    public IntelcardsBinder(Context context, DataBindAdapter dataBindAdapter, boolean isFromShowItem) {
        super(dataBindAdapter);
        mContext = context;
        this.isFromShowItem = isFromShowItem;
    }

    @Override
    public ViewHolder newViewHolder(ViewGroup parent) {
        View view = LayoutInflater.from(parent.getContext()).inflate(
                R.layout.widgets_intelcards, parent, false);

        mContext = parent.getContext();
        ViewHolder holder = new ViewHolder(view);
        addWidgetView(holder);
        return holder;
    }

    @Override
    public void bindViewHolder(final ViewHolder holder, int position) {
        if (isFromShowItem) {
            holder.mWidgetContainer.removeAllViews();
            addWidgetView(holder);
            isFromShowItem = false;
        }
    }

    public int getmWidgetId(){
        return mWidgetId;
    }

    private void addWidgetView(ViewHolder holder) {
        //其参数hostid大意是指定该AppWidgetHost 即本Activity的标记Id， 直接设置为一个整数值吧 。
//        mAppWidgetHost = new AppWidgetHost(mContext, HOST_ID);
        //为了保证AppWidget的及时更新 ， 必须在Activity的onCreate/onStar方法调用该方法
        // 当然可以在onStop方法中，调用mAppWidgetHost.stopListenering() 停止AppWidget更新
//        mAppWidgetHost.startListening();
        //获得AppWidgetManager对象
        appWidgetManager = AppWidgetManager.getInstance(mContext);

        ComponentName componentName = new ComponentName("com.ragentek.intelcards", "com.ragentek.intelcards.provider.CardWidgetProvider");
        AppWidgetProviderInfo appWidgetProviderInfo = findAppWidgetProviderInfoWithComponent(mContext, componentName);
        mWidgetId = ((Launcher)mContext).getAppWidgetHost().allocateAppWidgetId();
        Bundle options = null;
        boolean success = appWidgetManager.bindAppWidgetIdIfAllowed(
                mWidgetId, componentName, options);
        Log.i(TAG,"success:"+success);
        Log.i(TAG,"mWidgetID:"+mWidgetId);
        hostView = ((Launcher)mContext).getAppWidgetHost().createView(mContext, mWidgetId, appWidgetProviderInfo);
        //设置长宽  appWidgetProviderInfo 对象的 minWidth 和  minHeight 属性
        RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        //添加至LinearLayout父视图中
        holder.mWidgetContainer.addView(hostView, layoutParams);
    }

    @Override
    public int getItemCount() {
        return 1;
    }

    @Override
    public int getItemViewType() {
        return ViewItemType.VIEW_ITEM_INTELCARDS_WIDGET;
    }

    @Override
    public void destroy() {

    }

    @Override
    public void addData(ItemData data) {
    }

    @Override
    public void updateData(ItemData data) {
    }

    @Override
    public void removeData(ItemData data) {
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private LinearLayout mWidgetContainer;

        public ViewHolder(View view) {
            super(view);
            mWidgetContainer = (LinearLayout) view.findViewById(R.id.intelcard_widget_container);

            Context context = view.getContext();
            boolean isProject = context.getSharedPreferences(LauncherAppState.NAME_CUSTOM_SHARE, Context.MODE_PRIVATE).getBoolean(LauncherAppState.KEY_IS_PROJECTION_WALLPAPER, true);
            if (isProject) {
                ((CardView)view.findViewById(R.id.custom_card)).setCardBackgroundColor(context.getColor(R.color.card_bg_project));
                view.findViewById(R.id.custom_ll_title).setBackgroundColor(context.getColor(R.color.widget_title_bg_project));
                ((TextView)view.findViewById(R.id.widget_books_title)).setTextColor(context.getColor(R.color.widget_title_textcolor_project));
            } else {
                ((CardView)view.findViewById(R.id.custom_card)).setCardBackgroundColor(context.getColor(R.color.card_bg));
                view.findViewById(R.id.custom_ll_title).setBackgroundColor(context.getColor(R.color.widget_title_bg));
                ((TextView)view.findViewById(R.id.widget_books_title)).setTextColor(context.getColor(R.color.widget_title_textcolor));
            }

       }
    }

    /**
     * Attempts to find an AppWidgetProviderInfo that matches the given component.
     */
    static AppWidgetProviderInfo findAppWidgetProviderInfoWithComponent(Context context,
                                                                        ComponentName component) {
        List<AppWidgetProviderInfo> widgets =
                AppWidgetManager.getInstance(context).getInstalledProviders();
        for (AppWidgetProviderInfo info : widgets) {
            if (info.provider.equals(component)) {
                return info;
            }
        }
        return null;
    }
}
