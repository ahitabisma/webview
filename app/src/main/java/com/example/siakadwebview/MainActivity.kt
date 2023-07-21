package com.example.siakadwebview

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.webkit.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.onesignal.OneSignal


// OneSignal App ID
const val ONESIGNAL_APP_ID = "0ee2b640-33f0-4849-91ed-f06e3fee211b"

class MainActivity : AppCompatActivity(), SwipeRefreshLayout.OnRefreshListener {
    // Variable Webview
    private lateinit var webView : WebView
    private lateinit var refreshLayout: SwipeRefreshLayout
    private val url = "http://app.softwaresekolah.my.id/"

    private var fileUploadCallback: ValueCallback<Array<Uri>>? = null
    companion object {
        private const val FILE_CHOOSER_RESULT_CODE = 1
        private const val CAMERA_PERMISSION_REQUEST_CODE = 2
    }

    private val LOCATION_PERMISSION_REQUEST_CODE = 1


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Variable
        webView = findViewById(R.id.SIAKAD)
        webView.webViewClient = MyWebViewClient()
        webView.loadUrl(url)
        val javascriptInterface = MyJavascriptInterface(this)

        // Add JavaScriptInterface to WebView
        webView.addJavascriptInterface(javascriptInterface, "AndroidInterface")

        // Declare Variable WebSetting
        val webSettings = webView.settings

        // Enable Javascript, CSS
        webSettings.javaScriptEnabled = true
        webSettings.domStorageEnabled = true

        // Enable Camera, Location, Import File
        webSettings.allowFileAccess = true
        webSettings.allowContentAccess = true
        webSettings.allowFileAccessFromFileURLs = true
        webSettings.allowUniversalAccessFromFileURLs = true
        webSettings.setGeolocationEnabled(true)

        webView.webChromeClient = object : WebChromeClient() {
            // For Android 4.1+
            fun openFileChooser(uploadMsg: ValueCallback<Array<Uri>>, acceptType: String) {
                fileUploadCallback = uploadMsg
                requestCameraPermission()
            }

            // For Android 5.0+
            override fun onShowFileChooser(
                webView: WebView,
                filePathCallback: ValueCallback<Array<Uri>>,
                fileChooserParams: FileChooserParams
            ): Boolean {
                fileUploadCallback = filePathCallback
                requestCameraPermission()
                return true
            }

            val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager

            override fun onGeolocationPermissionsShowPrompt(
                origin: String?,
                callback: GeolocationPermissions.Callback
            ) {
                if (ContextCompat.checkSelfPermission(
                        this@MainActivity,
                        Manifest.permission.ACCESS_FINE_LOCATION
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    ActivityCompat.requestPermissions(
                        this@MainActivity,
                        arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                        LOCATION_PERMISSION_REQUEST_CODE
                    )
                } else {
                    val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
                    val location: Location? = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                    if (location != null) {
                        val latitude: String = java.lang.String.valueOf(location.latitude)
                        val longitude: String = java.lang.String.valueOf(location.longitude)
                        val url = "http://app.softwaresekolah.my.id?lat=$latitude&long=$longitude"
                        webView.loadUrl(url)
                        callback.invoke(origin, true, true)
                    } else {
                        callback.invoke(origin, false, false)
                    }
                }
            }


            override fun onPermissionRequest(request: PermissionRequest) {
                if (request.origin.toString().startsWith("http://")) {
                    request.grant(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION))
                } else {
                    super.onPermissionRequest(request)
                }
            }
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // Request location permission if not granted
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), LOCATION_PERMISSION_REQUEST_CODE)
        } else {
            // Load the web page if location permission is granted
            webView.loadUrl(url)
        }



        // Disable Zoom, Copy Paste
        webSettings.setSupportZoom(false)
        webSettings.builtInZoomControls = false
        webView.setOnLongClickListener { true }

        // Swipe Refresh
        refreshLayout = findViewById(R.id.swipefresh)
        refreshLayout.setOnRefreshListener(this)

        // OneSignal
        OneSignal.setLogLevel(OneSignal.LOG_LEVEL.VERBOSE, OneSignal.LOG_LEVEL.NONE)
        OneSignal.initWithContext(this)
        OneSignal.setAppId(ONESIGNAL_APP_ID)
        OneSignal.promptForPushNotifications()
    }

    override fun onBackPressed() {
        if(webView.canGoBack()){
            webView.goBack()
        }else{
            super.onBackPressed()
        }
    }

    // Swipe Refresh
    override fun onRefresh() {
        webView.reload()
        refreshLayout.isRefreshing = false
    }

    // Pull to Refresh
    private inner class MyWebViewClient : WebViewClient(){
        override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                return super.shouldOverrideUrlLoading(view, request)
        }
    }

    // Handle camera permission request
    private fun requestCameraPermission() {
        if (checkSelfPermission(android.Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            // Permission is already granted, show the camera dialog
            showCameraDialog()
        } else {
            // Permission has not been granted, request it
            requestPermissions(
                arrayOf(android.Manifest.permission.CAMERA),
                CAMERA_PERMISSION_REQUEST_CODE
            )
        }
    }

    // Show the camera dialog
    private fun showCameraDialog() {
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        if (intent.resolveActivity(packageManager) != null) {
            startActivityForResult(intent, FILE_CHOOSER_RESULT_CODE)
        }
    }

    // Handle the result of camera capture
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == FILE_CHOOSER_RESULT_CODE) {
            if (fileUploadCallback != null) {
                val results = WebChromeClient.FileChooserParams.parseResult(resultCode, data)
                fileUploadCallback?.onReceiveValue(results)
                fileUploadCallback = null
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

    // Handle the result of permission request
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String?>,
        grantResults: IntArray
    ) {
        when (requestCode) {
            CAMERA_PERMISSION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // Camera permission granted, show the camera dialog
                    showCameraDialog()
                } else {
                    // Camera permission denied, handle accordingly (e.g., show a message)
                }
            }
            LOCATION_PERMISSION_REQUEST_CODE -> {
                if (grantResults.size > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    webView.loadUrl(url)
                }
            }
            else -> {
                super.onRequestPermissionsResult(requestCode, permissions, grantResults)
            }
        }
    }
}