package com.qingcheng.home.custom;

public abstract class EnumListBindAdapter<E extends Enum<E>> extends ListBindAdapter {

    public <T extends DataBinder> T getDataBinder(E e) {
        return getDataBinder(e.ordinal());
    }
}
