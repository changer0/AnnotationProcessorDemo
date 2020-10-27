package com.lulu.annotationprocessordemo;

import android.util.Log;

import com.lulu.lib_annotations.ByteService;

/**
 * @author zhanglulu on 2020/10/27.
 * for
 */

@ByteService
public class ServiceImpl1 implements IService1 {
    private static final String TAG="ServiceImpl1";

    @Override
    public void doFun() {
        Log.d(TAG, "doFun: 执行");
    }
}