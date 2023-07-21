package com.example.siakadwebview

import android.content.Context
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import com.onesignal.OneSignal

class MyJavascriptInterface(private val context: Context) : WebChromeClient() {

    @JavascriptInterface
    fun getDeviceToken(): String? {
        val deviceState = OneSignal.getDeviceState()
        val deviceToken = deviceState?.userId
        return deviceToken
    }

    @JavascriptInterface
    fun sendtag(key:String, value:String) {
        OneSignal.sendTag(key,value);
    }
}

