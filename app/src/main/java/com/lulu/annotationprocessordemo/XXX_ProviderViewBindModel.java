package com.lulu.annotationprocessordemo;

import java.util.HashMap;
import java.util.Map;

public class XXX_ProviderViewBindModel {
    private BaseViewBindItem viewBindItem;
    private Map<Integer, IModel> viewModelMap = new HashMap<>();

    public XXX_ProviderViewBindModel(BaseViewBindItem viewBindItem) {
        this.viewBindItem = viewBindItem;
        //这里面生成
        viewModelMap.put(2341, viewBindItem.model);
    }

    public Map<Integer, IModel> getViewModelMap() {
        return viewModelMap;
    }
}
