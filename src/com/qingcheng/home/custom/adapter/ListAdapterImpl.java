package com.qingcheng.home.custom.adapter;


import android.content.Context;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.util.Log;

import com.qingcheng.home.RGKWidgetsRecyclerView;
import com.qingcheng.home.custom.DataBinder;
import com.qingcheng.home.custom.IntelcardsData;
import com.qingcheng.home.custom.ItemData;
import com.qingcheng.home.custom.ListBindAdapter;
import com.qingcheng.home.custom.NewsData;
import com.qingcheng.home.custom.QuicksearhData;
import com.qingcheng.home.custom.ReaderData;
import com.qingcheng.home.custom.RecommendAppsData;
import com.qingcheng.home.custom.ViewClick;
import com.qingcheng.home.custom.ViewItemType;
import com.qingcheng.home.custom.binder.IntelcardsBinder;
import com.qingcheng.home.custom.binder.ManageBinder;
import com.qingcheng.home.custom.binder.NewsBinder;
import com.qingcheng.home.custom.binder.QuickSearchBinder;
import com.qingcheng.home.custom.binder.ReaderBinder;
import com.qingcheng.home.custom.binder.RecommendAppsBinder;

import java.util.ArrayList;
import java.util.List;

import static com.qingcheng.home.custom.ViewItemType.VIEW_ITEM_COUNTS;


public class ListAdapterImpl extends ListBindAdapter {
    private static final String TAG="ListAdapterImpl";

    private ViewClick mViewClick;

    public ArrayList<Integer> mViewType = new ArrayList<>();
    private Context mContext;

    public ListAdapterImpl(Context context, ViewClick viewClick) {
        mViewClick = viewClick;
        mViewType.add(ViewItemType.VIEW_ITEM_QUICKSEARCH);
        mViewType.add(ViewItemType.VIEW_ITEM_RECOMMENDAPPS);
        mViewType.add(ViewItemType.VIEW_ITEM_INTELCARDS_WIDGET);
        mViewType.add(ViewItemType.VIEW_ITEM_NEWS);
        mViewType.add(ViewItemType.VIEW_ITEM_READER);
        mViewType.add(ViewItemType.VIEW_ITEM_MANAGE);

        mContext=context;
        addAllBinder(new QuickSearchBinder(this),
                new RecommendAppsBinder(this),
//                new MusicWidgetBinder(this, mViewClick),
                new IntelcardsBinder(context, this),
                new NewsBinder(this),
                new ReaderBinder(this),
                new ManageBinder(context,this));
    }

    public void setViewClick(ViewClick viewClick) {
        mViewClick = viewClick;
    }

    public int getViewType(int viewItem) {
        if (ViewItemType.VIEW_ITEM_DEFAULT == viewItem || mViewType.isEmpty() || !mViewType.contains(viewItem)) {
            return -1;
        } else {
            return mViewType.get(mViewType.indexOf(viewItem));
        }
    }

    public void showViewItem(int viewItemType) {
        if (!mViewType.contains(viewItemType)) {
            int position = -1;
            int nextItemType = viewItemType + 1;
            for (int i = 0; i < VIEW_ITEM_COUNTS; i++) {
                if (mViewType.contains(nextItemType)) {
                    position = mViewType.indexOf(nextItemType);
                    break;
                } else {
                    nextItemType++;
                }
            }
            if (position >= 0) {
                mViewType.add(position, viewItemType);
            } else {
                mViewType.add(viewItemType);
            }
            if(viewItemType == ViewItemType.VIEW_ITEM_RECOMMENDAPPS){
                //addBinder(ViewItemType.VIEW_ITEM_RECOMMENDAPPS,new RecommendAppsBinder(this));
                addBinder(position,new RecommendAppsBinder(this));
                notifyItemChanged(position);
                //notifyDataSetChanged();
            }
            if(viewItemType == ViewItemType.VIEW_ITEM_INTELCARDS_WIDGET){
                addBinder(position,new IntelcardsBinder(mContext,this,true));
                notifyItemChanged(position);
                //notifyDataSetChanged();
            }
            if(viewItemType == ViewItemType.VIEW_ITEM_NEWS){
                addBinder(position,new NewsBinder(this));
                notifyItemChanged(position);
                //notifyDataSetChanged();
            }
            if(viewItemType == ViewItemType.VIEW_ITEM_READER){
                addBinder(position,new ReaderBinder(this));
                notifyItemChanged(position);
                //notifyDataSetChanged();
            }
            if(viewItemType == ViewItemType.VIEW_ITEM_MANAGE){
                addBinder(position,new ManageBinder(mContext,this));
                notifyItemChanged(position);
                //notifyDataSetChanged();
            }
        }
    }

