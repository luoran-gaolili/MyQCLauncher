package com.qingcheng.home;

import android.annotation.TargetApi;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import com.qingcheng.home.custom.DataBinder;
import com.qingcheng.home.custom.DividerItemDecoration;
import com.qingcheng.home.custom.ItemData;
import com.qingcheng.home.custom.LauncherSettingActivity;
import com.qingcheng.home.custom.NewsData;
import com.qingcheng.home.custom.ObserverTouchEvent;
import com.qingcheng.home.custom.QuicksearhData;
import com.qingcheng.home.custom.ReaderData;
import com.qingcheng.home.custom.RecommendAppsData;
import com.qingcheng.home.custom.ViewClick;
import com.qingcheng.home.custom.ViewItemClickTag;
import com.qingcheng.home.custom.ViewItemType;
import com.qingcheng.home.custom.adapter.ListAdapterImpl;
import com.qingcheng.home.custom.binder.IntelcardsBinder;

import java.util.ArrayList;
import java.util.List;

import static com.qingcheng.home.custom.ViewItemType.BINDER_SP_NAME;
import static com.qingcheng.home.custom.ViewItemType.INTELCARDS_SP;
import static com.qingcheng.home.custom.ViewItemType.NEWS_SP;
import static com.qingcheng.home.custom.ViewItemType.READERS_SP;
import static com.qingcheng.home.custom.ViewItemType.RECAPP_SP;
import static com.qingcheng.home.custom.ViewItemType.SP_NAMES;
import static com.qingcheng.home.custom.ViewItemType.VIEW_ITEM_INTELCARDS_WIDGET;
import static com.qingcheng.home.custom.ViewItemType.VIEW_ITEM_MANAGE;
import static com.qingcheng.home.custom.ViewItemType.VIEW_ITEM_NEWS;
import static com.qingcheng.home.custom.ViewItemType.VIEW_ITEM_QUICKSEARCH;
import static com.qingcheng.home.custom.ViewItemType.VIEW_ITEM_READER;
import static com.qingcheng.home.custom.ViewItemType.VIEW_ITEM_RECOMMENDAPPS;

public class RGKWidgetsRecyclerView extends LinearLayout implements ViewClick ,SharedPreferences.OnSharedPreferenceChangeListener{
    private static final String TAG = "Launcher.RGK";
    private static int ITEM_NUMBER = 5;
    private RecyclerView mRecyclerView;
    private ImageView mMangeImageView;

    private ListAdapterImpl mListAdapterImpl;
//    private EnumMapAdapterImpl mEnumMapAdapter;
    private LinearLayoutManager mLinearLayoutManager;
//    private RelativeLayout mFullScreenViewHolder;
//    private ObserverTouchEvent mSdkNewsBinder;
    private boolean isRemoveIntelcards=false;
    public static final Uri CONTENT_URI=Uri.parse("content://com.ragentek.intelcards/intelcards");
    private UpdateView mUpdateView;
    private SharedPreferences sp;
    public ContentObserver cob = new ContentObserver(new Handler()) {
        @Override
        public void onChange(boolean selfChange) {
            super.onChange(selfChange);
            isIntelcardsShow();
        }
    };

