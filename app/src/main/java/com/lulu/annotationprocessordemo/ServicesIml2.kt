package com.lulu.annotationprocessordemo

import android.util.Log
import com.lulu.lib_annotations.ByteService

/**
 * @author zhanglulu on 2020/10/27.
 * for
 */
private const val TAG = "Services3"

@ByteService
class ServicesIml2 : IService2 {

    override fun doFun2() {
        Log.d(TAG, "doFun: 支持 Kotlin 不")
    }
}