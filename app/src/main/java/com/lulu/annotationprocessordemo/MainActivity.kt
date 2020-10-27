package com.lulu.annotationprocessordemo

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import com.lulu.lib_annotations.ByteService
import com.util.service.ServiceManager

private const val TAG = "MainActivity"
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        window.decorView.setOnClickListener {
            ServiceManager.getService(IService2::class.java).doFun2()
        }
    }
}