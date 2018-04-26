package com.qingcheng.home.custom.binder;

import com.qingcheng.home.AppInfo;
import com.qingcheng.home.AppInfoMarket;
import com.qingcheng.home.BubbleTextView;
import com.qingcheng.home.Launcher;
import com.qingcheng.home.LauncherAppState;
import com.qingcheng.home.LauncherModel;
import com.qingcheng.home.LauncherSettings;
import com.qingcheng.home.R;
import com.qingcheng.home.custom.DataBindAdapter;
import com.qingcheng.home.custom.DataBinder;
import com.qingcheng.home.custom.DividerItemDecoration;
import com.qingcheng.home.custom.ItemData;
import com.qingcheng.home.custom.QuicksearhData;
import com.qingcheng.home.custom.RecommendAppsData;
import com.qingcheng.home.custom.ViewItemType;


import android.content.ComponentName;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v7.widget.CardView;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class RecommendAppsBinder extends DataBinder<RecommendAppsBinder.ViewHolder> {

    private static final int MAX_APP_ITEMS = 5;
    private static final String TAG = "Launcher.AppsBinder";

    private ItemData mData = new QuicksearhData();

    private static LayoutInflater mInflater;
    private ViewHolder mHolder;
    private AppInfoMarket mAppInfoMarket = null;

    ContentObserver mContentObserver = new ContentObserver(new Handler()) {
        public void onChange(boolean selfChange, Uri uri) {
            initAppsViews();
        }
    };

    public RecommendAppsBinder(DataBindAdapter dataBindAdapter) {
        super(dataBindAdapter);
    }

    @Override
    public ViewHolder newViewHolder(ViewGroup parent) {
        View view = LayoutInflater.from(parent.getContext()).inflate(
                R.layout.widgets_apps, parent, false);
        mInflater = LayoutInflater.from(parent.getContext());
        return new ViewHolder(view);
    }

    @Override
    public void bindViewHolder(ViewHolder holder, int position) {
        mHolder = holder;
        LauncherAppState.getInstance().getContext().getContentResolver().registerContentObserver(LauncherSettings.APPS.APPS_URI, true,
                mContentObserver);
    }

    @Override
    public int getItemCount() {
        return 1;
    }

    @Override
    public int getItemViewType() {
        return ViewItemType.VIEW_ITEM_RECOMMENDAPPS;
    }

    @Override
    public void destroy() {
        LauncherAppState.getInstance().getContext().getContentResolver().unregisterContentObserver(mContentObserver);
    }

    @Override
    public void addData(ItemData data) {
        if(mHolder == null || mHolder.mAppAdapter == null){
            return;
        }

        if(mHolder.mAppAdapter.getItemCount() > 0){
            return;
        }
        initAppsViews();
    }

    @Override
    public void updateData(ItemData data) {
//        updateApps(((RecommendAppsData)data).mList);
//        notifyBinderItemChanged(0);//only 1 item
    }

    @Override
    public void removeData(ItemData data) {
//        removeApps(((RecommendAppsData)data).mList);
//        notifyBinderItemChanged(0);//only 1 item
    }

    public void clear() {
        mData.clear();
        notifyBinderDataSetChanged();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private RecyclerView mAppRecyclerView;
        private LinearLayoutManager mAppLayoutManager;
        private AppAdapter mAppAdapter;

        public ViewHolder(View view) {
            super(view);
            mAppRecyclerView = (RecyclerView)view.findViewById(R.id.apps_holder_view);
            mAppLayoutManager = new LinearLayoutManager(view.getContext());
            mAppLayoutManager.setOrientation(LinearLayoutManager.HORIZONTAL);
            mAppRecyclerView.setLayoutManager(mAppLayoutManager);
            mAppRecyclerView.addItemDecoration(new DividerItemDecoration(view.getContext(), DividerItemDecoration.HORIZONTAL_LIST));
            if (mAppAdapter == null) {
                mAppAdapter = new AppAdapter();
            }
            mAppRecyclerView.setAdapter(mAppAdapter);

            Context context = view.getContext();
            if (context.getSharedPreferences(LauncherAppState.NAME_CUSTOM_SHARE, Context.MODE_PRIVATE).getBoolean(LauncherAppState.KEY_IS_PROJECTION_WALLPAPER, true)) {
                ((CardView)view.findViewById(R.id.custom_card)).setCardBackgroundColor(context.getColor(R.color.card_bg_project));
                view.findViewById(R.id.custom_ll_title).setBackgroundColor(context.getColor(R.color.widget_title_bg_project));
                ((TextView)view.findViewById(R.id.custom_tv_title)).setTextColor(context.getColor(R.color.widget_title_textcolor_project));
            } else {
                ((CardView)view.findViewById(R.id.custom_card)).setCardBackgroundColor(context.getColor(R.color.card_bg));
                view.findViewById(R.id.custom_ll_title).setBackgroundColor(context.getColor(R.color.widget_title_bg));
                ((TextView)view.findViewById(R.id.custom_tv_title)).setTextColor(context.getColor(R.color.widget_title_textcolor));
            }
        }
    }


    private static class AppViewHolder extends RecyclerView.ViewHolder {
        public String mClassName;
        public String mPackageName;

        public AppViewHolder(View itemView) {
            super(itemView);
        }
    }

    public void initAppsViews() {
        if (mHolder == null) {
            return;
        }
        if (mHolder.mAppRecyclerView == null) {
            return;
        }

        LauncherAppState.getInstance().getItems().clear();

        //1. check apps db
        Cursor c = null;
        try {
            c = mHolder.mAppRecyclerView.getContext().getContentResolver().query(LauncherSettings.APPS.APPS_URI, new String[]{"distinct packagename", "classname"}, null, null, "_id desc");
            if (c != null) {
                while (c.moveToNext()) {
                    String packageName = c.getString(c.getColumnIndex(LauncherSettings.APPS.PACKAGENAME));
                    String classname = c.getString(c.getColumnIndex(LauncherSettings.APPS.CLASSNAME));
                    Log.d(TAG, "initAppsViews: packageName = " + packageName + " classname = " + classname);
                    AppInfo app = findAppByComponent(packageName, classname);
                    Log.d(TAG, "initAppsViews: app = " + app);
                    if (app != null) {
                        if (LauncherAppState.getInstance().getItems().size() < MAX_APP_ITEMS) {
                            if((("com.android.deskclock").equals(app.componentName.getPackageName()) && "com.android.deskclock.DeskClock".equals(app.componentName.getClassName()))
                                    ||(("com.greenorange.weather").equals(app.componentName.getPackageName()) && "com.greenorange.weather.StartActivity".equals(app.componentName.getClassName()))){
                                continue;
                            }
                            LauncherAppState.getInstance().getItems().add(app);
                        }
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "initAppsViews: query apps db error: " + e.toString());
        } finally {
            if (c != null) {
                c.close();
            }
        }

        Log.d(TAG, "initAppsViews: LauncherAppState.getInstance().getItems() size : " + LauncherAppState.getInstance().getItems().size());

        if (LauncherAppState.getInstance().getItems().size() < MAX_APP_ITEMS) {
            Collections.sort(LauncherAppState.getInstance().getApps(), LauncherModel.getAppNameComparator());

            PackageManager pm = mHolder.mAppRecyclerView.getContext().getPackageManager();
//        LauncherAppState.getInstance().getItems().clear();
            for (int i = 0; i < LauncherAppState.getInstance().getApps().size() - 1; i++) {
                AppInfo info = LauncherAppState.getInstance().getApps().get(i);

                if (info instanceof AppInfoMarket) {
//                if (LauncherAppState.getInstance().getItems().size() < MAX_APP_ITEMS) {
//                    if (mAppInfoMarket == null) {
//                        mAppInfoMarket = (AppInfoMarket) info;
//                        LauncherAppState.getInstance().getItems().add(info);
//                    }
//                }
                    continue;
                } else {
                    ResolveInfo resolveInfo = pm.resolveActivity(info.getIntent(), 0);
                    if (resolveInfo != null) {
                        if (/*(resolveInfo.activityInfo.applicationInfo.flags & android.content.pm.ApplicationInfo.FLAG_SYSTEM) != 0 ||*/
                                (("com.qingcheng.home").equals(info.componentName.getPackageName()) && "com.qingcheng.home.Launcher".equals(info.componentName.getClassName()))
                                        || (("com.android.deskclock").equals(info.componentName.getPackageName()) && "com.android.deskclock.DeskClock".equals(info.componentName.getClassName()))
                                        || (("com.greenorange.weather").equals(info.componentName.getPackageName()) && "com.greenorange.weather.StartActivity".equals(info.componentName.getClassName()))) {
                            continue;
                        }
                        if (LauncherAppState.getInstance().getItems().size() < MAX_APP_ITEMS) {
                            if(!LauncherAppState.getInstance().getItems().contains(info)){
                                LauncherAppState.getInstance().getItems().add(info);
                            }
                        }
                    }
                }
            }
        }

//        Log.d(TAG, "initAppsViews: items size = " + LauncherAppState.getInstance().getItems().size());
//        try {
//            if (LauncherAppState.getInstance().mMarketRecommed != null && LauncherAppState.getInstance().getItems().size() == MAX_APP_ITEMS) {
//                boolean needAdd = true;
//                for (AppInfo app : LauncherAppState.getInstance().getItems()) {
//                    if (app.getIntent().getComponent().getPackageName().equals(LauncherAppState.getInstance().mMarketRecommed.packageName)) {
//                        needAdd = false;
//                        break;
//                    }
//                }
//                if (needAdd) {
//                    LauncherAppState.getInstance().getItems().remove(MAX_APP_ITEMS - 1);
//                    LauncherAppState.getInstance().getItems().add(LauncherAppState.getInstance().mMarketRecommed);
//                }
//            }
//        } catch (Exception e) {
//            Log.e(TAG, "initAppsViews: init market app " + e.toString());
//        }
        mHolder.mAppAdapter.setItems(LauncherAppState.getInstance().getItems());
        mHolder.mAppAdapter.notifyDataSetChanged();
    }

    public void updateApps(ArrayList<AppInfo> list){
        removeAppsWithotInvalitate(list);
        addAppsWithoutInvalitate(list);
        initAppsViews();
    }

    private void addAppsWithoutInvalitate(ArrayList<AppInfo> list) {
        int count = list.size();
        for (int i = 0; i < count; i++) {
            AppInfo info = list.get(i);
            int index = Collections.binarySearch(LauncherAppState.getInstance().getApps(), info, LauncherModel.getAppNameComparator());

            if(index < 0){
                LauncherAppState.getInstance().getApps().add(-(index + 1), info);
            }
        }
    }

    public void removeApps(ArrayList<AppInfo> list){
        removeAppsWithotInvalitate(list);
        initAppsViews();
    }

    private void removeAppsWithotInvalitate(ArrayList<AppInfo> list) {
        int lenght = list.size();
        for (int i = 0; i < lenght; i++) {
            AppInfo info = list.get(i);

            if(info instanceof AppInfoMarket){
                continue;
            }

            int removeIndex = findAppByComponent(LauncherAppState.getInstance().getApps(), info);
            if(removeIndex  > -1){
                LauncherAppState.getInstance().getApps().remove(removeIndex);
            }
        }
    }

    private int findAppByComponent(ArrayList<AppInfo> list, AppInfo info) {
        ComponentName componentName = info.getIntent().getComponent();
        int lenght = list.size();
        for (int i = 0; i < lenght; i++) {
            AppInfo temp = list.get(i);

            if(temp instanceof AppInfoMarket){
                continue;
            }

            if(temp.getIntent().getComponent().equals(componentName)) {
                return i;
            }
        }
        return -1;
    }

    private static AppInfo findAppByComponent(AppInfoMarket info) {
        int lenght = LauncherAppState.getInstance().getApps().size();
        for (int i = 0; i < lenght; i++) {
            AppInfo temp = LauncherAppState.getInstance().getApps().get(i);

            if(temp instanceof AppInfoMarket){
                continue;
            }

            if(temp.getIntent().getComponent().getPackageName().equals(info.packageName) && temp.title.equals(info.title)) {
                return temp;
            }
        }
        return null;
    }

    private static AppInfo findAppByComponent(String packageName, String className) {
        int lenght = LauncherAppState.getInstance().getApps().size();
        for (int i = 0; i < lenght; i++) {
            AppInfo temp = LauncherAppState.getInstance().getApps().get(i);

            if(temp instanceof AppInfoMarket){
                continue;
            }

            if(temp.getIntent().getComponent().getPackageName().equals(packageName) && temp.getIntent().getComponent().getClassName().equals(className)) {
                return temp;
            }
        }
        return null;
    }

    private static class AppAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
        private ArrayList<AppInfo> mItems = new ArrayList<>();

        public void setItems(ArrayList<AppInfo> items) {
            mItems = items;
        }

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = mInflater.inflate(R.layout.widgets_app_icon, parent, false);
            View appIconView = view.findViewById(R.id.app_icon_view);
            appIconView.setOnClickListener((Launcher)appIconView.getContext());
            Context context = view.getContext();
            if (context.getSharedPreferences(LauncherAppState.NAME_CUSTOM_SHARE, Context.MODE_PRIVATE).getBoolean(LauncherAppState.KEY_IS_PROJECTION_WALLPAPER, true)) {
                ((TextView)view.findViewById(R.id.app_icon_view)).setTextColor(context.getColor(R.color.toutiao_list_item_project));
            } else {
                ((TextView)view.findViewById(R.id.app_icon_view)).setTextColor(context.getColor(R.color.toutiao_list_item));
            }
            return new AppViewHolder(view);
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
            AppInfo item = mItems.get(position);
            AppViewHolder appViewHolder = (AppViewHolder) holder;
            if (item instanceof AppInfoMarket) {
                //if the application is installed
                AppInfo appInfo = findAppByComponent((AppInfoMarket) item);
                if (appInfo != null) {// find the recommend market's app is installed
                    if (!appInfo.componentName.getClassName().equalsIgnoreCase(appViewHolder.mClassName)) {
                        appViewHolder.mClassName = appInfo.componentName.getClassName();
                        BubbleTextView textView = (BubbleTextView) appViewHolder.itemView.findViewById(R.id.app_icon_view);
                        textView.applyFromApplicationInfo(appInfo);
                        textView.setTag(appInfo);
                    }
                } else {// not found the recommend market's app
                    if (!((AppInfoMarket) item).packageName.equalsIgnoreCase(appViewHolder.mPackageName)) {
                        appViewHolder.mPackageName = ((AppInfoMarket) item).packageName;
                        BubbleTextView textView = (BubbleTextView) appViewHolder.itemView.findViewById(R.id.app_icon_view);
                        textView.applyFromApplicationInfo(item);
                        textView.setTag(item);
                    }
                }
            } else {
                if (!item.componentName.getClassName().equalsIgnoreCase(appViewHolder.mClassName)) {
                    appViewHolder.mClassName = item.componentName.getClassName();
                    BubbleTextView textView = (BubbleTextView) appViewHolder.itemView.findViewById(R.id.app_icon_view);
                    textView.applyFromApplicationInfo(item);
                    textView.setTag(item);
                }
            }
        }

        @Override
        public int getItemCount() {
            if (mItems == null) {
                return 0;
            } else {
                return mItems.size();
            }
        }
    }



}