    public void isIntelcardsShow(){
        Cursor cursor = getContext().getContentResolver().query(CONTENT_URI, null,
                "show = 1", null, null);
        if(cursor==null){
            return;
        }
        Log.i(TAG,"intelcards db change,counts="+cursor.getCount());
        if(cursor.getCount()==0){
            isRemoveIntelcards=true;
            //delete widgetId
            Launcher mLauncher=(Launcher)mRecyclerView.getContext();
            final LauncherAppWidgetHost appWidgetHost = mLauncher.getAppWidgetHost();
            DataBinder dataBinder=mListAdapterImpl.getDataBinder(VIEW_ITEM_INTELCARDS_WIDGET);
            if(dataBinder!=null&&dataBinder.getItemViewType()==ViewItemType.VIEW_ITEM_INTELCARDS_WIDGET){
                IntelcardsBinder intelcardsBinder= (IntelcardsBinder) dataBinder;
                if(intelcardsBinder!=null){
                    int appWidgetId=intelcardsBinder.getmWidgetId();
                    Log.i(TAG,"appWidgetId:"+appWidgetId);
                    new AsyncTask<Void, Void, Void>() {
                        public Void doInBackground(Void... args) {
                            appWidgetHost.deleteAppWidgetId(appWidgetId);
                            return null;
                        }
                    }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, (Void) null);
                }
            }
            //remove widget from launcher
            removeViewItem(VIEW_ITEM_INTELCARDS_WIDGET);
        }else {
            isRemoveIntelcards=false;
            if(mUpdateView == null){
                mUpdateView = new UpdateView();
            }
            removeCallbacks(mUpdateView);
            postDelayed(mUpdateView, 3000);
        }
        cursor.close();
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        updateWidgetsWithSP(sharedPreferences,key);
    }

    public void updateAllWidgets(){
        for(int i=0;i<SP_NAMES.length;i++){
            updateWidgetsWithSP(sp,SP_NAMES[i]);
        }
    }

    public void updateWidgetsWithSP(SharedPreferences sharedPreferences,String key){
        if(RECAPP_SP.equals(key)){
            boolean recapp_switch = sharedPreferences.getBoolean(RECAPP_SP, true);
            if(recapp_switch==true){
                showViewItem(VIEW_ITEM_RECOMMENDAPPS);
            }else {
                removeViewItem(VIEW_ITEM_RECOMMENDAPPS);
            }
        }
        if(INTELCARDS_SP.equals(key)){
            boolean intelcards_switch = sharedPreferences.getBoolean(INTELCARDS_SP, true);
            if(intelcards_switch==true&&!isRemoveIntelcards){
                showViewItem(VIEW_ITEM_INTELCARDS_WIDGET);
            }else {
                removeViewItem(VIEW_ITEM_INTELCARDS_WIDGET);
            }
        }
        if(NEWS_SP.equals(key)){
            boolean news_switch = sharedPreferences.getBoolean(NEWS_SP, true);
            if(news_switch==true){
                showViewItem(VIEW_ITEM_NEWS);
            }else {
                removeViewItem(VIEW_ITEM_NEWS);
            }
        }
        if(READERS_SP.equals(key)){
            boolean readers_switch = sharedPreferences.getBoolean(READERS_SP, true);
            if(readers_switch==true){
                showViewItem(VIEW_ITEM_READER);
            }else {
                removeViewItem(VIEW_ITEM_READER);
            }
        }
    }

    class UpdateView implements Runnable{

        @Override
        public void run() {
            Cursor cursor = getContext().getContentResolver().query(CONTENT_URI, null,
                    "show = 1", null, null);
            if(cursor!=null&&cursor.getCount()>0){
                Log.i(TAG,"intelcards db change,counts="+cursor.getCount());
                if (sp.getBoolean(INTELCARDS_SP, true)==true) {
                    showViewItem(ViewItemType.VIEW_ITEM_INTELCARDS_WIDGET);
                }
            }
        }
    }
    public RGKWidgetsRecyclerView(Context context) {
        super(context);
    }

    public RGKWidgetsRecyclerView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public RGKWidgetsRecyclerView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public RGKWidgetsRecyclerView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        mRecyclerView = (RecyclerView) findViewById(R.id.recycler_view);
//        mFullScreenViewHolder = (RelativeLayout) findViewById(R.id.full_screen_view_holder);
        mListAdapterImpl = new ListAdapterImpl(getContext(),this);
