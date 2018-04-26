package com.qingcheng.home.custom;

import android.util.Log;

import com.qingcheng.home.custom.binder.IntelcardsBinder;
import com.qingcheng.home.custom.binder.ManageBinder;
import com.qingcheng.home.custom.binder.NewsBinder;
import com.qingcheng.home.custom.binder.QuickSearchBinder;
import com.qingcheng.home.custom.binder.ReaderBinder;
import com.qingcheng.home.custom.binder.RecommendAppsBinder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Adapter class for managing data binders when the order of binder is in sequence
 */
public class ListBindAdapter extends DataBindAdapter {

    private List<DataBinder> mBinderList = new ArrayList<>();
    private static final String TAG="ListBindAdapter";

    @Override
    public int getItemCount() {
        int itemCount = 0;
        for (int i = 0, size = mBinderList.size(); i < size; i++) {
            DataBinder binder = mBinderList.get(i);
            itemCount += binder.getItemCount();
        }
        return itemCount;
    }

    @Override
    public int getItemViewType(int position) {
        /*Log.e("ListBindAdapter","getItemViewType position:"+position);
        int itemCount = 0;
        for (int viewType = 0, size = mBinderList.size(); viewType < size; viewType++) {
            itemCount += mBinderList.get(viewType).getItemCount();
            if (position < itemCount) {
                Log.e("ListBindAdapter","viewType:"+viewType);
                return viewType;
            }
        }
        throw new IllegalArgumentException("arg position is invalid");*/
        return mBinderList.get(position).getItemViewType();
    }

    @Override
    public <T extends DataBinder> T getDataBinder(int viewIndex) {
        /*Log.e("ListBindAdapter","viewIndex:"+viewIndex);
        return (T) mBinderList.get(viewIndex);*/
        if(viewIndex==-1){
            return null;
        }
        switch (viewIndex){
            case ViewItemType.VIEW_ITEM_QUICKSEARCH:{
                for(DataBinder dataBinder:mBinderList){
                    if(dataBinder instanceof QuickSearchBinder) {
                        return (T) dataBinder;
                    }
                }
                break;
            }
            case ViewItemType.VIEW_ITEM_RECOMMENDAPPS:{
                for(DataBinder dataBinder:mBinderList){
                    if(dataBinder instanceof RecommendAppsBinder) {
                        return (T) dataBinder;
                    }
                }
                break;
            }
            case ViewItemType.VIEW_ITEM_INTELCARDS_WIDGET:{
                for(DataBinder dataBinder:mBinderList){
                    if(dataBinder instanceof IntelcardsBinder) {
                        return (T) dataBinder;
                    }
                }
                break;
            }
            case ViewItemType.VIEW_ITEM_NEWS:{
                for(DataBinder dataBinder:mBinderList){
                    if(dataBinder instanceof NewsBinder) {
                        return (T) dataBinder;
                    }
                }
                break;
            }
            case ViewItemType.VIEW_ITEM_READER:{
                for(DataBinder dataBinder:mBinderList){
                    if(dataBinder instanceof ReaderBinder) {
                        return (T) dataBinder;
                    }
                }
                break;
            }
            case ViewItemType.VIEW_ITEM_MANAGE:{
                for(DataBinder dataBinder:mBinderList){
                    if(dataBinder instanceof ManageBinder) {
                        return (T) dataBinder;
                    }
                }
                break;
            }
        }
        if(viewIndex>=mBinderList.size()){
            Log.e(TAG,"getDataBinder return null viewIndex:"+viewIndex+",size:"+mBinderList.size());
            return null;
        }
        return (T) mBinderList.get(viewIndex);
    }

    public <T extends DataBinder> T getDataBinder(DataBinder dataBinder) {
        if (mBinderList.contains(dataBinder)) {
            return (T) mBinderList.get(mBinderList.indexOf(dataBinder));
        }
        return null;
    }

    @Override
    public int getPosition(DataBinder binder, int binderPosition) {
        int viewType = mBinderList.indexOf(binder);
        if (viewType < 0) {
            throw new IllegalStateException("binder does not exist in adapter");
        }

        int position = binderPosition;
        for (int i = 0; i < viewType; i++) {
            position += mBinderList.get(i).getItemCount();
        }

        return position;
    }

    @Override
    public int getBinderPosition(int position) {
        int binderItemCount;
        for (int i = 0, size = mBinderList.size(); i < size; i++) {
            binderItemCount = mBinderList.get(i).getItemCount();
            if (position - binderItemCount < 0) {
                break;
            }
            position -= binderItemCount;
        }
        return position;
    }

    @Override
    public void notifyBinderItemRangeChanged(DataBinder binder, int positionStart, int itemCount) {
        notifyItemRangeChanged(getPosition(binder, positionStart), itemCount);
    }

    @Override
    public void notifyBinderItemRangeInserted(DataBinder binder, int positionStart, int itemCount) {
        notifyItemRangeInserted(getPosition(binder, positionStart), itemCount);
    }

    @Override
    public void notifyBinderItemRangeRemoved(DataBinder binder, int positionStart, int itemCount) {
        notifyItemRangeRemoved(getPosition(binder, positionStart), itemCount);
    }

    public List<DataBinder> getBinderList() {
        return mBinderList;
    }

    public void addAllBinder(List<DataBinder> dataSet) {
        mBinderList.addAll(dataSet);
    }

    public void addAllBinder(DataBinder... dataSet) {
        mBinderList.addAll(Arrays.asList(dataSet));
    }

    public void addBinder(DataBinder binder) {
        addBinder(-1, binder);
    }

    public void addBinder(int position, DataBinder binder) {
        if (!mBinderList.contains(binder)) {
            if (position >= 0) {
                mBinderList.add(position, binder);
            } else {
                mBinderList.add(binder);
                position = mBinderList.indexOf(binder);
            }
            notifyItemInserted(position);
        }
    }

    public void removeBinder(DataBinder binder) {
        if (mBinderList.contains(binder)) {
            int position = mBinderList.indexOf(binder);
            mBinderList.remove(position);
            notifyItemRemoved(position);
        }
    }
}
