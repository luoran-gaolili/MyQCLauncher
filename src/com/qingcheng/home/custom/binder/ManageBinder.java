package com.qingcheng.home.custom.binder;

import android.appwidget.AppWidgetHost;
import android.appwidget.AppWidgetHostView;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProviderInfo;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.preference.PreferenceManager;
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
import com.qingcheng.home.custom.LauncherSettingActivity;
import com.qingcheng.home.custom.ViewItemType;

import java.util.List;

import static com.qingcheng.home.custom.ViewItemType.INTELCARDS_ORDER_SP;


public class ManageBinder extends DataBinder<ManageBinder.ViewHolder> {

    private static final String TAG = "ManageBinder";
    private Context mContext;

    public ManageBinder(Context context, DataBindAdapter dataBindAdapter) {
        super(dataBindAdapter);
        mContext = context;
    }

    @Override
    public ViewHolder newViewHolder(ViewGroup parent) {
        View view = LayoutInflater.from(parent.getContext()).inflate(
                R.layout.widgets_manage, parent, false);

        ViewHolder holder = new ViewHolder(view, mContext);
        return holder;
    }

    @Override
    public void bindViewHolder(final ViewHolder holder, int position) {
    }

    @Override
    public int getItemCount() {
        return 1;
    }

    @Override
    public int getItemViewType() {
        return ViewItemType.VIEW_ITEM_MANAGE;
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
        private ImageView manageCards;

        public ViewHolder(View view, final Context context) {
            super(view);
            manageCards = (ImageView) view.findViewById(R.id.manage);

            manageCards.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    try {
                        Intent intent = new Intent(context, LauncherSettingActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        v.getContext().startActivity(intent);

                    } catch (Exception e) {
                        Log.e(TAG, "onClick: cannot find explicit activity " + e.getMessage());
                    }
                }
            });
        }
    }
}