//        mEnumMapAdapter = new EnumMapAdapterImpl(this);
        mRecyclerView.setAdapter(mListAdapterImpl);
        mLinearLayoutManager = new LinearLayoutManager(getContext());
        mLinearLayoutManager.setAutoMeasureEnabled(true);
        mRecyclerView.setLayoutManager(mLinearLayoutManager);
        mRecyclerView.addItemDecoration(new DividerItemDecoration(getContext(), DividerItemDecoration.VERTICAL_LIST));
//        mEnumMapAdapter.setViewClick(this);
        getContext().getContentResolver().registerContentObserver(CONTENT_URI, true, cob);
        sp = getContext().getSharedPreferences(BINDER_SP_NAME, Context.MODE_MULTI_PROCESS);
        sp.registerOnSharedPreferenceChangeListener(this);
        isIntelcardsShow();
        updateAllWidgets();
        mMangeImageView = (ImageView) findViewById(R.id.manage);
        mMangeImageView.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    Intent intent = new Intent(getContext(), LauncherSettingActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    v.getContext().startActivity(intent);

                } catch (Exception e) {
                    Log.e(TAG, "onClick: cannot find explicit activity " + e.getMessage());
                }
            }
        });
        mRecyclerView.addOnLayoutChangeListener(new OnLayoutChangeListener() {
            @Override
            public void onLayoutChange(View v, int left, int top, int right, int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {
                Log.i(TAG,"addOnLayoutChangeListener");
                controlManageShow(mRecyclerView);
            }
        });
    }

    public void controlManageShow(RecyclerView recyclerView){
        post(new Runnable() {
            @Override
            public void run() {
                int y=mRecyclerView.computeVerticalScrollExtent() + mRecyclerView.computeVerticalScrollOffset();
                Log.i("scroll","extent+offset="+y+",range:"+mRecyclerView.computeVerticalScrollRange()+
                        "extent:"+mRecyclerView.computeVerticalScrollExtent()+"offset:"+mRecyclerView.computeVerticalScrollOffset());
                if(!canBottomShow(recyclerView)){
                    mListAdapterImpl.showViewItem(VIEW_ITEM_MANAGE);
                    if(mMangeImageView!=null) {
                        mMangeImageView.setVisibility(View.GONE);
                    }
                }else {
                    mListAdapterImpl.removeViewItem(VIEW_ITEM_MANAGE);
                    if(mMangeImageView!=null) {
                        mMangeImageView.setVisibility(View.VISIBLE);
                    }
                }
            }
        });
    }

    public boolean canBottomShow(RecyclerView recyclerView) {
        if (recyclerView == null) return false;
        if (recyclerView.computeVerticalScrollExtent()
                < recyclerView.computeVerticalScrollRange()||recyclerView.computeVerticalScrollOffset()>0) {
            return false;
        }
        return true;
    }

//    public boolean isFullScreenViewVisible() {
//        if (mFullScreenViewHolder != null && VISIBLE == mFullScreenViewHolder.getVisibility()) {
//            return true;
//        } else {
//            return false;
//        }
//    }
//
//    public void dismissFullScreenView(boolean showAnimation) {
//        if (mFullScreenViewHolder != null && VISIBLE == mFullScreenViewHolder.getVisibility()) {
//            if (showAnimation) {
//                new AnimationScaleOut(mFullScreenViewHolder)
//                        .setDuration(Animation.DURATION_LONG)
//                        .setPivotXY(mPivotX, mPivotY).animate();
//            } else {
//                mFullScreenViewHolder.setVisibility(GONE);
//            }
//        }
//    }

//    private int currentNewTabIndex;
//    private int lastIndex;
//    public boolean isLastNewsTab() {
//        return currentNewTabIndex == lastIndex;
//    }
//    private int mPivotX, mPivotY;

