package com.example.siakadwebview

import android.os.Bundle
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity

class DashboardActivity : AppCompatActivity() {
    private lateinit var webView: WebView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        webView = findViewById(R.id.SIAKAD)
        webView.webViewClient = WebViewClient()
        webView.loadUrl("http://softwaresekolah.my.id/view/Dashboard/no-payment.html")

        val javascriptInterface = MyJavascriptInterface(this)

        // Add JavaScriptInterface to WebView
        webView.addJavascriptInterface(javascriptInterface, "AndroidInterface")


    }
}