    public void removeViewItem(int viewItemType) {
        if (mViewType.contains(viewItemType)) {
            DataBinder dataBinder = getDataBinder(getViewType(viewItemType));
            mViewType.remove(mViewType.indexOf(viewItemType));
//            for(int x:mViewType){
//                Log.e(TAG,"x:"+x);
//            }
            removeBinder(dataBinder);
        }
    }

    public void setData(List<ItemData> dataSet) {
        for (ItemData item: dataSet) {
            if(item instanceof QuicksearhData){
                getDataBinder(mViewType.indexOf(ViewItemType.VIEW_ITEM_QUICKSEARCH)).addData(item);
            } else if(item instanceof RecommendAppsData){
                getDataBinder(mViewType.indexOf(ViewItemType.VIEW_ITEM_RECOMMENDAPPS)).addData(item);
            } else if(item instanceof NewsData){
                getDataBinder(mViewType.indexOf(ViewItemType.VIEW_ITEM_NEWS)).addData(item);
            }else if(item instanceof ReaderData){
                getDataBinder(mViewType.indexOf(ViewItemType.VIEW_ITEM_READER)).addData(item);
            }/*else if(item instanceof IntelcardsData){
                getDataBinder(mViewType.indexOf(ViewItemType.VIEW_ITEM_INTELCARDS_WIDGET)).addData(item);
            }*/
        }
    }

    public void updateData(List<ItemData> dataSet) {
        for (ItemData item: dataSet) {
            if(item instanceof QuicksearhData){
                getDataBinder(mViewType.indexOf(ViewItemType.VIEW_ITEM_QUICKSEARCH)).updateData(item);
            } else if(item instanceof RecommendAppsData){
                getDataBinder(mViewType.indexOf(ViewItemType.VIEW_ITEM_RECOMMENDAPPS)).updateData(item);
            } else if(item instanceof NewsData){
                getDataBinder(mViewType.indexOf(ViewItemType.VIEW_ITEM_NEWS)).updateData(item);
            }else if(item instanceof ReaderData){
                getDataBinder(mViewType.indexOf(ViewItemType.VIEW_ITEM_READER)).updateData(item);
            }/*else if(item instanceof IntelcardsData){
                getDataBinder(mViewType.indexOf(ViewItemType.VIEW_ITEM_INTELCARDS_WIDGET)).updateData(item);
            }*/
        }
    }

    public void removeData(List<ItemData> dataSet) {
        for (ItemData item: dataSet) {
            if(item instanceof QuicksearhData){
                getDataBinder(mViewType.indexOf(ViewItemType.VIEW_ITEM_QUICKSEARCH)).removeData(item);
            } else if(item instanceof RecommendAppsData){
                getDataBinder(mViewType.indexOf(ViewItemType.VIEW_ITEM_RECOMMENDAPPS)).removeData(item);
            } else if(item instanceof NewsData){
                getDataBinder(mViewType.indexOf(ViewItemType.VIEW_ITEM_NEWS)).removeData(item);
            }else if(item instanceof ReaderData){
                getDataBinder(mViewType.indexOf(ViewItemType.VIEW_ITEM_READER)).removeData(item);
            }/*else if(item instanceof IntelcardsData){
                getDataBinder(mViewType.indexOf(ViewItemType.VIEW_ITEM_INTELCARDS_WIDGET)).removeData(item);
            }*/
        }
    }

    public void onDestroy() {
        DataBinder dataBinder;
        for(int viewType:mViewType){
            dataBinder=getDataBinder(mViewType.indexOf(viewType));
            if(dataBinder!=null){
                dataBinder.destroy();
            }
        }
    }
}