//    public void showMoreNews(View v){
//        mFullScreenViewHolder.removeAllViews();
//        View fullscreenNews = LayoutInflater.from(getContext()).inflate(R.layout.newssdk_news_tab, mFullScreenViewHolder, true);
//        NewsTabView newsTabView = (NewsTabView) fullscreenNews.findViewById(R.id.view_news);
//        List<ONewsScenario> scenarios = NewsSdk.INSTAMCE.getONewsScenarios();
//        lastIndex = scenarios != null ? scenarios.size() - 1 : 0;
//        currentNewTabIndex = lastIndex;
//        newsTabView.initNewsTabView(false, 3 * 60, lastIndex);
//        newsTabView.setOnNewsTabChangedListener(new NewsTabView.OnNewsTabChangedListener() {
//            @Override
//            public void onPageChanged(int current, int total) {
//                currentNewTabIndex = current;
//            }
//        });
////                mFullScreenViewHolder.setVisibility(VISIBLE);
//        int[] location = new int[2];
//        v.getLocationOnScreen(location);
//        mPivotX = (int) LauncherApplication.getScreenWidthPixel() / 2;
//        mPivotY = location[1];
//        new AnimationScaleIn(mFullScreenViewHolder)
//                .setDuration(Animation.DURATION_LONG)
//                .setPivotXY(mPivotX, mPivotY).animate();
//    }

    public void moveToPosition(int n) {
        if(mLinearLayoutManager == null || mRecyclerView == null){
            return;
        }

        int firstItem = mLinearLayoutManager.findFirstVisibleItemPosition();
        int lastItem = mLinearLayoutManager.findLastVisibleItemPosition();
        if (n <= firstItem) {
            mRecyclerView.scrollToPosition(n);
        } else if (n <= lastItem) {
            int top = mRecyclerView.getChildAt(n - firstItem).getTop();
            mRecyclerView.scrollBy(0, top);
        } else {
            mRecyclerView.scrollToPosition(n);
        }

    }

    public void initData() {
        List<ItemData> data = getItemData();
        mListAdapterImpl.setData(data);
    }

    public void removeData(ArrayList<AppInfo> appInfos) {
        List<ItemData> dataSet = new ArrayList<>();
        RecommendAppsData recommendAppsData = new RecommendAppsData();
        recommendAppsData.mList = appInfos;
        dataSet.add(recommendAppsData);
        mListAdapterImpl.removeData(dataSet);
    }

    public void updateData(ArrayList<AppInfo> appInfos) {
        List<ItemData> dataSet = new ArrayList<>();
        RecommendAppsData recommendAppsData = new RecommendAppsData();
        recommendAppsData.mList = appInfos;
        dataSet.add(recommendAppsData);
        mListAdapterImpl.updateData(dataSet);
    }

