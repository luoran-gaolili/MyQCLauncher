package com.qingcheng.home.custom.adapter;


import com.qingcheng.home.custom.EnumListBindAdapter;
import com.qingcheng.home.custom.binder.NewsBinder;
import com.qingcheng.home.custom.binder.QuickSearchBinder;
import com.qingcheng.home.custom.binder.RecommendAppsBinder;

public class EnumListAdapterImpl
        extends EnumListBindAdapter<EnumListAdapterImpl.ViewType> {

    enum ViewType {
        QUICKSEARCH, RECOMMENDAPPS, NEWS
    }

    public EnumListAdapterImpl() {
        addAllBinder(new NewsBinder(this),
                new RecommendAppsBinder(this),
                new QuickSearchBinder(this));
    }

}
