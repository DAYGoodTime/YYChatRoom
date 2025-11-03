package com.yychat.common.model;

import java.util.ArrayList;
import java.util.List;

public class Page<T> {
    private long total;
    private int index;
    private int pageSize;
    private List<T> list;

    public Page() {
        this.total = 0;
        this.index = 0;
        this.pageSize = 10;
        this.list = new ArrayList<>();
    }
    public Page(Page<?> anyParmaPage,List<T> list) {
        this.total = anyParmaPage.getTotal();
        this.index = anyParmaPage.getIndex();
        this.pageSize = anyParmaPage.getPageSize();
        this.list = list;
    }

    public Page(int pageSize, int index) {
        this.pageSize = pageSize;
        this.index = index;
    }

    public long getTotal() {
        return total;
    }

    public Page<T> setTotal(long total) {
        this.total = total;
        return this;
    }

    public int getIndex() {
        return index;
    }

    public Page<T> setIndex(int index) {
        this.index = index;
        return this;
    }

    public int getPageSize() {
        return pageSize;
    }

    public Page<T> setPageSize(int pageSize) {
        this.pageSize = pageSize;
        return this;
    }

    public List<T> getList() {
        return list;
    }

    public Page<T> setList(List<T> list) {
        this.list = list;
        return this;
    }
}