/*    private List<ItemData> getItemData() {
        List<ItemData> dataSet = new ArrayList<>();
        for (int i = 0; i < ITEM_NUMBER; i++) {
            switch (i){
                case 0: //quick search
                    ItemData quickSearhData = new QuicksearhData();
                    dataSet.add(quickSearhData);
                    break;
                case 1: //recommend apps
                    ItemData recommendAppsData = new RecommendAppsData();
                    dataSet.add(recommendAppsData);
                    break;
                case 3: //news
                    ItemData newsData = new NewsData();
                    dataSet.add(newsData);
                    break;
                case 4: //reader
                    ItemData readerData = new ReaderData();
                    dataSet.add(readerData);
                    break;
                case 2: //Intelcards apps
                    ItemData IntelcardsData = new IntelcardsData();
                    dataSet.add(IntelcardsData);
                    break;
            }
        }
        return dataSet;
    }*/

    private List<ItemData> getItemData() {
        List<ItemData> dataSet = new ArrayList<>();
        for (int i = 0; i < mListAdapterImpl.mViewType.size(); i++) {
            switch (mListAdapterImpl.mViewType.get(i)){
                case VIEW_ITEM_QUICKSEARCH: //quick search
                    ItemData quickSearhData = new QuicksearhData();
                    dataSet.add(quickSearhData);
                    break;
                case VIEW_ITEM_RECOMMENDAPPS: //recommend apps
                    ItemData recommendAppsData = new RecommendAppsData();
                    dataSet.add(recommendAppsData);
                    break;
                case VIEW_ITEM_NEWS: //news
                    ItemData newsData = new NewsData();
                    dataSet.add(newsData);
                    break;
                case VIEW_ITEM_READER: //reader
                    ItemData readerData = new ReaderData();
                    dataSet.add(readerData);
                    break;
/*                case VIEW_ITEM_INTELCARDS_WIDGET: //Intelcards apps
                    ItemData IntelcardsData = new IntelcardsData();
                    dataSet.add(IntelcardsData);
                    break;*/
            }
        }
        return dataSet;
    }

    @Override
    public void handleViewClick(View view) {
        if (view.getTag() == null || !(view.getTag() instanceof ViewItemClickTag)) return;
        ViewItemClickTag itemTag = (ViewItemClickTag) view.getTag();

        if (ViewItemClickTag.TAG_REMOVE_ITEM.equals(itemTag.mTag)) {
            /*if (ViewItemType.VIEW_ITEM_MUSIC_WIDGET == itemTag.mViewItemType) {
                mListAdapterImpl.removeViewItem(ViewItemType.VIEW_ITEM_MUSIC_WIDGET);
            }*/
        } /*else if (ViewItemClickTag.TAG_NEWS.equals(itemTag.mTag)){
            showMoreNews(view);
        }*/
    }

    @Override
    public void addObserver(ObserverTouchEvent sdkNewsBinder) {
    }


    public boolean isSPShow(int viewItemType){
        SharedPreferences sp=PreferenceManager.getDefaultSharedPreferences(getContext());
        switch (viewItemType){
            case VIEW_ITEM_RECOMMENDAPPS:
                return sp.getBoolean(RECAPP_SP, true);
            case VIEW_ITEM_INTELCARDS_WIDGET:
                return sp.getBoolean(INTELCARDS_SP, true);
            case VIEW_ITEM_NEWS:
                return sp.getBoolean(NEWS_SP, true);
            case VIEW_ITEM_READER:
                return sp.getBoolean(READERS_SP, true);
        }
        return true;
    }

    public void showViewItem(int viewItemType) {
        if(isSPShow(viewItemType)) {
            mListAdapterImpl.showViewItem(viewItemType);
        }else {
            mListAdapterImpl.removeViewItem(viewItemType);
        }
    }

    public void removeViewItem(int viewItemType) {
        Log.i(TAG,"removeViewItem,viewItemType:"+viewItemType);
        mListAdapterImpl.removeViewItem(viewItemType);
    }

    public void updateNewsLayout() {
//         mRecyclerView.requestLayout();
//        mLinearLayoutManager.onItemsUpdated().requestLayout();
        mRecyclerView = null;
        mRecyclerView = (RecyclerView) findViewById(R.id.recycler_view);
        mListAdapterImpl = null;
        mListAdapterImpl = new ListAdapterImpl(getContext(),this);
        mRecyclerView.setAdapter(mListAdapterImpl);
        mLinearLayoutManager = new LinearLayoutManager(getContext());
        mLinearLayoutManager.setAutoMeasureEnabled(true);
        mRecyclerView.setLayoutManager(mLinearLayoutManager);
//        mRecyclerView.addItemDecoration(new DividerItemDecoration(getContext(), DividerItemDecoration.VERTICAL_LIST));
    }

    public void onDestroy() {
        mListAdapterImpl.onDestroy();
    }

//    @Override
//    public void addObserver(ObserverTouchEvent sdkNewsBinder) {
//        mSdkNewsBinder = sdkNewsBinder;
//    }
//
//    public boolean interceptTouchEvent(MotionEvent ev) {
//        return mSdkNewsBinder.interceptTouchEvent(ev);
//    }
}
