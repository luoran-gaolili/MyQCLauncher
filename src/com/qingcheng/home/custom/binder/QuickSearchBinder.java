package com.qingcheng.home.custom.binder;


import com.qingcheng.home.LauncherAppState;
import com.qingcheng.home.R;
import com.qingcheng.home.custom.DataBindAdapter;
import com.qingcheng.home.custom.DataBinder;
import com.qingcheng.home.custom.ItemData;
import com.qingcheng.home.custom.QuicksearhData;
import com.qingcheng.home.custom.ViewItemType;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

public class QuickSearchBinder extends DataBinder<QuickSearchBinder.ViewHolder> {
    private static final String TAG = "Launcher.QBinder";

    private ItemData mData = new QuicksearhData();

    public QuickSearchBinder(DataBindAdapter dataBindAdapter) {
        super(dataBindAdapter);
    }

    @Override
    public ViewHolder newViewHolder(ViewGroup parent) {
        View view = LayoutInflater.from(parent.getContext()).inflate(
                R.layout.widgets_searchbox, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void addData(ItemData data) {
        mData = data;
    }

    @Override
    public void updateData(ItemData data) {

    }

    @Override
    public void removeData(ItemData data) {

    }

    @Override
    public void bindViewHolder(ViewHolder holder, int position) {
        // TODO: 2017/2/23 setup view content (step 3)
    }

    @Override
    public int getItemCount() {
        return 1;
    }

    @Override
    public int getItemViewType() {
        return ViewItemType.VIEW_ITEM_QUICKSEARCH;
    }

    @Override
    public void destroy() {

    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        // TODO: 2017/2/23 setup find view by id,  prepare field for bindViewHolder  (step 1)
        public ViewHolder(final View view) {
            super(view);
            view.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    try {
                        Intent intent = new Intent();
                        intent.setClassName("com.android.quicksearchbox", "com.android.quicksearchbox.SearchActivity");
                        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        (view.getContext()).startActivity(intent);
                    }catch (Exception e){
                        Log.e(TAG, "onClick: cannot find explicit activity " + e.getMessage());
                        Intent intent = new Intent("android.search.action.GLOBAL_SEARCH");
//                        intent.setClassName("com.android.quicksearchbox", "com.android.quicksearchbox.SearchActivity");
                        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        (view.getContext()).startActivity(intent);
                    }
                }
            });
            // TODO: 2017/2/23  (step 2)
            Context context = view.getContext();
            if (context.getSharedPreferences(LauncherAppState.NAME_CUSTOM_SHARE, Context.MODE_PRIVATE).getBoolean(LauncherAppState.KEY_IS_PROJECTION_WALLPAPER, true)) {
                ((CardView)view.findViewById(R.id.custom_card)).setCardBackgroundColor(context.getColor(R.color.card_bg_project));
                Drawable drawable = context.getDrawable(R.drawable.ic_pi_search_white);
                drawable.setBounds(0, 0, drawable.getMinimumWidth(), drawable.getMinimumHeight());
                ((TextView)view.findViewById(R.id.widget_tv)).setCompoundDrawables(drawable, null, null, null);
            } else {
                ((CardView)view.findViewById(R.id.custom_card)).setCardBackgroundColor(context.getColor(R.color.card_bg));
                Drawable drawable = context.getDrawable(R.drawable.ic_pi_search);
                drawable.setBounds(0, 0, drawable.getMinimumWidth(), drawable.getMinimumHeight());
                ((TextView)view.findViewById(R.id.widget_tv)).setCompoundDrawables(drawable, null, null, null);
            }
        }
    }
}
